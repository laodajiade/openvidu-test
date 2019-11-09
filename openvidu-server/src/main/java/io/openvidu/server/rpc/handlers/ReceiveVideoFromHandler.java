package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

/**
 * @author geedow
 * @date 2019/11/5 17:19
 */
@Slf4j
@Service
public class ReceiveVideoFromHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Participant participant;
        try {
            participant = sanityCheckOfSession(rpcConnection, "subscribe");
        } catch (OpenViduException e) {
            return;
        }

        String senderName = getStringParam(request, ProtocolElements.RECEIVEVIDEO_SENDER_PARAM);
        senderName = senderName.substring(0, senderName.indexOf("_"));
        String sdpOffer = getStringParam(request, ProtocolElements.RECEIVEVIDEO_SDPOFFER_PARAM);

        sessionManager.subscribe(participant, senderName, sdpOffer, request.getId());
    }
}
