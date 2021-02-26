package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.StreamModeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.Continuation;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Time;
import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 17:19
 */
@Slf4j
@Service
public class TestReceiveVideoFromHandler extends RpcAbstractHandler {

    public static WebRtcEndpoint receiveEndpoint = null;
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String streamModeStr;
        StreamModeEnum streamMode = !StringUtils.isEmpty(streamModeStr = getStringOptionalParam(request, ProtocolElements.RECEIVEVIDEO_STREAM_MODE_PARAM))
                ? StreamModeEnum.valueOf(streamModeStr) : null;
        String senderName = getStringParam(request, ProtocolElements.RECEIVEVIDEO_SENDER_PARAM);
        senderName = senderName.substring(0, Objects.equals(StreamModeEnum.MIX_MAJOR_AND_SHARING, streamMode) ?
                senderName.lastIndexOf("_") : senderName.indexOf("_"));
        final String finalSenderName =senderName;
        String sdpOffer = getStringParam(request, ProtocolElements.RECEIVEVIDEO_SDPOFFER_PARAM);

        log.info("TestReceiveVideoFromHandler  111111111111");
        receiveEndpoint = new WebRtcEndpoint.Builder(TestStartConferenceRecordHandler.pipeline104).build();
        Participant participant = sessionManager.getParticipant(rpcConnection.getSessionId(), rpcConnection.getParticipantPrivateId());
        log.info("TestReceiveVideoFromHandler  222222222222");
        receiveEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

            @Override
            public void onEvent(IceCandidateFoundEvent event) {

                JsonObject response = new JsonObject();
                response.addProperty("id", "iceCandidate");
                response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                log.info("addIceCandidateFoundListener {}", response.toString());
                //notificationService.sendNotification(rpcConnection.getParticipantPrivateId(), "iceCandidate", response);
                ((KurentoParticipant)participant).sendIceCandidate(participant.getParticipantPublicId(),finalSenderName ,event.getCandidate());
            }
        });

        receiveEndpoint.addConnectionStateChangedListener(event -> {
            log.info("5555555555 receiveEndpoint connectionStateChanged oldState={}  newState={} ",event.getOldState().name(),event.getNewState().name());

        });



        //TestStartConferenceRecordHandler.distributionEp.connect(receiveEndpoint);

        String sdpAnswer = receiveEndpoint.processOffer(sdpOffer);



        JsonObject result = new JsonObject();
        result.addProperty("sdpAnswer", sdpAnswer);


        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), result);
        TestStartConferenceRecordHandler.passThrough.connect(receiveEndpoint,new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.info("passThrough connect receiveEndpoint success");
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.error("passThrough connect receiveEndpoint fail", cause);
            }
        });

        log.info(" passThrough connect receiveEndpoint");
        receiveEndpoint.gatherCandidates(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.info("66666666666666 sourceSubEndPoint EP: Internal endpoint started to gather candidates");
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("66666666666666 sourceSubEndPoint EP: Internal endpoint failed to start gathering candidates", cause);
            }
        });
    }
}
