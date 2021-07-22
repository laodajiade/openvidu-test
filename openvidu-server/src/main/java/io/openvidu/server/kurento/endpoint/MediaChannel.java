package io.openvidu.server.kurento.endpoint;

import com.alibaba.fastjson.JSON;
import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.server.common.enums.MediaChannelStateEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.kurento.core.DeliveryKmsManager;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.kurento.core.KurentoSession;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.kurento.client.*;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * -----------------------------------------------------------------------------------------------------------------------
 * |  kms1.pipeline                                     |         |    kms2.pipeline                                     |
 * |                                         |<----------            [MediaChannel]           ---------->|               |
 * |  client1 -> publisher -> passThrough -> subscriber |  -----> | publisher -> passThrough -> subscriber  -> client2   |
 * |                                                    |         |                                                      |
 * -----------------------------------------------------------------------------------------------------------------------
 */
@Slf4j
public class MediaChannel {
    private static final int PASS_LATCH_TIMEOUT = 15;
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
    private final MediaPipeline sourcePipeline;

    /**
     * kms2.pipeline，分发媒体服务器的pipeline
     */
    @Getter
    private final MediaPipeline targetPipeline;

    private final PassThrough sourcePassThrough;

    private WebRtcEndpoint subscriber;

    @Getter
    private PublisherEndpoint publisher;

    /**
     * 状态，READ和FLOWING是可用状态
     */
    @Getter
    private MediaChannelStateEnum state;

    private final DeliveryKmsManager deliveryKmsManager;

    private  CountDownLatch passLatch = new CountDownLatch(1);


    private MediaChannel(MediaPipeline sourcePipeline, PassThrough sourcePassThrough, MediaPipeline targetPipeline, String senderEndpointName,
                         DeliveryKmsManager deliveryKmsManager) {
        this.sourcePipeline = sourcePipeline;
        this.sourcePassThrough = sourcePassThrough;
        this.targetPipeline = targetPipeline;
        this.senderEndpointName = senderEndpointName;
        this.createAt = System.currentTimeMillis();
        this.deliveryKmsManager = deliveryKmsManager;
        this.mediaChannelName = "channel_" + senderEndpointName + sourcePipeline.getName() + " to " + targetPipeline.getName();
        this.id = senderEndpointName + "_" + RandomStringUtils.randomAlphabetic(6);
        state = MediaChannelStateEnum.INITIAL;
    }

    public MediaChannel(MediaPipeline sourcePipeline, PassThrough sourcePassThrough, MediaPipeline targetPipeline,
                        boolean web, KurentoParticipant publisherParticipant, String endpointName, OpenviduConfig openviduConfig,
                        DeliveryKmsManager deliveryKmsManager) {
        this(sourcePipeline, sourcePassThrough, targetPipeline, endpointName, deliveryKmsManager);

        this.publisher = new PublisherEndpoint(web, publisherParticipant, endpointName, targetPipeline, StreamType.MAJOR, openviduConfig);
        this.publisher.setStreamId(publisherParticipant.getPublisher(StreamType.MAJOR).getStreamId());
        this.publisher.setMediaOptions(publisherParticipant.getPublisherMediaOptions());
        this.publisher.createdAt = System.currentTimeMillis();

        this.subscriber = new WebRtcEndpoint.Builder(sourcePipeline).build();
        log.info("mediaChannel create id {}", id);
    }

    public void createChannel() {
        state = MediaChannelStateEnum.PREPARE;
        CountDownLatch countDownLatch = new CountDownLatch(1);
        publisher.internalEndpointInitialization(countDownLatch);
        try {
            if (countDownLatch.await(10, TimeUnit.SECONDS)) {
                createRtcChnAndSwitchIces(publisher.getWebEndpoint(), subscriber, sourcePassThrough);
                log.info("mediaChannel id {} createChannel", id);
            }
        } catch (InterruptedException e) {
            log.warn("createChannel timeout senderEndpointName = {}", senderEndpointName);
        }
    }

    /**
     * 流的方向是 subscriber ->  publisher
     */
    private void createRtcChnAndSwitchIces(WebRtcEndpoint publisherEndpoint, WebRtcEndpoint subscriber, PassThrough subscriberPassThru) {
        String sdpOffer = publisherEndpoint.generateOffer();
        log.debug("publisher create offer {}", sdpOffer);
        publisherEndpoint.addOnIceCandidateListener(event -> {
            IceCandidate candidate = event.getCandidate();
            log.info("publisher iceCandidate:{}", JSON.toJSON(candidate));
            subscriber.addIceCandidate(candidate, new Continuation<Void>() {

                @Override
                public void onSuccess(Void result) {
                    log.info("subscriber EP {}: Ice candidate added to the internal endpoint", subscriber.getId());
                }

                @Override
                public void onError(Throwable cause) {
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
                public void onSuccess(Void result) {
                    log.info("publisherEndpoint EP {}: Ice candidate added to the internal endpoint", publisherEndpoint.getId());
                }

                @Override
                public void onError(Throwable cause) {
                    log.error("publisherEndpoint EP {}: Failed to add ice candidate to the internal endpoint", publisherEndpoint.getId());
                }
            });
        });

        this.addMediaFlowStateChangeListeners(subscriber, "subscriber");

        String sdpAnswer = subscriber.processOffer(sdpOffer);
        log.info("subscriber sdpAnswer:\n" + sdpAnswer);

        subscriber.gatherCandidates(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) {
                log.info("subscriber EP {}: Internal endpoint started to gather candidates", subscriber.getId());
            }

            @Override
            public void onError(Throwable cause) {
                log.error("subscriber EP {}: Internal endpoint failed to start gathering candidates", subscriber.getId(), cause);
            }
        });


