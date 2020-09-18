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

package io.openvidu.server.kurento.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.*;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.manage.RoomManage;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.*;
import io.openvidu.server.kurento.endpoint.KurentoFilter;
import io.openvidu.server.kurento.endpoint.PublisherEndpoint;
import io.openvidu.server.kurento.endpoint.SdpType;
import io.openvidu.server.kurento.kms.Kms;
import io.openvidu.server.kurento.kms.KmsManager;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcNotificationService;
import io.openvidu.server.utils.JsonUtils;
import org.kurento.client.GenericMediaElement;
import org.kurento.client.IceCandidate;
import org.kurento.client.ListenerSubscription;
import org.kurento.jsonrpc.Props;
import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;

public class KurentoSessionManager extends SessionManager {

	private static final Logger log = LoggerFactory.getLogger(KurentoSessionManager.class);

	@Autowired
	private KmsManager kmsManager;

	@Autowired
	private KurentoSessionEventsHandler kurentoSessionEventsHandler;

	@Autowired
	private KurentoParticipantEndpointConfig kurentoEndpointConfig;

	@Autowired
	protected RpcNotificationService rpcNotificationService;

	@Autowired
	protected OpenviduConfig openviduConfig;

	@Autowired
	private RoomManage roomManage;

	@Resource
	private ApplicationContext applicationContext;

	@Override
	public synchronized void joinRoom(Participant participant, String sessionId, Conference conference, Integer transactionId) {
		Set<Participant> existingParticipants = null;
		try {

			KurentoSession kSession = (KurentoSession) sessions.get(sessionId);

			if (kSession == null) {
				// First user connecting to the session
				Session sessionNotActive = sessionsNotActive.remove(sessionId);

				if (sessionNotActive == null && this.isInsecureParticipant(participant.getParticipantPrivateId())) {
					// Insecure user directly call joinRoom RPC method, without REST API use
					sessionNotActive = new Session(sessionId,
							new SessionProperties.Builder().mediaMode(MediaMode.ROUTED)
									.recordingMode(RecordingMode.ALWAYS)
									.defaultRecordingLayout(RecordingLayout.BEST_FIT).build(),
							openviduConfig, recordingManager, livingManager);
				}

				Kms lessLoadedKms;
				try {
					lessLoadedKms = this.kmsManager.getLessLoadedKms();
					if (1 == openviduConfig.getKmsLoadLimitSwitch() && Double.compare(lessLoadedKms.getLoad(), Double.valueOf("0.0")) != 0) {
						throw new NoSuchElementException();
					}
				} catch (NoSuchElementException e) {
					// Restore session not active
					this.cleanCollections(sessionId);
					this.storeSessionNotActive(sessionNotActive);
					/*throw new OpenViduException(Code.ROOM_CANNOT_BE_CREATED_ERROR_CODE,
							"There is no available media server where to initialize session '" + sessionId + "'");*/
					rpcNotificationService.sendErrorResponseWithDesc(participant.getParticipantPrivateId(),
							transactionId, null, ErrorCodeEnum.COUNT_OF_CONFERENCE_LIMIT);
					return;
				}
				log.info("KMS less loaded is {} with a load of {}", lessLoadedKms.getUri(), lessLoadedKms.getLoad());
				kSession = createSession(sessionNotActive, lessLoadedKms);
				kSession.setConference(conference);
				kSession.setConferenceMode(conference.getConferenceMode() == 0 ? ConferenceModeEnum.SFU : ConferenceModeEnum.MCU);
				kSession.setPresetInfo(getPresetInfo(sessionId));
				kSession.setRuid(conference.getRuid());

				if (ConferenceModeEnum.MCU.equals(kSession.getConferenceMode())) {
					kSession.setCorpMcuConfig(roomManage.getCorpMcuConfig(conference.getProject()));
				}
			}

			if (kSession.isClosed()) {
				log.warn("'{}' is trying to join session '{}' but it is closing", participant.getParticipantPublicId(),
						sessionId);
				throw new OpenViduException(Code.ROOM_CLOSED_ERROR_CODE, "'" + participant.getParticipantPublicId()
						+ "' is trying to join session '" + sessionId + "' but it is closing");
			}

			existingParticipants = getParticipants(sessionId);
			participant.setApplicationContext(applicationContext);
			// 第一个入会者是主持人，所有权限都打开
			if (StreamType.MAJOR.equals(participant.getStreamType())) {
				SessionPreset preset = getPresetInfo(sessionId);
				if (existingParticipants.isEmpty()) {
					participant.setMicStatus(ParticipantMicStatus.on);
					participant.setVideoStatus(ParticipantVideoStatus.on);
					participant.setSharePowerStatus(ParticipantSharePowerStatus.on);
				} else {
					participant.setSharePowerStatus(ParticipantSharePowerStatus.valueOf(preset.getSharePowerInRoom().name()));
					participant.setMicStatus(ParticipantMicStatus.valueOf(preset.getMicStatusInRoom().name()));
					if (existingParticipants.size() > 6 && preset.getQuietStatusInRoom().equals(SessionPresetEnum.smart)) {
						participant.setMicStatus(ParticipantMicStatus.off);
					}
					participant.setVideoStatus(ParticipantVideoStatus.valueOf(preset.getVideoStatusInRoom().name()));
				}
				participant.setRoomSubject(preset.getRoomSubject());
			}

			// change the part role according to the mcu limit
			if (kSession.needToChangePartRoleAccordingToLimit(participant)) {
				participant.changePartRole(OpenViduRole.SUBSCRIBER);
			}
			// deal the default subtitle config
			participant.setSubtitleConfig(kSession.getSubtitleConfig());

			kSession.join(participant);

			// record share status.
			if (StreamType.SHARING.equals(participant.getStreamType())) {
				participant.setShareStatus(ParticipantShareStatus.on);
				Participant majorPart = getParticipant(sessionId, participant.getParticipantPrivateId());
				majorPart.changeShareStatus(ParticipantShareStatus.on);
			}

			// save part info
			roomManage.storePartHistory(participant, conference);
			// save max concurrent statistics
			cacheManage.updateMaxConcurrentOfDay(kSession.getMajorPartEachConnect().size(), conference.getProject());
		} catch (OpenViduException e) {
			log.warn("PARTICIPANT {}: Error joining/creating session {}", participant.getParticipantPublicId(),
					sessionId, e);
			sessionEventsHandler.onParticipantJoined(participant, sessionId, null,
					transactionId, e);
		}
		if (existingParticipants != null) {
			sessionEventsHandler.onParticipantJoined(participant, sessionId, existingParticipants,
					transactionId, null);
		}
	}

