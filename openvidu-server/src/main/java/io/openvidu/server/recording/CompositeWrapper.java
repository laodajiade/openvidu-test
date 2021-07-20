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

package io.openvidu.server.recording;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.server.common.enums.ConferenceModeEnum;
import io.openvidu.server.common.enums.RecordState;
import io.openvidu.server.core.EndpointTypeEnum;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.kurento.endpoint.PublisherEndpoint;
import org.kurento.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CompositeWrapper {

	private static final Logger log = LoggerFactory.getLogger(CompositeWrapper.class);

	private KurentoSession session;
	private Composite composite;
	private RecorderEndpoint recorderEndpoint;
	private HubPort compositeToRecorderHubPort;
	private Map<String, HubPort> hubPorts = new ConcurrentHashMap<>();
	private Map<String, PublisherEndpoint> publisherEndpoints = new ConcurrentHashMap<>();

	private AtomicBoolean isRecording = new AtomicBoolean(false);
	private long startTime;
	private long endTime;

	private AtomicBoolean isLiving = new AtomicBoolean(false);
	private LiveEndpoint liveEndpoint;
	private HubPort compositeToLiveHubPort;

	public CompositeWrapper(KurentoSession session, String path, EndpointTypeEnum typeEnum) {
		this.session = session;
		this.composite = session.getConferenceMode().equals(ConferenceModeEnum.SFU) ?
				new Composite.Builder(session.getPipeline()).build() : session.compositeService.getComposite();

		switch (typeEnum) {
			case recording:
				this.recorderEndpoint = new RecorderEndpoint.Builder(composite.getMediaPipeline(), path)
						.withMediaProfile(MediaProfileSpecType.TS).build();
				this.compositeToRecorderHubPort = new HubPort.Builder(composite).build();
				this.compositeToRecorderHubPort.connect(recorderEndpoint);
				break;
			case living:
				this.liveEndpoint = new LiveEndpoint.Builder(composite.getMediaPipeline(), path).build();
				this.compositeToLiveHubPort = new HubPort.Builder(composite).build();
				this.compositeToLiveHubPort.connect(liveEndpoint);
				break;
			default:
				log.info("Unknown EndpointTypeEnum:{} when init CompositeWrapper", typeEnum);
		}
	}

	public synchronized void startCompositeRecording(CountDownLatch startLatch) {
		this.recorderEndpoint.addRecordingListener(event -> {
			startTime = Long.parseLong(event.getTimestampMillis());
			log.info("Recording started event for video RecorderEndpoint of Composite in session {}",
					session.getSessionId());
			startLatch.countDown();

			// save conf record into DB
			session.getRecordingManager().getConferenceRecordManage().dealConfRecordEvent(session, event);
		});

		this.recorderEndpoint.addStoppedListener(event -> {
			endTime = Long.parseLong(event.getTimestampMillis());
			log.info("Recording stopped event for video RecorderEndpoint of Composite in session {}",
					session.getSessionId());

			// save conf record into DB
			session.getRecordingManager().getConferenceRecordManage().dealConfRecordEvent(session, event);
		});

		this.recorderEndpoint.addErrorListener(event -> log.error(event.getErrorCode() + " " + event.getDescription()));

		this.recorderEndpoint.record();
	}

	public synchronized void stopCompositeRecording(CountDownLatch stopLatch, Long timeOfKmsDisconnection) {
		if (timeOfKmsDisconnection == 0) {
			this.recorderEndpoint.addStoppedListener(event -> {
				List<Tag> tags = event.getTags();
				if (!CollectionUtils.isEmpty(tags)) {
					String state = tags.stream().filter(tag -> "recordState".equals(tag.getKey()))
							.findAny().map(Tag::getValue).orElse("3");
					RecordState recordState = RecordState.parseRecState(Integer.valueOf(state));
					log.info("Recording stopped event for video RecorderEndpoint of Composite in session {} and record state:{}",
							session.getSessionId(), recordState.name());
					if (RecordState.RECORD_STOP.equals(recordState) || RecordState.RECORD_EXCEPTION.equals(recordState)) {
						log.info("Release remote object resource in kurento media server.");
						recorderEndpoint.release();
						compositeToRecorderHubPort.release();
						stopLatch.countDown();
					}
				}
			});
			this.recorderEndpoint.stop();
		} else {
			endTime = timeOfKmsDisconnection;
			stopLatch.countDown();
			log.warn("Forcing composed video recording stop after KMS restart in session {}",
					this.session.getSessionId());
		}

	}

	public synchronized void startCompositeLiving() {
		this.liveEndpoint.startLive();
	}

	public synchronized void stopCompositeLiving() {
		this.liveEndpoint.stopLive();
	}

	public void connectPublisherEndpoint(PublisherEndpoint endpoint, EndpointTypeEnum typeEnum) throws OpenViduException {
		HubPort hubPort;
		if (session.getConferenceMode().equals(ConferenceModeEnum.MCU)) {
			hubPort = endpoint.getMajorShareHubPort();
		} else {
			if (Objects.equals(typeEnum, EndpointTypeEnum.recording)) {
				hubPort = endpoint.createRecordHubPort(composite);
			} else {
				hubPort = endpoint.createLiveHubPort(composite);
			}
			endpoint.connectRecordHubPort(hubPort);
		}
		String streamId = endpoint.getOwner().getPublisherStreamId();
		this.hubPorts.put(streamId, hubPort);
		this.publisherEndpoints.put(streamId, endpoint);

		switch (typeEnum) {
			case recording:
				if (isRecording.compareAndSet(false, true)) {
					log.info("First stream ({}) joined to Composite in session {}. Starting RecorderEndpoint for Composite",
							streamId, session.getSessionId());
					final CountDownLatch startLatch = new CountDownLatch(1);
					this.startCompositeRecording(startLatch);
					try {
						if (!startLatch.await(5, TimeUnit.SECONDS)) {
							log.error("Error waiting for RecorderEndpoint of Composite to start in session {}",
									session.getSessionId());
							throw new OpenViduException(Code.RECORDING_START_ERROR_CODE,
									"Couldn't initialize RecorderEndpoint of Composite");
						}
						log.info("RecorderEnpoint of Composite is now recording for session {}", session.getSessionId());
					} catch (InterruptedException e) {
						log.error("Exception while waiting for state change", e);
					}
				}
				break;
			case living:
				if (isLiving.compareAndSet(false, true)) {
					log.info("First stream ({}) joined to Composite in session {}. Starting LiveEndpoint for Composite",
							streamId, session.getSessionId());
					this.startCompositeLiving();
				}
				break;
			default:
				log.info("Unknown EndpointTypeEnum:{} when connectPublisherEndpoint", typeEnum);
		}

		log.info("Composite for session {} has now {} connected publishers", this.session.getSessionId(),
				this.composite.getChildren().size() - 1);
	}

	public void disconnectPublisherEndpoint(String streamId) {
		HubPort hubPort = this.hubPorts.remove(streamId);
		PublisherEndpoint publisherEndpoint = this.publisherEndpoints.remove(streamId);
		if (Objects.nonNull(hubPort)) {
			publisherEndpoint.disconnectRecordHubPort(hubPort);
			hubPort.release();
		}
		log.info("Composite for session {} has now {} connected publishers", this.session.getSessionId(),
				this.composite.getChildren().size() - 1);
	}

	public void disconnectAllPublisherEndpoints() {
		log.info("disconnecting all publisher endpoints...");
		if (Objects.equals(session.getConferenceMode(), ConferenceModeEnum.SFU)) {
			this.publisherEndpoints.keySet().forEach(streamId -> {
				PublisherEndpoint endpoint = this.publisherEndpoints.get(streamId);
				HubPort hubPort = this.hubPorts.get(streamId);
				endpoint.disconnectRecordHubPort(hubPort);
				hubPort.release();
			});

			this.composite.release();
		}
		this.publisherEndpoints.clear();
		this.hubPorts.clear();
	}

	public long getDuration() {
		return this.endTime - this.startTime;
	}

	public Map<String, PublisherEndpoint> getPublisherEndpoints() {
		return publisherEndpoints;
	}

	public Map<String, HubPort> getHubPorts() {
		return hubPorts;
	}

	public Composite getComposite() {
		return composite;
	}

	public String getLiveEndPointUri() {
		return this.liveEndpoint.getUri();
	}
}
