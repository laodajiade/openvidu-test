package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.StreamModeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

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

        KurentoSession session = (KurentoSession) sessionManager.getSession(rpcConnection.getSessionId());

        String streamModeStr;
        StreamModeEnum streamMode = !StringUtils.isEmpty(streamModeStr = getStringOptionalParam(request, ProtocolElements.RECEIVEVIDEO_STREAM_MODE_PARAM))
                ? StreamModeEnum.valueOf(streamModeStr) : null;

        String senderName = getStringParam(request, ProtocolElements.RECEIVEVIDEO_SENDER_PARAM);
        senderName = senderName.substring(0, Objects.equals(StreamModeEnum.MIX_MAJOR_AND_SHARING, streamMode) ?
                senderName.lastIndexOf("_") : senderName.indexOf("_"));
        String sdpOffer = getStringParam(request, ProtocolElements.RECEIVEVIDEO_SDPOFFER_PARAM);
        sessionManager.subscribe(participant, senderName, streamMode, sdpOffer, request.getId());
    }
}