	@Override
	public synchronized boolean leaveRoom(Participant participant, Integer transactionId, EndReason reason,
			boolean closeWebSocket) {
		log.debug("Request [LEAVE_ROOM] ({})", participant.getParticipantPublicId());

		boolean sessionClosedByLastParticipant = false;

		KurentoParticipant kParticipant = (KurentoParticipant) participant;
		KurentoSession session = kParticipant.getSession();
		String sessionId = session.getSessionId();

		if (session.isClosed()) {
			log.warn("'{}' is trying to leave from session '{}' but it is closing",
					participant.getParticipantPublicId(), sessionId);
			throw new OpenViduException(Code.ROOM_CLOSED_ERROR_CODE, "'" + participant.getParticipantPublicId()
					+ "' is trying to leave from session '" + sessionId + "' but it is closing");
		}

		session.leaveRoom(participant, reason);

		//update partInfo
		if (StreamType.MAJOR.equals(participant.getStreamType()) && !OpenViduRole.THOR.equals(participant.getRole())) {
			roomManage.updatePartHistory(session.getRuid(), participant.getUuid(), participant.getCreatedAt());
		}

		// Update control data structures
		if (sessionidParticipantpublicidParticipant.get(sessionId) != null) {
			Participant p = sessionidParticipantpublicidParticipant.get(sessionId)
					.remove(participant.getParticipantPublicId());
			boolean stillParticipant = false;
			if (Objects.nonNull(p)) {
				for (Session s : sessions.values()) {
					if (s.getParticipantByPrivateId(p.getParticipantPrivateId()) != null) {
						stillParticipant = true;
						break;
					}
				}
				if (!stillParticipant) {
					insecureUsers.remove(p.getParticipantPrivateId());
				}
			}
		}

		if (Objects.equals(StreamType.SHARING, participant.getStreamType())) {
			changeSharingStatusInConference(session, participant);
		}

		// Close Session if no more participants
		Set<Participant> remainingParticipants = null;
		try {
			remainingParticipants = getParticipants(sessionId);
		} catch (OpenViduException e) {
			log.info("Possible collision when closing the session '{}' (not found)", sessionId);
			remainingParticipants = Collections.emptySet();
		}

		if (!EndReason.forceDisconnectByUser.equals(reason) &&
				!EndReason.forceCloseSessionByUser.equals(reason) && !EndReason.closeSessionByModerator.equals(reason)) {
			sessionEventsHandler.onParticipantLeft(participant, sessionId, remainingParticipants, transactionId, null,
					reason);
		}

		// adjust order notify after onLeft
		session.dealParticipantOrder(participant,rpcNotificationService);
		if (!EndReason.sessionClosedByServer.equals(reason)) {
			// If session is closed by a call to "DELETE /api/sessions" do NOT stop the
			// recording. Will be stopped after in method
			// "SessionManager.closeSessionAndEmptyCollections"
			if (remainingParticipants.isEmpty()) {
				if (openviduConfig.isRecordingModuleEnabled()
						&& MediaMode.ROUTED.equals(session.getSessionProperties().mediaMode())
						&& (this.recordingManager.sessionIsBeingRecorded(sessionId))) {
					// Start countdown to stop recording. Will be aborted if a Publisher starts
					// before timeout
					log.info(
							"Last participant left. Starting {} seconds countdown for stopping recording of session {}",
							this.openviduConfig.getOpenviduRecordingAutostopTimeout(), sessionId);
					recordingManager.stopRecording(session, null, EndReason.automaticStop);
				}

				if (openviduConfig.isLivingModuleEnabled()
						&& MediaMode.ROUTED.equals(session.getSessionProperties().mediaMode())
						&& (this.livingManager.sessionIsBeingLived(sessionId))) {
					log.info("Last participant left. Stop living of session {}", sessionId);
					livingManager.stopLiving(session, null, EndReason.automaticStop);
				}
			} /*else if (remainingParticipants.size() == 1 && openviduConfig.isRecordingModuleEnabled()
					&& MediaMode.ROUTED.equals(session.getSessionProperties().mediaMode())
					&& this.recordingManager.sessionIsBeingRecorded(sessionId)
					&& ProtocolElements.RECORDER_PARTICIPANT_PUBLICID
							.equals(remainingParticipants.iterator().next().getParticipantPublicId())) {
				// Start countdown
				log.info("Last participant left. Starting {} seconds countdown for stopping recording of session {}",
						this.openviduConfig.getOpenviduRecordingAutostopTimeout(), sessionId);
				recordingManager.initAutomaticRecordingStopThread(session);
			}*/
		}

		// Finally close websocket session if required
		if (closeWebSocket) {
			sessionEventsHandler.closeRpcSession(participant.getParticipantPrivateId());
		}

		return sessionClosedByLastParticipant;
	}

    @Override
    public void changeSharingStatusInConference(KurentoSession session, Participant participant) {
		// change composite and sharing publisher share status
		if (Objects.equals(session.getConferenceMode(), ConferenceModeEnum.MCU)) {
            session.compositeService.setExistSharing(false);
            session.compositeService.setShareStreamId(null);
        }
		// record share status.
		participant.setShareStatus(ParticipantShareStatus.off);
		Participant majorPart = session.getPartByPrivateIdAndStreamType(participant.getParticipantPrivateId(), StreamType.MAJOR);
		if (!Objects.isNull(majorPart)) {
            majorPart.changeShareStatus(ParticipantShareStatus.off);
        }
	}

	@Override
	public RpcConnection accessOut(RpcConnection rpcConnection) {
		if (!Objects.isNull(rpcConnection)) {
			sessionEventsHandler.closeRpcSession(rpcConnection.getParticipantPrivateId());
//			cacheManage.updateTerminalStatus(rpcConnection, TerminalStatus.offline);
		}
		return rpcConnection;
	}

