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

package io.openvidu.server.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.java.client.SessionProperties;
import io.openvidu.server.cdr.CDREventRecording;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.constants.CacheKeyConstants;
import io.openvidu.server.common.dao.AppointConferenceMapper;
import io.openvidu.server.common.dao.ConferenceMapper;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferenceSearch;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.coturn.CoturnCredentialsService;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.kurento.core.KurentoTokenOptions;
import io.openvidu.server.living.Living;
import io.openvidu.server.living.service.LivingManager;
import io.openvidu.server.recording.service.RecordingManager;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcNotificationService;
import io.openvidu.server.utils.FormatChecker;
import io.openvidu.server.utils.GeoLocation;
import org.apache.commons.lang3.RandomStringUtils;
import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public abstract class SessionManager {

	private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

	@Autowired
	protected SessionEventsHandler sessionEventsHandler;

	@Autowired
	protected RecordingManager recordingManager;

	@Autowired
	protected LivingManager livingManager;

	@Autowired
	protected OpenviduConfig openviduConfig;

	@Autowired
	protected CoturnCredentialsService coturnCredentialsService;

	@Autowired
	protected TokenGenerator tokenGenerator;

	@Autowired
	protected CacheManage cacheManage;

	@Autowired
	RpcNotificationService notificationService;

	@Autowired
	AppointConferenceMapper appointConferenceMapper;

	@Resource
	ConferenceMapper conferenceMapper;

	public FormatChecker formatChecker = new FormatChecker();

	protected ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();
	protected ConcurrentMap<String, Session> sessionsNotActive = new ConcurrentHashMap<>();
	protected ConcurrentMap<String, ConcurrentHashMap<String, Participant>> sessionidParticipantpublicidParticipant = new ConcurrentHashMap<>();
	protected ConcurrentMap<String, ConcurrentHashMap<String, FinalUser>> sessionidFinalUsers = new ConcurrentHashMap<>();
	protected ConcurrentMap<String, ConcurrentLinkedQueue<CDREventRecording>> sessionidAccumulatedRecordings = new ConcurrentHashMap<>();

	protected ConcurrentMap<String, Boolean> insecureUsers = new ConcurrentHashMap<>();
	public ConcurrentMap<String, ConcurrentHashMap<String, Token>> sessionidTokenTokenobj = new ConcurrentHashMap<>();

	protected ConcurrentMap<String, ConcurrentHashMap<String, String>> sessionidConferenceInfo = new ConcurrentHashMap<>();
	protected ConcurrentMap<String, SessionPreset> sessionidPreset = new ConcurrentHashMap<>();

	public abstract void joinRoom(Participant participant, String sessionId, Conference conference, Integer transactionId);

	public abstract boolean leaveRoom(Participant participant, Integer transactionId, EndReason reason,
			boolean closeWebSocket);

	public abstract boolean leaveRoomSimple(Participant participant, Integer transactionId, EndReason reason,
									  boolean closeWebSocket);

	public abstract void changeSharingStatusInConference(KurentoSession session, Participant participant);

	public abstract RpcConnection accessOut(RpcConnection rpcConnection);

	public abstract void publishVideo(Participant participant, MediaOptions mediaOptions, Integer transactionId);

	public abstract void unpublishVideo(Participant participant, Participant moderator, Integer transactionId,
			EndReason reason);

	public abstract void subscribe(Participant participant, String senderName, StreamModeEnum streamMode,
								   String sdpOffer, Integer transactionId);

	public abstract void unsubscribe(Participant participant, String senderName, Integer transactionId);

	public abstract void switchVoiceMode(Participant participant, VoiceMode operation);

	public abstract void pauseAndResumeStream(Participant pausePart,Participant targetPart, OperationMode operation, String mediaType);

	public abstract void sendMessage(Participant participant, String message, Integer transactionId);

	public abstract void streamPropertyChanged(Participant participant, Integer transactionId, String streamId,
			String property, JsonElement newValue, String changeReason);

	public abstract void onIceCandidate(Participant participant, String endpointName, String candidate,
			int sdpMLineIndex, String sdpMid, Integer transactionId);

	public abstract boolean unpublishStream(Session session, String streamId, Participant moderator,
			Integer transactionId, EndReason reason);

	public abstract boolean evictParticipant(Participant evictedParticipant, Participant moderator,
			Integer transactionId, EndReason reason);

	public abstract boolean evictParticipantByCloseRoom(Participant evictedParticipant, Participant moderator,
											 Integer transactionId, EndReason reason);

	public abstract void applyFilter(Session session, String streamId, String filterType, JsonObject filterOptions,
			Participant moderator, Integer transactionId, String reason);

	public abstract void execFilterMethod(Session session, String streamId, String filterMethod,
			JsonObject filterParams, Participant moderator, Integer transactionId, String reason);

	public abstract void removeFilter(Session session, String streamId, Participant moderator, Integer transactionId,
			String reason);

	public abstract void addFilterEventListener(Session session, Participant subscriber, String streamId,
			String eventType);

	public abstract void removeFilterEventListener(Session session, Participant subscriber, String streamId,
			String eventType);

	public abstract String getParticipantPrivateIdFromStreamId(String sessionId, String streamId)
			throws OpenViduException;

	public abstract void evictParticipantWhenDisconnect(RpcConnection rpcConnection, List<EvictParticipantStrategy> evictStrategies);

	public abstract void evictParticipantByPrivateId(String sessionId, String privateId, List<EvictParticipantStrategy> evictStrategies);

    public abstract void evictParticipantByUUID(String sessionId, String uuid, List<EvictParticipantStrategy> evictStrategies);

    public abstract void setLayoutAndNotifyWhenLeaveRoom(String sessionId, Participant participant, String moderatePublicId);

	public abstract void updateRoomAndPartInfoAfterKMSDisconnect(String sessionId);

	/**
	 * Returns a Session given its id
	 *
	 * @return Session
	 */
	public Session getSession(String sessionId) {
		return sessions.get(sessionId);
	}

	/**
	 * Returns all currently active (opened) sessions.
	 *
	 * @return set of the session's identifiers
	 */
	public Collection<Session> getSessions() {
		return sessions.values();
	}

	public Session getSessionNotActive(String sessionId) {
		return this.sessionsNotActive.get(sessionId);
	}

	public Collection<Session> getSessionsWithNotActive() {
		Collection<Session> allSessions = new HashSet<>();
		allSessions.addAll(this.sessionsNotActive.values().stream()
				.filter(sessionNotActive -> !sessions.containsKey(sessionNotActive.getSessionId()))
				.collect(Collectors.toSet()));
		allSessions.addAll(this.getSessions());
		return allSessions;
	}

	public Collection<Session> getCorpSessions(String project) {
		return sessions.values().stream().filter(session -> Objects.nonNull(session) && !session.isClosed()
				&& session.getConference().getProject().equals(project)).collect(Collectors.toSet());
	}

	/**
	 * Returns all the participants inside a session.
	 *
	 * @param sessionId identifier of the session
	 * @return set of {@link Participant}
	 * @throws OpenViduException in case the session doesn't exist
	 */
	public Set<Participant> getParticipants(String sessionId) throws OpenViduException {
		Session session = sessions.get(sessionId);
		if (session == null) {
			log.error("Session:{} not found.", sessionId);
			return Collections.emptySet();
		}
		Set<Participant> participants = session.getParticipants();
		participants.removeIf(Participant::isClosed);
		return participants;
	}


	public Set<Participant> getMajorParticipants(String sessionId) throws OpenViduException {
		Session session = sessions.get(sessionId);
		if (session == null) {
			log.error("Session:{} not found.", sessionId);
			return Collections.emptySet();
		}
		Set<Participant> participants = session.getMajorPartEachIncludeThorConnect();
		participants.removeIf(Participant::isClosed);
		return participants;
	}

	public Participant getParticipantByPrivateAndPublicId(String sessionId, String participantPrivateId, String participantPublicId) throws OpenViduException {
		Session session = sessions.get(sessionId);
		if (session == null) {
			throw new OpenViduException(Code.ROOM_NOT_FOUND_ERROR_CODE, "Session '" + sessionId + "' not found");
		}
		Participant participant = session.getPartByPrivateIdAndPublicId(participantPrivateId, participantPublicId);
		if (participant == null) {
			throw new OpenViduException(Code.USER_NOT_FOUND_ERROR_CODE,
					"Participant '" + participantPublicId + "' not found in session '" + sessionId + "'");
		}
		return participant;
	}

	/**
	 * Returns a participant in a session
	 *
	 * @param sessionId            identifier of the session
	 * @param participantPrivateId private identifier of the participant
	 * @param streamType type of stream
	 * @return {@link Participant}
	 * @throws OpenViduException in case the session doesn't exist or the
	 *                           participant doesn't belong to it
	 */
	public Participant getParticipant(String sessionId, String participantPrivateId, StreamType streamType) throws OpenViduException {
		Session session = sessions.get(sessionId);
		if (session == null) {
			throw new OpenViduException(Code.ROOM_NOT_FOUND_ERROR_CODE, "Session '" + sessionId + "' not found");
		}
		return session.getPartByPrivateIdAndStreamType(participantPrivateId, streamType);
		/*if (participant == null) {
			throw new OpenViduException(Code.USER_NOT_FOUND_ERROR_CODE,
					"Participant '" + participantPrivateId + "' not found in session '" + sessionId + "'");
		}*/
	}

	/**
	 * Returns a participant in a session
	 *
	 * @param sessionId            identifier of the session
	 * @param participantPrivateId private identifier of the participant
	 * @return {@link Participant}
	 * @throws OpenViduException in case the session doesn't exist or the
	 *                           participant doesn't belong to it
	 */
	public Participant getParticipant(String sessionId, String participantPrivateId) throws OpenViduException {
		return this.getParticipant(sessionId, participantPrivateId, StreamType.MAJOR);
	}

	/**
	 * Returns a participant
	 *
	 * @param participantPrivateId private identifier of the participant
	 * @param streamType type of stream
	 * @return {@link Participant}
	 * @throws OpenViduException in case the participant doesn't exist
	 */
	public Participant getParticipant(String participantPrivateId, StreamType streamType) throws OpenViduException {
		for (Session session : sessions.values()) {
			if (!session.isClosed()) {
				Participant participant = session.getPartByPrivateIdAndStreamType(participantPrivateId, streamType);
				if (!Objects.isNull(participant)) {
					return participant;
				}
//				if (Objects.isNull(participant))
//					throw new OpenViduException(Code.USER_NOT_FOUND_ERROR_CODE,
//							"No participant with private id '" + participantPrivateId + "' was found");
//				return participant;
			}
		}
//		throw new OpenViduException(Code.USER_NOT_FOUND_ERROR_CODE,
//				"No participant with private id '" + participantPrivateId + "' was found");
		log.warn("No participant with private id:{} was found.", participantPrivateId);
		return null;
	}

	/**
	 * Returns a participant
	 *
	 * @param participantPrivateId private identifier of the participant
	 * @return {@link Participant}
	 * @throws OpenViduException in case the participant doesn't exist
	 */
	public Participant getParticipant(String participantPrivateId) throws OpenViduException {
		return this.getParticipant(participantPrivateId, StreamType.MAJOR);
	}

	public Participant getSpeakerPart(String sessionId) {
		Session session = sessions.get(sessionId);
		if (Objects.nonNull(session)) {
			return session.getSpeakerPart();
		}

		return null;
	}

	public Participant getModeratorPart(String sessionId) {
		Session session = sessions.get(sessionId);
		if (Objects.nonNull(session)) {
			return session.getModeratorPart();
		}

		return null;
	}

	public Map<String, FinalUser> getFinalUsers(String sessionId) {
		return this.sessionidFinalUsers.get(sessionId);
	}

	public Map<String, FinalUser> removeFinalUsers(String sessionId) {
		return this.sessionidFinalUsers.remove(sessionId);
	}

	public Collection<CDREventRecording> getAccumulatedRecordings(String sessionId) {
		return this.sessionidAccumulatedRecordings.get(sessionId);
	}

	public Collection<CDREventRecording> removeAccumulatedRecordings(String sessionId) {
		return this.sessionidAccumulatedRecordings.remove(sessionId);
	}

	public MediaOptions generateMediaOptions(Request<JsonObject> request) {
		return null;
	}

	public Session storeSessionNotActive(String sessionId, SessionProperties sessionProperties) {
		Session sessionNotActive = new Session(sessionId, sessionProperties, openviduConfig, recordingManager, livingManager);
		dealSessionNotActiveStored(sessionId, sessionNotActive);
		showTokens();
		return sessionNotActive;
	}

	public Session storeSessionNotActive(Session sessionNotActive) {
		final String sessionId = sessionNotActive.getSessionId();
		dealSessionNotActiveStored(sessionId, sessionNotActive);
		showTokens();
		return sessionNotActive;
	}

	public Session storeSessionNotActiveWhileRoomCreated(String sessionId) {
		Session sessionNotActive = new Session(sessionId,
				new SessionProperties.Builder().customSessionId(sessionId).build(), openviduConfig, recordingManager, livingManager);
		dealSessionNotActiveStored(sessionId, sessionNotActive);
		return sessionNotActive;
	}

	private void dealSessionNotActiveStored(String sessionId, Session sessionNotActive) {
		this.sessionsNotActive.put(sessionId, sessionNotActive);
		log.info("sessionidParticipantpublicidParticipant sessionId:{}, value:{}", sessionId, sessionidParticipantpublicidParticipant.get(sessionId));
		this.sessionidParticipantpublicidParticipant.putIfAbsent(sessionId, new ConcurrentHashMap<>());
		log.info("sessionidParticipantpublicidParticipant sessionId:{}, value:{}", sessionId, sessionidParticipantpublicidParticipant.get(sessionId));
		this.sessionidFinalUsers.putIfAbsent(sessionId, new ConcurrentHashMap<>());
		if (this.openviduConfig.isRecordingModuleEnabled()) {
			this.sessionidAccumulatedRecordings.putIfAbsent(sessionId, new ConcurrentLinkedQueue<>());
		}
	}

	public String newToken(String sessionId, OpenViduRole role, String serverMetadata,
			KurentoTokenOptions kurentoTokenOptions) throws OpenViduException {

		ConcurrentHashMap<String, Token> map = this.sessionidTokenTokenobj.putIfAbsent(sessionId,
				new ConcurrentHashMap<>());
		if (map != null) {

			if (!formatChecker.isServerMetadataFormatCorrect(serverMetadata)) {
				log.error("Data invalid format");
				throw new OpenViduException(Code.GENERIC_ERROR_CODE, "Data invalid format");
			}

			Token token = tokenGenerator.generateToken(sessionId, role, serverMetadata, kurentoTokenOptions);

			map.putIfAbsent(token.getToken(), token);
			showTokens();
			return token.getToken();

		} else {
			this.sessionidTokenTokenobj.remove(sessionId);
			log.error("sessionId [" + sessionId + "] was not found");
			throw new OpenViduException(Code.ROOM_NOT_FOUND_ERROR_CODE, "sessionId [" + sessionId + "] not found");
		}
	}

	/*public boolean isTokenValidInSession(String token, String sessionId, String participanPrivatetId) {
		if (!this.isInsecureParticipant(participanPrivatetId)) {
			if (this.sessionidTokenTokenobj.get(sessionId) != null) {
				return this.sessionidTokenTokenobj.get(sessionId).containsKey(token);
			} else {
				return false;
			}
		} else {
			this.sessionidParticipantpublicidParticipant.putIfAbsent(sessionId, new ConcurrentHashMap<>());
			this.sessionidFinalUsers.putIfAbsent(sessionId, new ConcurrentHashMap<>());
			if (this.openviduConfig.isRecordingModuleEnabled()) {
				this.sessionidAccumulatedRecordings.putIfAbsent(sessionId, new ConcurrentLinkedQueue<>());
			}
			this.sessionidTokenTokenobj.putIfAbsent(sessionId, new ConcurrentHashMap<>());
			this.sessionidTokenTokenobj.get(sessionId).putIfAbsent(token,
					new Token(token, OpenViduRole.PUBLISHER, "",
							this.coturnCredentialsService.isCoturnAvailable()
									? this.coturnCredentialsService.createUser()
									: null,
							null));
			return true;
		}
	}*/

	/*public void recordParticipantBySessionId(String sessionId) {
		this.sessionidParticipantpublicidParticipant.putIfAbsent(sessionId, new ConcurrentHashMap<>());
		this.sessionidFinalUsers.putIfAbsent(sessionId, new ConcurrentHashMap<>());
		if (this.openviduConfig.isRecordingModuleEnabled()) {
			this.sessionidAccumulatedRecordings.putIfAbsent(sessionId, new ConcurrentLinkedQueue<>());
		}
	}*/

	public boolean isPublisherInSession(String sessionId, Participant participant,SessionPresetEnum sessionPresetEnum) {
		if (!this.isInsecureParticipant(participant.getParticipantPrivateId())) {
			if (this.sessionidParticipantpublicidParticipant.get(sessionId) != null) {

				if (OpenViduRole.PUBLISHER.equals(participant.getRole()) || OpenViduRole.MODERATOR.equals(participant.getRole())
						|| (OpenViduRole.ONLY_SHARE.equals(participant.getRole()) && participant.getStreamType().equals(StreamType.SHARING)
						|| participant.getTerminalType() == TerminalTypeEnum.S)
				) {
					return true;
				}
				return sessionPresetEnum.equals(SessionPresetEnum.on) && OpenViduRole.SUBSCRIBER.equals(participant.getRole());
			} else {
				return false;
			}
		} else {
			return true;
		}
	}

	public boolean isModeratorInSession(String sessionId, Participant participant) {
		if (!this.isInsecureParticipant(participant.getParticipantPrivateId())) {
			if (this.sessionidParticipantpublicidParticipant.get(sessionId) != null) {
				return OpenViduRole.MODERATOR_ROLES.contains(participant.getRole());
			} else {
				return false;
			}
		} else {
			return true;
		}
	}

	public boolean isInsecureParticipant(String participantPrivateId) {
		if (this.insecureUsers.containsKey(participantPrivateId)) {
			log.info("The user with private id {} is an INSECURE user", participantPrivateId);
			return true;
		}
		return false;
	}

	public void newInsecureParticipant(String participantPrivateId) {
		this.insecureUsers.put(participantPrivateId, true);
	}

//	public Participant newParticipant(String sessionId, String participantPrivatetId, Token token,
	public Participant newParticipant(Long userId, String sessionId, String participantPrivatetId, String clientMetadata, String role,
									  String streamType, GeoLocation location, String platform, String finalUserId, String ability,String functionality) {
		if (this.sessionidParticipantpublicidParticipant.get(sessionId) != null) {
			String participantPublicId = RandomStringUtils.randomAlphanumeric(16).toLowerCase();
//			Participant p = new Participant(finalUserId, participantPrivatetId, participantPublicId, sessionId, token,
			Participant p = new Participant(userId, finalUserId, participantPrivatetId, participantPublicId, sessionId, OpenViduRole.parseRole(role),
					StreamType.valueOf(streamType), clientMetadata, location, platform, null, ability, functionality);
			while (this.sessionidParticipantpublicidParticipant.get(sessionId).putIfAbsent(participantPublicId,
					p) != null) {
				participantPublicId = RandomStringUtils.randomAlphanumeric(16).toLowerCase();
				p.setParticipantPublicId(participantPublicId);
			}

			FinalUser finalUser = this.sessionidFinalUsers.get(sessionId).get(finalUserId);
			if (finalUser == null) {
				//First connection for new final user
				log.info("Participant {} of session {} belongs to a new final user", p.getParticipantPublicId(),
						sessionId);
				this.sessionidFinalUsers.get(sessionId).put(finalUserId, new FinalUser(finalUserId, sessionId, p));
			} else {
				// New connection for previously existing final user
				log.info("Participant {} of session {} belongs to a previously existing user",
						p.getParticipantPublicId(), sessionId);
				finalUser.addConnection(p);
			}

			return p;
		} else {
			throw new OpenViduException(Code.ROOM_NOT_FOUND_ERROR_CODE, sessionId);
		}
	}

//	public Participant newRecorderParticipant(String sessionId, String participantPrivatetId, Token token,
	public Participant newRecorderParticipant(Long userId, String sessionId, String participantPrivatetId, String clientMetadata,
											  String role, String streamType) {
		if (this.sessionidParticipantpublicidParticipant.get(sessionId) != null) {

			Participant p = new Participant(userId, null, participantPrivatetId, ProtocolElements.RECORDER_PARTICIPANT_PUBLICID,
					sessionId, OpenViduRole.parseRole(role), StreamType.valueOf(streamType), clientMetadata, null, null, null, null, null);
			this.sessionidParticipantpublicidParticipant.get(sessionId)
					.put(ProtocolElements.RECORDER_PARTICIPANT_PUBLICID, p);
			return p;
		} else {
			throw new OpenViduException(Code.ROOM_NOT_FOUND_ERROR_CODE, sessionId);
		}
	}

	public Token consumeToken(String sessionId, String participantPrivateId, String token) {
		if (this.sessionidTokenTokenobj.get(sessionId) != null) {
			Token t = this.sessionidTokenTokenobj.get(sessionId).remove(token);
			if (t != null) {
				return t;
			} else {
				throw new OpenViduException(Code.TOKEN_CANNOT_BE_CREATED_ERROR_CODE, sessionId);
			}
		} else {
			throw new OpenViduException(Code.ROOM_NOT_FOUND_ERROR_CODE, sessionId);
		}
	}

	public void showTokens() {
		log.info("<SESSIONID, TOKENS>: {}", this.sessionidTokenTokenobj.toString());
	}

	/**
	 * Closes all resources. This method has been annotated with the @PreDestroy
	 * directive (javax.annotation package) so that it will be automatically called
	 * when the SessionManager instance is container-managed. <br/>
	 * <strong>Dev advice:</strong> Send notifications to all participants to inform
	 * that their session has been forcibly closed.
	 *
//	 * @see SessionManmager#closeSession(String)
	 */
	@PreDestroy
	public void close() {
		log.info("Closing all sessions and update user/device online status");
		notificationService.getRpcConnections().forEach(rpcConnection ->
				cacheManage.updateTerminalStatus(rpcConnection, TerminalStatus.offline));
		for (String sessionId : sessions.keySet()) {
			try {
				// stop the record task
				stopRecording(sessionId);
				closeSession(sessionId, EndReason.openviduServerStopped);
			} catch (Exception e) {
				log.warn("Error closing session '{}'", sessionId, e);
			}
		}
	}

	/**
	 * Closes an existing session by releasing all resources that were allocated for
	 * it. Once closed, the session can be reopened (will be empty and it will use
	 * another Media Pipeline). Existing participants will be evicted. <br/>
	 * <strong>Dev advice:</strong> The session event handler should send
	 * notifications to the existing participants in the session to inform that it
	 * was forcibly closed.
	 *
	 * @param sessionId identifier of the session
	 * @return
	 * @return set of {@link Participant} POJOS representing the session's
	 *         participants
	 * @throws OpenViduException in case the session doesn't exist or has been
	 *                           already closed
	 */
	public Set<Participant> closeSession(String sessionId, EndReason reason) {
		Session session = sessions.get(sessionId);
		if (session == null) {
			throw new OpenViduException(Code.ROOM_NOT_FOUND_ERROR_CODE, "Session '" + sessionId + "' not found");
		}
		if (session.isClosed()) {
			this.closeSessionAndEmptyCollections(session, reason);
			throw new OpenViduException(Code.ROOM_CLOSED_ERROR_CODE, "Session '" + sessionId + "' already closed");
		}
		Set<Participant> participants = getParticipants(sessionId);

		// set session status: closing
        session.setClosing(true);
		boolean sessionClosedByLastParticipant = false;
		if (openviduConfig.isRecordingModuleEnabled() && session.isRecording.get()) {
			// stop recording
			log.info("Stopping recording of close session {}", sessionId);
			stopRecording(sessionId);
		}
		UseTime.point("closeSession");
		UseTime.Point point = UseTime.getPoint("closeSession.evictParticipant");
		for (Participant p : participants) {
			try {
                sessionClosedByLastParticipant = this.evictParticipantByCloseRoom(p, null, null, reason);
			} catch (OpenViduException e) {
				log.warn("Error evicting participant '{}' from session '{}'", p.getParticipantPublicId(), sessionId, e);
			}
		}
		point.updateTime();

		if (!sessionClosedByLastParticipant) {
			// This code should never be executed, as last evicted participant must trigger
			// session close
			this.closeSessionAndEmptyCollections(session, reason);
		}

		return participants;
	}

	public void closeSessionAndEmptyCollections(Session session, EndReason reason) {

//		if (openviduConfig.isRecordingModuleEnabled()
//				&& this.recordingManager.sessionIsBeingRecorded(session.getSessionId())) {
//			recordingManager.stopRecording(session, null, RecordingManager.finalReason(reason));
//		}

		if (session.close(reason)) {
			sessionEventsHandler.onSessionClosed(session.getSessionId(), reason);
		}

		this.cleanCollections(session.getSessionId());
		this.updateConferenceInfo(session.getSessionId());

		log.info("Session '{}' removed and closed", session.getSessionId());
	}

	protected void cleanCollections(String sessionId) {
		// clean the room info in cache
		cacheManage.delRoomInfo(sessionId);

		cleanCacheCollections(sessionId);
		sessions.remove(sessionId);
		sessionsNotActive.remove(sessionId);
		log.info("sessionidParticipantpublicidParticipant sessionId:{}, value:{}", sessionId, sessionidParticipantpublicidParticipant.get(sessionId));
		sessionidParticipantpublicidParticipant.remove(sessionId);
		log.info("sessionidParticipantpublicidParticipant sessionId:{}, value:{}", sessionId, sessionidParticipantpublicidParticipant.get(sessionId));
		sessionidFinalUsers.remove(sessionId);
		sessionidAccumulatedRecordings.remove(sessionId);
		sessionidTokenTokenobj.remove(sessionId);
	}

	public void cleanCacheCollections(String sessionId) {
		if (sessionidConferenceInfo.containsKey(sessionId)) {
			sessionidConferenceInfo.remove(sessionId);
		}

		if (sessionidPreset.containsKey(sessionId)) {
			sessionidPreset.remove(sessionId);
		}
	}

	public void updateConferenceInfo(String sessionId) {
		// TODO. update sd_conference status info.
		ConferenceSearch search = new ConferenceSearch();
		search.setRoomId(sessionId);
		search.setStatus(1);
		List<Conference> conferences = conferenceMapper.selectBySearchCondition(search);
		if (conferences == null || conferences.isEmpty()) {
			log.warn("can not find conference {} when closed.", sessionId);
			return;
		}

		conferences.forEach(this::endConferenceInfo);
		conferences.forEach(this::endApptConferenceInfo);
	}

	public void endConferenceInfo(Conference conference) {
		conference.setStatus(2);
		conference.setEndTime(new Date());
		conferenceMapper.updateByPrimaryKey(conference);
	}

	private void endApptConferenceInfo(Conference conference) {
		if (conference.getRuid().startsWith("appt-")) {
			appointConferenceMapper.changeStatusByRuid(ConferenceStatus.FINISHED.getStatus(), conference.getRuid());
		}
	}

	public boolean isNewSessionIdValid(String sessionId) {
		// TODO
//		if (sessionidConferenceInfo.containsKey(sessionId)) return false;
		sessionidConferenceInfo.put(sessionId, new ConcurrentHashMap<>());
		return true;
	}

	public boolean isSessionIdValid(String sessionId) {
		return sessionidConferenceInfo.containsKey(sessionId);
	}

	public boolean isConflictSharing(String sessionId, String sourceId) {
		ConcurrentHashMap<String, String> sessionInfo = sessionidConferenceInfo.get(sessionId);
		if (sessionInfo.containsKey("sharingSourceId")) return false;
		sessionInfo.put("sharingSourceId", sourceId);
		return true;
	}

	public boolean setPresetInfo(String sessionId, SessionPreset preset) {
	    /*if (!Objects.isNull(sessionidPreset.get(sessionId))) {
            log.info("session {} {} replace preset info {}", sessionId, preset, sessionidPreset.get(sessionId));
        }*/
	    sessionidPreset.put(sessionId, preset);
        return true;
    }

    public SessionPreset getPresetInfo(String sessionId) {
	    if (Objects.isNull(sessionidPreset.get(sessionId))) {
	        sessionidPreset.put(sessionId, new SessionPreset());
        }

        return sessionidPreset.get(sessionId);
    }

	public ConcurrentHashMap<String, String> getSessionInfo(String sessionId) {
		return sessionidConferenceInfo.get(sessionId);
	}

	public void unpublishAllStream(String sessionId, EndReason reason) {
		Session session = sessions.get(sessionId);
		if (session == null) {
			throw new OpenViduException(Code.ROOM_NOT_FOUND_ERROR_CODE, "Session '" + sessionId + "' not found");
		}
		if (session.isClosed()) {
			this.closeSessionAndEmptyCollections(session, reason);
			throw new OpenViduException(Code.ROOM_CLOSED_ERROR_CODE, "Session '" + sessionId + "' already closed");
		}

		Set<Participant> participants = getParticipants(sessionId);

		if (EndReason.forceCloseSessionByUser.equals(reason) ||
				EndReason.closeSessionByModerator.equals(reason)) {
			for (Participant p : participants) {
				try {
					this.unpublishVideo(p, null, null, reason);
				} catch (OpenViduException e) {
					log.warn("Error evicting participant '{}' from session '{}'", p.getParticipantPublicId(), sessionId, e);
				}
			}
		}
	}

	public void dealSessionClose(String sessionId, EndReason endReason) {
		Session session = this.getSession(sessionId);
		if (Objects.isNull(session)) {
			return;
		} else {
			session.setClosing(true);
		}
		stopRecording(sessionId);
		session.getParticipants().forEach(p -> {
			if (!Objects.equals(StreamType.MAJOR, p.getStreamType())) return;
			notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.CLOSE_ROOM_NOTIFY_METHOD, new JsonObject());
			RpcConnection rpcConnect = notificationService.getRpcConnection(p.getParticipantPrivateId());
			if (!Objects.isNull(rpcConnect) && !Objects.isNull(rpcConnect.getSerialNumber())) {
				cacheManage.setDeviceStatus(rpcConnect.getSerialNumber(), DeviceStatus.online.name());
			}});
//		this.unpublishAllStream(sessionId, endReason);
		this.closeSession(sessionId, endReason);
	}

	public ErrorCodeEnum setRollCallInSession(Session conferenceSession, Participant targetPart) {
		ErrorCodeEnum errorCode = ErrorCodeEnum.SUCCESS;
		Set<Participant> participants = conferenceSession.getParticipants();
		Participant moderatorPart = conferenceSession.getModeratorPart();
		if (moderatorPart == null) {
			return ErrorCodeEnum.MODERATOR_NOT_FOUND;
		}
		boolean isMcu = Objects.equals(conferenceSession.getConferenceMode(), ConferenceModeEnum.MCU);
		String sourceConnectionId;
		String targetConnectionId;
		Participant existSpeakerPart = null;
		for (Participant participant : participants) {
			if (Objects.equals(StreamType.MAJOR, participant.getStreamType())) {
				if (Objects.equals(ParticipantHandStatus.speaker, participant.getHandStatus())) {
					existSpeakerPart = participant;
				}
			}
		}

		// do nothing when set roll call to the same speaker
		if (Objects.equals(existSpeakerPart, targetPart)) {
			return ErrorCodeEnum.SET_ROLL_CALL_SAME_PART;
		}

		if (Objects.isNull(existSpeakerPart)) {
			// switch layout
			if (conferenceSession.getMajorShareMixLinkedArr().size() > 0) {
				JsonObject firstOrderPart = conferenceSession.getMajorShareMixLinkedArr().get(0).getAsJsonObject();
				if (firstOrderPart.get("streamType").getAsString().equals(StreamType.SHARING.name())) {
					sourceConnectionId = conferenceSession.getMajorShareMixLinkedArr().get(1).getAsJsonObject().get("connectionId").getAsString();
				} else {
					sourceConnectionId = firstOrderPart.get("connectionId").getAsString();
				}
			} else {
				sourceConnectionId = null;
			}

		} else {
			// switch layout with current speaker participant
			sourceConnectionId = existSpeakerPart.getParticipantPublicId();
			// change current speaker part status and send notify
			existSpeakerPart.changeHandStatus(ParticipantHandStatus.endSpeaker);
			JsonObject params = new JsonObject();
			params.addProperty(ProtocolElements.END_ROLL_CALL_ROOM_ID_PARAM, conferenceSession.getSessionId());
			params.addProperty(ProtocolElements.END_ROLL_CALL_SOURCE_ID_PARAM, moderatorPart.getUuid());
			params.addProperty(ProtocolElements.END_ROLL_CALL_TARGET_ID_PARAM, existSpeakerPart.getUuid());

			boolean sendChangeRole;
			if (sendChangeRole = (existSpeakerPart.getOrder() > openviduConfig.getSfuPublisherSizeLimit() - 1)) {
				existSpeakerPart.changePartRole(OpenViduRole.SUBSCRIBER);
				//下墙处理音频及共享
				setMicStatusAndDealExistsSharing(existSpeakerPart, moderatorPart, conferenceSession.getSessionId());
			}
			JsonArray changeRoleNotifiParam = sendChangeRole ? conferenceSession.getPartRoleChangedNotifyParamArr(existSpeakerPart,
					OpenViduRole.PUBLISHER, OpenViduRole.SUBSCRIBER) : null;
			sendEndRollCallNotify(participants, params, sendChangeRole, changeRoleNotifiParam);
		}

		assert targetPart != null;
		targetPart.changeHandStatus(ParticipantHandStatus.speaker);
		targetConnectionId = targetPart.getParticipantPublicId();

		if (isMcu) {
			// change conference layout
			conferenceSession.replacePartOrderInConference(sourceConnectionId, targetConnectionId);
			// json RPC notify KMS layout changed.
			conferenceSession.invokeKmsConferenceLayout();
		}

		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.SET_ROLL_CALL_ROOM_ID_PARAM, conferenceSession.getSessionId());
		params.addProperty(ProtocolElements.SET_ROLL_CALL_SOURCE_ID_PARAM, moderatorPart.getUuid());
		params.addProperty(ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM, targetPart.getUuid());

		boolean sendChangeRole;
		if (sendChangeRole = (targetPart.getOrder() > openviduConfig.getSfuPublisherSizeLimit() - 1)) {
			targetPart.changePartRole(OpenViduRole.PUBLISHER);
		}
		JsonArray changeRoleNotifiParam = sendChangeRole ? conferenceSession.getPartRoleChangedNotifyParamArr(targetPart,
				OpenViduRole.SUBSCRIBER, OpenViduRole.PUBLISHER) : null;

		// broadcast the changes of layout
		participants.forEach(participant -> {
			if (!Objects.equals(StreamType.MAJOR, participant.getStreamType())) {
				return;
			}
			// SetRollCall notify
			this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.SET_ROLL_CALL_METHOD, params);
			if (sendChangeRole) {
				this.notificationService.sendNotification(participant.getParticipantPrivateId(),
						ProtocolElements.NOTIFY_PART_ROLE_CHANGED_METHOD, changeRoleNotifiParam);
			}
			if (isMcu) {
				// broadcast the changes of layout
				this.notificationService.sendNotification(participant.getParticipantPrivateId(),
						ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, conferenceSession.getLayoutNotifyInfo());
			}
			JsonArray targetIds = new JsonArray();
			targetIds.add(targetPart.getUuid());
			if (ParticipantSpeakerStatus.off.equals(targetPart.getSpeakerStatus())) {
				JsonObject audioSpeakerParams = new JsonObject();
				audioSpeakerParams.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_ID_PARAM, conferenceSession.getSessionId());
				audioSpeakerParams.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_SOURCE_ID_PARAM, moderatorPart.getUuid());
				audioSpeakerParams.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_STATUS_PARAM,"on");
				audioSpeakerParams.add(ProtocolElements.SET_AUDIO_SPEAKER_TARGET_ID_PARAM, targetIds);
				this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.SET_AUDIO_SPEAKER_STATUS_METHOD, audioSpeakerParams);
			}
			if (ParticipantMicStatus.off.equals(targetPart.getMicStatus())) {
				JsonObject audioParams = new JsonObject();
				audioParams.addProperty(ProtocolElements.SET_AUDIO_ROOM_ID_PARAM, conferenceSession.getSessionId());
				audioParams.addProperty(ProtocolElements.SET_AUDIO_SOURCE_PARAM, moderatorPart.getUuid());
				audioParams.addProperty(ProtocolElements.SET_AUDIO_STATUS_PARAM,"on");
				audioParams.add(ProtocolElements.SET_VIDEO_TARGETS_PARAM, targetIds);
				this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.SET_AUDIO_STATUS_METHOD, audioParams);
			}
		});
		targetPart.setSpeakerStatus(ParticipantSpeakerStatus.on);
		targetPart.setMicStatus(ParticipantMicStatus.on);
		return errorCode;
	}

	public void setMicStatusAndDealExistsSharing(Participant participant, Participant moderatorPart, String sessionId) {
		boolean micStatusFlag;
		JsonObject audioParams = new JsonObject();
		if (micStatusFlag = ParticipantMicStatus.on.equals(participant.getMicStatus())) {
			participant.setMicStatus(ParticipantMicStatus.off);
			JsonArray targetIds = new JsonArray();
			targetIds.add(participant.getUuid());
			audioParams.addProperty(ProtocolElements.SET_AUDIO_ROOM_ID_PARAM, sessionId);
			audioParams.addProperty(ProtocolElements.SET_AUDIO_SOURCE_PARAM, moderatorPart.getUuid());
			audioParams.addProperty(ProtocolElements.SET_AUDIO_STATUS_PARAM,"off");
			audioParams.add(ProtocolElements.SET_VIDEO_TARGETS_PARAM, targetIds);
		}
		//判断是否存在共享
		Participant sharePart = getSession(sessionId).getPartByPrivateIdAndStreamType(participant.getParticipantPrivateId(), StreamType.SHARING);
		boolean existsSharingFlag;
		JsonObject stopSharingParams = new JsonObject();
		if (existsSharingFlag = Objects.nonNull(sharePart)) {
			leaveRoom(sharePart, null, EndReason.sessionClosedByServer, false);
			stopSharingParams.addProperty(ProtocolElements.SHARING_CONTROL_ROOMID_PARAM, sessionId);
			stopSharingParams.addProperty(ProtocolElements.SHARING_CONTROL_SOURCEID_PARAM, "");
			stopSharingParams.addProperty(ProtocolElements.SHARING_CONTROL_TARGETID_PARAM, sharePart.getUuid());
			stopSharingParams.addProperty(ProtocolElements.SHARING_CONTROL_OPERATION_PARAM, ParticipantShareStatus.off.name());
			stopSharingParams.addProperty(ProtocolElements.SHARING_CONTROL_MODE_PARAM, 0);
		}
		getParticipants(sessionId).forEach(part ->{
			if (Objects.equals(StreamType.MAJOR, part.getStreamType())) {
				if (micStatusFlag) {
					this.notificationService.sendNotification(part.getParticipantPrivateId(), ProtocolElements.SET_AUDIO_STATUS_METHOD, audioParams);
				}
				if (existsSharingFlag) {
					this.notificationService.sendNotification(part.getParticipantPrivateId(),
							ProtocolElements.SHARING_CONTROL_NOTIFY, stopSharingParams);
				}
			}
		});
	}

	private void sendEndRollCallNotify(Set<Participant> participants, JsonObject params,boolean sendChangeRole, JsonArray changeRoleNotifiParam) {
		participants.forEach(participant -> {
			if (!Objects.equals(StreamType.MAJOR, participant.getStreamType())) {
				return;
			}
			this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.END_ROLL_CALL_METHOD, params);
			if (sendChangeRole) {
				this.notificationService.sendNotification(participant.getParticipantPrivateId(),
						ProtocolElements.NOTIFY_PART_ROLE_CHANGED_METHOD, changeRoleNotifiParam);
			}
		});
	}

	public void setStartRecordingTime(String sessionId, Long startRecordingTime) {
		Session session = sessions.get(sessionId);
		if (session == null) {
			throw new OpenViduException(Code.ROOM_NOT_FOUND_ERROR_CODE, "Session '" + sessionId + "' not found");
		}
		session.setStartRecordingTime(startRecordingTime);
	}

	public void setStopRecordingTime(String sessionId, Long stopRecordingTime) {
		Session session = sessions.get(sessionId);
		if (session == null) {
			throw new OpenViduException(Code.ROOM_NOT_FOUND_ERROR_CODE, "Session '" + sessionId + "' not found");
		}
		session.setStopRecordingTime(stopRecordingTime);
	}

	public void setStartLivingTime(String sessionId, Long startLivingTime) {
		Session session = sessions.get(sessionId);
		if (session == null) {
			throw new OpenViduException(Code.ROOM_NOT_FOUND_ERROR_CODE, "Session '" + sessionId + "' not found");
		}
		session.setStartLivingTime(startLivingTime);
	}

	public String getLivingUrl(Session session) {
		if (Objects.isNull(session)) return null;
		Living living = livingManager.getLiving(session.getSessionId());
		return Objects.isNull(living) ? null : living.getUrl();
	}

	public boolean joinRoomDuplicately(String uuid) {
		/*Session session;
		return StreamType.MAJOR.equals(streamType) && Objects.nonNull(session = getSession(sessionId))
				&& Objects.nonNull(session.getParticipantByUUID(userUuid));*/
		return cacheManage.existsConferenceRelativeInfo(CacheKeyConstants.getParticipantKey(uuid));
	}


	public Session storeSessionNotActiveWhileAppointCreate(String roomId, Conference conference) {
        log.info("===>storeSessionNotActiveWhileAppointCreate:{}", roomId);
        Session session = storeSessionNotActiveWhileRoomCreated(roomId);
        session.setConference(conference);
        session.setRuid(conference.getRuid());
        return session;
    }

	public abstract void startRecording(String sessionId);

	public abstract void stopRecording(String sessionId);

	public abstract void updateRecording(String sessionId);

	public abstract void updateRecording(String sessionId, Participant curParticipant);

    public abstract void handleRecordErrorEvent(Object msg);

	/**
	 * 创建分发的隧道
	 * @param participant
	 */
	public abstract void createDeliverChannel(Participant participant);
}
