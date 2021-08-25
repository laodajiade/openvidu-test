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
import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
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
import io.openvidu.server.kurento.endpoint.PublisherEndpoint;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    @Resource
    protected InviteCompensationManage inviteCompensationManage;

    @Resource
    protected TimerManager timerManager;

    public FormatChecker formatChecker = new FormatChecker();

    protected ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();
    protected ConcurrentMap<String, Session> sessionsNotActive = new ConcurrentHashMap<>();
    //@Deprecated // delete 2.0废弃
    //protected ConcurrentMap<String, ConcurrentHashMap<String, Participant>> sessionidParticipantpublicidParticipant = new ConcurrentHashMap<>();
    //@Deprecated // delete 2.0废弃
    //protected ConcurrentMap<String, ConcurrentHashMap<String, FinalUser>> sessionidFinalUsers = new ConcurrentHashMap<>();
    //@Deprecated // delete 2.0废弃
    //protected ConcurrentMap<String, ConcurrentLinkedQueue<CDREventRecording>> sessionidAccumulatedRecordings = new ConcurrentHashMap<>();

    protected ConcurrentMap<String, Boolean> insecureUsers = new ConcurrentHashMap<>();
    public ConcurrentMap<String, ConcurrentHashMap<String, Token>> sessionidTokenTokenobj = new ConcurrentHashMap<>();

    protected ConcurrentMap<String, ConcurrentHashMap<String, String>> sessionidConferenceInfo = new ConcurrentHashMap<>();

    public abstract void joinRoom(Participant participant, String sessionId, Conference conference, Integer transactionId);

    public abstract boolean leaveRoom(Participant participant, Integer transactionId, EndReason reason,
                                      boolean closeWebSocket);

    public abstract boolean leaveRoomSimple(Participant participant, Integer transactionId, EndReason reason,
                                            boolean closeWebSocket);

    // public abstract void changeSharingStatusInConference(KurentoSession session, Participant participant);

    public abstract RpcConnection accessOut(RpcConnection rpcConnection);

    public abstract void publishVideo(Participant participant, MediaOptions mediaOptions, Integer transactionId, StreamType streamType);

    public abstract void unpublishVideo(Participant participant, String publishId, Integer transactionId,
                                        EndReason reason);

    public abstract void subscribe(Participant participant, Participant sender, StreamType streamType, StreamModeEnum streamMode,
                                   String sdpOffer, String publishStreamId, Integer transactionId);

    public abstract void unsubscribe(Participant participant, String senderName, Integer transactionId);

    public abstract void switchVoiceMode(Participant participant, VoiceMode operation);

    public abstract void pauseAndResumeStream(Participant pausePart, String subscribeId, OperationMode operation, String mediaType);

    public abstract void sendMessage(Participant participant, String message, Integer transactionId);

    //delete 2.0
//    public abstract void streamPropertyChanged(Participant participant, Integer transactionId, String streamId,
//                                               String property, JsonElement newValue, String changeReason);

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

    public abstract void evictParticipantByUUID(String sessionId, String uuid, List<EvictParticipantStrategy> evictStrategies, EndReason endReason);

    public abstract void evictParticipantByUUIDEx(String sessionId, String uuid, List<EvictParticipantStrategy> evictStrategies, EndReason endReason);

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
        Set<Participant> participants = session.getParticipants();
        participants.removeIf(Participant::isClosed);
        return participants;
    }

    //delete 2.0
//    public Participant getParticipantByPrivateAndPublicId(String sessionId, String participantPrivateId, String participantPublicId) throws OpenViduException {
//        Session session = sessions.get(sessionId);
//        if (session == null) {
//            throw new OpenViduException(Code.ROOM_NOT_FOUND_ERROR_CODE, "Session '" + sessionId + "' not found");
//        }
//        Participant participant = session.getPartByPrivateIdAndPublicId(participantPrivateId, participantPublicId);
//        if (participant == null) {
//            throw new OpenViduException(Code.USER_NOT_FOUND_ERROR_CODE,
//                    "Participant '" + participantPublicId + "' not found in session '" + sessionId + "'");
//        }
//        return participant;
//    }

    /**
     * Returns a participant in a session
     *
     * @param sessionId            identifier of the session
     * @param participantPrivateId private identifier of the participant
     * @param streamType           type of stream
     * @return {@link Participant}
     * @throws OpenViduException in case the session doesn't exist or the
     *                           participant doesn't belong to it
     */
    // delete 2.0
    //   @Deprecated