        // connect the passThru and sourceSubEndPoint
        subscriberPassThru.connect(subscriber, new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) {
                log.info("Elements have been connected (source {} -> sink {})", subscriberPassThru.getId(), subscriber.getId());
                state = MediaChannelStateEnum.READY;
                passLatch.countDown();
            }

            @Override
            public void onError(Throwable cause) {
                log.error("Failed to connect media elements (source {} -> sink {})", subscriberPassThru.getId(), subscriber.getId(), cause);
                state = MediaChannelStateEnum.FAILED;
                passLatch.countDown();
            }
        });

        publisherEndpoint.processAnswer(sdpAnswer);
        publisherEndpoint.gatherCandidates(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) {
                log.info("publisherEndpoint EP {}: Internal endpoint started to gather candidates", publisherEndpoint.getId());
            }

            @Override
            public void onError(Throwable cause) {
                log.error("publisherEndpoint EP {}: Internal endpoint failed to start gathering candidates", publisherEndpoint.getId(), cause);
            }
        });

    }

    private void addMediaFlowStateChangeListeners(WebRtcEndpoint endpoint, String typeOfEndpoint) {
        endpoint.addMediaFlowInStateChangeListener(event -> {
            String msg = "KMS event [MediaFlowInStateChange] -> endpoint: " + endpoint.getId() + " ("
                    + typeOfEndpoint + ") | state: " + event.getState() + " | pad: " + event.getPadName()
                    + " | mediaType: " + event.getMediaType() + " | timestamp: " + event.getTimestampMillis();
            if (event.getState() == MediaFlowState.FLOWING) {
                state = MediaChannelStateEnum.FLOWING;
                passLatch.countDown();
            }
            log.info(msg);
        });

        endpoint.addMediaFlowOutStateChangeListener(event -> {
            String msg = "KMS event [MediaFlowOutStateChange] -> endpoint: " + endpoint.getId() + " ("
                    + typeOfEndpoint + ") | state: " + event.getState() + " | pad: " + event.getPadName()
                    + " | mediaType: " + event.getMediaType() + " | timestamp: " + event.getTimestampMillis();
            if (event.getState() == MediaFlowState.FLOWING) {
                state = MediaChannelStateEnum.FLOWING;
                passLatch.countDown();
            }
            log.info(msg);
        });
    }

    @Override
    public String toString() {
        return "MediaChannel " + this.id;
    }

    public void release() {
        state = MediaChannelStateEnum.CLOSE;

        publisher.unregisterErrorListeners();
        releaseElement("publisher", publisher.getEndpoint());
        releaseElement("subscriber", subscriber);
        log.info("deliveryKmsManager.getDispatcherMap() size = {} before", this.deliveryKmsManager.getDispatcherMap().size());
        this.deliveryKmsManager.getDispatcherMap().remove(senderEndpointName);
        log.info("deliveryKmsManager.getDispatcherMap() size = {} after", this.deliveryKmsManager.getDispatcherMap().size());
    }


    private void releaseElement(final String typeName, final MediaElement element) {
        if (Objects.isNull(element)) return;
        final String eid = element.getId();
        try {
            element.release(new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) {
                    log.debug("mediaChannelName {}: Released successfully media element #{} for {}",
                            id, eid, typeName);
                }

                @Override
                public void onError(Throwable cause) {
                    log.warn("mediaChannelName {}: Could not release media element #{} for {}", id,
                            eid, typeName, cause);
                }
            });
        } catch (Exception e) {
            log.error("PARTICIPANT {}: Error calling release on elem #{} for {}", id, eid,
                    typeName, e);
        }
    }

    public MediaChannelStateEnum getStateSync() {
        try {
            if (!passLatch.await(PASS_LATCH_TIMEOUT, TimeUnit.SECONDS)) {
                throw new OpenViduException(OpenViduException.Code.MEDIA_ENDPOINT_ERROR_CODE,
                        "Timeout reached when creating media channel");
            }
        } catch (InterruptedException e) {
            throw new OpenViduException(OpenViduException.Code.MEDIA_ENDPOINT_ERROR_CODE,
                    "Interrupted when creating media channel: " + e.getMessage());
        }
        return state;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("kmsIp", deliveryKmsManager.getKms().getIp());
        json.addProperty("createAt", createAt);
        json.addProperty("sourcePipeline", sourcePipeline.getId());
        json.addProperty("targetPipeline", targetPipeline.getId());
        json.addProperty("state", state.name());
        json.add("publisher", publisher.toJson());
        return json;
    }

    public boolean waitToReady() {
        int tryCnt = 0;
        while (state.isInitStage()) {
            try {
                TimeUnit.MILLISECONDS.sleep(10);
                if (tryCnt++ > 300) {
                    return !state.isInitStage();
                }
            } catch (InterruptedException e) {
                return !state.isInitStage();
            }
        }
        return !state.isInitStage();
    }
}
