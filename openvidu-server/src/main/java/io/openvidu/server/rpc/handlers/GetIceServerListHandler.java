package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

/**
 * @author chosongi
 * @date 2020/10/21 10:52
 */
@Service
public class GetIceServerListHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        JsonObject respObj = new JsonObject();
        respObj.add("stunServer", gson.fromJson(openviduConfig.getStunServers(), JsonArray.class));
        respObj.add("turnServer", gson.fromJson(openviduConfig.getLBRTNsStrings(), JsonArray.class));

        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respObj);
    }
}
