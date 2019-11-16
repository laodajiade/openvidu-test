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

import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.openvidu.server.common.enums.StreamModeEnum;
import org.kurento.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.kurento.core.KurentoParticipant;

/**
 * Subscriber aspect of the {@link MediaEndpoint}.
 *
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class SubscriberEndpoint extends MediaEndpoint {
	private final static Logger log = LoggerFactory.getLogger(SubscriberEndpoint.class);

	private AtomicBoolean connectedToPublisher = new AtomicBoolean(false);

	private HubPort majorHubPortOut = null;
	private ListenerSubscription majorHubPortOutSubscription = null;

	private HubPort majorShareHubPortOut = null;
	private ListenerSubscription majorShareHubPortOutSubscription = null;

	private PublisherEndpoint publisher = null;

	public SubscriberEndpoint(boolean web, KurentoParticipant owner, String endpointName, MediaPipeline pipeline,
							  OpenviduConfig openviduConfig) {
		super(web, owner, endpointName, pipeline, openviduConfig, log);

		majorHubPortOut = new HubPort.Builder(getMajorComposite()).build();
		majorHubPortOutSubscription = registerElemErrListener(majorHubPortOut);

		majorShareHubPortOut = new HubPort.Builder(getMajorShareComposite()).build();
		majorShareHubPortOutSubscription = registerElemErrListener(majorShareHubPortOut);
	}

	public synchronized String subscribe(String sdpOffer, PublisherEndpoint publisher, StreamModeEnum streamMode) {
		registerOnIceCandidateEventListener(publisher.getOwner().getParticipantPublicId());
		String sdpAnswer = processOffer(sdpOffer);
		gatherCandidates();
		if (Objects.equals(StreamModeEnum.SFU_SHARING, streamMode)) {
			publisher.connect(this.getEndpoint());
		} else {
			publisher.checkInnerConnect();
			switch (streamMode) {
				case MIX_MAJOR:
					internalSinkConnect(majorHubPortOut, this.getEndpoint());
					break;
				case MIX_MAJOR_AND_SHARING:
					internalSinkConnect(majorShareHubPortOut, this.getEndpoint());
					break;
			}
		}

		setConnectedToPublisher(true);
		setPublisher(publisher);
		this.createdAt = System.currentTimeMillis();
		return sdpAnswer;
	}

	private void internalSinkConnect(final MediaElement source, final MediaElement sink) {
		source.connect(sink, new Continuation<Void>() {
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

	public boolean isConnectedToPublisher() {
		return connectedToPublisher.get();
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
}
