package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionPreset;
import io.openvidu.server.core.SessionPresetEnum;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 20:00
 */
@Slf4j
@Service
public class CloseRoomHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Session session;
        ErrorCodeEnum errCode = ErrorCodeEnum.SUCCESS;
        String sessionId = getStringParam(request, ProtocolElements.CLOSE_ROOM_ID_PARAM);
        if (Objects.isNull(session = sessionManager.getSession(sessionId)) || session.isClosed()) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_ALREADY_CLOSED);
            return ;
        }

        Participant participant = session.getParticipantByPrivateId(rpcConnection.getParticipantPrivateId());
        if (Objects.nonNull(participant)) {
            if (!participant.getRole().isController()) {
                errCode = ErrorCodeEnum.PERMISSION_LIMITED;
            }
        } else {
            // once participant reconnected, close the room directly without joining room
            // find the participant related to the previous connection and verify the operation permission
            Map partInfo = cacheManage.getPartInfo(rpcConnection.getUserUuid());
            if (!partInfo.isEmpty() && Objects.nonNull(participant = session.getParticipantByUUID(rpcConnection.getUserUuid()))
                    && !participant.getRole().isController()) {
                errCode = ErrorCodeEnum.PERMISSION_LIMITED;
            }

        }
        if (!ErrorCodeEnum.SUCCESS.equals(errCode)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, errCode);
            return ;
        }

        // set session status: closing
        session.setClosing(true);
        sessionManager.getSession(sessionId).getParticipants().forEach(p -> {
            if (!Objects.equals(StreamType.MAJOR, p.getStreamType())) return;
            notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.CLOSE_ROOM_NOTIFY_METHOD, new JsonObject());
            RpcConnection rpcConnect = notificationService.getRpcConnection(p.getParticipantPrivateId());
            if (!Objects.isNull(rpcConnect) && !Objects.isNull(rpcConnect.getSerialNumber())) {
                cacheManage.setDeviceStatus(rpcConnect.getSerialNumber(), DeviceStatus.online.name());
            }});

        //cancel invite
        cancelAllInviteCompensation(sessionId);
        // TODO: compatible to the delay of leaving room
        this.sessionManager.updateConferenceInfo(sessionId);

        //close room stopPolling
        SessionPreset sessionPreset = session.getPresetInfo();
        sessionPreset.setPollingStatusInRoom(SessionPresetEnum.off);
        timerManager.stopPollingCompensation(sessionId);
        //send notify
        session.getMajorPartEachIncludeThorConnect().forEach(part -> notificationService.sendNotification(part.getParticipantPrivateId(),
                ProtocolElements.STOP_POLLING_NODIFY_METHOD, request.getParams()));

        this.sessionManager.closeSession(sessionId, EndReason.closeSessionByModerator);
        rpcConnection.setReconnected(false);

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
