package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;

public class GetUnfinishedMeetingsHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        boolean isAdmin = getBooleanParam(request, ProtocolElements.GET_UNFINISHED_MEETINGS_ISADMIN_PAPM);


    }
}