	/**
	 * Represents a client's request to start streaming her local media to anyone
	 * inside the room. The media elements should have been created using the same
	 * pipeline as the publisher's. The streaming media endpoint situated on the
	 * server can be connected to itself thus realizing what is known as a loopback
	 * connection. The loopback is performed after applying all additional media
	 * elements specified as parameters (in the same order as they appear in the
	 * params list).
	 * <p>
	 * <br/>
	 * <strong>Dev advice:</strong> Send notifications to the existing participants
	 * in the room to inform about the new stream that has been published. Answer to
	 * the peer's request by sending it the SDP response (answer or updated offer)
	 * generated by the WebRTC endpoint on the server.
	 *
	 * @param participant   Participant publishing video
//	 * @param MediaOptions  configuration of the stream to publish
	 * @param transactionId identifier of the Transaction
	 * @throws OpenViduException on error
	 */
	@Override
	public void publishVideo(Participant participant, MediaOptions mediaOptions, Integer transactionId)
			throws OpenViduException {

		Set<Participant> participants = null;
		String sdpAnswer = null;

		KurentoMediaOptions kurentoOptions = (KurentoMediaOptions) mediaOptions;
		KurentoParticipant kParticipant = (KurentoParticipant) participant;

		log.debug(
				"Request [PUBLISH_MEDIA] isOffer={} sdp={} "
						+ "loopbackAltSrc={} lpbkConnType={} doLoopback={} mediaElements={} ({})",
				kurentoOptions.isOffer, kurentoOptions.sdpOffer, kurentoOptions.loopbackAlternativeSrc,
				kurentoOptions.loopbackConnectionType, kurentoOptions.doLoopback, kurentoOptions.mediaElements,
				participant.getParticipantPublicId());

		SdpType sdpType = kurentoOptions.isOffer ? SdpType.OFFER : SdpType.ANSWER;
		KurentoSession kSession = kParticipant.getSession();

		kParticipant.createPublishingEndpoint(mediaOptions);

		/*
		 * for (MediaElement elem : kurentoOptions.mediaElements) {
		 * kurentoParticipant.getPublisher().apply(elem); }
		 */

		sdpAnswer = kParticipant.publishToRoom(sdpType, kurentoOptions.sdpOffer, kurentoOptions.doLoopback,
				kurentoOptions.loopbackAlternativeSrc, kurentoOptions.loopbackConnectionType);

		if (sdpAnswer == null) {
			OpenViduException e = new OpenViduException(Code.MEDIA_SDP_ERROR_CODE,
					"Error generating SDP response for publishing user " + participant.getParticipantPublicId());
			log.error("PARTICIPANT {}: Error publishing media", participant.getParticipantPublicId(), e);
			sessionEventsHandler.onPublishMedia(participant, null, kParticipant.getPublisher().createdAt(),
					kSession.getSessionId(), mediaOptions, sdpAnswer, participants, transactionId, e);
		}

		if (kSession.getSessionProperties().recordingMode().equals(RecordingMode.ALWAYS) && !kSession.isRecording.get()
				&& kSession.sessionAllowedStartToRecord()) {
			// Start automatic recording for sessions configured with RecordingMode.ALWAYS
			new Thread(() -> recordingManager.startRecording(kSession,
					new RecordingProperties.Builder().name("")
							.outputMode(kSession.getSessionProperties().defaultOutputMode())
							.recordingLayout(kSession.getSessionProperties().defaultRecordingLayout())
							.customLayout(kSession.getSessionProperties().defaultCustomLayout()).build())).start();
		} else if (kSession.isRecording.get()) {
			new Thread(() -> recordingManager.connectStreamToExistingRecorderComposite(kSession, participant)).start();
		}

		if (kSession.getSessionProperties().livingMode().equals(LivingMode.ALWAYS) && !kSession.isLiving.get()
				&& kSession.sessionAllowedStartToLive()) {
			// Start automatic recording for sessions configured with RecordingMode.ALWAYS
			new Thread(() -> livingManager.startLiving(kSession,
					new LivingProperties.Builder()
							.name("")
							.outputMode(kSession.getSessionProperties().defaultOutputMode())
							.livingLayout(kSession.getSessionProperties().defaultLivingLayout())
							.customLayout(kSession.getSessionProperties().defaultCustomLayout())
							.build(),
					participant.getUuid())).start();
		} else if (kSession.isLiving.get()) {
			new Thread(() -> livingManager.connectStreamToExistingLiveComposite(kSession, participant)).start();
		}


		/*if (this.openviduConfig.isRecordingModuleEnabled()
				&& MediaMode.ROUTED.equals(kSession.getSessionProperties().mediaMode())
				&& kSession.getActivePublishers() == 0) {
			if (RecordingMode.ALWAYS.equals(kSession.getSessionProperties().recordingMode())
					&& !recordingManager.sessionIsBeingRecorded(kSession.getSessionId())
					&& !kSession.recordingManuallyStopped.get()) {
				// Start automatic recording for sessions configured with RecordingMode.ALWAYS
				new Thread(() -> {
					recordingManager.startRecording(kSession,
							new RecordingProperties.Builder().name("")
									.outputMode(kSession.getSessionProperties().defaultOutputMode())
									.recordingLayout(kSession.getSessionProperties().defaultRecordingLayout())
									.customLayout(kSession.getSessionProperties().defaultCustomLayout()).build());
				}).start();
			} else if (RecordingMode.MANUAL.equals(kSession.getSessionProperties().recordingMode())
					&& recordingManager.sessionIsBeingRecorded(kSession.getSessionId())) {
				// Abort automatic recording stop (user published before timeout)
				log.info("Participant {} published before timeout finished. Aborting automatic recording stop",
						participant.getParticipantPublicId());
				boolean stopAborted = recordingManager.abortAutomaticRecordingStopThread(kSession,
						EndReason.automaticStop);
				if (stopAborted) {
					log.info("Automatic recording stopped successfully aborted");
				} else {
					log.info("Automatic recording stopped couldn't be aborted. Recording of session {} has stopped",
							kSession.getSessionId());
				}
			}
		}*/

		kSession.newPublisher(participant);

		participants = kParticipant.getSession().getParticipants();
		if (Objects.equals(StreamType.SHARING, participant.getStreamType())
				&& ConferenceModeEnum.MCU.equals(kSession.getConferenceMode())) {
			kSession.compositeService.setExistSharing(true);
		}

		if (sdpAnswer != null) {
			sessionEventsHandler.onPublishMedia(participant, participant.getPublisherStreamId(),
					kParticipant.getPublisher().createdAt(), kSession.getSessionId(), mediaOptions, sdpAnswer,
					participants, transactionId, null);
		}
		kParticipant.getPublisher().gatherCandidates();
	}

	@Override
	public void unpublishVideo(Participant participant, Participant moderator, Integer transactionId,
			EndReason reason) {
		try {
			KurentoParticipant kParticipant = (KurentoParticipant) participant;
			KurentoSession session = kParticipant.getSession();

			log.debug("Request [UNPUBLISH_MEDIA] ({})", participant.getParticipantPublicId());
			if (!participant.isStreaming()) {
				log.warn(
						"PARTICIPANT {}: Requesting to unpublish video of user {} "
								+ "in session {} but user is not streaming media",
						moderator != null ? moderator.getParticipantPublicId() : participant.getParticipantPublicId(),
						participant.getParticipantPublicId(), session.getSessionId());
				/*throw new OpenViduException(Code.USER_NOT_STREAMING_ERROR_CODE,
						"Participant '" + participant.getParticipantPublicId() + "' is not streaming media");*/
			}
			kParticipant.unpublishMedia(reason, 0);
			session.cancelPublisher(participant, reason);

			Set<Participant> participants = session.getParticipants();
			sessionEventsHandler.onUnpublishMedia(participant, participants, moderator, transactionId, null, reason);

		} catch (OpenViduException e) {
			log.warn("PARTICIPANT {}: Error unpublishing media", participant.getParticipantPublicId(), e);
			sessionEventsHandler.onUnpublishMedia(participant, new HashSet<>(Arrays.asList(participant)), moderator,
					transactionId, e, null);
		}
	}

