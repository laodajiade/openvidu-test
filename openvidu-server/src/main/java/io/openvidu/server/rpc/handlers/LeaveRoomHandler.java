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

import java.util.Objects;
import java.util.Set;

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
        String streamType = getStringParam(request, ProtocolElements.LEAVEROOM_STREAM_TYPE_PARAM);
        if (StringUtils.isEmpty(sessionId) || StringUtils.isEmpty(sourceId) || StringUtils.isEmpty(streamType)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        Participant participant;
        try {
            participant = sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId(),
                    StreamType.valueOf(streamType));
            if (Objects.isNull(participant)) {
                log.info("when participants are disconnected and reconnected, they can leave the meeting without joining.");
                /*Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
                participant = sessionManager.getParticipant(sessionId, String.valueOf(userInfo.get("reconnect")));
                if (Objects.isNull(participant)) {
                    notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
                    return;
                }*/
                updateReconnectInfo(rpcConnection);
                notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
                // json RPC notify KMS layout changed.
                Session conferenceSession = sessionManager.getSession(sessionId);
                if (!Objects.isNull(conferenceSession)) {
                    conferenceSession.invokeKmsConferenceLayout();
                    // broadcast the changes of layout
                    JsonObject notifyResult = new JsonObject();
                    notifyResult.addProperty(ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY_MODE_PARAM, conferenceSession.getLayoutMode().getMode());
                    notifyResult.add(ProtocolElements.CONFERENCELAYOUTCHANGED_PARTLINKEDLIST_PARAM, conferenceSession.getCurrentPartInMcuLayout());
                    notifyResult.addProperty(ProtocolElements.CONFERENCELAYOUTCHANGED_AUTOMATICALLY_PARAM, conferenceSession.isAutomatically());

                    conferenceSession.getParticipants().forEach(part -> {
                        if (!Objects.equals(StreamType.MAJOR, part.getStreamType())) return;
                        // broadcast the changes of layout
                        this.notificationService.sendNotification(part.getParticipantPrivateId(), ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, notifyResult);
                    });
                }
                return;

            }
        } catch (OpenViduException e) {
            if (updateReconnectInfo(rpcConnection)) {
                log.info("close previous participant info.");
                notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
            }
            return;
        }

        if (Objects.isNull(sessionManager.getSession(sessionId))) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }

//        this.sessionManager.dealParticipantLeaveRoom(participant, false, request.getId());
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
        if (Objects.equals(session.getConferenceMode(), ConferenceModeEnum.MCU)) {
            session.leaveRoomSetLayout(participant, !Objects.equals(speakerId, participant.getParticipantPublicId()) ?
                    speakerId : moderatePublicId);
            // json RPC notify KMS layout changed.
            session.invokeKmsConferenceLayout();
        }

        if (Objects.equals(ParticipantHandStatus.speaker, participant.getHandStatus())) {
            participant.setHandStatus(ParticipantHandStatus.endSpeaker);
        }
        sessionManager.leaveRoom(participant, request.getId(), EndReason.disconnect, false);

        if (Objects.equals(session.getConferenceMode(), ConferenceModeEnum.MCU)) {
            for (Participant participant1 : participants) {
                if (!Objects.equals(StreamType.MAJOR, participant1.getStreamType())) continue;
                notificationService.sendNotification(participant1.getParticipantPrivateId(),
                        ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, session.getLayoutNotifyInfo());
            }
        }

        if (!Objects.isNull(rpcConnection.getSerialNumber()) && !Objects.equals(StreamType.SHARING, participant.getStreamType())) {
            cacheManage.setDeviceStatus(rpcConnection.getSerialNumber(), DeviceStatus.online.name());
        }
        log.info("Participant {} has left session {}", participant.getParticipantPublicId(),
                rpcConnection.getSessionId());
        if (Objects.nonNull(session = sessionManager.getSession(sessionId)) && !session.isClosed()) {
            session.putPartOnWallAutomatically(sessionManager);
        }
    }
}
