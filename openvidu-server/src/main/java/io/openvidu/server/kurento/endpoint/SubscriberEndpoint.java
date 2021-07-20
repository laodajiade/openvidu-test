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
import io.openvidu.server.common.enums.StreamModeEnum;
import io.openvidu.server.common.enums.VoiceMode;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.kurento.core.CompositeService;
import io.openvidu.server.kurento.core.KurentoParticipant;
import org.kurento.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map.Entry;
import java.util.Objects;
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

	public SubscriberEndpoint(boolean web, KurentoParticipant owner, String endpointName, MediaPipeline pipeline,
							  CompositeService compositeService, OpenviduConfig openviduConfig) {
		super(web, owner, endpointName, pipeline, openviduConfig, log);
		this.setCompositeService(compositeService);
	}

	public synchronized String subscribeVideo(String sdpOffer, PublisherEndpoint publisher, StreamModeEnum streamMode) {
		registerOnIceCandidateEventListener(Objects.equals(StreamModeEnum.MIX_MAJOR_AND_SHARING, streamMode) ?
				getCompositeService().getMixStreamId() : publisher.getOwner().getUuid());

		String sdpAnswer = processOffer(sdpOffer);
		gatherCandidates();
		if (Objects.equals(StreamModeEnum.MIX_MAJOR_AND_SHARING, streamMode)) {
			internalSinkConnect(getCompositeService().getHubPortOut(), this.getEndpoint(), MediaType.VIDEO);
		} else {
			publisher.connect(this.getEndpoint());
		}

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

	private void internalSinkConnect(final MediaElement source, final MediaElement sink, MediaType mediaType) {
		source.connect(sink, mediaType, new Continuation<Void>() {
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
		switch (voiceMode) {
			case on:
				publisher.sfuDisconnectFrom(this.getEndpoint(), mediaType);
				break;
			case off:
				publisher.connect(this.getEndpoint(), mediaType);
				break;
		}
	}
}
