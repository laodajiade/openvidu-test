package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BindValidate;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

@Slf4j
@Service(ProtocolElements.REPLACE_PARTICIPANTS_ORDER_METHOD)
public class ReplaceParticipantsOrderHandler extends ExRpcAbstractHandler<JsonObject> {

    @Override
    public RespResult<?> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject params) {
        if (!sanityCheckOfSession(rpcConnection).getRole().isController()) {
            return RespResult.fail(ErrorCodeEnum.PERMISSION_LIMITED);
        }

        String source = BindValidate.notEmptyAndGet(params, "source");
        String target = BindValidate.notEmptyAndGet(params, "target");

        Participant sourcePart = sanityCheckOfSession(rpcConnection.getSessionId(), source);
        Participant targetPart = sanityCheckOfSession(rpcConnection.getSessionId(), target);
        Participant operatePart = sanityCheckOfSession(rpcConnection);

        ErrorCodeEnum errorCodeEnum = sessionManager.getSession(rpcConnection.getSessionId()).dealReplaceOrder(sourcePart, targetPart, sessionManager, operatePart);
        return RespResult.end(errorCodeEnum);
    }
}
