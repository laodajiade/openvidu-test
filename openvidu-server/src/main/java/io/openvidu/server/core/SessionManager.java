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
import io.openvidu.server.client.RtcUserClient;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.dao.AppointConferenceMapper;
import io.openvidu.server.common.dao.ConferenceMapper;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferenceSearch;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.coturn.CoturnCredentialsService;
import io.openvidu.server.exception.BizException;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.kurento.core.KurentoTokenOptions;
import io.openvidu.server.kurento.endpoint.PublisherEndpoint;
import io.openvidu.server.living.service.LivingManager;
import io.openvidu.server.recording.service.RecordingManager;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcNotificationService;
import io.openvidu.server.service.SessionEventRecord;
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

    @Resource
    protected RtcUserClient rtcUserClient;

    public FormatChecker formatChecker = new FormatChecker();

    protected ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();
    protected ConcurrentMap<String, Session> sessionsNotActive = new ConcurrentHashMap<>();

    protected ConcurrentMap<String, Boolean> insecureUsers = new ConcurrentHashMap<>();
    public ConcurrentMap<String, ConcurrentHashMap<String, Token>> sessionidTokenTokenobj = new ConcurrentHashMap<>();

    protected ConcurrentMap<String, ConcurrentHashMap<String, String>> sessionidConferenceInfo = new ConcurrentHashMap<>();

    public abstract void joinRoom(Participant participant, String sessionId, Conference conference, Integer transactionId);

    public abstract void setMuteAll(String sessionId, String originator, SessionPresetEnum sessionPresetEnum);

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

    public MediaOptions generateMediaOptions(Request<JsonObject> request) {
        return null;
    }

    public KurentoSession createSession(String sessionId, Conference conference, SessionPreset presetInfo) throws OpenViduException {
        return null;
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

            return p;
        } else {
            throw new BizException(ErrorCodeEnum.CONFERENCE_NOT_EXIST);
        }
    }

    public Participant newRecorderParticipant(Long userId, String sessionId, String participantPrivatetId, String clientMetadata,
                                              String role) {
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
        SessionEventRecord.closeRoom(session, reason);
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
    }

    public void cleanCacheCollections(String sessionId) {
        sessionidConferenceInfo.remove(sessionId);
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
            appointConferenceMapper.changeStatusByRuid(ConferenceStatus.FINISHED.getStatus(), conference.getRuid(), null, null);
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
        Participant moderatorPart = conferenceSession.getModeratorPart();
        if (moderatorPart == null) {
            return ErrorCodeEnum.MODERATOR_NOT_FOUND;
        }
        if (conferenceSession.getSpeakerPart().isPresent()) {
            if (conferenceSession.getSpeakerPart().get().getUuid().equals(targetPart.getUuid())) {
                return ErrorCodeEnum.SET_ROLL_CALL_SAME_PART;
            } else {
                return ErrorCodeEnum.SPEAKER_ALREADY_EXIST;
            }
        }

        // SetRollCall notify  NEW
        setSpeaker(conferenceSession, targetPart, connectionPart.getUuid());

        return errorCode;
    }

    public void setMicStatusAndDealExistsSharing(Participant participant, Participant moderatorPart, String sessionId) {


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

    public void setSharing(Session session, Participant sharingPart, String originatorUuid) {
        synchronized (session.getSharingOrSpeakerLock()) {
            sharingPart.setShareStatus(ParticipantShareStatus.on);
            session.setSharingPart(sharingPart);
            JsonObject result = new JsonObject();
            result.addProperty("roomId", session.getSessionId());
            result.addProperty("shareId", sharingPart.getUuid());
            result.addProperty("originator", originatorUuid);

            notificationService.sendBatchNotificationConcurrent(session.getParticipants(), ProtocolElements.APPLY_SHARE_NOTIFY_METHOD, result);


            //changeOrder
            if (!session.isSpeaker(sharingPart.getUuid())) {
                Map<String, Integer> orderMap = new HashMap<>();
                if (session.getModeratorPart() == null) {
                    orderMap.put(sharingPart.getUuid(), 0);
                } else {
                    orderMap.put(sharingPart.getUuid(), 1);
                }
                session.dealPartOrderAfterRoleChanged(orderMap, this, session.getParticipantByUUID(originatorUuid).get());
            }

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

    public void setSpeaker(Session session, Participant speaker, String originatorUuid) {
        synchronized (session.getSharingOrSpeakerLock()) {

            speaker.changeHandStatus(ParticipantHandStatus.speaker);
            boolean sendChangeRole;
            OpenViduRole oldRole = speaker.getRole();
            if (sendChangeRole = (speaker.getOrder() > session.getPresetInfo().getSfuPublisherThreshold() - 1)) {
                speaker.changePartRole(OpenViduRole.PUBLISHER);
            }

            session.setSpeakerPart(speaker);
            JsonObject result = new JsonObject();
            JsonArray roleChange = new JsonArray();
            JsonArray setAudioSpeakerStatusArr = new JsonArray();
            JsonArray setAudioStatusArr = new JsonArray();

            result.addProperty("roomId", session.getSessionId());
            result.addProperty("targetId", speaker.getUuid());
            result.addProperty("originator", originatorUuid);
            //是否发生过角色变更
            if (sendChangeRole) {
                JsonObject roleChangeObj = new JsonObject();
                roleChangeObj.addProperty("uuid", speaker.getUuid());
                roleChangeObj.addProperty("originalRole", oldRole.name());
                roleChangeObj.addProperty("presentRole", OpenViduRole.PUBLISHER.name());
                roleChange.add(roleChangeObj);
            }

            if (ParticipantSpeakerStatus.off.equals(speaker.getSpeakerStatus())) {
                speaker.changeSpeakerStatus(ParticipantSpeakerStatus.on);

                JsonObject setAudioSpeakerStatus = new JsonObject();
                setAudioSpeakerStatus.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_ID_PARAM, session.getSessionId());
                setAudioSpeakerStatus.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_SOURCE_ID_PARAM, originatorUuid);
                setAudioSpeakerStatus.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_STATUS_PARAM, ParticipantSpeakerStatus.on.name());
                setAudioSpeakerStatus.addProperty(ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM, speaker.getUuid());
                setAudioSpeakerStatusArr.add(setAudioSpeakerStatus);
            }
            if (ParticipantMicStatus.off.equals(speaker.getMicStatus())) {
                speaker.changeMicStatus(ParticipantMicStatus.on);

                JsonObject setAudioStatus = new JsonObject();
                setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_ROOM_ID_PARAM, session.getSessionId());
                setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_SOURCE_ID_PARAM, originatorUuid);
                setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_STATUS_PARAM, ParticipantMicStatus.on.name());
                setAudioStatus.addProperty(ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM, speaker.getUuid());
                setAudioStatusArr.add(setAudioStatus);
            }
            if (roleChange.size() != 0) result.add("roleChange", roleChange);
            if (setAudioSpeakerStatusArr.size() != 0) result.add("setAudioSpeakerStatus", setAudioSpeakerStatusArr);
            if (setAudioStatusArr.size() != 0) result.add("setAudioStatus", setAudioStatusArr);
            notificationService.sendBatchNotificationConcurrent(session.getParticipants(), ProtocolElements.SET_ROLL_CALL_NOTIFY_METHOD, result);

            if (session.getConferenceMode() == ConferenceModeEnum.MCU) {
                session.getCompositeService().asyncUpdateComposite();
            }

            // update recording
            if (session.ableToUpdateRecord()) {
                updateRecording(session.getSessionId());

                session.getRecorderService().updateParticipantStatus(speaker.getUuid(), "isSpeaker",
                        session.isSpeaker(speaker.getUuid()));
            }
        }
    }

    public void endSpeaker(Session session, Participant speaker, String originatorUuid) {
        synchronized (session.getSharingOrSpeakerLock()) {
            if (!session.getSpeakerPart().isPresent() || !session.getSpeakerPart().get().getUuid().equals(speaker.getUuid())) {
                return;
            }
            session.setSpeakerPart(null);

            JsonObject result = new JsonObject();
            //如果在墙下 通知角色变更 设置麦克风状态  通知开启/关闭共享流
            if (speaker.getOrder() > session.getPresetInfo().getSfuPublisherThreshold() - 1) {
                speaker.changePartRole(OpenViduRole.SUBSCRIBER);
                JsonArray changeRoleNotifiedParam = session.getPartRoleChangedNotifyParamArr(speaker,
                        OpenViduRole.PUBLISHER, OpenViduRole.SUBSCRIBER);
                //  通知角色变更

                result.add("roleChange", changeRoleNotifiedParam);
                boolean micStatusChange;

                JsonArray audioParamsArr = new JsonArray();
                if (micStatusChange = ParticipantMicStatus.on.equals(speaker.getMicStatus())) {
                    speaker.changeMicStatus(ParticipantMicStatus.off);

                    JsonObject audioParams = new JsonObject();
                    audioParams.addProperty(ProtocolElements.SET_AUDIO_ROOM_ID_PARAM, session.getSessionId());
                    audioParams.addProperty(ProtocolElements.SET_AUDIO_SOURCE_PARAM, originatorUuid);
                    audioParams.addProperty(ProtocolElements.SET_AUDIO_STATUS_PARAM, ParticipantMicStatus.off.name());
                    audioParams.addProperty(ProtocolElements.SET_AUDIO_TARGET_IDS_PARAM, speaker.getUuid());
                    audioParamsArr.add(audioParams);
                }
                // 如果分享的也是发言者，需要结束分享
                if (session.isShare(speaker.getUuid())) {
                    endSharing(session, speaker, originatorUuid);
                }
                KurentoParticipant kPart = (KurentoParticipant) speaker;
                for (PublisherEndpoint endpoint : kPart.getPublishers().values()) {
                    this.unpublishVideo(kPart, endpoint.getStreamId(), null, EndReason.endRollCall);
                }

                if (micStatusChange) {
                    result.add("setAudioStatus", audioParamsArr);
                }
            }
            session.setSpeakerPart(null);
            speaker.changeHandStatus(ParticipantHandStatus.endSpeaker);

            result.addProperty("roomId", session.getSessionId());
            result.addProperty("targetId", speaker.getUuid());
            result.addProperty("originator", originatorUuid);
            notificationService.sendBatchNotificationConcurrent(session.getParticipants(), ProtocolElements.END_ROLL_CALL_NOTIFY_METHOD, result);

            if (session.ableToUpdateRecord()) {
                session.getRecorderService().updateParticipantStatus(speaker.getUuid(), "isSpeaker",
                        session.isSpeaker(speaker.getUuid()));
                updateRecording(session.getSessionId());
            }
            if (session.getConferenceMode() == ConferenceModeEnum.MCU) {
                session.getCompositeService().asyncUpdateComposite();
            }
        }
    }

    public void replaceSpeaker(Session session, Participant endPart, Participant startPart, String originatorUuid) {
        synchronized (session.getSharingOrSpeakerLock()) {
            JsonObject result = new JsonObject();
            JsonArray roleChangeArr = new JsonArray();
            JsonObject setAudioSpeakerStatus = new JsonObject();
            JsonArray setAudioStatusArr = new JsonArray();
            JsonObject stopSharingParams = new JsonObject();
            session.setSpeakerPart(startPart);


            if (startPart.getOrder() > session.getPresetInfo().getSfuPublisherThreshold() - 1) {
                startPart.setRole(OpenViduRole.PUBLISHER);
                JsonArray startPartRoleChange = session.getPartRoleChangedNotifyParamArr(startPart,
                        OpenViduRole.SUBSCRIBER, OpenViduRole.PUBLISHER);
                roleChangeArr.addAll(startPartRoleChange);
            }
            if (endPart.getOrder() > session.getPresetInfo().getSfuPublisherThreshold() - 1) {
                endPart.setRole(OpenViduRole.SUBSCRIBER);
                JsonArray endPartRoleChange = session.getPartRoleChangedNotifyParamArr(endPart,
                        OpenViduRole.PUBLISHER, OpenViduRole.SUBSCRIBER);
                roleChangeArr.addAll(endPartRoleChange);
            }


            if (ParticipantSpeakerStatus.off.equals(startPart.getSpeakerStatus())) {
                startPart.changeSpeakerStatus(ParticipantSpeakerStatus.on);

                setAudioSpeakerStatus.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_ID_PARAM, session.getSessionId());
                setAudioSpeakerStatus.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_SOURCE_ID_PARAM, originatorUuid);
                setAudioSpeakerStatus.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_STATUS_PARAM, ParticipantSpeakerStatus.on.name());
                setAudioSpeakerStatus.addProperty(ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM, startPart.getUuid());
            }

            if (ParticipantMicStatus.off.equals(startPart.getMicStatus())) {
                startPart.changeMicStatus(ParticipantMicStatus.on);

                JsonObject setAudioStatus = new JsonObject();
                setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_ROOM_ID_PARAM, session.getSessionId());
                setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_SOURCE_ID_PARAM, originatorUuid);
                setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_STATUS_PARAM, ParticipantMicStatus.on.name());
                setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_TARGET_IDS_PARAM, startPart.getUuid());
                setAudioStatusArr.add(setAudioStatus);
            }

            if (ParticipantMicStatus.on.equals(endPart.getMicStatus())) {
                endPart.changeMicStatus(ParticipantMicStatus.off);

                JsonObject setAudioStatus = new JsonObject();
                setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_ROOM_ID_PARAM, session.getSessionId());
                setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_SOURCE_PARAM, endPart.getUuid());
                setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_STATUS_PARAM, ParticipantMicStatus.off.name());
                setAudioStatus.addProperty(ProtocolElements.SET_AUDIO_TARGET_IDS_PARAM, endPart.getUuid());
                setAudioStatusArr.add(setAudioStatus);
            }


            session.setSpeakerPart(startPart);
            endPart.changeHandStatus(ParticipantHandStatus.down);
            startPart.changeHandStatus(ParticipantHandStatus.speaker);

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

        if (!endPart.getRole().needToPublish()) {
            // 如果分享的也是发言者，需要结束分享
            if (session.isShare(endPart.getUuid())) {
                endSharing(session, endPart, originatorUuid);
            }
            KurentoParticipant kPart = (KurentoParticipant) endPart;
            for (PublisherEndpoint endpoint : kPart.getPublishers().values()) {
                this.unpublishVideo(kPart, endpoint.getStreamId(), null, EndReason.endRollCall);
            }
        }


        if (session.ableToUpdateRecord()) {
            session.getRecorderService().updateParticipantStatus(endPart.getUuid(), "isSpeaker",
                    session.isSpeaker(endPart.getUuid()));

            updateRecording(session.getSessionId());

            session.getRecorderService().updateParticipantStatus(startPart.getUuid(), "isSpeaker",
                    session.isSpeaker(startPart.getUuid()));
        }
        if (session.getConferenceMode() == ConferenceModeEnum.MCU) {
            session.getCompositeService().asyncUpdateComposite();
        }
    }

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
