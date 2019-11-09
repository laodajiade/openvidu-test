package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

/**
 * @author geedow
 * @date 2019/11/5 17:08
 */
@Service
public class UnlockSessionHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.UNLOCK_SESSION_ROOM_ID_PARAM);
        if (sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId()).getRole() != OpenViduRole.MODERATOR) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(), null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        if (sessionManager.getSession(sessionId).isLocking() &&
                !sessionManager.getSession(sessionId).setLocking(false)) {
            JsonObject params = new JsonObject();
            params.addProperty(ProtocolElements.UNLOCK_SESSION_ROOM_ID_PARAM, sessionId);

            sessionManager.getParticipants(sessionId).forEach(participant ->
                    this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.UNLOCK_SESSION_METHOD, params));
        }
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
