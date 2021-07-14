package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UpdateParticipantsOrderHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        if (!sanityCheckOfSession(rpcConnection).getRole().isController()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(), null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        JsonArray orderedParts = getParam(request, ProtocolElements.UPDATE_PARTICIPANTS_ORDER_ORDEREDPARTS_PARAM).getAsJsonArray();
        ErrorCodeEnum errorCodeEnum = ErrorCodeEnum.SUCCESS;
        if (orderedParts.size() > 0) {
            Map<String, Integer> partOrderMap = new HashMap<>(orderedParts.size());
            for (JsonElement jsonElement : orderedParts) {
                JsonObject orderPart = jsonElement.getAsJsonObject();
                partOrderMap.putIfAbsent(orderPart.get("account").getAsString(), orderPart.get("order").getAsInt());
            }
            Participant moderatorPart = sessionManager.getSession(rpcConnection.getSessionId()).getModeratorPart();
            errorCodeEnum = sessionManager.getSession(rpcConnection.getSessionId()).dealPartOrderAfterRoleChanged(partOrderMap, sessionManager, moderatorPart);
        }
        if (!ErrorCodeEnum.SUCCESS.equals(errorCodeEnum)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, errorCodeEnum);
            return;
        }
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
