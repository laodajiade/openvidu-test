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

    @Autowired
    TestReceiveVideoFromHandler testReceiveVideoFromHandler;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {

        if (rpcConnection.getUserUuid().endsWith("5")) {
            log.info("80103600005 testReceiveVideoFromHandler");
            testReceiveVideoFromHandler.handRpcRequest(rpcConnection, request);
            return;
        }

        Participant participant;
        try {
            participant = sanityCheckOfSession(rpcConnection, "subscribe");
        } catch (OpenViduException e) {
            return;
        }

        KurentoSession session = (KurentoSession) sessionManager.getSession(rpcConnection.getSessionId());
        // 如果是墙下，且服务器开启了媒体级联，则从分发服务器上进行接收
        if (!participant.getRole().needToPublish() && !session.getDeliveryKmsManagers().isEmpty()) {
            log.info("{} goto delivery receive", rpcConnection.getUserUuid());
            receiveVideoFromDelivery(rpcConnection, request);
            return;
        }


        String streamModeStr;
        StreamModeEnum streamMode = !StringUtils.isEmpty(streamModeStr = getStringOptionalParam(request, ProtocolElements.RECEIVEVIDEO_STREAM_MODE_PARAM))
                ? StreamModeEnum.valueOf(streamModeStr) : null;

        String senderName = getStringParam(request, ProtocolElements.RECEIVEVIDEO_SENDER_PARAM);
        senderName = senderName.substring(0, Objects.equals(StreamModeEnum.MIX_MAJOR_AND_SHARING, streamMode) ?
                senderName.lastIndexOf("_") : senderName.indexOf("_"));
        String sdpOffer = getStringParam(request, ProtocolElements.RECEIVEVIDEO_SDPOFFER_PARAM);
        log.info("11111111111 senderName {}", senderName);
        sessionManager.subscribe(participant, senderName, streamMode, sdpOffer, request.getId());
    }

    private void receiveVideoFromDelivery(RpcConnection rpcConnection, Request<JsonObject> request) {
        Participant participant;
        try {
            participant = sanityCheckOfSession(rpcConnection, "subscribe");
        } catch (OpenViduException e) {
            return;
        }

        String streamModeStr;
        StreamModeEnum streamMode = !StringUtils.isEmpty(streamModeStr = getStringOptionalParam(request, ProtocolElements.RECEIVEVIDEO_STREAM_MODE_PARAM))
                ? StreamModeEnum.valueOf(streamModeStr) : null;

        String senderName = getStringParam(request, ProtocolElements.RECEIVEVIDEO_SENDER_PARAM);
        senderName = senderName.substring(0, Objects.equals(StreamModeEnum.MIX_MAJOR_AND_SHARING, streamMode) ?
                senderName.lastIndexOf("_") : senderName.indexOf("_"));
        String sdpOffer = getStringParam(request, ProtocolElements.RECEIVEVIDEO_SDPOFFER_PARAM);

        sessionManager.subscribe(participant, senderName, streamMode, sdpOffer, request.getId());

//        EventListener<IceCandidateFoundEvent> eventListener = new EventListener<IceCandidateFoundEvent>() {
//            @Override
//            public void onEvent(IceCandidateFoundEvent event) {
//
//                JsonObject response = new JsonObject();
//                response.addProperty("id", "iceCandidate");
//                response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
//                log.info("addIceCandidateFoundListener {}", response.toString());
//                notificationService.sendNotification(rpcConnection.getParticipantPrivateId(), "onIceCandidate", response);
//            }
//        };

    }
}
