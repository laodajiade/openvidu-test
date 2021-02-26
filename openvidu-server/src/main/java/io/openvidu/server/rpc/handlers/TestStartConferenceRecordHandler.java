package io.openvidu.server.rpc.handlers;

import com.alibaba.fastjson.JSON;
import com.google.gson.JsonObject;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.kurento.endpoint.PublisherEndpoint;
import io.openvidu.server.kurento.kms.KmsManager;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.*;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class TestStartConferenceRecordHandler extends RpcAbstractHandler {

    @Resource
    KmsManager kmsManager;

    KurentoClient kurentoClient104 = KurentoClient.create("ws://172.25.11.201:8888/kurento");

    public static MediaPipeline pipeline104 = null;

    public static WebRtcEndpoint distributionEp=null;

    public static PassThrough passThrough = null;
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = rpcConnection.getSessionId();
        if (pipeline104 != null) {
            pipeline104.release();
            pipeline104 = null;
        }

        pipeline104 = kurentoClient104.createMediaPipeline();
        log.info("xxxxxxxxxx pipeline104 {}",pipeline104.getId());
        //WebRtcEndpoint distributionEp = new WebRtcEndpoint.Builder(pipeline104).recvonly().build();
        //Dispatcher dispatcher = new Dispatcher.Builder(pipeline104).build(); //104的调度器，相当于录制服务RecordCompositeWrapper：67
        //Dispatcher dispatcher = new Dispatcher.Builder(pipeline104).build(); //104的调度器，相当于录制服务RecordCompositeWrapper：67
       // HubPort dispatcherHubPort = new HubPort.Builder(dispatcher).build();

//        UriEndpoint endpoint = new PlayerEndpoint.Builder(pipeline104, recording.getPath())
//                .withMediaProfile(recording.getMediaProfileSpecType())
//                .build();
        passThrough = new PassThrough.Builder(pipeline104).build();

        distributionEp = new WebRtcEndpoint.Builder(pipeline104).build();
        distributionEp.setMinOutputBitrate(2000000);
        distributionEp.setMaxOutputBitrate(2000000);
        //dispatcherHubPort.connect(distributionEp);


        distributionEp.connect(passThrough);

        Session session = sessionManager.getSession(sessionId);
        KurentoParticipant moderatorPart = (KurentoParticipant) sessionManager.getModeratorPart(sessionId);

        // 录制的获取源媒体，我应该是直接获取到session中的pipeline
        //MediaPipeline mediaPipeline = kms.getKurentoClient().getById(mediaSourceObj.getString("mediaPipelineId"), MediaPipeline.class);
        MediaPipeline mediaPipeline = moderatorPart.getPipeline();
        log.info("xxxxxxxxxx sourcePipeline {}", mediaPipeline.getId());
        WebRtcEndpoint moderatorEndpoint = new WebRtcEndpoint.Builder(mediaPipeline).build();
        //代替 this.passThrough = kms.getKurentoClient().getById(passThruId, PassThrough.class);
        PublisherEndpoint publisherEndpoint = moderatorPart.getPublisher();
        PassThrough passThru = publisherEndpoint.getPassThru();//主持人的passThru


        createRtcChnAndSwitchIces(distributionEp, moderatorEndpoint, passThru);


        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }

    public void createRtcChnAndSwitchIces(WebRtcEndpoint distributionEp, WebRtcEndpoint publisherEndpoint, PassThrough passThru) {
        String sdpOffer = distributionEp.generateOffer();


        distributionEp.addOnIceCandidateListener(event -> {
            IceCandidate candidate = event.getCandidate();
            log.info("RecordRecvWebRtcEndPoint iceCandidate:{}", JSON.toJSON(candidate));
            publisherEndpoint.addIceCandidate(candidate, new Continuation<Void>() {

                @Override
                public void onSuccess(Void result) throws Exception {
                    log.info("sourceSubEndPoint EP {}: Ice candidate added to the internal endpoint", publisherEndpoint.getId());
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    log.error("sourceSubEndPoint EP {}: Failed to add ice candidate to the internal endpoint", publisherEndpoint.getId());
                }
            });
        });


        // set recordRecvWebRtcEndPoint IceCandidate Event Listener
        log.info("add recordRecvWebRtcEndPoint IceComponentStateChangeListener.");
        distributionEp.addIceComponentStateChangeListener(event -> {
            String msg = "KMS event [IceComponentStateChange]: -> endpoint: " + distributionEp.getId()
                    + " (distributionEp) | state: " + event.getState().name() + " | componentId: "
                    + event.getComponentId() + " | streamId: " + event.getStreamId() + " | timestamp: "
                    + event.getTimestampMillis();
            log.info(msg);
        });

        // set sourceSubEndPoint IceCandidate Event Listener
        log.info("add sourceSubEndPoint OnIceCandidateListener.");
        publisherEndpoint.addOnIceCandidateListener(event -> {
            IceCandidate candidate = event.getCandidate();
            log.info("SourceSubEndPoint iceCandidate:{}", JSON.toJSON(candidate));
            distributionEp.addIceCandidate(candidate, new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) throws Exception {
                    log.info("recordRecvWebRtcEndPoint EP {}: Ice candidate added to the internal endpoint", distributionEp.getId());
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    log.error("recordRecvWebRtcEndPoint EP {}: Failed to add ice candidate to the internal endpoint", distributionEp.getId());
                }
            });
        });

        String sdpAnswer = publisherEndpoint.processOffer(sdpOffer);
        log.info("sourceSubEndPoint sdpAnswer:\n" + sdpAnswer);

        publisherEndpoint.gatherCandidates(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.info("sourceSubEndPoint EP {}: Internal endpoint started to gather candidates", publisherEndpoint.getId());
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.error("sourceSubEndPoint EP {}: Internal endpoint failed to start gathering candidates", publisherEndpoint.getId(), cause);
            }
        });


        // connect the passThru and sourceSubEndPoint
        passThru.connect(publisherEndpoint, new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.info("Elements have been connected (source {} -> sink {})", passThru.getId(), publisherEndpoint.getId());
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.error("Failed to connect media elements (source {} -> sink {})", passThru.getId(), publisherEndpoint.getId(), cause);
            }
        });

        // recordRecvWebRtcEndPoint process sdpAnswer
        distributionEp.processAnswer(sdpAnswer);
        distributionEp.gatherCandidates(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.info("recordRecvWebRtcEndPoint EP {}: Internal endpoint started to gather candidates", distributionEp.getId());
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.error("recordRecvWebRtcEndPoint EP {}: Internal endpoint failed to start gathering candidates", distributionEp.getId(), cause);
            }
        });

    }
}