	@Override
	public void subscribe(Participant participant, String senderName, StreamModeEnum streamMode, String sdpOffer, Integer transactionId) {
		String sdpAnswer = null;
		Session session = null;
		try {
			log.debug("Request [SUBSCRIBE] remoteParticipant={} sdpOffer={} ({})", senderName, sdpOffer,
					participant.getParticipantPublicId());

			KurentoParticipant kParticipant = (KurentoParticipant) participant;
			session = ((KurentoParticipant) participant).getSession();

			Participant senderParticipant;
			if (!StreamModeEnum.MIX_MAJOR_AND_SHARING
                    .equals(streamMode)) {
				senderParticipant = session.getParticipantByPublicId(senderName);
            } else {
				if (!Objects.equals(OpenViduRole.THOR, participant.getRole())) {
					senderParticipant = participant;
				} else {
					senderParticipant = getSession(participant.getSessionId()).getParticipants().stream().filter(part ->
							part.getUserId().equals(participant.getUserId()) && !Objects.equals(OpenViduRole.THOR, part.getRole()) &&
							Objects.equals(StreamType.MAJOR, part.getStreamType())).findFirst().orElse(null);
					if (senderParticipant == null) {
						log.info("Not found the same account login terminal cause the delay of inviting parts.");
						Thread.sleep(3600);
						if (Objects.isNull(getSession(participant.getSessionId()))) return;
						senderParticipant = getSession(participant.getSessionId()).getParticipants().stream().filter(part ->
								part.getUserId().equals(participant.getUserId()) && !Objects.equals(OpenViduRole.THOR, part.getRole()) &&
										Objects.equals(StreamType.MAJOR, part.getStreamType())).findAny().orElse(null);
					}
				}
			}

			if (senderParticipant == null) {
				log.warn(
						"PARTICIPANT {}: Requesting to recv media from user {} "
								+ "in session {} but user could not be found",
						participant.getParticipantPublicId(), senderName, session.getSessionId());
				sessionEventsHandler.sendSuccessResp(participant.getParticipantPrivateId(), transactionId);
				return;
			}
			if (!Objects.equals(StreamModeEnum.MIX_MAJOR_AND_SHARING, streamMode) && !senderParticipant.isStreaming()) {
				log.warn(
						"PARTICIPANT {}: Requesting to recv media from user {} "
								+ "in session {} but user is not streaming media",
						participant.getParticipantPublicId(), senderName, session.getSessionId());
				throw new OpenViduException(Code.USER_NOT_STREAMING_ERROR_CODE,
						"User '" + senderName + " not streaming media in session '" + session.getSessionId() + "'");
			}

			sdpAnswer = kParticipant.receiveMediaFrom(senderParticipant, streamMode, sdpOffer, senderName);
			if (sdpAnswer == null) {
				throw new OpenViduException(Code.MEDIA_SDP_ERROR_CODE,
						"Unable to generate SDP answer when subscribing '" + participant.getParticipantPublicId()
								+ "' to '" + senderName + "'");
			}
		} catch (OpenViduException e) {
			log.error("PARTICIPANT {}: Error subscribing to {}", participant.getParticipantPublicId(), senderName, e);
			sessionEventsHandler.onSubscribe(participant, session, null, transactionId, e);
		} catch (InterruptedException e) {
			e.printStackTrace();
			log.error("Exception:", e);
		}

		if (Objects.equals(participant.getVoiceMode(), VoiceMode.on)) {
			switchVoiceModeWithPublicId(participant, participant.getVoiceMode(), senderName);
		}

		if (sdpAnswer != null) {
			sessionEventsHandler.onSubscribe(participant, session, sdpAnswer, transactionId, null);
		}
	}

	@Override
	public void unsubscribe(Participant participant, String senderName, Integer transactionId) {
		log.debug("Request [UNSUBSCRIBE] remoteParticipant={} ({})", senderName, participant.getParticipantPublicId());

		KurentoParticipant kParticipant = (KurentoParticipant) participant;
		kParticipant.cancelReceivingMedia(senderName, EndReason.unsubscribe);

		sessionEventsHandler.onUnsubscribe(participant, transactionId, null);
	}

	@Override
	public void switchVoiceMode(Participant participant, VoiceMode operation) {
		participant.setVoiceMode(operation);
		KurentoParticipant kParticipant = (KurentoParticipant) participant;
		kParticipant.switchVoiceModeInSession(operation, kParticipant.getSubscribers().keySet());
	}

	@Override
	public void pauseAndResumeStream(Participant participant, OperationMode operation, String mediaType) {
		KurentoParticipant kParticipant = (KurentoParticipant) participant;
		kParticipant.pauseAndResumeStreamInSession(operation, mediaType, kParticipant.getSubscribers().keySet());
	}

	private void switchVoiceModeWithPublicId(Participant participant, VoiceMode operation, String senderName) {
		KurentoParticipant kParticipant = (KurentoParticipant) participant;
		kParticipant.switchVoiceModeInSession(operation, Collections.singleton(senderName));
	}

	@Override
	public void sendMessage(Participant participant, String message, Integer transactionId) {
		try {
			JsonObject messageJson = new JsonParser().parse(message).getAsJsonObject();
			KurentoParticipant kParticipant = (KurentoParticipant) participant;
			sessionEventsHandler.onSendMessage(participant, messageJson,
					getParticipants(kParticipant.getSession().getSessionId()), transactionId, null);
		} catch (JsonSyntaxException | IllegalStateException e) {
			throw new OpenViduException(Code.SIGNAL_FORMAT_INVALID_ERROR_CODE,
					"Provided signal object '" + message + "' has not a valid JSON format");
		}
	}

	@Override
	public void streamPropertyChanged(Participant participant, Integer transactionId, String streamId, String property,
			JsonElement newValue, String reason) {
		KurentoParticipant kParticipant = (KurentoParticipant) participant;
		streamId = kParticipant.getPublisherStreamId();
		MediaOptions streamProperties = kParticipant.getPublisherMediaOptions();

		Boolean hasAudio = streamProperties.hasAudio();
		Boolean hasVideo = streamProperties.hasVideo();
		Boolean audioActive = streamProperties.isAudioActive();
		Boolean videoActive = streamProperties.isVideoActive();
		String typeOfVideo = streamProperties.getTypeOfVideo();
		Integer frameRate = streamProperties.getFrameRate();
		String videoDimensions = streamProperties.getVideoDimensions();
		KurentoFilter filter = streamProperties.getFilter();

		switch (property) {
		case "audioActive":
			audioActive = newValue.getAsBoolean();
			break;
		case "videoActive":
			videoActive = newValue.getAsBoolean();
			break;
		case "videoDimensions":
			videoDimensions = newValue.getAsString();
			break;
		}

		kParticipant.setPublisherMediaOptions(new MediaOptions(hasAudio, hasVideo, audioActive, videoActive,
				typeOfVideo, frameRate, videoDimensions, filter));

		sessionEventsHandler.onStreamPropertyChanged(participant, transactionId,
				kParticipant.getSession().getParticipants(), streamId, property, newValue, reason);
	}

	@Override
	public void onIceCandidate(Participant participant, String endpointName, String candidate, int sdpMLineIndex,
			String sdpMid, Integer transactionId) {
		try {
			KurentoParticipant kParticipant = (KurentoParticipant) participant;
			log.debug("Request [ICE_CANDIDATE] endpoint={} candidate={} " + "sdpMLineIdx={} sdpMid={} ({})",
					endpointName, candidate, sdpMLineIndex, sdpMid, participant.getParticipantPublicId());
			kParticipant.addIceCandidate(endpointName, new IceCandidate(candidate, sdpMid, sdpMLineIndex));
			sessionEventsHandler.onRecvIceCandidate(participant, transactionId, null);
		} catch (OpenViduException e) {
			log.error("PARTICIPANT {}: Error receiving ICE " + "candidate (epName={}, candidate={})",
					participant.getParticipantPublicId(), endpointName, candidate, e);
			sessionEventsHandler.onRecvIceCandidate(participant, transactionId, e);
		}
	}

	/**
	 * Creates a session with the already existing not-active session in the
	 * indicated KMS, if it doesn't already exist
	 *
	 * @throws OpenViduException in case of error while creating the session
	 */
	public KurentoSession createSession(Session sessionNotActive, Kms kms) throws OpenViduException {
		KurentoSession session = (KurentoSession) sessions.get(sessionNotActive.getSessionId());
		if (session != null) {
			throw new OpenViduException(Code.ROOM_CANNOT_BE_CREATED_ERROR_CODE,
					"Session '" + session.getSessionId() + "' already exists");
		}
		session = new KurentoSession(sessionNotActive, kms, kurentoSessionEventsHandler, kurentoEndpointConfig,
				kmsManager.destroyWhenUnused());

		sessions.put(session.getSessionId(), session);
		/*KurentoSession oldSession = (KurentoSession) sessions.putIfAbsent(session.getSessionId(), session);
		if (oldSession != null) {
			log.warn("Session '{}' has just been created by another thread", session.getSessionId());
			return oldSession;
		}*/

		// Also associate the KurentoSession with the Kms
		kms.addKurentoSession(session);

		log.warn("No session '{}' exists yet. Created one on KMS '{}'", session.getSessionId(), kms.getUri());

		sessionEventsHandler.onSessionCreated(session);
		return session;
	}

