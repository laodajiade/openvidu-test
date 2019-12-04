package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author geedow
 * @date 2019/11/5 20:00
 */
@Slf4j
@Service
public class CloseRoomHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.CLOSE_ROOM_ID_PARAM);
        ErrorCodeEnum errCode = ErrorCodeEnum.SUCCESS;
        if (Objects.isNull(sessionManager.getSession(sessionId)) || sessionManager.getSession(sessionId).isClosed()) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_ALREADY_CLOSED);
            return ;
        }


        Participant participant = sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId());
        if (!Objects.isNull(participant)) {
            if (!OpenViduRole.MODERATOR_ROLES.contains(participant.getRole()))
                errCode = ErrorCodeEnum.PERMISSION_LIMITED;
        } else {
            // once participant reconnected, close the room directly without joining room
            // find the participant related to the previous connection and verify the operation permission
            Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
            participant = sessionManager.getParticipant(sessionId, String.valueOf(userInfo.get("reconnect")));
            if (!Objects.isNull(participant) && !OpenViduRole.MODERATOR_ROLES.contains(participant.getRole()))
                errCode = ErrorCodeEnum.PERMISSION_LIMITED;

        }
        if (!ErrorCodeEnum.SUCCESS.equals(errCode)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, errCode);
            return ;
        }

        updateReconnectInfo(rpcConnection);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
        sessionManager.getSession(sessionId).getParticipants().forEach(p -> {
            notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.CLOSE_ROOM_NOTIFY_METHOD, new JsonObject());
            RpcConnection rpcConnect = notificationService.getRpcConnection(p.getParticipantPrivateId());
            if (!Objects.isNull(rpcConnect.getSerialNumber())) {
                cacheManage.setDeviceStatus(rpcConnect.getSerialNumber(), DeviceStatus.online.name());
            }});
        this.sessionManager.unpublishAllStream(sessionId, EndReason.closeSessionByModerator);
        this.sessionManager.closeSession(sessionId, EndReason.closeSessionByModerator);
    }
}
