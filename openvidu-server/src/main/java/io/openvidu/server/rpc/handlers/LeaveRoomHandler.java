package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
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
        StreamType streamType = StreamType.valueOf(getStringParam(request, ProtocolElements.LEAVEROOM_STREAM_TYPE_PARAM));
        if (StringUtils.isEmpty(sessionId) || (StringUtils.isEmpty(sourceId) && UserType.register.equals(rpcConnection.getUserType()))
                || StringUtils.isEmpty(streamType)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        Participant participant;
        try {
            participant = sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId(), streamType);
            if (Objects.isNull(participant)) {
                if (StreamType.MAJOR.equals(streamType) && AccessTypeEnum.terminal.equals(rpcConnection.getAccessType())) {
                    log.info("when participants are disconnected and reconnected, they can leave the meeting without joining.");
                    Map partInfo = cacheManage.getPartInfo(rpcConnection.getUserUuid());
                    if (!partInfo.isEmpty()) {
                        sessionManager.evictParticipantByUUID(partInfo.get("roomId").toString(),
                                rpcConnection.getUserUuid(), Collections.singletonList(EvictParticipantStrategy.CLOSE_ROOM_WHEN_EVICT_MODERATOR));
                    }
                }

                notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
                return;
            }
        } catch (OpenViduException e) {
            log.info("close previous participant info exception:{}", e);
            return;
        }

        if (Objects.isNull(sessionManager.getSession(sessionId))) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }

        String moderatePublicId = null;
        String speakerId = null;
        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        if (Objects.equals(ParticipantHandStatus.speaker, participant.getHandStatus())) {
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

        Session session = sessionManager.getSession(sessionId);
        if (Objects.equals(session.getConferenceMode(), ConferenceModeEnum.MCU)
                && participant.getStreamType().isStreamTypeMixInclude()) {
            sessionManager.setLayoutAndNotifyWhenLeaveRoom(sessionId, participant,
                    !Objects.equals(speakerId, participant.getParticipantPublicId()) ? speakerId : moderatePublicId);
        }

        if (Objects.equals(ParticipantHandStatus.speaker, participant.getHandStatus())) {
            participant.changeHandStatus(ParticipantHandStatus.endSpeaker);
        }
        sessionManager.leaveRoom(participant, request.getId(), EndReason.disconnect, false);

        if (!Objects.isNull(rpcConnection.getSerialNumber()) && StreamType.MAJOR.equals(participant.getStreamType())) {
            cacheManage.setDeviceStatus(rpcConnection.getSerialNumber(), DeviceStatus.online.name());
        }
        log.info("Participant {} has left session {}", participant.getParticipantPublicId(),
                rpcConnection.getSessionId());
        if (Objects.nonNull(session = sessionManager.getSession(sessionId)) && !session.isClosed()
                && StreamType.MAJOR.equals(participant.getStreamType()) && participant.getRole().needToPublish()) {
            session.putPartOnWallAutomatically(sessionManager);
        }
        //del accessIn privateId
        cacheManage.delAccessInParticipantPrivateId(rpcConnection.getUserUuid());
        rpcConnection.setReconnected(false);
    }
}
