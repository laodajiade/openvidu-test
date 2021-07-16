package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.core.*;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author geedow
 * @date 2019/11/5 17:15
 */
@Slf4j
@Service
public class LeaveRoomHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.LEAVEROOM_ROOM_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.LEAVEROOM_SOURCE_ID_PARAM);
        if (StringUtils.isEmpty(sessionId) || (StringUtils.isEmpty(sourceId) && UserType.register.equals(rpcConnection.getUserType()))) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }
        UseTime.point("p1");
        Session session = sessionManager.getSession(sessionId);
        if (Objects.isNull(session)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }
        Participant participant;
        try {
            Optional<Participant> participantOptional = session.getParticipantByUUID(rpcConnection.getUserUuid());
            if (!participantOptional.isPresent() || !participantOptional.get().getParticipantPrivateId().equals(rpcConnection.getParticipantPrivateId()) ) {
                log.info("when participants are disconnected and reconnected, they can leave the meeting without joining.");
                Map partInfo = cacheManage.getPartInfo(rpcConnection.getUserUuid());
                if (!partInfo.isEmpty()) {
                    sessionManager.evictParticipantByUUID(sessionId, rpcConnection.getUserUuid(),
                            Arrays.asList(EvictParticipantStrategy.CLOSE_WEBSOCKET_CONNECTION,
                                    EvictParticipantStrategy.CLOSE_ROOM_WHEN_EVICT_MODERATOR));
                }

                notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
                return;
            }
            participant = participantOptional.get();
        } catch (OpenViduException e) {
            log.info("close previous participant info exception", e);
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.SERVER_INTERNAL_ERROR);
            return;
        }
        UseTime.point("p2");

        if (session.isClosing()) {
            log.info("call closeRoom method after again participant:{} call leaveRoom method roomId:{}", participant.getUuid() + participant.getParticipantPrivateId(), sessionId);
            notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
            return;
        }

        String moderatePublicId = null;
        String speakerId = null;
        Set<Participant> participants = sessionManager.getParticipants(sessionId);

        // 如果是举手发言状态，则需要广播结束发言
        if (Objects.equals(ParticipantHandStatus.speaker, participant.getHandStatus())) {
            participant.changeHandStatus(ParticipantHandStatus.endSpeaker);

            JsonObject params = new JsonObject();
            params.addProperty(ProtocolElements.END_ROLL_CALL_ROOM_ID_PARAM, sessionId);
            params.addProperty(ProtocolElements.END_ROLL_CALL_TARGET_ID_PARAM, sourceId);

            for (Participant participant1 : participants) {
                if (!Objects.equals(StreamType.MAJOR, participant1.getStreamType())) continue;
                if (participant1.getRole().equals(OpenViduRole.MODERATOR))
                    moderatePublicId = participant1.getParticipantPublicId();
                if (Objects.equals(ParticipantHandStatus.speaker, participant1.getHandStatus()))
                    speakerId = participant1.getParticipantPublicId();
                this.notificationService.sendNotification(participant1.getParticipantPrivateId(),
                        ProtocolElements.END_ROLL_CALL_METHOD, params);
            }
        }

        if (Objects.equals(session.getConferenceMode(), ConferenceModeEnum.MCU)
                && participant.getStreamType().isStreamTypeMixInclude()) {
            sessionManager.setLayoutAndNotifyWhenLeaveRoom(sessionId, participant,
                    !Objects.equals(speakerId, participant.getParticipantPublicId()) ? speakerId : moderatePublicId);
        }
        UseTime.point("p3");
        sessionManager.leaveRoom(participant, request.getId(), EndReason.disconnect, false);
        UseTime.point("p4");
        //判断轮询是否开启
        SessionPreset preset = session.getPresetInfo();
        if (SessionPresetEnum.on.equals(preset.getPollingStatusInRoom()) && StreamType.MAJOR.equals(participant.getStreamType()) && !OpenViduRole.MODERATOR.equals(participant.getRole())) {
            Set<Participant> parts = session.getPartsExcludeModeratorAndSpeaker();
            if (CollectionUtils.isEmpty(parts)) {
                //close room stopPolling
                preset.setPollingStatusInRoom(SessionPresetEnum.off);
                timerManager.stopPollingCompensation(sessionId);
                //send notify
                JsonObject params = new JsonObject();
                params.addProperty("roomId", sessionId);
                notificationService.sendBatchNotification(session.getParticipants(), ProtocolElements.STOP_POLLING_NODIFY_METHOD, params);
            } else {
                //获取当前轮询信息
                Map<String, Integer> map = timerManager.getPollingCompensationScheduler(sessionId);
                int pollingOrder = map.get("order");
                int index = map.get("index");
                if (participant.getOrder() == pollingOrder) {
                    if (participant.getOrder() > openviduConfig.getSfuPublisherSizeLimit() - 1) {
                        index = 0;
                    }
                    timerManager.leaveRoomStartPollingAgainCompensation(sessionId, preset.getPollingIntervalTime(), index == 0 ? index : index - 1);
                }
            }
        } else if (SessionPresetEnum.on.equals(preset.getPollingStatusInRoom()) && StreamType.MAJOR.equals(participant.getStreamType())
                && OpenViduRole.MODERATOR.equals(participant.getRole())) {
            //close room stopPolling
            preset.setPollingStatusInRoom(SessionPresetEnum.off);
            timerManager.stopPollingCompensation(sessionId);
            //send notify
            JsonObject params = new JsonObject();
            params.addProperty("roomId", sessionId);
            session.getParticipants().forEach(part -> notificationService.sendNotification(part.getParticipantPrivateId(),
                    ProtocolElements.STOP_POLLING_NODIFY_METHOD, params));
        }
        UseTime.point("p5");
        if (!Objects.isNull(rpcConnection.getSerialNumber()) && StreamType.MAJOR.equals(participant.getStreamType())) {
            cacheManage.setDeviceStatus(rpcConnection.getSerialNumber(), DeviceStatus.online.name());
        }
        log.info("Participant {} has left session {}", participant.getParticipantPublicId(),
                rpcConnection.getSessionId());
        if (Objects.nonNull(session = sessionManager.getSession(sessionId)) && !session.isClosed()
                && StreamType.MAJOR.equals(participant.getStreamType()) && participant.getRole().needToPublish()) {
            session.putPartOnWallAutomatically(sessionManager);
        }
        rpcConnection.setReconnected(false);
    }
}
