/*
 * (C) Copyright 2017-2019 OpenVidu (https://openvidu.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.openvidu.server.kurento.endpoint;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.StreamModeEnum;
import io.openvidu.server.common.enums.VoiceMode;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.UseTime;
import io.openvidu.server.kurento.core.CompositeService;
import io.openvidu.server.kurento.core.KurentoParticipant;
import lombok.Getter;
import lombok.Setter;
import org.kurento.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Subscriber aspect of the {@link MediaEndpoint}.
 *
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class SubscriberEndpoint extends MediaEndpoint {
    private final static Logger log = LoggerFactory.getLogger(SubscriberEndpoint.class);

    private AtomicBoolean connectedToPublisher = new AtomicBoolean(false);

    private PublisherEndpoint publisher = null;

    @Getter
    @Setter
    private HubPort mixHubPort = null;

    @Getter
    @Setter
    private HubPort pubHubPort = null;

    public SubscriberEndpoint(boolean web, KurentoParticipant owner, String endpointName, MediaPipeline pipeline,
                              CompositeService compositeService, OpenviduConfig openviduConfig) {
        super(web, owner, endpointName, pipeline, openviduConfig, log);
        this.setCompositeService(compositeService);
    }

    public SdpEndpoint createEndpoint(String msTraceId, CountDownLatch endpointLatch) {
        addProperties(msTraceId);
        return createEndpoint(endpointLatch);
    }

    public void addProperties(String msTraceId) {
        epProperties.add("traceId", msTraceId);
        epProperties.add("createAt", String.valueOf(System.currentTimeMillis()));
        epProperties.add("roomId", getOwner().getSessionId());
        epProperties.add("trackDirect", OpenViduRole.SUBSCRIBER.name());

        epProperties.add("userUuid", getOwner().getUuid());
        epProperties.add("userName", getOwner().getUsername());
        epProperties.add("userPhone", "");
    }

    public synchronized String subscribeVideo(String sdpOffer, PublisherEndpoint publisher, StreamModeEnum streamMode) {
        registerOnIceCandidateEventListener(Objects.equals(StreamModeEnum.MIX_MAJOR, streamMode) ?
                getCompositeService().getMixStreamId() : publisher.getOwner().getUuid());

        UseTime.point("processOffer start");
        String sdpAnswer = processOffer(sdpOffer);
        UseTime.point("processOffer end");
        // gatherCandidates();
        UseTime.point("connect start");
        if (Objects.equals(StreamModeEnum.MIX_MAJOR, streamMode)) {
            getCompositeService().sinkConnect(this);
            //internalSinkConnect(getCompositeService().getHubPortOut(), this.getEndpoint());
        } else {
            publisher.connect(this.getEndpoint());
        }
        UseTime.point("connect end");

        setConnectedToPublisher(true);
        setPublisher(publisher);
        this.createdAt = System.currentTimeMillis();
        return sdpAnswer;
    }

    public synchronized String subscribeAudio(PublisherEndpoint publisher) {
        if (Objects.isNull(publisher)) {
            log.info("web subscribe all audio mix output. but it is not input.");
            internalSinkConnect(getCompositeService().getHubPortOut(), this.getEndpoint(), MediaType.AUDIO);
        } else {
            publisher.connectAudioOut(this.getEndpoint());
        }

        return "";
    }

    private void internalSinkConnect(final MediaElement source, final MediaElement sink) {
        source.connect(sink, new Continuation<Void>() {
            //source.connect(sink, mediaType, new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.info("SUB_EP {}: Elements have been connected (source {} -> sink {})", getEndpointName(),
                        source.getId(), sink.getId());
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("SUB_EP {}: Failed to connect media elements (source {} -> sink {})", getEndpointName(),
                        source.getId(), sink.getId(), cause);
            }
        });
    }

    private void internalSinkConnect(final MediaElement source, final MediaElement sink, MediaType mediaType) {
        source.connect(sink, mediaType, new Continuation<Void>() {
            //source.connect(sink, mediaType, new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.debug("SUB_EP {}: Elements have been connected (source {} -> sink {})", getEndpointName(),
                        source.getId(), sink.getId());
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("SUB_EP {}: Failed to connect media elements (source {} -> sink {})", getEndpointName(),
                        source.getId(), sink.getId(), cause);
            }
        });
    }

    public void setConnectedToPublisher(boolean connectedToPublisher) {
        this.connectedToPublisher.set(connectedToPublisher);
    }

    public void setPublisher(PublisherEndpoint publisher) {
        this.publisher = publisher;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        try {
            json.addProperty("pipeline", this.getPipeline().getId());
            json.addProperty("streamId", this.publisher.getStreamId());
        } catch (NullPointerException ex) {
            json.addProperty("streamId", "NOT_FOUND");
        }
        return json;
    }

    @Override
    public JsonObject withStatsToJson() {
        JsonObject json = super.withStatsToJson();
        JsonObject toJson = this.toJson();
        for (Entry<String, JsonElement> entry : toJson.entrySet()) {
            json.add(entry.getKey(), entry.getValue());
        }
        return json;
    }

    public synchronized void controlMediaTypeLink(MediaType mediaType, VoiceMode voiceMode) {
        try {
            if (publisher == null && this.getMixHubPort() != null) {//MCU
                log.info("MCU  voiceMode {}",voiceMode.name());
                switch (voiceMode) {
                    case on:
                        this.getMixHubPort().disconnect(this.getEndpoint(), mediaType);
                        break;
                    case off:
                        this.getMixHubPort().connect(this.getEndpoint(), mediaType);
                        break;
                }
            } else {//SFU
                log.info("SFU  voiceMode {}",voiceMode.name());
                switch (voiceMode) {
                    case on:
                        publisher.sfuDisconnectFrom(this.getEndpoint(), mediaType);
                        break;
                    case off:
                        publisher.connect(this.getEndpoint(), mediaType);
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("controlMediaTypeLink Error mediaType :{}  voiceMode :{}",mediaType.name(),voiceMode.name());
        }


    }

    public static void main(String[] args) {
        String aa= null;
        VoiceMode voiceMode = VoiceMode.valueOf("off");
//        VoiceMode voiceMode =VoiceMode.on;
        switch (voiceMode){
            case on:
                System.out.println("aa");
                break;
            case off:
                System.out.println("bb");
                break;
            default:
                System.out.println("ddd");
        }
    }
}
