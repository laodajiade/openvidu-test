package io.openvidu.server.kurento.core;

import com.alibaba.fastjson.JSON;
import io.openvidu.server.common.enums.ParticipantHandStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.kurento.endpoint.DispatcherEndpoint;
import io.openvidu.server.kurento.endpoint.PublisherEndpoint;
import io.openvidu.server.kurento.kms.Kms;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.janino.IClass;
import org.kurento.client.*;
import org.kurento.client.EventListener;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DeliveryKmsManager {

    private final Kms kms;
    @Getter
    private final String id;
    private final Session session;

    @Setter
    public MediaPipeline pipeline;

    @Setter
    @Getter
    public CountDownLatch pipelineLatch = new CountDownLatch(1);

    @Setter
    @Getter
    public Throwable pipelineCreationErrorCause;

    SessionManager sessionManager;

    public Map<String, DispatcherEndpoint> dispatcherMap = new ConcurrentHashMap<>();

    public DeliveryKmsManager(Kms kms,Session session) {
        this.kms = kms;
        this.session = session;
        this.id = kms.getId() + "_" + session.getSessionId();
    }


    public MediaPipeline getPipeline() {
        try {
            pipelineLatch.await(KurentoSession.ASYNC_LATCH_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return this.pipeline;
    }

    public void dispatcher(Session session) {
        log.info("begin kms dispatcher sessionId {}", session.getSessionId());

        Set<Participant> participants = session.getParticipants();

        List<KurentoParticipant> publisher = new ArrayList<>();

        for (Participant participant : participants) {
            if (!participant.getStreamType().isStreamTypeMixInclude()) {
                continue;
            }

            if (StreamType.SHARING.equals(participant.getStreamType())) {
                //publisher.add((KurentoParticipant) participant);
            }

            if (ParticipantHandStatus.speaker.equals(participant.getHandStatus())) {
                //publisher.add((KurentoParticipant) participant);
            }

            if (participant.getRole().isController() && participant.getRole().needToPublish()
                    && participant.getStreamType().isSelfStream()) {
                publisher.add((KurentoParticipant) participant);//moderator
            }

            if (participant.getRole().needToPublish()) {
                //publisher.add(participant);
            }
        }

        for (KurentoParticipant participant : publisher) {
            dispatcher(participant);
        }

    }

    public void dispatcher(KurentoParticipant participant){
        WebRtcEndpoint dispatcherEndpoint = new WebRtcEndpoint.Builder(this.getPipeline()).build();//删除recvonly
        dispatcherEndpoint.setMinOutputBitrate(8000000);
        dispatcherEndpoint.setMaxOutputBitrate(8000000);

        PassThrough passThrough = new PassThrough.Builder(this.getPipeline()).build();
        dispatcherEndpoint.connect(passThrough);

        MediaPipeline sourcePipeline = participant.getPipeline();
        WebRtcEndpoint dispatcherOutEndPoint = new WebRtcEndpoint.Builder(sourcePipeline).recvonly().build();

        //代替 this.passThrough = kms.getKurentoClient().getById(passThruId, PassThrough.class);
        PublisherEndpoint publisherEndpoint = participant.getPublisher();
        if (publisherEndpoint == null) {
            log.warn("PARTICIPANT {},{}, unpublisher", participant.getUuid(), participant.getParticipantPublicId());
            return;
        }
        createRtcChnAndSwitchIces(dispatcherEndpoint, dispatcherOutEndPoint, publisherEndpoint.getPassThru());

        dispatcherMap.put(participant.getPublisherStreamId(), new DispatcherEndpoint(dispatcherEndpoint,dispatcherOutEndPoint,passThrough));
        participant.deliveryKmsManagers.put(this.getId(), this);
    }

    public void createRtcChnAndSwitchIces(WebRtcEndpoint dispatcherInEndPoint, WebRtcEndpoint publisherEndpoint, PassThrough passThru) {
        String sdpOffer = dispatcherInEndPoint.generateOffer();

        log.info("add distributionEp OnIceCandidateListener.");
        dispatcherInEndPoint.addOnIceCandidateListener(event -> {
            IceCandidate candidate = event.getCandidate();
            log.info("dispatcherEndPoint OnIceCandidate:{}", JSON.toJSON(candidate));
            publisherEndpoint.addIceCandidate(candidate, new Continuation<Void>() {

                @Override
                public void onSuccess(Void result) throws Exception {
                    log.info("sourceSubEndPoint EP {}: Ice candidate added to the internal endpoint", publisherEndpoint.getId());
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    log.warn("sourceSubEndPoint EP {}: Failed to add ice candidate to the internal endpoint", publisherEndpoint.getId());
                }
            });
        });


        // set recordRecvWebRtcEndPoint IceCandidate Event Listener
        log.info("add dispatcherEndPoint IceComponentStateChangeListener.");
        dispatcherInEndPoint.addIceComponentStateChangeListener(event -> {
            String msg = "KMS event [IceComponentStateChange]: -> endpoint: " + dispatcherInEndPoint.getId()
                    + " (subscriber) | state: " + event.getState().name() + " | componentId: "
                    + event.getComponentId() + " | streamId: " + event.getStreamId() + " | timestamp: "
                    + event.getTimestampMillis();
            log.info(msg);
        });

        // set sourceSubEndPoint IceCandidate Event Listener
        log.info("add publisherEndpoint OnIceCandidateListener.");
        publisherEndpoint.addOnIceCandidateListener(event -> {
            IceCandidate candidate = event.getCandidate();
            log.info("publisherEndpoint iceCandidate:{}", JSON.toJSON(candidate));
            dispatcherInEndPoint.addIceCandidate(candidate, new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) throws Exception {
                    log.info("dispatcherEndPoint EP {}: Ice candidate added to the internal endpoint", dispatcherInEndPoint.getId());
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    log.warn("dispatcherEndPoint EP {}: Failed to add ice candidate to the internal endpoint", dispatcherInEndPoint.getId());
                }
            });
        });

        String sdpAnswer = publisherEndpoint.processOffer(sdpOffer);
        log.debug("sourceSubEndPoint sdpAnswer:" + sdpAnswer);

        publisherEndpoint.gatherCandidates(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.info("sourceSubEndPoint EP {}: Internal endpoint started to gather candidates", publisherEndpoint.getId());
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("sourceSubEndPoint EP {}: Internal endpoint failed to start gathering candidates", publisherEndpoint.getId(), cause);
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
                log.warn("Failed to connect media elements (source {} -> sink {})", passThru.getId(), publisherEndpoint.getId(), cause);
            }
        });

        // recordRecvWebRtcEndPoint process sdpAnswer
        dispatcherInEndPoint.processAnswer(sdpAnswer);
        dispatcherInEndPoint.gatherCandidates(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.info("recordRecvWebRtcEndPoint EP {}: Internal endpoint started to gather candidates", dispatcherInEndPoint.getId());
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("recordRecvWebRtcEndPoint EP {}: Internal endpoint failed to start gathering candidates", dispatcherInEndPoint.getId(), cause);
            }
        });

    }
}
