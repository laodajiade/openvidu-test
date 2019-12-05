package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.ParticipantHandStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
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
                Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
                participant = sessionManager.getParticipant(sessionId, String.valueOf(userInfo.get("reconnect")));
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

        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        if (Objects.equals(ParticipantHandStatus.speaker, participant.getHandStatus())) {
            participant.setHandStatus(ParticipantHandStatus.endSpeaker);

            JsonObject params = new JsonObject();
            params.addProperty(ProtocolElements.END_ROLL_CALL_ROOM_ID_PARAM, sessionId);
            params.addProperty(ProtocolElements.END_ROLL_CALL_TARGET_ID_PARAM, sourceId);

            for (Participant participant1 : participants) {
                this.notificationService.sendNotification(participant1.getParticipantPrivateId(),
                        ProtocolElements.END_ROLL_CALL_METHOD, params);
            }
        }

        Session session = sessionManager.getSession(sessionId);
        session.leaveRoomSetLayout(participant);
        // json RPC notify KMS layout changed.
        session.invokeKmsConferenceLayout();

        sessionManager.leaveRoom(participant, request.getId(), EndReason.disconnect, false);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY_MODE_PARAM, session.getLayoutMode().getMode());
        jsonObject.addProperty(ProtocolElements.CONFERENCELAYOUTCHANGED_AUTOMATICALLY_PARAM, session.isAutomatically());
        jsonObject.add(ProtocolElements.CONFERENCELAYOUTCHANGED_PARTLINKEDLIST_PARAM, session.getCurrentPartInMcuLayout());
        for (Participant participant1 : participants) {
            notificationService.sendNotification(participant1.getParticipantPrivateId(),
                    ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, jsonObject);
        }
        if (!Objects.isNull(rpcConnection.getSerialNumber())) {
            cacheManage.setDeviceStatus(rpcConnection.getSerialNumber(), DeviceStatus.online.name());
        }
        log.info("Participant {} has left session {}", participant.getParticipantPublicId(),
                rpcConnection.getSessionId());
    }
}