	@Override
	public boolean evictParticipant(Participant evictedParticipant, Participant moderator, Integer transactionId,
			EndReason reason) throws OpenViduException {

		boolean sessionClosedByLastParticipant = false;

		if (evictedParticipant != null && !evictedParticipant.isClosed()) {
			KurentoParticipant kParticipant = (KurentoParticipant) evictedParticipant;
			Set<Participant> participants = kParticipant.getSession().getParticipants();
			sessionClosedByLastParticipant = this.leaveRoom(kParticipant, null, reason, false);
			this.sessionEventsHandler.onForceDisconnect(moderator, evictedParticipant, participants, transactionId,
					null, reason);
//			sessionEventsHandler.closeRpcSession(evictedParticipant.getParticipantPrivateId());
		} else {
			if (moderator != null && transactionId != null) {
				this.sessionEventsHandler.onForceDisconnect(moderator, evictedParticipant,
						new HashSet<>(Arrays.asList(moderator)), transactionId,
						new OpenViduException(Code.USER_NOT_FOUND_ERROR_CODE,
								"Connection not found when calling 'forceDisconnect'"),
						null);
			}
		}

		return sessionClosedByLastParticipant;
	}

	@Override
	public void evictParticipantWhenDisconnect(RpcConnection rpcConnection, List<EvictParticipantStrategy> evictStrategies) {
		if (StringUtils.isEmpty(rpcConnection.getSessionId())) {
			rpcNotificationService.closeRpcSession(rpcConnection.getParticipantPrivateId());
			return;
		}
		Session session;
		Map partInfo = cacheManage.getPartInfo(rpcConnection.getUserUuid());
		if (partInfo != null && !partInfo.isEmpty() && partInfo.containsKey("roomId")
				&& rpcConnection.getSessionId().equals(partInfo.get("roomId").toString())
				&& Objects.nonNull(session = getSession(partInfo.get("roomId").toString()))) {
			Map<String, Participant> samePrivateIdParts = session.getSamePrivateIdParts(rpcConnection.getParticipantPrivateId());
			if (samePrivateIdParts == null || samePrivateIdParts.isEmpty()) {
				rpcNotificationService.closeRpcSession(rpcConnection.getParticipantPrivateId());
				return;
			}

			// construct break line notify params
			List<JsonObject> breakLineNotifyParams = new ArrayList<>();
			samePrivateIdParts.values().forEach(participant -> {
				JsonObject singleNotifyParam = new JsonObject();
				singleNotifyParam.addProperty(ProtocolElements.USER_BREAK_LINE_CONNECTION_ID_PARAM, participant.getParticipantPublicId());
				breakLineNotifyParams.add(singleNotifyParam);
			});

			// send user break line
			session.getMajorPartEachIncludeThorConnect().forEach(participant ->
					breakLineNotifyParams.forEach(jsonObject ->
							rpcNotificationService.sendNotification(participant.getParticipantPrivateId(),
									ProtocolElements.USER_BREAK_LINE_METHOD, jsonObject)));

			// evict same privateId parts
			evictParticipantWithSamePrivateId(samePrivateIdParts, evictStrategies);
		}
	}

    @Override
    public void evictParticipantByPrivateId(String sessionId, String privateId, List<EvictParticipantStrategy> evictStrategies) {
        Session session;
        if (Objects.nonNull(session = getSession(sessionId))) {
            Map<String, Participant> samePrivateIdParts = session.getSamePrivateIdParts(privateId);
            if (samePrivateIdParts != null && !samePrivateIdParts.isEmpty()) {
                // evict same privateId parts
                evictParticipantWithSamePrivateId(samePrivateIdParts, evictStrategies);
            }
        }
    }

    @Override
    public void evictParticipantByUUID(String sessionId, String uuid, List<EvictParticipantStrategy> evictStrategies) {
        Session session;
        if (Objects.nonNull(session = getSession(sessionId))) {
            Map<String, Participant> samePrivateIdParts = session.getSameAccountParticipants(uuid);
            if (samePrivateIdParts != null && !samePrivateIdParts.isEmpty()) {
                // evict same privateId parts
                evictParticipantWithSamePrivateId(samePrivateIdParts, evictStrategies);
            }
        }
    }

    @Override
    public void setLayoutAndNotifyWhenLeaveRoom(String sessionId, Participant participant, String moderatePublicId) {
        Session session;
        if (Objects.nonNull(session = getSession(sessionId)) && ConferenceModeEnum.MCU.equals(session.getConferenceMode())) {
            if (session.leaveRoomSetLayout(participant, moderatePublicId)) {
                // notify kms mcu layout changed
                session.invokeKmsConferenceLayout();

                // notify clients mcu layout changed
                JsonObject notifyParam = session.getLayoutNotifyInfo();
                session.getMajorPartEachIncludeThorConnect().forEach(part -> rpcNotificationService.sendNotification(part.getParticipantPrivateId(),
                        ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, notifyParam));
            }
        }
    }

	@Override
	public void updateRoomAndPartInfoAfterKMSDisconnect(String sessionId) {
		Session session;
		if (Objects.nonNull(session = getSession(sessionId))) {
			// del part info in cache
			session.getMajorPartEachConnect().forEach(participant -> cacheManage.delPartInfo(participant.getUuid()));
			// del room info in cache
			cacheManage.delRoomInfo(sessionId);
			// update conference status in DB
			updateConferenceInfo(sessionId);
		}
	}

	private void evictParticipantWithSamePrivateId(Map<String, Participant> samePrivateIdParts, List<EvictParticipantStrategy> evictStrategies) {
		// check if include moderator
		Session session;
		Participant majorPart = samePrivateIdParts.get(StreamType.MAJOR.name());
		Set<Participant> participants = (session = getSession(majorPart.getSessionId())).getMajorPartEachIncludeThorConnect();
		if (OpenViduRole.MODERATOR.equals(majorPart.getRole())
                && evictStrategies.contains(EvictParticipantStrategy.CLOSE_ROOM_WHEN_EVICT_MODERATOR)) {	// close the room
			dealSessionClose(majorPart.getSessionId(), EndReason.sessionClosedByServer);
		} else {
			// check if MAJOR is speaker
			if (ParticipantHandStatus.speaker.equals(majorPart.getHandStatus())) {
				JsonObject params = new JsonObject();
				params.addProperty(ProtocolElements.END_ROLL_CALL_ROOM_ID_PARAM, majorPart.getSessionId());
				params.addProperty(ProtocolElements.END_ROLL_CALL_TARGET_ID_PARAM, majorPart.getUuid());

				// send end roll call
				participants.forEach(participant -> rpcNotificationService.sendNotification(participant.getParticipantPrivateId(),
						ProtocolElements.END_ROLL_CALL_METHOD, params));
			}
			// check if exists SHARING
			Participant sharePart;
			if (Objects.nonNull(sharePart = samePrivateIdParts.get(StreamType.SHARING.name()))) {
				JsonObject params = new JsonObject();
				params.addProperty(ProtocolElements.RECONNECTPART_STOP_PUBLISH_SHARING_CONNECTIONID_PARAM,
						sharePart.getParticipantPublicId());

				// send stop SHARING
				participants.forEach(participant -> rpcNotificationService.sendNotification(participant.getParticipantPrivateId(),
						ProtocolElements.RECONNECTPART_STOP_PUBLISH_SHARING_METHOD, params));
				// change session share status
				if (ConferenceModeEnum.MCU.equals(session.getConferenceMode())) {
					KurentoSession kurentoSession = (KurentoSession) session;
					kurentoSession.compositeService.setExistSharing(false);
					kurentoSession.compositeService.setShareStreamId(null);
				}

			}

			// change the layout if mode is MCU
            if (ConferenceModeEnum.MCU.equals(session.getConferenceMode())) {
                Map<String, String> layoutRelativePartIdMap = session.getLayoutRelativePartId();
                boolean layoutChanged = false;
                for (Participant part : samePrivateIdParts.values()) {
                    if (part.getStreamType().isStreamTypeMixInclude()) {
                        layoutChanged |= session.leaveRoomSetLayout(part,
                                !Objects.equals(layoutRelativePartIdMap.get("speakerId"), part.getParticipantPublicId())
                                        ? layoutRelativePartIdMap.get("speakerId") : layoutRelativePartIdMap.get("moderatorId"));
                    }
                }

                if (layoutChanged) {
                    // notify kms change the layout of MCU
                    session.invokeKmsConferenceLayout();

                    // notify client the change of layout
                    JsonObject params = session.getLayoutNotifyInfo();
                    participants.forEach(participant -> rpcNotificationService.sendNotification(participant.getParticipantPrivateId(),
                            ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, params));
                }
            }

			// evict participants
            samePrivateIdParts.values().forEach(participant -> evictParticipant(participant, null,
                    null, EndReason.sessionClosedByServer));

            // deal auto on wall
            session.putPartOnWallAutomatically(this);
		}

		// clear the rpc connection if necessary
        if (evictStrategies.contains(EvictParticipantStrategy.CLOSE_WEBSOCKET_CONNECTION)) {
            rpcNotificationService.closeRpcSession(majorPart.getParticipantPrivateId());
        }
	}

