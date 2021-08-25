package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

@Slf4j
@Service(ProtocolElements.ECHO_METHOD)
public class EchoHandler extends ExRpcAbstractHandler<JsonObject> {

    @Override
    public RespResult<JsonObject> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject params) {
        return RespResult.ok(params);
    }
}