//    public Participant getParticipant(String sessionId, String participantPrivateId, StreamType streamType) throws OpenViduException {
//        Session session = sessions.get(sessionId);
//        if (session == null) {
//            throw new OpenViduException(Code.ROOM_NOT_FOUND_ERROR_CODE, "Session '" + sessionId + "' not found");
//        }
//        return session.getPartByPrivateIdAndStreamType(participantPrivateId, streamType);
//		/*if (participant == null) {
//			throw new OpenViduException(Code.USER_NOT_FOUND_ERROR_CODE,
//					"Participant '" + participantPrivateId + "' not found in session '" + sessionId + "'");
//		}*/
//    }

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
        Session session = sessions.get(sessionId);
        if (session == null) {
            throw new OpenViduException(Code.ROOM_NOT_FOUND_ERROR_CODE, "Session '" + sessionId + "' not found");
        }
        return session.getParticipants().stream().filter(p -> p.getParticipantPrivateId().equals(participantPrivateId)).findAny().orElseGet(null);
    }

    public Optional<Participant> getParticipantByUUID(String sessionId, String uuid) throws OpenViduException {
        Session session = sessions.get(sessionId);
        if (session == null) {
            return Optional.empty();
        }
        return session.getParticipantByUUID(uuid);
    }

    /**
     * Returns a participant
     *
     * @param participantPrivateId private identifier of the participant
     * @param streamType           type of stream
     * @return {@link Participant}
     * @throws OpenViduException in case the participant doesn't exist
     */
    // delete 2.0