	@Override
	public KurentoMediaOptions generateMediaOptions(Request<JsonObject> request) throws OpenViduException {

		String sdpOffer = RpcAbstractHandler.getStringParam(request, ProtocolElements.PUBLISHVIDEO_SDPOFFER_PARAM);
		boolean hasAudio = RpcAbstractHandler.getBooleanParam(request, ProtocolElements.PUBLISHVIDEO_HASAUDIO_PARAM);
		boolean hasVideo = RpcAbstractHandler.getBooleanParam(request, ProtocolElements.PUBLISHVIDEO_HASVIDEO_PARAM);

		Boolean audioActive = null, videoActive = null;
		String typeOfVideo = null, videoDimensions = null;
		Integer frameRate = null;
		KurentoFilter kurentoFilter = null;

		try {
			audioActive = RpcAbstractHandler.getBooleanParam(request, ProtocolElements.PUBLISHVIDEO_AUDIOACTIVE_PARAM);
			videoActive = RpcAbstractHandler.getBooleanParam(request, ProtocolElements.PUBLISHVIDEO_VIDEOACTIVE_PARAM);
			typeOfVideo = RpcAbstractHandler.getStringOptionalParam(request, ProtocolElements.PUBLISHVIDEO_TYPEOFVIDEO_PARAM);
			if (Objects.isNull(typeOfVideo)) typeOfVideo = "CAMERA";
			videoDimensions = RpcAbstractHandler.getStringOptionalParam(request, ProtocolElements.PUBLISHVIDEO_VIDEODIMENSIONS_PARAM);
			frameRate = RpcAbstractHandler.getIntOptionalParam(request, ProtocolElements.PUBLISHVIDEO_FRAMERATE_PARAM);
		} catch (RuntimeException noParameterFound) {
			log.error("Exception:{}\n", noParameterFound);
		}

		try {
			JsonObject kurentoFilterJson = (JsonObject) RpcAbstractHandler.getParam(request,
					ProtocolElements.PUBLISHVIDEO_KURENTOFILTER_PARAM);
			if (kurentoFilterJson != null) {
				try {
					kurentoFilter = new KurentoFilter(kurentoFilterJson.get("type").getAsString(),
							kurentoFilterJson.get("options").getAsJsonObject());
				} catch (Exception e) {
					throw new OpenViduException(Code.FILTER_NOT_APPLIED_ERROR_CODE,
							"'filter' parameter wrong:" + e.getMessage());
				}
			}
		} catch (OpenViduException e) {
			throw e;
		} catch (RuntimeException noParameterFound) {
		}

		boolean doLoopback = RpcAbstractHandler.getBooleanParam(request, ProtocolElements.PUBLISHVIDEO_DOLOOPBACK_PARAM);

		return new KurentoMediaOptions(true, sdpOffer, null, null, hasAudio, hasVideo, audioActive, videoActive,
				typeOfVideo, frameRate, videoDimensions, kurentoFilter, doLoopback);
	}

	@Override
	public boolean unpublishStream(Session session, String streamId, Participant moderator, Integer transactionId,
			EndReason reason) {
        log.info("Stream:{} unPublish in session:{}", streamId, session.getSessionId());
		KurentoSession kSession = (KurentoSession) session;
//		String participantPrivateId = kSession.getParticipantPrivateIdFromStreamId(streamId);
		Participant unPubPart = kSession.getParticipantByStreamId(streamId);
		if (Objects.isNull(unPubPart)) {
		    return false;
        }
		String moderatorPublicId = null, speakerId = null;
		Set<Participant> participants = session.getParticipants();
		for (Participant participant : participants) {
			if (Objects.equals(OpenViduRole.MODERATOR, participant.getRole()) &&
					Objects.equals(StreamType.MAJOR, participant.getStreamType())) {
				moderatorPublicId = participant.getParticipantPublicId();
			}
			if (Objects.equals(ParticipantHandStatus.speaker, participant.getHandStatus())) {
				speakerId = participant.getParticipantPublicId();
				break;
			}
		}

        this.unpublishVideo(unPubPart, moderator, transactionId, reason);
        if (Objects.equals(kSession.getConferenceMode(), ConferenceModeEnum.MCU)) {
            // change conference layout and notify kms
            session.leaveRoomSetLayout(unPubPart, Objects.equals(speakerId, unPubPart.getParticipantPublicId())
                    ? moderatorPublicId : speakerId);
            session.invokeKmsConferenceLayout();
        }
        if (Objects.equals(StreamType.SHARING, unPubPart.getStreamType())) {
            changeSharingStatusInConference(kSession, unPubPart);
        }

        return true;
	}

