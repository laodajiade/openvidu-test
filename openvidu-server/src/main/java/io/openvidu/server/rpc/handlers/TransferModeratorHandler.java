package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 20:16
 */
@Slf4j
@Service
public class TransferModeratorHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        // TODO. Need combine createConference and conferenceControl power. Not only used MODERATOR.
        String sessionId = getStringParam(request, ProtocolElements.TRANSFER_MODERATOR_ID_PARAM);
        String targetId = getStringParam(request, ProtocolElements.TRANSFER_MODERATOR_TARGET_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.TRANSFER_MODERATOR_SOURCE_ID_PARAM);

        if (sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId()).getRole() != OpenViduRole.MODERATOR) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        JsonObject params = new JsonObject();

        params.addProperty(ProtocolElements.TRANSFER_MODERATOR_ID_PARAM, sessionId);
        params.addProperty(ProtocolElements.TRANSFER_MODERATOR_SOURCE_ID_PARAM, sourceId);
        params.addProperty(ProtocolElements.TRANSFER_MODERATOR_TARGET_ID_PARAM, targetId);
        sessionManager.getParticipants(sessionId).forEach(p -> {
            long userId = gson.fromJson(p.getClientMetadata(), JsonObject.class).get("clientData").getAsLong();
            if (Objects.equals(String.valueOf(userId), targetId)) {
                p.setRole(OpenViduRole.MODERATOR);
            }

            this.notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.TRANSFER_MODERATOR_METHOD, params);
        });

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