//    public Participant getParticipant(String participantPrivateId, StreamType streamType) throws OpenViduException {
//        for (Session session : sessions.values()) {
//            if (!session.isClosed()) {
//                Participant participant = session.getParticipantByPrivateId(participantPrivateId, streamType);
//                if (!Objects.isNull(participant)) {
//                    return participant;
//                }
////				if (Objects.isNull(participant))
////					throw new OpenViduException(Code.USER_NOT_FOUND_ERROR_CODE,
////							"No participant with private id '" + participantPrivateId + "' was found");
////				return participant;
//            }
//        }
////		throw new OpenViduException(Code.USER_NOT_FOUND_ERROR_CODE,
////				"No participant with private id '" + participantPrivateId + "' was found");
//        log.warn("No participant with private id:{} was found.", participantPrivateId);
//        return null;
//    }

    /**
     * Returns a participant
     *
     * @param participantPrivateId private identifier of the participant
     * @return {@link Participant}
     * @throws OpenViduException in case the participant doesn't exist
     */
    public Participant getParticipant(String participantPrivateId) throws OpenViduException {
        for (Session session : sessions.values()) {
            if (!session.isClosed()) {
                Participant participant = session.getParticipantByPrivateId(participantPrivateId);
                if (!Objects.isNull(participant)) {
                    return participant;
                }
            }
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

    // delete 2.0
//    public Map<String, FinalUser> getFinalUsers(String sessionId) {
//        return this.sessionidFinalUsers.get(sessionId);
//    }

    // delete 2.0
//    public Map<String, FinalUser> removeFinalUsers(String sessionId) {
//        return this.sessionidFinalUsers.remove(sessionId);
//    }

    // delete 2.0
//    public Collection<CDREventRecording> getAccumulatedRecordings(String sessionId) {
//        return this.sessionidAccumulatedRecordings.get(sessionId);
//    }

    // delete 2.0
//    public Collection<CDREventRecording> removeAccumulatedRecordings(String sessionId) {
//        return this.sessionidAccumulatedRecordings.remove(sessionId);
//    }

    public MediaOptions generateMediaOptions(Request<JsonObject> request) {
        return null;
    }

//    @Deprecated //delete 2.0 废弃
//    public Session storeSessionNotActive(String sessionId, SessionProperties sessionProperties) {
//        Session sessionNotActive = new Session(sessionId, sessionProperties, openviduConfig, recordingManager, livingManager);
//        dealSessionNotActiveStored(sessionId, sessionNotActive);
//        showTokens();
//        return sessionNotActive;
//    }

    //delete 2.0 废弃
//    public Session storeSessionNotActive(Session sessionNotActive) {
//        final String sessionId = sessionNotActive.getSessionId();
//        dealSessionNotActiveStored(sessionId, sessionNotActive);
//        showTokens();
//        return sessionNotActive;
//    }

//    @Deprecated //delete 2.0 废弃
//    public Session storeSessionNotActiveWhileRoomCreated(String sessionId) {
//        Session sessionNotActive = new Session(sessionId,
//                new SessionProperties.Builder().customSessionId(sessionId).build(), openviduConfig, recordingManager, livingManager);
//        dealSessionNotActiveStored(sessionId, sessionNotActive);
//        return sessionNotActive;
//    }

//    public Session SessionNotActiveWhileRoomCreated(String sessionId, Conference conference) {
//        Session sessionNotActive = new Session(sessionId,
//                new SessionProperties.Builder().customSessionId(sessionId).build(), openviduConfig, recordingManager, livingManager);
//        sessionNotActive.setConference(conference);
//        sessionNotActive.setRuid(conference.getRuid());
//        this.sessionsNotActive.put(sessionId, sessionNotActive);
//        return sessionNotActive;
//    }

    public KurentoSession createSession(String sessionId, Conference conference) throws OpenViduException {
        return null;
    }

//    @Deprecated //delete 2.0 废弃
//    private void dealSessionNotActiveStored(String sessionId, Session sessionNotActive) {
//        this.sessionsNotActive.put(sessionId, sessionNotActive);
//        log.info("sessionidParticipantpublicidParticipant sessionId:{}, value:{}", sessionId, sessionidParticipantpublicidParticipant.get(sessionId));
//        this.sessionidParticipantpublicidParticipant.putIfAbsent(sessionId, new ConcurrentHashMap<>());
//        log.info("sessionidParticipantpublicidParticipant sessionId:{}, value:{}", sessionId, sessionidParticipantpublicidParticipant.get(sessionId));
//        //this.sessionidFinalUsers.putIfAbsent(sessionId, new ConcurrentHashMap<>());
//        if (this.openviduConfig.isRecordingModuleEnabled()) {
//            this.sessionidAccumulatedRecordings.putIfAbsent(sessionId, new ConcurrentLinkedQueue<>());
//        }
//    }

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

    public boolean isPublisherInSession(String sessionId, Participant participant, SessionPresetEnum sessionPresetEnum) {
        if (OpenViduRole.PUBLISHER.equals(participant.getRole()) || OpenViduRole.MODERATOR.equals(participant.getRole())
                || (OpenViduRole.ONLY_SHARE.equals(participant.getRole())
                || participant.getTerminalType() == TerminalTypeEnum.S)
        ) {
            return true;
        }
        return sessionPresetEnum.equals(SessionPresetEnum.on) && OpenViduRole.SUBSCRIBER.equals(participant.getRole());
    }

    public boolean isModeratorInSession(String sessionId, Participant participant) {
        if (participant.getSessionId().equals(sessionId)) {
            return OpenViduRole.MODERATOR_ROLES.contains(participant.getRole());
        }
        return false;
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

    public Participant newParticipant(Long userId, String sessionId, String participantPrivatetId, String clientMetadata, String role,
                                      GeoLocation location, String platform, String deviceModel, String ability, String functionality) {
        Session session = getSession(sessionId);
        if (session != null) {
            String participantPublicId = RandomStringUtils.randomAlphanumeric(16).toLowerCase();
            Participant p = new Participant(userId, participantPrivatetId, participantPublicId, sessionId, OpenViduRole.parseRole(role),
                    clientMetadata, location, platform, deviceModel, null, ability, functionality);
//			while (this.sessionidParticipantpublicidParticipant.get(sessionId).putIfAbsent(participantPublicId,
//					p) != null) {
//				participantPublicId = RandomStringUtils.randomAlphanumeric(16).toLowerCase();
//				p.setParticipantPublicId(participantPublicId);
//			}

            //FinalUser finalUser = this.sessionidFinalUsers.get(sessionId).get(finalUserId);
//			if (finalUser == null) {
//				//First connection for new final user
//				log.info("Participant {} of session {} belongs to a new final user", p.getParticipantPublicId(),
//						sessionId);
//				this.sessionidFinalUsers.get(sessionId).put(finalUserId, new FinalUser(finalUserId, sessionId, p));
//			} else {
//				// New connection for previously existing final user
//				log.info("Participant {} of session {} belongs to a previously existing user",
//						p.getParticipantPublicId(), sessionId);
//				finalUser.addConnection(p);
//			}

            return p;
        } else {
            throw new OpenViduException(Code.ROOM_NOT_FOUND_ERROR_CODE, sessionId);
        }
    }

    //	public Participant newRecorderParticipant(String sessionId, String participantPrivatetId, Token token,
    public Participant newRecorderParticipant(Long userId, String sessionId, String participantPrivatetId, String clientMetadata,
                                              String role, String streamType) {
        Participant p = new Participant(userId, participantPrivatetId, ProtocolElements.RECORDER_PARTICIPANT_PUBLICID,
                sessionId, OpenViduRole.parseRole(role), clientMetadata, null, null, null, null, null, null);
        return p;
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
     * <p>
     * //	 * @see SessionManmager#closeSession(String)
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

    public void closeRoom(Session session) {
        UseTime.point("closeRoom p1");
        String sessionId = session.getSessionId();
        // set session status: closing
        session.setClosing(true);

//		sessionManager.getSession(sessionId).getParticipants().forEach(p -> {
//			if (!Objects.equals(StreamType.MAJOR, p.getStreamType())) return;
//			notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.CLOSE_ROOM_NOTIFY_METHOD, new JsonObject());
//			RpcConnection rpcConnect = notificationService.getRpcConnection(p.getParticipantPrivateId());
//			if (!Objects.isNull(rpcConnect) && !Objects.isNull(rpcConnect.getSerialNumber())) {
//				cacheManage.setDeviceStatus(rpcConnect.getSerialNumber(), DeviceStatus.online.name());
//			}
//		});

        Set<Participant> participants = session.getParticipants();
        notificationService.sendBatchNotificationConcurrent(participants, ProtocolElements.CLOSE_ROOM_NOTIFY_METHOD, new JsonObject());

        new Thread(() -> {
            for (Participant p : participants) {
                RpcConnection rpcConnect = notificationService.getRpcConnection(p.getParticipantPrivateId());
                if (!Objects.isNull(rpcConnect) && !Objects.isNull(rpcConnect.getSerialNumber())) {
                    cacheManage.setDeviceStatus(rpcConnect.getSerialNumber(), DeviceStatus.online.name());
                }
            }
        }).start();

        UseTime.point("closeRoom p2");
        //cancel invite
        inviteCompensationManage.disableAllInviteCompensation(sessionId);
        updateConferenceInfo(sessionId);
        //close room stopPolling
        if (session.getPresetInfo().getPollingStatusInRoom().equals(SessionPresetEnum.on)) {
            SessionPreset sessionPreset = session.getPresetInfo();
            sessionPreset.setPollingStatusInRoom(SessionPresetEnum.off);
            timerManager.stopPollingCompensation(sessionId);
            //send notify
            JsonObject params = new JsonObject();
            params.addProperty("roomId", sessionId);
            notificationService.sendBatchNotificationConcurrent(participants, ProtocolElements.STOP_POLLING_NODIFY_METHOD, params);
        }
        UseTime.Point point = UseTime.getPoint("sessionManager.closeSession.Point");
        this.closeSession(sessionId, EndReason.closeSessionByModerator);
        point.updateTime();
        UseTime.point("closeRoom p5");
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
     * @return set of {@link Participant} POJOS representing the session's
     * participants
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
        // log.info("sessionidParticipantpublicidParticipant sessionId:{}, value:{}", sessionId, sessionidParticipantpublicidParticipant.get(sessionId));
        // sessionidParticipantpublicidParticipant.remove(sessionId);
        // log.info("sessionidParticipantpublicidParticipant sessionId:{}, value:{}", sessionId, sessionidParticipantpublicidParticipant.get(sessionId));
        //sessionidFinalUsers.remove(sessionId);
        // sessionidAccumulatedRecordings.remove(sessionId);
        sessionidTokenTokenobj.remove(sessionId);
    }

    public void cleanCacheCollections(String sessionId) {
        if (sessionidConferenceInfo.containsKey(sessionId)) {
            sessionidConferenceInfo.remove(sessionId);
        }

//		if (sessionidPreset.containsKey(sessionId)) {
//			sessionidPreset.remove(sessionId);
//		}
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

    public SessionPreset getPresetInfo(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            return null;
        }
        return session.getPresetInfo();
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
            notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.CLOSE_ROOM_NOTIFY_METHOD, new JsonObject());
            RpcConnection rpcConnect = notificationService.getRpcConnection(p.getParticipantPrivateId());
            if (!Objects.isNull(rpcConnect) && !Objects.isNull(rpcConnect.getSerialNumber())) {
                cacheManage.setDeviceStatus(rpcConnect.getSerialNumber(), DeviceStatus.online.name());
            }
        });
//		this.unpublishAllStream(sessionId, endReason);
        this.closeSession(sessionId, endReason);
    }

    public ErrorCodeEnum setRollCallInSession(Session conferenceSession, Participant targetPart, Participant connectionPart) {
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
            if (Objects.equals(ParticipantHandStatus.speaker, participant.getHandStatus())) {
                existSpeakerPart = participant;
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
            if (sendChangeRole = (existSpeakerPart.getOrder() > conferenceSession.getPresetInfo().getSfuPublisherThreshold() - 1)) {
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


        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.SET_ROLL_CALL_ROOM_ID_PARAM, conferenceSession.getSessionId());
        params.addProperty(ProtocolElements.SET_ROLL_CALL_SOURCE_ID_PARAM, moderatorPart.getUuid());
        params.addProperty(ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM, targetPart.getUuid());

        boolean sendChangeRole;
        OpenViduRole oldRole = targetPart.getRole();
        if (sendChangeRole = (targetPart.getOrder() > conferenceSession.getPresetInfo().getSfuPublisherThreshold() - 1)) {
            targetPart.changePartRole(OpenViduRole.PUBLISHER);
        }
        JsonArray changeRoleNotifiParam = sendChangeRole ? conferenceSession.getPartRoleChangedNotifyParamArr(targetPart,
                OpenViduRole.SUBSCRIBER, OpenViduRole.PUBLISHER) : null;

        // SetRollCall notify  NEW
        setSpeaker(conferenceSession, targetPart, connectionPart.getUuid(), sendChangeRole, oldRole);
        // broadcast the changes of layout
/*        participants.forEach(participant -> {
            // SetRollCall notify  old
//			this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.SET_ROLL_CALL_METHOD, params);
            if (sendChangeRole) {
                this.notificationService.sendNotification(participant.getParticipantPrivateId(),
                        ProtocolElements.NOTIFY_PART_ROLE_CHANGED_METHOD, changeRoleNotifiParam);
            }
            JsonArray targetIds = new JsonArray();
            targetIds.add(targetPart.getUuid());
            if (ParticipantSpeakerStatus.off.equals(targetPart.getSpeakerStatus())) {
                JsonObject audioSpeakerParams = new JsonObject();
                audioSpeakerParams.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_ID_PARAM, conferenceSession.getSessionId());
                audioSpeakerParams.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_SOURCE_ID_PARAM, moderatorPart.getUuid());
                audioSpeakerParams.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_STATUS_PARAM, "on");
                audioSpeakerParams.add(ProtocolElements.SET_AUDIO_SPEAKER_TARGET_ID_PARAM, targetIds);
                this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.SET_AUDIO_SPEAKER_STATUS_METHOD, audioSpeakerParams);
            }
            if (ParticipantMicStatus.off.equals(targetPart.getMicStatus())) {
                JsonObject audioParams = new JsonObject();
                audioParams.addProperty(ProtocolElements.SET_AUDIO_ROOM_ID_PARAM, conferenceSession.getSessionId());
                audioParams.addProperty(ProtocolElements.SET_AUDIO_SOURCE_PARAM, moderatorPart.getUuid());
                audioParams.addProperty(ProtocolElements.SET_AUDIO_STATUS_PARAM, "on");
                audioParams.add(ProtocolElements.SET_VIDEO_TARGETS_PARAM, targetIds);
                this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.SET_AUDIO_STATUS_METHOD, audioParams);
            }
        });*/
        targetPart.setSpeakerStatus(ParticipantSpeakerStatus.on);
        targetPart.setMicStatus(ParticipantMicStatus.on);
        return errorCode;
    }

    public void setMicStatusAndDealExistsSharing(Participant participant, Participant moderatorPart, String sessionId) {

        Session session = getSession(sessionId);
        JsonObject result = new JsonObject();
        //如果在墙下      通知角色变更 设置麦克风状态  通知开启/关闭共享流
        if (participant.getOrder() > session.getPresetInfo().getSfuPublisherThreshold() - 1 && !OpenViduRole.ONLY_SHARE.equals(participant.getRole())) {
            participant.changePartRole(OpenViduRole.SUBSCRIBER);
            JsonArray changeRoleNotifiParam = session.getPartRoleChangedNotifyParamArr(participant,
                    OpenViduRole.PUBLISHER, OpenViduRole.SUBSCRIBER);
            //  通知角色变更

            result.add("roleChange", changeRoleNotifiParam);
            boolean micStatusFlag;
            JsonObject audioParams = new JsonObject();
            if (micStatusFlag = ParticipantMicStatus.on.equals(participant.getMicStatus())) {
                participant.setMicStatus(ParticipantMicStatus.off);
                audioParams.addProperty(ProtocolElements.SET_AUDIO_ROOM_ID_PARAM, sessionId);
                audioParams.addProperty(ProtocolElements.SET_AUDIO_SOURCE_PARAM, moderatorPart.getUuid());
                audioParams.addProperty(ProtocolElements.SET_AUDIO_STATUS_PARAM, "off");
                audioParams.addProperty(ProtocolElements.SET_VIDEO_TARGET_IDS_PARAM, participant.getUuid());
            }
            //判断是否存在共享
            Participant sharePart = getSession(sessionId).getSharingPart().orElse(null);
            boolean existsSharingFlag;
            JsonObject stopSharingParams = new JsonObject();
            if (existsSharingFlag = Objects.nonNull(sharePart)) {
                leaveRoom(sharePart, null, EndReason.sessionClosedByServer, false);
                stopSharingParams.addProperty(ProtocolElements.SHARING_CONTROL_ROOMID_PARAM, sessionId);
                stopSharingParams.addProperty(ProtocolElements.SHARING_CONTROL_SOURCEID_PARAM, "");
                stopSharingParams.addProperty(ProtocolElements.SHARING_CONTROL_TARGETID_PARAM, sharePart.getUuid());
                stopSharingParams.addProperty(ProtocolElements.SHARING_CONTROL_OPERATION_PARAM, ParticipantShareStatus.off.name());
//                stopSharingParams.addProperty(ProtocolElements.SHARING_CONTROL_MODE_PARAM, 0);
            }

            if (micStatusFlag) {
                result.add("setAudioStatus", audioParams);
            }
            if (existsSharingFlag) {
                result.add("endShareNotify", stopSharingParams);
            }

        }
        session.setSpeakerPart(null);
        result.addProperty("roomId", session.getSessionId());
        result.addProperty("targetId", participant.getUuid());
        result.addProperty("originator", moderatorPart.getUuid());
        notificationService.sendBatchNotificationConcurrent(session.getParticipants(), ProtocolElements.END_ROLL_CALL_NOTIFY_METHOD, result);

        if (session.getConferenceMode() == ConferenceModeEnum.MCU) {
            session.getCompositeService().asyncUpdateComposite();
        }

    }

    private void sendEndRollCallNotify(Set<Participant> participants, JsonObject params, boolean sendChangeRole, JsonArray changeRoleNotifiParam) {
        participants.forEach(participant -> {
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

    public void setSharing(Session session, Participant sharingPart, String originatorUuid) {
        synchronized (session.getSharingOrSpeakerLock()) {
            sharingPart.setShareStatus(ParticipantShareStatus.on);
            session.setSharingPart(sharingPart);
            JsonObject result = new JsonObject();
            result.addProperty("roomId", session.getSessionId());
            result.addProperty("shareId", sharingPart.getUuid());
            result.addProperty("originator", originatorUuid);

            notificationService.sendBatchNotificationConcurrent(session.getParticipants(), ProtocolElements.APPLY_SHARE_NOTIFY_METHOD, result);
            if (session.getConferenceMode() == ConferenceModeEnum.MCU) {
                session.getCompositeService().asyncUpdateComposite();
            }
        }
    }

    public void setSpeaker(Session session, Participant speakPart, String originatorUuid, boolean sendChangeRole, OpenViduRole oldRole) {
        synchronized (session.getSharingOrSpeakerLock()) {
            session.setSpeakerPart(speakPart);
            JsonObject result = new JsonObject();
            JsonArray roleChange = new JsonArray();
            JsonObject setAudioSpeakerStatus = new JsonObject();
            JsonObject setAudioStatus = new JsonObject();
            JsonObject roleChangeObj = new JsonObject();
            result.addProperty("roomId", session.getSessionId());
            result.addProperty("targetId", speakPart.getUuid());
            result.addProperty("originator", originatorUuid);
            //是否发生过角色变更
            if (sendChangeRole) {
                roleChangeObj.addProperty("uuid", speakPart.getUuid());
                roleChangeObj.addProperty("originalRole", oldRole.name());
                roleChangeObj.addProperty("resentRole", OpenViduRole.PUBLISHER.name());
                roleChange.add(roleChangeObj);
            }

            if (ParticipantSpeakerStatus.off.equals(speakPart.getSpeakerStatus())) {
                setAudioSpeakerStatus.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_ID_PARAM, session.getSessionId());
                setAudioSpeakerStatus.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_SOURCE_ID_PARAM, originatorUuid);
                setAudioSpeakerStatus.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_STATUS_PARAM, "on");
                setAudioSpeakerStatus.addProperty(ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM, speakPart.getUuid());
            }
            if (ParticipantMicStatus.off.equals(speakPart.getMicStatus())) {
                setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_ROOM_ID_PARAM, session.getSessionId());
                setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_SOURCE_ID_PARAM, originatorUuid);
                setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_STATUS_PARAM, "on");
                setAudioStatus.addProperty(ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM, speakPart.getUuid());
            }
            if (roleChange.size() != 0) result.add("roleChange", roleChange);
            if (setAudioSpeakerStatus.size() != 0) result.add("setAudioSpeakerStatus", setAudioSpeakerStatus);
            if (setAudioStatus.size() != 0) result.add("setAudioStatus", setAudioStatus);
            notificationService.sendBatchNotificationConcurrent(session.getParticipants(), ProtocolElements.SET_ROLL_CALL_NOTIFY_METHOD, result);
            if (session.getConferenceMode() == ConferenceModeEnum.MCU) {
                session.getCompositeService().asyncUpdateComposite();
            }
        }
    }

    public void endSharing(Session session, Participant sharingPart, String originatorUuid) {
        synchronized (session.getSharingOrSpeakerLock()) {
            sharingPart.setShareStatus(ParticipantShareStatus.off);
            session.setSharingPart(null);

            JsonObject result = new JsonObject();
            result.addProperty("roomId", session.getSessionId());
            result.addProperty("shareId", sharingPart.getUuid());
            result.addProperty("originator", originatorUuid);
            notificationService.sendBatchNotificationConcurrent(session.getParticipants(), ProtocolElements.END_SHARE_NOTIFY_METHOD, result);

            PublisherEndpoint sharingEp;
            if (!Objects.isNull(sharingEp = sharingPart.getPublisher(StreamType.SHARING))) {
                this.unpublishVideo(sharingPart, sharingEp.getStreamId(), null, EndReason.forceUnpublishByUser);
            }

            if (session.getConferenceMode() == ConferenceModeEnum.MCU) {
                session.getCompositeService().asyncUpdateComposite();
            }

            if (session.getIsRecording()) {
                this.updateRecording(session.getSessionId());
            }
        }
    }

    public void endSpeaker(Session session, Participant speaker, String originatorUuid) {
        synchronized (session.getSharingOrSpeakerLock()) {
            session.setSpeakerPart(null);

            JsonObject result = new JsonObject();
            result.addProperty("roomId", session.getSessionId());
            result.addProperty("targetId", speaker.getUuid());
            result.addProperty("originator", originatorUuid);
            notificationService.sendBatchNotificationConcurrent(session.getParticipants(), ProtocolElements.END_ROLL_CALL_NOTIFY_METHOD, result);

            if (session.getConferenceMode() == ConferenceModeEnum.MCU) {
                session.getCompositeService().asyncUpdateComposite();
            }
        }
    }

    public void replaceSpeaker(Session session, Participant endPart, Participant startPart, String originatorUuid) {
        Set<Participant> participants = session.getParticipants();
        JsonObject result = new JsonObject();
        JsonArray roleChangeArr = new JsonArray();
        JsonObject setAudioSpeakerStatus = new JsonObject();
        JsonArray setAudioStatusArr = new JsonArray();
        JsonObject stopSharingParams = new JsonObject();
        session.setSpeakerPart(startPart);
        participants.forEach(participant -> {
            if (endPart.getUuid().equals(participant.getUuid())) {
                participant.changeHandStatus(ParticipantHandStatus.down);
            }
            if (startPart.getUuid().equals(participant.getUuid())) {
                participant.changeHandStatus(ParticipantHandStatus.speaker);
            }
        });

        if (startPart.getOrder() > session.getPresetInfo().getSfuPublisherThreshold() - 1) {
            startPart.setRole(OpenViduRole.PUBLISHER);
            JsonArray startPartRoleChange = session.getPartRoleChangedNotifyParamArr(startPart,
                    OpenViduRole.SUBSCRIBER, OpenViduRole.PUBLISHER);
            roleChangeArr.addAll(startPartRoleChange);
        }
        if (endPart.getOrder() > session.getPresetInfo().getSfuPublisherThreshold() - 1) {
            endPart.setRole(OpenViduRole.SUBSCRIBER);
            JsonArray endPartRoleChange = session.getPartRoleChangedNotifyParamArr(startPart,
                    OpenViduRole.PUBLISHER, OpenViduRole.SUBSCRIBER);
            roleChangeArr.addAll(endPartRoleChange);
        }

        if (ParticipantSpeakerStatus.off.equals(startPart.getSpeakerStatus())) {
            setAudioSpeakerStatus.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_ID_PARAM, session.getSessionId());
            setAudioSpeakerStatus.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_SOURCE_ID_PARAM, originatorUuid);
            setAudioSpeakerStatus.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_STATUS_PARAM, "on");
            setAudioSpeakerStatus.addProperty(ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM, startPart.getUuid());
        }

        if (ParticipantMicStatus.off.equals(startPart.getMicStatus())) {
            JsonObject setAudioStatus = new JsonObject();
            setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_ROOM_ID_PARAM, session.getSessionId());
            setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_SOURCE_ID_PARAM, originatorUuid);
            setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_STATUS_PARAM, "on");
            setAudioStatus.addProperty(ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM, startPart.getUuid());
            setAudioStatusArr.add(setAudioStatus);
        }

        if (ParticipantMicStatus.on.equals(endPart.getMicStatus())) {
            JsonObject setAudioStatus = new JsonObject();
            endPart.setMicStatus(ParticipantMicStatus.off);
            setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_ROOM_ID_PARAM, session.getSessionId());
            setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_SOURCE_PARAM, endPart.getUuid());
            setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_STATUS_PARAM, "off");
            setAudioStatus.addProperty(ProtocolElements.SET_VIDEO_TARGET_IDS_PARAM, endPart.getUuid());
            setAudioStatusArr.add(setAudioStatus);
        }

        //判断是否存在共享
        Participant sharePart = session.getSharingPart().orElse(null);

        if (Objects.nonNull(sharePart)) {
            leaveRoom(sharePart, null, EndReason.sessionClosedByServer, false);
            stopSharingParams.addProperty(ProtocolElements.SHARING_CONTROL_ROOMID_PARAM, session.getSessionId());
            stopSharingParams.addProperty(ProtocolElements.SHARING_CONTROL_SOURCEID_PARAM, "");
            stopSharingParams.addProperty(ProtocolElements.SHARING_CONTROL_TARGETID_PARAM, sharePart.getUuid());
            stopSharingParams.addProperty(ProtocolElements.SHARING_CONTROL_OPERATION_PARAM, ParticipantShareStatus.off.name());
//                stopSharingParams.addProperty(ProtocolElements.SHARING_CONTROL_MODE_PARAM, 0);
        }


        synchronized (session.getSharingOrSpeakerLock()) {
            session.setSpeakerPart(startPart);
            result.addProperty(ProtocolElements.REPLACE_ROLL_CALL_ROOM_ID_PARAM, session.getSessionId());
            result.addProperty(ProtocolElements.REPLACE_ROLL_CALL_END_TARGET_ID_PARAM, endPart.getUuid());
            result.addProperty(ProtocolElements.REPLACE_ROLL_CALL_START_TARGET_ID_PARAM, startPart.getUuid());
            result.addProperty(ProtocolElements.REPLACE_ROLL_CALL_ORIGINATOR_PARAM, originatorUuid);
            //添加角色变更
            if (roleChangeArr.size() != 0) result.add("roleChange", roleChangeArr);
            //添加音频状态
            if (setAudioSpeakerStatus.size() != 0) result.add("setAudioSpeakerStatus", setAudioSpeakerStatus);
            //添加音频状态麦克风
            if (setAudioStatusArr.size() != 0) result.add("setAudioStatus", setAudioStatusArr);
            //关闭共享流
            if (stopSharingParams.size() != 0) result.add("endShareNotify", stopSharingParams);
            notificationService.sendBatchNotificationConcurrent(session.getParticipants(), ProtocolElements.REPLACE_ROLL_CALL_NOTIFY_METHOD, result);
        }
    }


//    @Deprecated //delete 2.0 废弃
//    public Session storeSessionNotActiveWhileAppointCreate(String roomId, Conference conference) {
//        log.info("===>storeSessionNotActiveWhileAppointCreate:{}", roomId);
//        Session session = storeSessionNotActiveWhileRoomCreated(roomId);
//        session.setConference(conference);
//        session.setRuid(conference.getRuid());
//        return session;
//    }

    public abstract void startRecording(String sessionId);

    public abstract void stopRecording(String sessionId);

    public abstract void updateRecording(String sessionId);

    public abstract void handleRecordErrorEvent(Object msg);

    /**
     * 创建分发的隧道
     *
     * @param participant
     */
    public abstract void createDeliverChannel(Participant participant, StreamType streamType);
}
