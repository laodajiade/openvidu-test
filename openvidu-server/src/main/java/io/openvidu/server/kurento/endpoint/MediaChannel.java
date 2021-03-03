package io.openvidu.server.kurento.endpoint;

import com.alibaba.fastjson.JSON;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.kurento.core.KurentoParticipant;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.kurento.client.*;

import java.text.MessageFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * -----------------------------------------------------------------------------------------------------------------------
 * |  kms1.pipeline                                     |         |    kms2.pipeline                                     |
 * |                          |<----------              mediaChannel             ---------->|                            |
 * |  client1 -> publisher -> passThrough -> subscriber |  -----> | publisher -> passThrough -> subscriber  -> client2   |
 * |                                                    |         |                                                      |
 * |                                                    |         |                                                      |
 * -----------------------------------------------------------------------------------------------------------------------
 */
@Slf4j
public class MediaChannel {

    @Getter
    private final String id;

    @Getter
    private final String mediaChannelName;

    @Getter
    private final String senderEndpointName;

    @Getter
    private final long createAt;

    /**
     * kms1.pipeline，主媒体服务器的pipeline
     */
    @Getter
    public MediaPipeline sourcePipeline;

    /**
     * kms2.pipeline，分发媒体服务器的pipeline
     */
    @Getter
    public MediaPipeline targetPipeline;

    public PassThrough sourcePassThrough;

    public WebRtcEndpoint subscriber;

    @Getter
    public PublisherEndpoint publisher;

    // 0 = 未准备  1=正在准备  2=准备完毕
    public int state = 0;


    private MediaChannel(MediaPipeline sourcePipeline, PassThrough sourcePassThrough, MediaPipeline targetPipeline, String senderEndpointName) {
        this.sourcePipeline = sourcePipeline;
        this.sourcePassThrough = sourcePassThrough;
        this.targetPipeline = targetPipeline;
        this.senderEndpointName = senderEndpointName;
        this.createAt = System.currentTimeMillis();

        this.mediaChannelName = "channel_" + senderEndpointName + sourcePipeline.getId() + " to " + targetPipeline.getId();
        this.id = senderEndpointName + "_" + RandomStringUtils.randomAlphabetic(6);
    }

    public MediaChannel(MediaPipeline sourcePipeline, PassThrough sourcePassThrough, MediaPipeline targetPipeline,
                        boolean web, KurentoParticipant owner, String endpointName, OpenviduConfig openviduConfig) {
        this(sourcePipeline, sourcePassThrough, targetPipeline, endpointName);

        this.publisher = new PublisherEndpoint(web, owner, endpointName, targetPipeline, openviduConfig);

        this.subscriber = new WebRtcEndpoint.Builder(sourcePipeline).build();
        log.info("mediaChannel create id {}", id);
    }

    public void createChannel() {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        publisher.internalEndpointInitialization(countDownLatch);
        try {
            if (countDownLatch.await(10, TimeUnit.SECONDS)) {
                createRtcChnAndSwitchIces(publisher.getWebEndpoint(), subscriber, sourcePassThrough);
                log.info("mediaChannel id {} createChannel", id);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 流的方向是 subscriber ->  publisher
     *
     * @param publisherEndpoint
     * @param subscriber
     * @param subscriberPassThru
     */
    private void createRtcChnAndSwitchIces(WebRtcEndpoint publisherEndpoint, WebRtcEndpoint subscriber, PassThrough subscriberPassThru) {
        String sdpOffer = publisherEndpoint.generateOffer();
        log.info("publisher create offer," + sdpOffer);
        publisherEndpoint.addOnIceCandidateListener(event -> {
            IceCandidate candidate = event.getCandidate();
            log.info("publisher iceCandidate:{}", JSON.toJSON(candidate));
            subscriber.addIceCandidate(candidate, new Continuation<Void>() {

                @Override
                public void onSuccess(Void result) throws Exception {
                    log.info("subscriber EP {}: Ice candidate added to the internal endpoint", subscriber.getId());
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    log.error("subscriber EP {}: Failed to add ice candidate to the internal endpoint", subscriber.getId());
                }
            });
        });


        log.info("add publisherEndpoint IceComponentStateChangeListener.");
        publisherEndpoint.addIceComponentStateChangeListener(event -> {
            String msg = "KMS event [IceComponentStateChange]: -> endpoint: " + publisherEndpoint.getId()
                    + " (publisherEndpoint) | state: " + event.getState().name() + " | componentId: "
                    + event.getComponentId() + " | streamId: " + event.getStreamId() + " | timestamp: "
                    + event.getTimestampMillis();
            log.info(msg);
        });


        this.addMediaFlowStateChangeListeners(publisherEndpoint, "publisher");


        // set sourceSubEndPoint IceCandidate Event Listener
        log.info("add sourceSubEndPoint OnIceCandidateListener.");
        subscriber.addOnIceCandidateListener(event -> {
            IceCandidate candidate = event.getCandidate();
            log.info("subscriber iceCandidate:{}", JSON.toJSON(candidate));
            publisherEndpoint.addIceCandidate(candidate, new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) throws Exception {
                    log.info("publisherEndpoint EP {}: Ice candidate added to the internal endpoint", publisherEndpoint.getId());
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    log.error("publisherEndpoint EP {}: Failed to add ice candidate to the internal endpoint", publisherEndpoint.getId());
                }
            });
        });

        this.addMediaFlowStateChangeListeners(subscriber, "subscriber");

        String sdpAnswer = subscriber.processOffer(sdpOffer);
        log.info("subscriber sdpAnswer:\n" + sdpAnswer);

        subscriber.gatherCandidates(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.info("subscriber EP {}: Internal endpoint started to gather candidates", subscriber.getId());
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.error("subscriber EP {}: Internal endpoint failed to start gathering candidates", subscriber.getId(), cause);
            }
        });


        // connect the passThru and sourceSubEndPoint
        subscriberPassThru.connect(subscriber, new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.info("Elements have been connected (source {} -> sink {})", subscriberPassThru.getId(), subscriber.getId());
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.error("Failed to connect media elements (source {} -> sink {})", subscriberPassThru.getId(), subscriber.getId(), cause);
            }
        });

        // recordRecvWebRtcEndPoint process sdpAnswer
        publisherEndpoint.processAnswer(sdpAnswer);
        publisherEndpoint.gatherCandidates(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.info("publisherEndpoint EP {}: Internal endpoint started to gather candidates", publisherEndpoint.getId());
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.error("publisherEndpoint EP {}: Internal endpoint failed to start gathering candidates", publisherEndpoint.getId(), cause);
            }
        });

    }

    private void addMediaFlowStateChangeListeners(WebRtcEndpoint endpoint, String typeOfEndpoint) {
        endpoint.addMediaFlowInStateChangeListener(event -> {
            String msg = "KMS event [MediaFlowInStateChange] -> endpoint: " + endpoint.getId() + " ("
                    + typeOfEndpoint + ") | state: " + event.getState() + " | pad: " + event.getPadName()
                    + " | mediaType: " + event.getMediaType() + " | timestamp: " + event.getTimestampMillis();
            log.info(msg);
        });

        endpoint.addMediaFlowOutStateChangeListener(event -> {
            String msg = "KMS event [MediaFlowOutStateChange] -> endpoint: " + endpoint.getId() + " ("
                    + typeOfEndpoint + ") | state: " + event.getState() + " | pad: " + event.getPadName()
                    + " | mediaType: " + event.getMediaType() + " | timestamp: " + event.getTimestampMillis();
            log.info(msg);
        });
    }

    @Override
    public String toString() {
        return "MediaChannel " + this.id;
    }
}