	@Override
	public void applyFilter(Session session, String streamId, String filterType, JsonObject filterOptions,
			Participant moderator, Integer transactionId, String filterReason) {
		String participantPrivateId = ((KurentoSession) session).getParticipantPrivateIdFromStreamId(streamId);
		if (participantPrivateId != null) {
			Participant publisher = this.getParticipant(participantPrivateId);
			moderator = (moderator != null
					&& publisher.getParticipantPublicId().equals(moderator.getParticipantPublicId())) ? null
							: moderator;
			log.debug("Request [APPLY_FILTER] over stream [{}] for reason [{}]", streamId, filterReason);
			KurentoParticipant kParticipantPublisher = (KurentoParticipant) publisher;
			if (!publisher.isStreaming()) {
				log.warn(
						"PARTICIPANT {}: Requesting to applyFilter to user {} "
								+ "in session {} but user is not streaming media",
						moderator != null ? moderator.getParticipantPublicId() : publisher.getParticipantPublicId(),
						publisher.getParticipantPublicId(), session.getSessionId());
				throw new OpenViduException(Code.USER_NOT_STREAMING_ERROR_CODE,
						"User '" + publisher.getParticipantPublicId() + " not streaming media in session '"
								+ session.getSessionId() + "'");
			} else if (kParticipantPublisher.getPublisher().getFilter() != null) {
				log.warn(
						"PARTICIPANT {}: Requesting to applyFilter to user {} "
								+ "in session {} but user already has a filter",
						moderator != null ? moderator.getParticipantPublicId() : publisher.getParticipantPublicId(),
						publisher.getParticipantPublicId(), session.getSessionId());
				throw new OpenViduException(Code.EXISTING_FILTER_ALREADY_APPLIED_ERROR_CODE,
						"User '" + publisher.getParticipantPublicId() + " already has a filter applied in session '"
								+ session.getSessionId() + "'");
			} else {
				try {
					KurentoFilter filter = new KurentoFilter(filterType, filterOptions);
					this.applyFilterInPublisher(kParticipantPublisher, filter);
					Set<Participant> participants = kParticipantPublisher.getSession().getParticipants();
					sessionEventsHandler.onFilterChanged(publisher, moderator, transactionId, participants, streamId,
							filter, null, filterReason);
				} catch (OpenViduException e) {
					log.warn("PARTICIPANT {}: Error applying filter", publisher.getParticipantPublicId(), e);
					sessionEventsHandler.onFilterChanged(publisher, moderator, transactionId, new HashSet<>(), streamId,
							null, e, "");
				}
			}

			log.info("State of filter for participant {}: {}", publisher.getParticipantPublicId(),
					((KurentoParticipant) publisher).getPublisher().filterCollectionsToString());

		} else {
			log.warn("PARTICIPANT {}: Requesting to applyFilter to stream {} "
					+ "in session {} but the owner cannot be found", streamId, session.getSessionId());
			throw new OpenViduException(Code.USER_NOT_FOUND_ERROR_CODE,
					"Owner of stream '" + streamId + "' not found in session '" + session.getSessionId() + "'");
		}
	}

	@Override
	public void removeFilter(Session session, String streamId, Participant moderator, Integer transactionId,
			String filterReason) {
		String participantPrivateId = ((KurentoSession) session).getParticipantPrivateIdFromStreamId(streamId);
		if (participantPrivateId != null) {
			Participant participant = this.getParticipant(participantPrivateId);
			log.debug("Request [REMOVE_FILTER] over stream [{}] for reason [{}]", streamId, filterReason);
			KurentoParticipant kParticipant = (KurentoParticipant) participant;
			if (!participant.isStreaming()) {
				log.warn(
						"PARTICIPANT {}: Requesting to removeFilter to user {} "
								+ "in session {} but user is not streaming media",
						moderator != null ? moderator.getParticipantPublicId() : participant.getParticipantPublicId(),
						participant.getParticipantPublicId(), session.getSessionId());
				throw new OpenViduException(Code.USER_NOT_STREAMING_ERROR_CODE,
						"User '" + participant.getParticipantPublicId() + " not streaming media in session '"
								+ session.getSessionId() + "'");
			} else if (kParticipant.getPublisher().getFilter() == null) {
				log.warn(
						"PARTICIPANT {}: Requesting to removeFilter to user {} "
								+ "in session {} but user does NOT have a filter",
						moderator != null ? moderator.getParticipantPublicId() : participant.getParticipantPublicId(),
						participant.getParticipantPublicId(), session.getSessionId());
				throw new OpenViduException(Code.FILTER_NOT_APPLIED_ERROR_CODE,
						"User '" + participant.getParticipantPublicId() + " has no filter applied in session '"
								+ session.getSessionId() + "'");
			} else {
				this.removeFilterInPublisher(kParticipant);
				Set<Participant> participants = kParticipant.getSession().getParticipants();
				sessionEventsHandler.onFilterChanged(participant, moderator, transactionId, participants, streamId,
						null, null, filterReason);
			}

			log.info("State of filter for participant {}: {}", kParticipant.getParticipantPublicId(),
					kParticipant.getPublisher().filterCollectionsToString());

		} else {
			log.warn("PARTICIPANT {}: Requesting to removeFilter to stream {} "
					+ "in session {} but the owner cannot be found", streamId, session.getSessionId());
			throw new OpenViduException(Code.USER_NOT_FOUND_ERROR_CODE,
					"Owner of stream '" + streamId + "' not found in session '" + session.getSessionId() + "'");
		}
	}

	@Override
	public void execFilterMethod(Session session, String streamId, String filterMethod, JsonObject filterParams,
			Participant moderator, Integer transactionId, String filterReason) {
		String participantPrivateId = ((KurentoSession) session).getParticipantPrivateIdFromStreamId(streamId);
		if (participantPrivateId != null) {
			Participant participant = this.getParticipant(participantPrivateId);
			log.debug("Request [EXEC_FILTER_MTEHOD] over stream [{}] for reason [{}]", streamId, filterReason);
			KurentoParticipant kParticipant = (KurentoParticipant) participant;
			if (!participant.isStreaming()) {
				log.warn(
						"PARTICIPANT {}: Requesting to execFilterMethod to user {} "
								+ "in session {} but user is not streaming media",
						moderator != null ? moderator.getParticipantPublicId() : participant.getParticipantPublicId(),
						participant.getParticipantPublicId(), session.getSessionId());
				throw new OpenViduException(Code.USER_NOT_STREAMING_ERROR_CODE,
						"User '" + participant.getParticipantPublicId() + " not streaming media in session '"
								+ session.getSessionId() + "'");
			} else if (kParticipant.getPublisher().getFilter() == null) {
				log.warn(
						"PARTICIPANT {}: Requesting to execFilterMethod to user {} "
								+ "in session {} but user does NOT have a filter",
						moderator != null ? moderator.getParticipantPublicId() : participant.getParticipantPublicId(),
						participant.getParticipantPublicId(), session.getSessionId());
				throw new OpenViduException(Code.FILTER_NOT_APPLIED_ERROR_CODE,
						"User '" + participant.getParticipantPublicId() + " has no filter applied in session '"
								+ session.getSessionId() + "'");
			} else {
				KurentoFilter updatedFilter = this.execFilterMethodInPublisher(kParticipant, filterMethod,
						filterParams);
				Set<Participant> participants = kParticipant.getSession().getParticipants();
				sessionEventsHandler.onFilterChanged(participant, moderator, transactionId, participants, streamId,
						updatedFilter, null, filterReason);
			}
		} else {
			log.warn("PARTICIPANT {}: Requesting to removeFilter to stream {} "
					+ "in session {} but the owner cannot be found", streamId, session.getSessionId());
			throw new OpenViduException(Code.USER_NOT_FOUND_ERROR_CODE,
					"Owner of stream '" + streamId + "' not found in session '" + session.getSessionId() + "'");
		}
	}

