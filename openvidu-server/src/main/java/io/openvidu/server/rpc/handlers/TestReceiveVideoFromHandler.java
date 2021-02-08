package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.StreamModeEnum;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
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
        String sdpOffer = getStringParam(request, ProtocolElements.RECEIVEVIDEO_SDPOFFER_PARAM);

        log.info("TestReceiveVideoFromHandler  111111111111");
        receiveEndpoint = new WebRtcEndpoint.Builder(TestStartConferenceRecordHandler.pipeline104).build();


        receiveEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

            @Override
            public void onEvent(IceCandidateFoundEvent event) {

                JsonObject response = new JsonObject();
                response.addProperty("id", "iceCandidate");
                response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                log.info("addIceCandidateFoundListener {}", response.toString());
                notificationService.sendNotification(rpcConnection.getParticipantPrivateId(), "onIceCandidate", response);
            }
        });


        TestStartConferenceRecordHandler.distributionEp.connect(receiveEndpoint);
        String sdpAnswer = receiveEndpoint.processOffer(sdpOffer);



        JsonObject result = new JsonObject();
        result.addProperty("sdpAnswer", sdpAnswer);

        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), result);

        receiveEndpoint.gatherCandidates();
    }
}
