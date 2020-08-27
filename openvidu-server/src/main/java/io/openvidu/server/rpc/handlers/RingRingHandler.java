package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Objects;

/**
 * @author chosongi
 * @date 2020/3/26 23:17
 */
@Slf4j
@Service
public class RingRingHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sourceId = getStringParam(request, ProtocolElements.RINGRING_SOURCE_ID_PARAM);
        String targetId = getStringParam(request, ProtocolElements.RINGRING_TARGET_ID_PARAM);

        cancelInviteCompensation(rpcConnection.getUserUuid());
        RpcConnection rpcConnect = notificationService.getRpcConnections().stream()
                .filter(r -> !Objects.isNull(r.getUserUuid()) && r.getUserUuid().compareTo(targetId) == 0)
                .max(Comparator.comparing(RpcConnection::getCreateTime)).orElse(null);
        if (!Objects.isNull(rpcConnect)) {
            JsonObject param = new JsonObject();
            param.addProperty(ProtocolElements.RINGRING_SOURCE_ID_PARAM, sourceId);
            param.addProperty(ProtocolElements.RINGRING_TARGET_ID_PARAM, targetId);
            notificationService.sendNotification(rpcConnect.getParticipantPrivateId(), ProtocolElements.RINGRINGNOTIFY_METHOD, param);
        }

        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
