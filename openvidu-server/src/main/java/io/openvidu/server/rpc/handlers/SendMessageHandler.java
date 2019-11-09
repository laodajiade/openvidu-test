package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

/**
 * @author geedow
 * @date 2019/11/5 19:24
 */
@Service
public class SendMessageHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Participant participant;
        try {
            participant = sanityCheckOfSession(rpcConnection, "signal");
        } catch (OpenViduException e) {
            return;
        }

        String message = getStringParam(request, ProtocolElements.SENDMESSAGE_MESSAGE_PARAM);
        sessionManager.sendMessage(participant, message, request.getId());
    }
}