	@Override
	public void addFilterEventListener(Session session, Participant userSubscribing, String streamId, String eventType)
			throws OpenViduException {
		String publisherPrivateId = ((KurentoSession) session).getParticipantPrivateIdFromStreamId(streamId);
		if (publisherPrivateId != null) {
			log.debug("Request [ADD_FILTER_LISTENER] over stream [{}]", streamId);
			KurentoParticipant kParticipantPublishing = (KurentoParticipant) this.getParticipant(publisherPrivateId);
			KurentoParticipant kParticipantSubscribing = (KurentoParticipant) userSubscribing;
			if (!kParticipantPublishing.isStreaming()) {
				log.warn(
						"PARTICIPANT {}: Requesting to addFilterEventListener to stream {} "
								+ "in session {} but the publisher is not streaming media",
						userSubscribing.getParticipantPublicId(), streamId, session.getSessionId());
				throw new OpenViduException(Code.USER_NOT_STREAMING_ERROR_CODE,
						"User '" + kParticipantPublishing.getParticipantPublicId() + " not streaming media in session '"
								+ session.getSessionId() + "'");
			} else if (kParticipantPublishing.getPublisher().getFilter() == null) {
				log.warn(
						"PARTICIPANT {}: Requesting to addFilterEventListener to user {} "
								+ "in session {} but user does NOT have a filter",
						kParticipantSubscribing.getParticipantPublicId(),
						kParticipantPublishing.getParticipantPublicId(), session.getSessionId());
				throw new OpenViduException(Code.FILTER_NOT_APPLIED_ERROR_CODE,
						"User '" + kParticipantPublishing.getParticipantPublicId()
								+ " has no filter applied in session '" + session.getSessionId() + "'");
			} else {
				try {
					this.addFilterEventListenerInPublisher(kParticipantPublishing, eventType);
					kParticipantPublishing.getPublisher().addParticipantAsListenerOfFilterEvent(eventType,
							userSubscribing.getParticipantPublicId());
				} catch (OpenViduException e) {
					throw e;
				}
			}

			log.info("State of filter for participant {}: {}", kParticipantPublishing.getParticipantPublicId(),
					kParticipantPublishing.getPublisher().filterCollectionsToString());

		} else {
			throw new OpenViduException(Code.USER_NOT_FOUND_ERROR_CODE,
					"Not user found for streamId '" + streamId + "' in session '" + session.getSessionId() + "'");
		}
	}

	@Override
	public void removeFilterEventListener(Session session, Participant subscriber, String streamId, String eventType)
			throws OpenViduException {
		String participantPrivateId = ((KurentoSession) session).getParticipantPrivateIdFromStreamId(streamId);
		if (participantPrivateId != null) {
			log.debug("Request [REMOVE_FILTER_LISTENER] over stream [{}]", streamId);
			Participant participantPublishing = this.getParticipant(participantPrivateId);
			KurentoParticipant kParticipantPublishing = (KurentoParticipant) participantPublishing;
			if (!participantPublishing.isStreaming()) {
				log.warn(
						"PARTICIPANT {}: Requesting to removeFilterEventListener to stream {} "
								+ "in session {} but user is not streaming media",
						subscriber.getParticipantPublicId(), streamId, session.getSessionId());
				throw new OpenViduException(Code.USER_NOT_STREAMING_ERROR_CODE,
						"User '" + participantPublishing.getParticipantPublicId() + " not streaming media in session '"
								+ session.getSessionId() + "'");
			} else if (kParticipantPublishing.getPublisher().getFilter() == null) {
				log.warn(
						"PARTICIPANT {}: Requesting to removeFilterEventListener to user {} "
								+ "in session {} but user does NOT have a filter",
						subscriber.getParticipantPublicId(), participantPublishing.getParticipantPublicId(),
						session.getSessionId());
				throw new OpenViduException(Code.FILTER_NOT_APPLIED_ERROR_CODE,
						"User '" + participantPublishing.getParticipantPublicId()
								+ " has no filter applied in session '" + session.getSessionId() + "'");
			} else {
				try {
					PublisherEndpoint pub = kParticipantPublishing.getPublisher();
					if (pub.removeParticipantAsListenerOfFilterEvent(eventType, subscriber.getParticipantPublicId())) {
						// If there are no more participants listening to the event remove the event
						// from the GenericMediaElement
						this.removeFilterEventListenerInPublisher(kParticipantPublishing, eventType);
					}
				} catch (OpenViduException e) {
					throw e;
				}
			}

			log.info("State of filter for participant {}: {}", kParticipantPublishing.getParticipantPublicId(),
					kParticipantPublishing.getPublisher().filterCollectionsToString());

		}
	}

	@Override
	public String getParticipantPrivateIdFromStreamId(String sessionId, String streamId) {
		Session session = this.getSession(sessionId);
		return ((KurentoSession) session).getParticipantPrivateIdFromStreamId(streamId);
	}

	public KmsManager getKmsManager() {
		return this.kmsManager;
	}

	private void applyFilterInPublisher(KurentoParticipant kParticipant, KurentoFilter filter)
			throws OpenViduException {
		GenericMediaElement.Builder builder = new GenericMediaElement.Builder(kParticipant.getPipeline(),
				filter.getType());
		Props props = new JsonUtils().fromJsonObjectToProps(filter.getOptions());
		props.forEach(prop -> {
			builder.withConstructorParam(prop.getName(), prop.getValue());
		});
		kParticipant.getPublisher().apply(builder.build());
		kParticipant.getPublisher().getMediaOptions().setFilter(filter);
	}

	private void removeFilterInPublisher(KurentoParticipant kParticipant) {
		kParticipant.getPublisher().cleanAllFilterListeners();
		kParticipant.getPublisher().revert(kParticipant.getPublisher().getFilter());
		kParticipant.getPublisher().getMediaOptions().setFilter(null);
	}

	private KurentoFilter execFilterMethodInPublisher(KurentoParticipant kParticipant, String method,
			JsonObject params) {
		kParticipant.getPublisher().execMethod(method, params);
		KurentoFilter filter = kParticipant.getPublisher().getMediaOptions().getFilter();
		KurentoFilter updatedFilter = new KurentoFilter(filter.getType(), filter.getOptions(), method, params);
		kParticipant.getPublisher().getMediaOptions().setFilter(updatedFilter);
		return updatedFilter;
	}

	private void addFilterEventListenerInPublisher(KurentoParticipant kParticipant, String eventType)
			throws OpenViduException {
		PublisherEndpoint pub = kParticipant.getPublisher();
		if (!pub.isListenerAddedToFilterEvent(eventType)) {
			final String connectionId = kParticipant.getParticipantPublicId();
			final String streamId = kParticipant.getPublisherStreamId();
			final String filterType = kParticipant.getPublisherMediaOptions().getFilter().getType();
			try {
				ListenerSubscription listener = pub.getFilter().addEventListener(eventType, event -> {
					sessionEventsHandler.onFilterEventDispatched(connectionId, streamId, filterType, event.getType(),
							event.getData(), kParticipant.getSession().getParticipants(),
							kParticipant.getPublisher().getPartipantsListentingToFilterEvent(eventType));
				});
				pub.storeListener(eventType, listener);
			} catch (Exception e) {
				log.error("Request to addFilterEventListener to stream {} gone wrong. Error: {}", streamId,
						e.getMessage());
				throw new OpenViduException(Code.FILTER_EVENT_LISTENER_NOT_FOUND,
						"Request to addFilterEventListener to stream " + streamId + " gone wrong: " + e.getMessage());
			}
		}
	}

	private void removeFilterEventListenerInPublisher(KurentoParticipant kParticipant, String eventType) {
		PublisherEndpoint pub = kParticipant.getPublisher();
		if (pub.isListenerAddedToFilterEvent(eventType)) {
			GenericMediaElement filter = kParticipant.getPublisher().getFilter();
			filter.removeEventListener(pub.removeListener(eventType));
		}
	}

}
