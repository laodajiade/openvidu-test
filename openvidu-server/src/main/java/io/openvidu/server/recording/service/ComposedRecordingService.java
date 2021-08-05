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

package io.openvidu.server.recording.service;

import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.java.client.RecordingProperties;
import io.openvidu.server.cdr.CallDetailRecord;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.EndpointTypeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.recording.CompositeWrapper;
import io.openvidu.server.recording.Recording;
import io.openvidu.server.recording.RecordingDownloader;
import org.kurento.client.internal.server.KurentoServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class ComposedRecordingService extends RecordingService {
	private static final Logger log = LoggerFactory.getLogger(ComposedRecordingService.class);

	private Map<String, CompositeWrapper> composites = new ConcurrentHashMap<>();

	ComposedRecordingService(RecordingManager recordingManager, RecordingDownloader recordingDownloader,
							 OpenviduConfig openviduConfig, CallDetailRecord cdr) {
		super(recordingManager, recordingDownloader, openviduConfig, cdr);
	}

	@Override
	public Recording startRecording(Session session, RecordingProperties properties) throws OpenViduException {
		PropertiesRecordingId updatePropertiesAndRecordingId = this.setFinalRecordingNameAndGetFreeRecordingId(session,
				properties);
		properties = updatePropertiesAndRecordingId.properties;
		String recordingId = updatePropertiesAndRecordingId.recordingId;

		// Instantiate and store recording object
		Recording recording = new Recording(session.getSessionId(), recordingId, properties);
		this.recordingManager.startingRecordings.put(recording.getId(), recording);

		// Kurento composite used
		return this.startRecordingWithVideo(session, recording, properties);
	}

	@Override
	public Recording stopRecording(Session session, Recording recording, EndReason reason) {
//		recording = this.sealRecordingMetadataFileAsStopped(recording);
		return this.stopRecordingWithVideo(session, recording, reason, 0);
	}

	Recording stopRecording(Session session, Recording recording, EndReason reason, long kmsDisconnectionTime) {
		return this.stopRecordingWithVideo(session, recording, reason, kmsDisconnectionTime);
	}

	void joinPublisherEndpointToComposite(Session session, Participant participant)
			throws OpenViduException {
		log.info("Joining single stream {} to Composite in session {}", participant.getPublisherStreamId(),
				session.getSessionId());

		KurentoParticipant kurentoParticipant = (KurentoParticipant) participant;
		CompositeWrapper compositeWrapper = this.composites.get(session.getSessionId());

		try {
			compositeWrapper.connectPublisherEndpoint(kurentoParticipant.getPublisher(), EndpointTypeEnum.recording);
			// Multiplex MCU layout
			session.dealParticipantDefaultOrder(kurentoParticipant, EndpointTypeEnum.recording);
		} catch (OpenViduException e) {
			if (Code.RECORDING_START_ERROR_CODE.getValue() == e.getCodeValue()) {
				// First user publishing triggered RecorderEnpoint start, but it failed
				throw e;
			}
		}
	}

	void removePublisherEndpointFromComposite(String sessionId, String streamId) {
		CompositeWrapper compositeWrapper = this.composites.get(sessionId);
		compositeWrapper.disconnectPublisherEndpoint(streamId);
		if (compositeWrapper.getHubPorts().isEmpty() || compositeWrapper.getPublisherEndpoints().isEmpty()) {
			log.warn("THERE IS NO MORE hubPorts and publisherEndPoints in '{}' and composite id :{}",
					sessionId, compositeWrapper.getComposite().getId());
		}
	}

	private Recording startRecordingWithVideo(Session session, Recording recording, RecordingProperties properties)
			throws OpenViduException {

		log.info("Starting composed video recording {} of session {}", recording.getId(),
				recording.getSessionId());

		CompositeWrapper compositeWrapper = null;
		try {
			compositeWrapper = new CompositeWrapper((KurentoSession) session,
					"file://" + this.openviduConfig.getOpenViduRecordingPath() + recording.getId() + "/" + properties.name()
							+ ".ts", EndpointTypeEnum.recording);
		} catch (KurentoServerException e) {
			log.error("Error create Recorder CompositeWrapper in session {}", session.getSessionId());
			throw this.failStartRecording(session, recording, e.getMessage());
		}
		this.composites.put(session.getSessionId(), compositeWrapper);

		for (Participant p : session.getParticipants()) {
			if (p.isStreaming(StreamType.MAJOR)) {
				try {
					this.joinPublisherEndpointToComposite(session, p);
				} catch (OpenViduException e) {
					log.error("Error waiting for RecorderEndpoint of Composite to start in session {}",
							session.getSessionId());
					throw this.failStartRecording(session, recording, e.getMessage());
				}
			}
		}

//		this.generateRecordingMetadataFile(recording);
		return recording;
	}

	private Recording stopRecordingWithVideo(Session session, Recording recording, EndReason reason,
											 long kmsDisconnectionTime) {

		log.info("Stopping composed video recording {} of session {}. Reason: {}", recording.getId(),
				recording.getSessionId(), reason);

		String sessionId;
		if (session == null) {
			log.warn(
					"Existing recording {} does not have an active session associated. This means the recording "
							+ "has been automatically stopped after last user left and {} seconds timeout passed",
					recording.getId(), this.openviduConfig.getOpenviduRecordingAutostopTimeout());
			sessionId = recording.getSessionId();
		} else {
			sessionId = session.getSessionId();
		}

		CompositeWrapper compositeWrapper = this.composites.remove(sessionId);
		if (null != compositeWrapper) {
			final CountDownLatch stoppedCountDown = new CountDownLatch(1);
			compositeWrapper.stopCompositeRecording(stoppedCountDown, kmsDisconnectionTime);

			try {
				if (!stoppedCountDown.await(5, TimeUnit.SECONDS)) {
					recording.setStatus(io.openvidu.java.client.Recording.Status.failed);
					log.error("Error waiting for RecorderEndpoint of Composite to stop in session {}",
							recording.getSessionId());
				}
			} catch (InterruptedException e) {
				recording.setStatus(io.openvidu.java.client.Recording.Status.failed);
				log.error("Exception while waiting for state change", e);
			}

			compositeWrapper.disconnectAllPublisherEndpoints();
		}
		this.cleanRecordingMaps(session, recording);
		return recording;

		/*// TODO: DOWNLOAD FILE IF SCALABILITY MODE
		final Recording[] finalRecordingArray = new Recording[1];
		finalRecordingArray[0] = recording;
		try {
			this.recordingDownloader.downloadRecording(finalRecordingArray[0], null, () -> {
				String filesPath = this.openviduConfig.getOpenViduRecordingPath() + finalRecordingArray[0].getId()
						+ "/";
				File videoFile = new File(filesPath + finalRecordingArray[0].getName() + ".webm");
				long finalSize = videoFile.length();
				double finalDuration = (double) compositeWrapper.getDuration() / 1000;
				this.updateFilePermissions(filesPath);
				finalRecordingArray[0] = this.sealRecordingMetadataFileAsReady(finalRecordingArray[0], finalSize,
						finalDuration,
						filesPath + RecordingManager.RECORDING_ENTITY_FILE + finalRecordingArray[0].getId());

				final long timestamp = System.currentTimeMillis();
				cdr.recordRecordingStatusChanged(finalRecordingArray[0], reason, timestamp,
						finalRecordingArray[0].getStatus());
			});
		} catch (IOException e) {
			log.error("Error while downloading recording {}: {}", finalRecordingArray[0].getName(), e.getMessage());
		}

		if (reason != null && session != null) {
			this.recordingManager.sessionHandler.sendRecordingStoppedNotification(session, finalRecordingArray[0],
					reason);
		}

		return finalRecordingArray[0];*/
	}


}
