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

import com.google.gson.*;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.java.client.SessionProperties;
import io.openvidu.server.client.RtcRoomClient;
import io.openvidu.server.common.broker.RedisPublisher;
import io.openvidu.server.common.broker.ToOpenviduElement;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.constants.BrokerChannelConstans;
import io.openvidu.server.common.constants.CommonConstants;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.manage.RoomManage;
import io.openvidu.server.common.manage.UserManage;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.common.redis.RecordingRedisPublisher;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.*;
import io.openvidu.server.exception.BizException;
import io.openvidu.server.kurento.endpoint.KurentoFilter;
import io.openvidu.server.kurento.endpoint.PublisherEndpoint;
import io.openvidu.server.kurento.endpoint.SdpType;
import io.openvidu.server.kurento.kms.Kms;
import io.openvidu.server.kurento.kms.KmsManager;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcNotificationService;
import io.openvidu.server.service.SessionEventRecord;
import io.openvidu.server.utils.JsonUtils;
import org.kurento.client.GenericMediaElement;
import org.kurento.client.IceCandidate;
import org.kurento.jsonrpc.Props;
import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private UserManage userManage;

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private CacheManage cacheManage;

    @Resource
    private RecordingRedisPublisher recordingRedisPublisher;

    @Resource
    private RedisPublisher redisPublisher;

    @Resource
    protected TimerManager timerManager;

    @Autowired
    private RtcRoomClient rtcRoomClient;

    @Override
    public void joinRoom(Participant participant, String sessionId, Conference conference, Integer transactionId) {
        UseTime.point("join room synchronized joinRoom");
        Set<Participant> existingParticipants = null;
        try {

            KurentoSession kSession = (KurentoSession) sessions.get(sessionId);
            if (kSession.isClosed()) {
                log.warn("'{}' is trying to join session '{}' but it is closing", participant.getParticipantPublicId(),
                        sessionId);
                throw new OpenViduException(Code.ROOM_CLOSED_ERROR_CODE, "'" + participant.getParticipantPublicId()
                        + "' is trying to join session '" + sessionId + "' but it is closing");
            }
            kSession.getParticipantByUUID(participant.getUuid())
                    .ifPresent(existPart -> evictParticipant(existPart, Collections.emptyList(), EndReason.reconnect));

            existingParticipants = getParticipants(sessionId);
            participant.setApplicationContext(applicationContext);
            //set the part order
            UseTime.point("setMajorPartsOrder start");
            kSession.setMajorPartsOrder(participant, rpcNotificationService);
            UseTime.point("setMajorPartsOrder end");
            // ??????????????????????????????????????????
            SessionPreset preset = getPresetInfo(sessionId);
            if (!OpenViduRole.MODERATOR.equals(participant.getRole())) {
                if (preset.getQuietStatusInRoom().equals(SessionPresetEnum.off)) {
                    participant.setMicStatus(ParticipantMicStatus.off);
                } else if (participant.getOrder() >= openviduConfig.getSmartMicOnThreshold() || participant.getOrder() >= kSession.getPresetInfo().getSfuPublisherThreshold()) {
                    participant.setMicStatus(ParticipantMicStatus.off);
                }
            }
            participant.setRoomSubject(preset.getRoomSubject());

            // change the part role according to the sfu limit
            if (participant.getOrder() > kSession.getPresetInfo().getSfuPublisherThreshold() - 1
                    && !participant.getRole().equals(OpenViduRole.MODERATOR)) {
                participant.changePartRole(OpenViduRole.SUBSCRIBER);
            }

            // deal the default subtitle config
            participant.setSubtitleConfig(kSession.getSubtitleConfig());
            UseTime.point("join 1");
            kSession.join(participant);
            UseTime.point("join 2");
            // save part info
            roomManage.storePartHistory(participant, conference);
            // save max concurrent statistics
            cacheManage.updateMaxConcurrentOfDay(kSession.getParticipants().size(), conference.getProject());
            //save max concurrent in conference
            Conference concurrentCon = new Conference();
            concurrentCon.setConcurrentNumber(kSession.getParticipants().size());
            concurrentCon.setId(conference.getId());
            roomManage.storeConcurrentNumber(concurrentCon);
        } catch (OpenViduException e) {
            log.warn("PARTICIPANT {}: Error joining/creating session {}", participant.getParticipantPublicId(),
                    sessionId, e);
            sessionEventsHandler.onParticipantJoined(conference, participant, sessionId, null,
                    transactionId, e);
        }
        UseTime.point("join room p3");
        if (existingParticipants != null) {
            sessionEventsHandler.onParticipantJoined(conference, participant, sessionId, existingParticipants,
                    transactionId, null);
        }
        UseTime.point("join room p4");
    }

    @Override
    public void setMuteAll(String sessionId, String originator, SessionPresetEnum sessionPresetEnum) {
        SessionPreset preset = getPresetInfo(sessionId);
        Session session = getSession(sessionId);
        preset.setQuietStatusInRoom(sessionPresetEnum);

        if (SessionPresetEnum.off.equals(sessionPresetEnum)) {
            session.getMajorPartExcludeModeratorConnect().forEach(participant -> {
                //?????????????????? ?????????
                if (participant.getShareStatus().equals(ParticipantShareStatus.off)) {
                    participant.changeMicStatus(ParticipantMicStatus.off);
                }
            });
        }

        JsonObject result = new JsonObject();
        result.addProperty("roomId", sessionId);
        result.addProperty("originator", originator);
        result.addProperty("quietStatusInRoom", sessionPresetEnum.name());
        rpcNotificationService.sendBatchNotificationConcurrent(session.getParticipants(), ProtocolElements.SET_MUTE_ALL_NOTIFY, result);
    }

    private static final Object leaveRoomLock = new Object();

    @Override
    public boolean leaveRoom(Participant participant, Integer transactionId, EndReason reason,
                             boolean closeWebSocket) {
        synchronized (leaveRoomLock) {
            UseTime.point("ip1");
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
            SessionEventRecord.leaveRoom(session, participant, reason);
            UseTime.point("ip2");
            session.leaveRoom(participant, reason);
            UseTime.point("ip3");
            //update partInfo
            roomManage.updatePartHistory(session.getRuid(), participant.getUuid(), participant.getCreatedAt());

            if (session.isShare(participant.getUuid())) {
                endSharing(session, participant, participant.getUuid());
            }
            if (session.isSpeaker(participant.getUuid())) {
                endSpeaker(session, participant, participant.getUuid());
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

            // ???????????????????????????????????????????????????
            if (participant.getRole() == OpenViduRole.MODERATOR && reason != EndReason.reconnect && session.getConferenceMode() == ConferenceModeEnum.MCU) {
                session.getCompositeService().switchAutoMode(true);
            }
            if (participant.getRole() == OpenViduRole.MODERATOR && session.isRecording.get()) {
                session.getRecorderService().switchAutoMode(true);
            }

            UseTime.point("ip4");
            // adjust order notify after onLeft
            session.dealPartOrderInSessionAfterLeaving(participant, rpcNotificationService);
            UseTime.point("ip5");
            if (!EndReason.sessionClosedByServer.equals(reason) && !EndReason.reconnect.equals(reason)) {
                // If session is closed by a call to "DELETE /api/sessions" do NOT stop the
                // recording. Will be stopped after in method
                // "SessionManager.closeSessionAndEmptyCollections"
                if (remainingParticipants.isEmpty() && (!session.getRuid().startsWith("appt-") || session.getEndTime() < System.currentTimeMillis())) {
                    log.info("last part left closing session,remainingParticipants.size = {}", remainingParticipants.size());
                    session.setClosing(true);
                }
            }

            // Finally close websocket session if required
            if (closeWebSocket) {
                sessionEventsHandler.closeRpcSession(participant.getParticipantPrivateId());
            }

            // update recording
            if (session.ableToUpdateRecord() && participant.ableToUpdateRecord()) {
                updateRecording(session.getSessionId());
            }

            if (session.getConferenceMode() == ConferenceModeEnum.MCU && participant.getRole().needToPublish()) {
                session.getCompositeService().asyncUpdateComposite();
            }

            if (session.isClosing()) {
                getMajorParticipants(sessionId).forEach(p -> rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
                        ProtocolElements.CLOSE_ROOM_NOTIFY_METHOD, new JsonObject()));
                closeSession(sessionId, EndReason.lastParticipantLeft);
            }
            UseTime.point("ip6");
            return sessionClosedByLastParticipant;
        }
    }

    /**
     * leaveRoom ???????????????????????????closeRoom???????????????
     * ???????????? ???????????????????????????????????????????????????????????????
     */
    @Override
    public boolean leaveRoomSimple(Participant participant, Integer transactionId, EndReason reason,
                                   boolean closeWebSocket) {
        //synchronized (leaveRoomLock) {
        //UseTime.point("ip1");
        //log.debug("Request [leaveRoomSimple] ({})", participant.getParticipantPublicId());

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
        UseTime.point("ip2");
        session.leaveRoom(participant, reason);
        UseTime.point("ip3");
        //update partInfo
        if (!OpenViduRole.THOR.equals(participant.getRole())) {
            roomManage.updatePartHistory(session.getRuid(), participant.getUuid(), participant.getCreatedAt());
        }
        UseTime.point("ip4");

        // Update control data structures
//        if (sessionidParticipantpublicidParticipant.get(sessionId) != null) {
//            Participant p = sessionidParticipantpublicidParticipant.get(sessionId)
//                    .remove(participant.getParticipantPublicId());
//            boolean stillParticipant = false;
//            if (Objects.nonNull(p)) {
//                for (Session s : sessions.values()) {
//                    if (s.getParticipantByPrivateId(p.getParticipantPrivateId()) != null) {
//                        stillParticipant = true;
//                        break;
//                    }
//                }
//                if (!stillParticipant) {
//                    insecureUsers.remove(p.getParticipantPrivateId());
//                }
//            }
//        }

//        if (session.isShare(participant.getUuid())) {
//            changeSharingStatusInConference(session, participant);
//        }

        // Close Session if no more participants
        Set<Participant> remainingParticipants = null;
        try {
            remainingParticipants = getParticipants(sessionId);
        } catch (OpenViduException e) {
            log.info("Possible collision when closing the session '{}' (not found)", sessionId);
            remainingParticipants = Collections.emptySet();
        }

//			if (!EndReason.forceDisconnectByUser.equals(reason) &&
//					!EndReason.forceCloseSessionByUser.equals(reason) && !EndReason.closeSessionByModerator.equals(reason)) {
//				sessionEventsHandler.onParticipantLeft(participant, sessionId, remainingParticipants, transactionId, null,
//						reason);
//			}
        // UseTime.point("ip4");
        // adjust order notify after onLeft
        //session.dealParticipantOrder(participant, rpcNotificationService);
        // UseTime.point("ip5");
        if (!EndReason.sessionClosedByServer.equals(reason)) {
            // If session is closed by a call to "DELETE /api/sessions" do NOT stop the
            // recording. Will be stopped after in method
            // "SessionManager.closeSessionAndEmptyCollections"
            if (remainingParticipants.isEmpty() && (!session.getRuid().startsWith("appt-") || session.getEndTime() < System.currentTimeMillis())) {
                session.setClosing(true);
            }
        }

        // Finally close websocket session if required
        if (closeWebSocket) {
            sessionEventsHandler.closeRpcSession(participant.getParticipantPrivateId());
        }

        // update recording
        if (session.ableToUpdateRecord() && participant.ableToUpdateRecord()) {
            updateRecording(session.getSessionId());
        }

//			if (session.isClosing()) {
//				closeSession(sessionId, EndReason.lastParticipantLeft);
//			}
        return sessionClosedByLastParticipant;
        //}

    }

//    @Override
//    public void changeSharingStatusInConference(KurentoSession session, Participant participant) {
//        // change composite and sharing publisher share status
//        if (Objects.equals(session.getConferenceMode(), ConferenceModeEnum.MCU)) {
//            session.getCompositeService().setExistSharing(false);
//        }
//    }

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
     *                      //	 * @param MediaOptions  configuration of the stream to publish
     * @param transactionId identifier of the Transaction
     * @throws OpenViduException on error
     */
    @Override
    public void publishVideo(Participant participant, MediaOptions mediaOptions, Integer transactionId, StreamType streamType)
            throws OpenViduException {

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

        PublisherEndpoint publishingEndpoint = kParticipant.createPublishingEndpoint(mediaOptions, streamType);

        String sdpAnswer = kParticipant.publishToRoom(sdpType, kurentoOptions.sdpOffer, kurentoOptions.doLoopback,
                kurentoOptions.loopbackAlternativeSrc, kurentoOptions.loopbackConnectionType, streamType);
        // http://task.sudi.best/browse/BASE121-2455 sdk???????????????????????????????????????
        publishingEndpoint.internalAddIceCandidateCache();
        if (sdpAnswer == null) {
            OpenViduException e = new OpenViduException(Code.MEDIA_SDP_ERROR_CODE,
                    "Error generating SDP response for publishing user " + participant.getParticipantPublicId());
            log.error("PARTICIPANT {}: Error publishing media", participant.getParticipantPublicId(), e);
            sessionEventsHandler.onPublishMedia(participant, publishingEndpoint.getStreamId(),
                    kParticipant.getPublisher(streamType).createdAt(), sdpAnswer,
                    transactionId, null, kParticipant.getPublisher(streamType), streamType);
        }

        kSession.registerPublisher();

        if (sdpAnswer != null) {
            sessionEventsHandler.onPublishMedia(participant, publishingEndpoint.getEndpointName(),
                    kParticipant.getPublisher(streamType).createdAt(), sdpAnswer,
                    transactionId, null, kParticipant.getPublisher(streamType), streamType);
        }

        kParticipant.getPublisher(streamType).gatherCandidates();
    }

    @Override
    public void createDeliverChannel(Participant participant, StreamType streamType) {
        KurentoParticipant kParticipant = (KurentoParticipant) participant;
        KurentoSession session = kParticipant.getSession();

        for (DeliveryKmsManager deliveryKmsManager : session.getDeliveryKmsManagers()) {
            log.debug("dispatcher uuid {}, publisherId = {}", kParticipant.getUuid(), kParticipant.getPublisher(StreamType.MAJOR).getStreamId());
            deliveryKmsManager.dispatcher(kParticipant, kParticipant.getPublisher(streamType));
        }
    }

    @Override
    public void unpublishVideo(Participant participant, String publishId, Integer transactionId,
                               EndReason reason) {
        try {
            KurentoParticipant kParticipant = (KurentoParticipant) participant;
            KurentoSession session = kParticipant.getSession();

            PublisherEndpoint publisherEndpoint = kParticipant.getPublisher(publishId);


            log.debug("Request [UNPUBLISH_MEDIA] ({})", participant.getParticipantPublicId());
            if (publisherEndpoint == null || publisherEndpoint.getEndpoint() == null) {
                log.warn("PARTICIPANT {}: Requesting to unpublish video of {} in session {} but user is not streaming media",
                        participant.getUuid(), publishId, session.getSessionId());
                return;
            }

            kParticipant.unpublishMedia(publisherEndpoint, reason, 0);
            session.cancelPublisher(publisherEndpoint, reason);

            Set<Participant> participants = session.getParticipants();
            sessionEventsHandler.onUnpublishMedia(participant, participants, publisherEndpoint, transactionId, null, reason);
        } catch (OpenViduException e) {
            log.warn("PARTICIPANT {}: Error unpublishing media", participant.getParticipantPublicId(), e);
            sessionEventsHandler.onUnpublishMedia(participant, new HashSet<>(Arrays.asList(participant)), null,
                    transactionId, e, null);
        }
    }

//    @Override
//    public void subscribe(Participant participant, String senderName, StreamModeEnum streamMode, String sdpOffer, Integer transactionId) {
//        String sdpAnswer = null;
//        KurentoSession session = null;
//        try {
//            log.debug("Request [SUBSCRIBE] remoteParticipant={} sdpOffer={} ({})", senderName, sdpOffer,
//                    participant.getParticipantPublicId());
//
//            KurentoParticipant kParticipant = (KurentoParticipant) participant;
//            session = ((KurentoParticipant) participant).getSession();
//
//            Participant senderParticipant;
//            if (!StreamModeEnum.MIX_MAJOR_AND_SHARING.equals(streamMode)) {
//                senderParticipant = session.getParticipantByPublicId(senderName);
//            } else {
//                if (!Objects.equals(OpenViduRole.THOR, participant.getRole())) {
//                    senderParticipant = participant;
//                } else {
//                    senderParticipant = getInviteDelayPart(participant.getSessionId(), participant.getUserId());
//                }
//            }
//
//            if (senderParticipant == null) {
//                log.warn(
//                        "PARTICIPANT {}: Requesting to recv media from user {} "
//                                + "in session {} but user could not be found",
//                        participant.getParticipantPublicId(), senderName, session.getSessionId());
//                sessionEventsHandler.sendSuccessResp(participant.getParticipantPrivateId(), transactionId);
//                return;
//            }
//            if (!Objects.equals(StreamModeEnum.MIX_MAJOR_AND_SHARING, streamMode) && !senderParticipant.isStreaming()) {
//                log.warn(
//                        "PARTICIPANT {}: Requesting to recv media from user {} "
//                                + "in session {} but user is not streaming media",
//                        participant.getParticipantPublicId(), senderName, session.getSessionId());
//                throw new OpenViduException(Code.USER_NOT_STREAMING_ERROR_CODE,
//                        "User '" + senderName + " not streaming media in session '" + session.getSessionId() + "'");
//            }
//
//            sdpAnswer = kParticipant.receiveMediaFrom(senderParticipant, streamMode, sdpOffer, senderName);
//            if (sdpAnswer == null) {
//                throw new OpenViduException(Code.MEDIA_SDP_ERROR_CODE,
//                        "Unable to generate SDP answer when subscribing '" + participant.getParticipantPublicId()
//                                + "' to '" + senderName + "'");
//            }
//        } catch (OpenViduException e) {
//            log.error("PARTICIPANT {}: Error subscribing to {}", participant.getParticipantPublicId(), senderName, e);
//            sessionEventsHandler.onSubscribe(participant, session, null, transactionId, e);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//            log.error("Exception:", e);
//        }
//
//        if (Objects.equals(participant.getVoiceMode(), VoiceMode.on)) {
//            switchVoiceModeWithPublicId(participant, participant.getVoiceMode(), senderName);
//        }
//
//        if (sdpAnswer != null) {
//            sessionEventsHandler.onSubscribe(participant, session, sdpAnswer, transactionId, null);
//        }
//    }
//

    @Override
    public void subscribe(Participant participant, Participant senderParticipant, StreamType streamType, StreamModeEnum streamMode,
                          String sdpOffer, String publishStreamId, Integer transactionId) {
        String sdpAnswer = null;
        KurentoSession session = null;
        Map<String, Object> resultObj = new HashMap<>();
        try {
            KurentoParticipant kParticipant = (KurentoParticipant) participant;
            session = ((KurentoParticipant) participant).getSession();

            if (StreamModeEnum.MIX_MAJOR == streamMode) {
                senderParticipant = participant;
            }

            if (!Objects.equals(StreamModeEnum.MIX_MAJOR, streamMode) && !senderParticipant.isStreaming(streamType)) {
                log.warn("PARTICIPANT {}: Requesting to recv media from user {} "
                                + "in session {} but user is not streaming media",
                        participant.getParticipantPublicId(), senderParticipant.getUuid(), session.getSessionId());
                throw new OpenViduException(Code.USER_NOT_STREAMING_ERROR_CODE,
                        "User '" + senderParticipant.getUuid() + " not streaming media in session '" + session.getSessionId() + "'");
            }
            UseTime.point("sdpAnswer before");
            sdpAnswer = kParticipant.receiveMediaFrom((KurentoParticipant) senderParticipant, streamMode, sdpOffer,
                    streamType, publishStreamId, resultObj);
            UseTime.point("sdpAnswer after");
            if (sdpAnswer == null) {
                throw new OpenViduException(Code.MEDIA_SDP_ERROR_CODE,
                        "Unable to generate SDP answer when subscribing '" + participant.getUuid()
                                + "' to '" + publishStreamId + "'");
            }
        } catch (OpenViduException e) {
            log.error("PARTICIPANT {}: Error subscribing to {}", participant.getParticipantPublicId(), publishStreamId, e);
            sessionEventsHandler.onSubscribe(participant, session, null, resultObj, transactionId, e);
            return;
        } catch (Exception e) {
            log.error("Exception:", e);
        }

        if (Objects.equals(participant.getVoiceMode(), VoiceMode.on)) {
            switchVoiceModeWithPublicId(participant, participant.getVoiceMode(), publishStreamId);
        }

        if (sdpAnswer != null) {
            sessionEventsHandler.onSubscribe(participant, session, sdpAnswer, resultObj, transactionId, null);
        }
    }

    @Override
    public void unsubscribe(Participant participant, String subscribeId, Integer transactionId) {
        KurentoParticipant kParticipant = (KurentoParticipant) participant;
        kParticipant.cancelReceivingMedia(subscribeId, EndReason.unsubscribe);

        sessionEventsHandler.onUnsubscribe(participant, transactionId, null);
    }

    @Override
    public void switchVoiceMode(Participant participant, VoiceMode operation) {
        participant.changeVoiceMode(operation);
        KurentoParticipant kParticipant = (KurentoParticipant) participant;
        kParticipant.switchVoiceModeInSession(operation, kParticipant.getSubscribers().keySet());
    }

//    @Override
//    public void pauseAndResumeStream(Participant pausePart, Participant targetPart, OperationMode operation, String mediaType) {
//        KurentoParticipant kParticipant = (KurentoParticipant) pausePart;
//        Set<String> publicIds = kParticipant.getSubscribers().keySet();
//        kParticipant.pauseAndResumeStreamInSession(targetPart, operation, mediaType, publicIds);
//    }

    @Override
    public void pauseAndResumeStream(Participant pausePart, String subscribeId, OperationMode operation, String mediaType) {
        KurentoParticipant kParticipant = (KurentoParticipant) pausePart;
        kParticipant.pauseAndResumeStreamInSession(operation, mediaType, subscribeId);
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

    //delete 2.0
    // @Override
//    public void streamPropertyChanged(Participant participant, Integer transactionId, String streamId, String property,
//                                      JsonElement newValue, String reason) {
//        KurentoParticipant kParticipant = (KurentoParticipant) participant;
//        streamId = kParticipant.getPublisher(StreamType.MAJOR).getStreamId();
//        //MediaOptions streamProperties = kParticipant.getPublisherMediaOptions();
//
////        Boolean hasAudio = streamProperties.hasAudio();
////        Boolean hasVideo = streamProperties.hasVideo();
////        Boolean audioActive = streamProperties.isAudioActive();
////        Boolean videoActive = streamProperties.isVideoActive();
////        String typeOfVideo = streamProperties.getTypeOfVideo();
////        Integer frameRate = streamProperties.getFrameRate();
////        String videoDimensions = streamProperties.getVideoDimensions();
////        KurentoFilter filter = streamProperties.getFilter();
//
//        switch (property) {
//            case "audioActive":
//                audioActive = newValue.getAsBoolean();
//                break;
//            case "videoActive":
//                videoActive = newValue.getAsBoolean();
//                break;
//            case "videoDimensions":
//                videoDimensions = newValue.getAsString();
//                break;
//        }
//
//        kParticipant.setPublisherMediaOptions(new MediaOptions(hasAudio, hasVideo, audioActive, videoActive,
//                typeOfVideo, frameRate, videoDimensions, filter));
//
//        sessionEventsHandler.onStreamPropertyChanged(participant, transactionId,
//                kParticipant.getSession().getParticipants(), streamId, property, newValue, reason);
//    }

    @Override
    public void onIceCandidate(Participant participant, String endpointName, String candidate, int sdpMLineIndex,
                               String sdpMid, Integer transactionId) {
        try {
            KurentoParticipant kParticipant = (KurentoParticipant) participant;
            log.debug("Request [ICE_CANDIDATE] endpoint={} candidate={} " + "sdpMLineIdx={} sdpMid={} ({})",
                    endpointName, candidate, sdpMLineIndex, sdpMid, participant.getParticipantPublicId());
            ErrorCodeEnum errorCodeEnum = kParticipant.addIceCandidate(endpointName, new IceCandidate(candidate, sdpMid, sdpMLineIndex));
            sessionEventsHandler.onRecvIceCandidate(participant, transactionId, errorCodeEnum);
        } catch (OpenViduException e) {
            log.error("PARTICIPANT {}: Error receiving ICE " + "candidate (epName={}, candidate={})",
                    participant.getParticipantPublicId(), endpointName, candidate, e);
            sessionEventsHandler.onRecvIceCandidate(participant, transactionId, ErrorCodeEnum.ENP_POINT_NAME_NOT_EXIST);
        }
    }

    /**
     * Creates a session with the already existing not-active session in the
     * indicated KMS, if it doesn't already exist
     *
     * @throws OpenViduException in case of error while creating the session
     */
    private KurentoSession createSession(String sessionId, Conference conference) throws OpenViduException {
        KurentoSession session = (KurentoSession) sessions.get(sessionId);
        if (session != null) {
            String msg = "Session '" + session.getSessionId() + "' already exists";
            log.warn(msg);
            throw new BizException(ErrorCodeEnum.CONFERENCE_ALREADY_EXIST, msg);
        }

        Session sessionNotActive = new Session(sessionId,
                new SessionProperties.Builder().customSessionId(sessionId).build(), openviduConfig, recordingManager, livingManager);

        Kms lessLoadedKms;
        try {
            lessLoadedKms = this.kmsManager.getLessLoadedKms();
            if (1 == openviduConfig.getKmsLoadLimitSwitch() && Double.compare(lessLoadedKms.getLoad(), Double.parseDouble("0.0")) != 0) {
                throw new NoSuchElementException();
            }
        } catch (NoSuchElementException e) {
            throw new BizException(ErrorCodeEnum.COUNT_OF_CONFERENCE_LIMIT);
        }


        session = new KurentoSession(sessionNotActive, lessLoadedKms, kurentoSessionEventsHandler, kurentoEndpointConfig,
                kmsManager.destroyWhenUnused());

        session.setEndTime(sessionNotActive.getEndTime());
        lessLoadedKms.addKurentoSession(session);

        session.setConference(conference);
        //session.setConferenceMode(conference.getConferenceMode() == 0 ? ConferenceModeEnum.SFU : ConferenceModeEnum.MCU);
        session.setConferenceMode(ConferenceModeEnum.SFU);
        session.setRuid(conference.getRuid());
        sessions.put(session.getSessionId(), session);
        new RoomLeaseThread(session).start();

        if (ConferenceModeEnum.MCU.equals(session.getConferenceMode())) {
            session.setCorpMcuConfig(roomManage.getCorpMcuConfig(conference.getProject()));
        }

        log.warn("No session '{}' exists yet. Created one on KMS '{}'", session.getSessionId(), lessLoadedKms.getUri());

        return session;
    }

    @Override
    public KurentoSession createSession(String sessionId, Conference conference, SessionPreset preset) throws OpenViduException {
        final KurentoSession session = this.createSession(sessionId, conference);
        session.setPresetInfo(preset);
        rtcRoomClient.registerRoom(sessionId, session.getPresetInfo().getInstanceId(), conference.getProject(), preset.getShortId());
        return session;
    }

    @Override
    public boolean evictParticipant(Participant evictedParticipant, Participant moderator, Integer transactionId,
                                    EndReason reason) throws OpenViduException {

        boolean sessionClosedByLastParticipant = false;

        if (evictedParticipant != null && !evictedParticipant.isClosed()) {
            KurentoParticipant kParticipant = (KurentoParticipant) evictedParticipant;
            this.sessionEventsHandler.onForceDisconnect(moderator, evictedParticipant, kParticipant.getSession().getParticipants(), transactionId,
                    null, reason);
            sessionClosedByLastParticipant = this.leaveRoom(kParticipant, null, reason, false);
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
    public boolean evictParticipantByCloseRoom(Participant evictedParticipant, Participant moderator, Integer transactionId,
                                               EndReason reason) throws OpenViduException {

        boolean sessionClosedByLastParticipant = false;

        if (evictedParticipant != null && !evictedParticipant.isClosed()) {
            KurentoParticipant kParticipant = (KurentoParticipant) evictedParticipant;
            Set<Participant> participants = kParticipant.getSession().getParticipants();
            sessionClosedByLastParticipant = this.leaveRoomSimple(kParticipant, null, reason, false);
            this.sessionEventsHandler.onForceDisconnect(moderator, evictedParticipant, participants, transactionId,
                    null, reason);
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
            Participant participant = session.getParticipantByPrivateId(rpcConnection.getParticipantPrivateId());
            if (participant == null) {
                rpcNotificationService.closeRpcSession(rpcConnection.getParticipantPrivateId());
                return;
            }

            JsonObject singleNotifyParam = new JsonObject();
            singleNotifyParam.addProperty(ProtocolElements.USER_BREAK_LINE_CONNECTION_ID_PARAM, participant.getParticipantPublicId());
            singleNotifyParam.addProperty("uuid", participant.getUuid());

            rpcNotificationService.sendBatchNotificationConcurrent(session.getParticipantsExclude(participant),
                    ProtocolElements.USER_BREAK_LINE_METHOD, singleNotifyParam);

            // evict same privateId parts
            evictParticipant(participant, evictStrategies, EndReason.lastParticipantLeft);
        }
    }

    @Override
    public void evictParticipantByPrivateId(String sessionId, String privateId, List<EvictParticipantStrategy> evictStrategies) {
        Session session;
        if (Objects.nonNull(session = getSession(sessionId))) {
            Participant participant = session.getParticipantByPrivateId(privateId);
            if (participant != null) {
                evictParticipant(participant, evictStrategies, EndReason.sessionClosedByServer);
            }
        }
    }

    @Override
    public void evictParticipantByUUID(String sessionId, String uuid, List<EvictParticipantStrategy> evictStrategies, EndReason endReason) {
        Session session;
        if (Objects.nonNull(session = getSession(sessionId))) {
            Optional<Participant> participantOptional = session.getParticipantByUUID(uuid);
            participantOptional.ifPresent(participant -> evictParticipant(participant, evictStrategies, endReason));
        }
    }

    /**
     * ????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param sessionId
     * @param uuid
     * @param evictStrategies
     * @param endReason
     */
    @Override
    public void evictParticipantByUUIDEx(String sessionId, String uuid, List<EvictParticipantStrategy> evictStrategies, EndReason endReason) {
        Session session;
        if (Objects.nonNull(session = getSession(sessionId))) {
            Optional<Participant> participantOptional = session.getParticipantByUUID(uuid);
            participantOptional.ifPresent(participant -> evictParticipant(participant, evictStrategies, endReason));
        } else {
            JsonObject msg = new JsonObject();
            msg.addProperty("method", ToOpenviduElement.EVICT_PARTICIPANT_BY_UUID_METHOD);

            JsonObject params = new JsonObject();
            params.addProperty("roomId", sessionId);
            params.addProperty("uuid", uuid);
            JsonArray strategies = new JsonArray();
            evictStrategies.forEach(s -> strategies.add(s.name()));
            params.add("evictStrategies", strategies);
            params.addProperty("endReason", endReason.name());
            msg.add("params", params);

            cacheManage.publish(BrokerChannelConstans.TO_OPENVIDU_CHANNEL, msg.toString());
        }
    }

    //copy from evictParticipantWithSamePrivateId
    private void evictParticipant(@NotNull Participant evictParticipant, List<EvictParticipantStrategy> evictStrategies, EndReason reason) {
        // check if include moderator
        Session session;
        //Participant majorPart = samePrivateIdParts.get(StreamType.MAJOR.name());
        Set<Participant> participants = (session = getSession(evictParticipant.getSessionId())).getParticipants();
        if (OpenViduRole.MODERATOR.equals(evictParticipant.getRole()) && session.getPresetInfo().getPollingStatusInRoom().equals(SessionPresetEnum.on)) {
            //stop polling
            SessionPreset sessionPreset = session.getPresetInfo();
            sessionPreset.setPollingStatusInRoom(SessionPresetEnum.off);
            timerManager.stopPollingCompensation(evictParticipant.getSessionId());
            JsonObject params = new JsonObject();
            params.addProperty(ProtocolElements.STOP_POLLING_ROOMID_PARAM, evictParticipant.getSessionId());

            rpcNotificationService.sendBatchNotificationConcurrent(participants, ProtocolElements.STOP_POLLING_NODIFY_METHOD, params);
        }
        if (OpenViduRole.MODERATOR.equals(evictParticipant.getRole())
                && evictStrategies.contains(EvictParticipantStrategy.CLOSE_ROOM_WHEN_EVICT_MODERATOR)) {    // close the room
            dealSessionClose(evictParticipant.getSessionId(), EndReason.sessionClosedByServer);
        } else {
            // check if MAJOR is speaker
            if (ParticipantHandStatus.speaker.equals(evictParticipant.getHandStatus())) {
                endSpeaker(session, evictParticipant, evictParticipant.getUuid());
            }
            evictParticipant(evictParticipant, null, null, reason);
        }

        // clear the rpc connection if necessary
        if (evictStrategies.contains(EvictParticipantStrategy.CLOSE_WEBSOCKET_CONNECTION)) {
            rpcNotificationService.closeRpcSession(evictParticipant.getParticipantPrivateId());
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
//                JsonObject notifyParam = session.getLayoutNotifyInfo();
//                session.getParticipants().forEach(part -> rpcNotificationService.sendNotification(part.getParticipantPrivateId(),
//                        ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, notifyParam));
            }
        }
    }

    @Override
    public void updateRoomAndPartInfoAfterKMSDisconnect(String sessionId) {
        Session session;
        if (Objects.nonNull(session = getSession(sessionId))) {
            // del part info in cache
            session.getParticipants().forEach(participant -> cacheManage.delPartInfo(participant.getUuid()));
            // del room info in cache
            cacheManage.delRoomInfo(sessionId);
            // update conference status in DB
            updateConferenceInfo(sessionId);
        }
    }

    //delete 2.0 Deprecated use evictParticipant()
    // @Deprecated
//    private void evictParticipantWithSamePrivateId(Map<String, Participant> samePrivateIdParts, List<EvictParticipantStrategy> evictStrategies, EndReason reason) {
//        // check if include moderator
//        Session session;
//        Participant majorPart = samePrivateIdParts.get(StreamType.MAJOR.name());
//        Set<Participant> participants = (session = getSession(majorPart.getSessionId())).getParticipants();
//        if (OpenViduRole.MODERATOR.equals(majorPart.getRole()) && session.getPresetInfo().getPollingStatusInRoom().equals(SessionPresetEnum.on)) {
//            //stop polling
//            SessionPreset sessionPreset = session.getPresetInfo();
//            sessionPreset.setPollingStatusInRoom(SessionPresetEnum.off);
//            timerManager.stopPollingCompensation(majorPart.getSessionId());
//            JsonObject params = new JsonObject();
//            params.addProperty(ProtocolElements.STOP_POLLING_ROOMID_PARAM, majorPart.getSessionId());
//            participants.forEach(part -> rpcNotificationService.sendNotification(part.getParticipantPrivateId(),
//                    ProtocolElements.STOP_POLLING_NODIFY_METHOD, params));
//        }
//        if (OpenViduRole.MODERATOR.equals(majorPart.getRole())
//                && evictStrategies.contains(EvictParticipantStrategy.CLOSE_ROOM_WHEN_EVICT_MODERATOR)) {    // close the room
//
//            dealSessionClose(majorPart.getSessionId(), EndReason.sessionClosedByServer);
//        } else {
//            // check if MAJOR is speaker
//            if (ParticipantHandStatus.speaker.equals(majorPart.getHandStatus())) {
//                JsonObject params = new JsonObject();
//                params.addProperty(ProtocolElements.END_ROLL_CALL_ROOM_ID_PARAM, majorPart.getSessionId());
//                params.addProperty(ProtocolElements.END_ROLL_CALL_TARGET_ID_PARAM, majorPart.getUuid());
//
//                // send end roll call
//                participants.forEach(participant -> rpcNotificationService.sendNotification(participant.getParticipantPrivateId(),
//                        ProtocolElements.END_ROLL_CALL_METHOD, params));
//            }
//            // check if exists SHARING
//            Participant sharePart;
//            if (Objects.nonNull(sharePart = samePrivateIdParts.get(StreamType.SHARING.name()))) {
//                JsonObject params = new JsonObject();
//                params.addProperty(ProtocolElements.RECONNECTPART_STOP_PUBLISH_SHARING_CONNECTIONID_PARAM,
//                        sharePart.getParticipantPublicId());
//
//                // send stop SHARING
//                participants.forEach(participant -> rpcNotificationService.sendNotification(participant.getParticipantPrivateId(),
//                        ProtocolElements.RECONNECTPART_STOP_PUBLISH_SHARING_METHOD, params));
//                // change session share status
//                if (ConferenceModeEnum.MCU.equals(session.getConferenceMode())) {
//                    KurentoSession kurentoSession = (KurentoSession) session;
//                    kurentoSession.getCompositeService().setExistSharing(false);
//                    kurentoSession.getCompositeService().setShareStreamId(null);
//                }
//
//            }
//
//            // change the layout if mode is MCU
//            if (ConferenceModeEnum.MCU.equals(session.getConferenceMode())) {
//                Map<String, String> layoutRelativePartIdMap = session.getLayoutRelativePartId();
//                boolean layoutChanged = false;
//                for (Participant part : samePrivateIdParts.values()) {
//                    layoutChanged |= session.leaveRoomSetLayout(part,
//                            !Objects.equals(layoutRelativePartIdMap.get("speakerId"), part.getParticipantPublicId())
//                                    ? layoutRelativePartIdMap.get("speakerId") : layoutRelativePartIdMap.get("moderatorId"));
//                }
//
//                if (layoutChanged) {
//                    // notify kms change the layout of MCU
//                    session.invokeKmsConferenceLayout();
//
//                    // notify client the change of layout
////                    JsonObject params = session.getLayoutNotifyInfo();
////                    participants.forEach(participant -> rpcNotificationService.sendNotification(participant.getParticipantPrivateId(),
////                            ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, params));
//                }
//            }
//
//            // evict participants
//            samePrivateIdParts.values().forEach(participant -> evictParticipant(participant, null,
//                    null, reason));
//
//            // delete deal auto on wall
//            //session.putPartOnWallAutomatically(this);
//        }
//
//        // clear the rpc connection if necessary
//        if (evictStrategies.contains(EvictParticipantStrategy.CLOSE_WEBSOCKET_CONNECTION)) {
//            rpcNotificationService.closeRpcSession(majorPart.getParticipantPrivateId());
//        }
//    }

    @Override
    public KurentoMediaOptions generateMediaOptions(Request<JsonObject> request) throws OpenViduException {

        String sdpOffer = RpcAbstractHandler.getStringParam(request, ProtocolElements.PUBLISHVIDEO_SDPOFFER_PARAM);
        boolean hasAudio = RpcAbstractHandler.getBooleanParam(request, ProtocolElements.PUBLISHVIDEO_HASAUDIO_PARAM);
        boolean hasVideo = RpcAbstractHandler.getBooleanParam(request, ProtocolElements.PUBLISHVIDEO_HASVIDEO_PARAM);

        String videoStatus = RpcAbstractHandler.getStringOptionalParam(request, ProtocolElements.PUBLISHVIDEO_VIDEOSTATUS_PARAM);
        String micStatus = RpcAbstractHandler.getStringOptionalParam(request, ProtocolElements.PUBLISHVIDEO_MICSTATUS_PARAM);
        Boolean audioActive = null, videoActive = null;
        String typeOfVideo = null, videoDimensions = null;
        Integer frameRate = null;
        KurentoFilter kurentoFilter = null;

        try {
            audioActive = RpcAbstractHandler.getBooleanOptionalParam(request, ProtocolElements.PUBLISHVIDEO_AUDIOACTIVE_PARAM);
            if (!StringUtils.isEmpty(micStatus)) {
                audioActive = ParticipantMicStatus.on.name().equals(micStatus);
            }
            videoActive = RpcAbstractHandler.getBooleanOptionalParam(request, ProtocolElements.PUBLISHVIDEO_VIDEOACTIVE_PARAM);
            if (!StringUtils.isEmpty(videoStatus)) {
                videoActive = ParticipantVideoStatus.on.name().equals(videoStatus);
            }
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
    public boolean unpublishStream(Session session, String streamId, Participant unPubPart, Integer transactionId,
                                   EndReason reason) {
        log.info("Stream:{} unPublish in session:{}", streamId, session.getSessionId());
        KurentoSession kSession = (KurentoSession) session;

        this.unpublishVideo(unPubPart, streamId, transactionId, reason);

        if (Objects.equals(kSession.getConferenceMode(), ConferenceModeEnum.MCU)) {
            session.getCompositeService().asyncUpdateComposite();
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
            if (!publisher.isStreaming(StreamType.MAJOR)) {
                log.warn(
                        "PARTICIPANT {}: Requesting to applyFilter to user {} "
                                + "in session {} but user is not streaming media",
                        moderator != null ? moderator.getParticipantPublicId() : publisher.getParticipantPublicId(),
                        publisher.getParticipantPublicId(), session.getSessionId());
                throw new OpenViduException(Code.USER_NOT_STREAMING_ERROR_CODE,
                        "User '" + publisher.getParticipantPublicId() + " not streaming media in session '"
                                + session.getSessionId() + "'");
            } else if (kParticipantPublisher.getPublisher(StreamType.MAJOR).getFilter() != null) {
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
                    ((KurentoParticipant) publisher).getPublisher(StreamType.MAJOR).filterCollectionsToString());

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
            if (!participant.isStreaming(StreamType.MAJOR)) {
                log.warn(
                        "PARTICIPANT {}: Requesting to removeFilter to user {} "
                                + "in session {} but user is not streaming media",
                        moderator != null ? moderator.getParticipantPublicId() : participant.getParticipantPublicId(),
                        participant.getParticipantPublicId(), session.getSessionId());
                throw new OpenViduException(Code.USER_NOT_STREAMING_ERROR_CODE,
                        "User '" + participant.getParticipantPublicId() + " not streaming media in session '"
                                + session.getSessionId() + "'");
            } else if (kParticipant.getPublisher(StreamType.MAJOR).getFilter() == null) {
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
                    kParticipant.getPublisher(StreamType.MAJOR).filterCollectionsToString());

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
            if (!participant.isStreaming(StreamType.MAJOR)) {
                log.warn(
                        "PARTICIPANT {}: Requesting to execFilterMethod to user {} "
                                + "in session {} but user is not streaming media",
                        moderator != null ? moderator.getParticipantPublicId() : participant.getParticipantPublicId(),
                        participant.getParticipantPublicId(), session.getSessionId());
                throw new OpenViduException(Code.USER_NOT_STREAMING_ERROR_CODE,
                        "User '" + participant.getParticipantPublicId() + " not streaming media in session '"
                                + session.getSessionId() + "'");
            } else if (kParticipant.getPublisher(StreamType.MAJOR).getFilter() == null) {
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
            if (!kParticipantPublishing.isStreaming(StreamType.MAJOR)) {
                log.warn(
                        "PARTICIPANT {}: Requesting to addFilterEventListener to stream {} "
                                + "in session {} but the publisher is not streaming media",
                        userSubscribing.getParticipantPublicId(), streamId, session.getSessionId());
                throw new OpenViduException(Code.USER_NOT_STREAMING_ERROR_CODE,
                        "User '" + kParticipantPublishing.getParticipantPublicId() + " not streaming media in session '"
                                + session.getSessionId() + "'");
            } else if (kParticipantPublishing.getPublisher(StreamType.MAJOR).getFilter() == null) {
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
                    //this.addFilterEventListenerInPublisher(kParticipantPublishing, eventType);
                    kParticipantPublishing.getPublisher(StreamType.MAJOR).addParticipantAsListenerOfFilterEvent(eventType,
                            userSubscribing.getParticipantPublicId());
                } catch (OpenViduException e) {
                    throw e;
                }
            }

            log.info("State of filter for participant {}: {}", kParticipantPublishing.getParticipantPublicId(),
                    kParticipantPublishing.getPublisher(StreamType.MAJOR).filterCollectionsToString());

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
            if (!participantPublishing.isStreaming(StreamType.MAJOR)) {
                log.warn(
                        "PARTICIPANT {}: Requesting to removeFilterEventListener to stream {} "
                                + "in session {} but user is not streaming media",
                        subscriber.getParticipantPublicId(), streamId, session.getSessionId());
                throw new OpenViduException(Code.USER_NOT_STREAMING_ERROR_CODE,
                        "User '" + participantPublishing.getParticipantPublicId() + " not streaming media in session '"
                                + session.getSessionId() + "'");
            } else if (kParticipantPublishing.getPublisher(StreamType.MAJOR).getFilter() == null) {
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
                    PublisherEndpoint pub = kParticipantPublishing.getPublisher(StreamType.MAJOR);
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
                    kParticipantPublishing.getPublisher(StreamType.MAJOR).filterCollectionsToString());

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
        kParticipant.getPublisher(StreamType.MAJOR).apply(builder.build());
        kParticipant.getPublisher(StreamType.MAJOR).getMediaOptions().setFilter(filter);
    }

    private void removeFilterInPublisher(KurentoParticipant kParticipant) {
        kParticipant.getPublisher(StreamType.MAJOR).cleanAllFilterListeners();
        kParticipant.getPublisher(StreamType.MAJOR).revert(kParticipant.getPublisher(StreamType.MAJOR).getFilter());
        kParticipant.getPublisher(StreamType.MAJOR).getMediaOptions().setFilter(null);
    }

    private KurentoFilter execFilterMethodInPublisher(KurentoParticipant kParticipant, String method,
                                                      JsonObject params) {
        kParticipant.getPublisher(StreamType.MAJOR).execMethod(method, params);
        KurentoFilter filter = kParticipant.getPublisher(StreamType.MAJOR).getMediaOptions().getFilter();
        KurentoFilter updatedFilter = new KurentoFilter(filter.getType(), filter.getOptions(), method, params);
        kParticipant.getPublisher(StreamType.MAJOR).getMediaOptions().setFilter(updatedFilter);
        return updatedFilter;
    }
//delete 2.0
//    private void addFilterEventListenerInPublisher(KurentoParticipant kParticipant, String eventType)
//            throws OpenViduException {
//        PublisherEndpoint pub = kParticipant.getPublisher(StreamType.MAJOR);
//        if (!pub.isListenerAddedToFilterEvent(eventType)) {
//            final String connectionId = kParticipant.getParticipantPublicId();
//            final String streamId = kParticipant.getPublisher(StreamType.MAJOR).getStreamId();
//            final String filterType = kParticipant.getPublisherMediaOptions().getFilter().getType();
//            try {
//                ListenerSubscription listener = pub.getFilter().addEventListener(eventType, event -> {
//                    sessionEventsHandler.onFilterEventDispatched(connectionId, streamId, filterType, event.getType(),
//                            event.getData(), kParticipant.getSession().getParticipants(),
//                            kParticipant.getPublisher(StreamType.MAJOR).getPartipantsListentingToFilterEvent(eventType));
//                });
//                pub.storeListener(eventType, listener);
//            } catch (Exception e) {
//                log.error("Request to addFilterEventListener to stream {} gone wrong. Error: {}", streamId,
//                        e.getMessage());
//                throw new OpenViduException(Code.FILTER_EVENT_LISTENER_NOT_FOUND,
//                        "Request to addFilterEventListener to stream " + streamId + " gone wrong: " + e.getMessage());
//            }
//        }
//    }

    private void removeFilterEventListenerInPublisher(KurentoParticipant kParticipant, String eventType) {
        PublisherEndpoint pub = kParticipant.getPublisher(StreamType.MAJOR);
        if (pub.isListenerAddedToFilterEvent(eventType)) {
            GenericMediaElement filter = kParticipant.getPublisher(StreamType.MAJOR).getFilter();
            filter.removeEventListener(pub.removeListener(eventType));
        }
    }

    @Override
    public void startRecording(String sessionId) {
        Session session;
        if (Objects.isNull(session = getSession(sessionId))) {
            log.info("Start recording but session:{} is closed.", sessionId);
            return;
        }

        RecorderService recorderService = session.getRecorderService();
        if (recorderService == null) {
            recorderService = session.createRecorderService(this.recordingRedisPublisher);
        }

        log.info("Start recording and sessionId is {}", sessionId);
        recorderService.startRecording();
    }

    @Override
    public void stopRecording(String sessionId) {
        Session session;
        if (Objects.isNull(session = getSession(sessionId))) {
            log.info("Stop recording but session:{} is closed.", sessionId);
            return;
        }

        log.info("Stop recording and sessionId is {}", sessionId);
        // pub stop recording task
        if (session.getRecorderService() != null) {
            session.getRecorderService().stopRecording();
        }
    }

    @Override
    public void updateRecording(String sessionId) {
        Session session;
        if (Objects.nonNull(session = getSession(sessionId))) {
            log.info("Update recording and sessionId is {}", sessionId);
            session.getRecorderService().updateRecording();
        } else {
            log.info("Update recording but session:{} is closed.", sessionId);
        }
    }


    //delete 2.0
//    private boolean constructMediaSources(ConferenceRecordingProperties recordingProperties, KurentoSession kurentoSession, Participant curParticipant) {
//        Participant sharingPart = kurentoSession.getSharingPart().orElse(null), moderatorPart = null, speakerPart = kurentoSession.getSpeakerPart().orElse(null);
//        Set<Participant> participants = kurentoSession.getParticipants();
//        for (Participant participant : participants) {
//
//            if (participant.getRole().isController() && participant.getRole().needToPublish()) {
//                moderatorPart = participant;
//            }
//        }
//
//        JsonObject mediaSourceObj = new JsonObject();
//        mediaSourceObj.addProperty("kmsLocated", kurentoSession.getKms().getIp());
//        mediaSourceObj.addProperty("mediaPipelineId", kurentoSession.getPipeline().getId());
//
//        int order = 1;
//        JsonArray passThruList = new JsonArray();
//        if (Objects.isNull(sharingPart)) {
//            // layout of recording is the same as MCU layout
//            if (ConferenceModeEnum.SFU == kurentoSession.getConferenceMode()) {
//                List<Participant> parts = kurentoSession.getOrderedMajorAndOnWallParts();
//                recordingProperties.setLayoutMode(parts.size());
//                boolean haveMajorStream = false;
//                if (Objects.nonNull(speakerPart)) {
//                    haveMajorStream = true;
//
//                    passThruList.add(constructPartRecordInfo(speakerPart, order));
//                    order++;
//                }
//                List<Participant> notSpeakerParts = parts.stream().filter(participant -> !ParticipantHandStatus.speaker.equals(participant.getHandStatus())).collect(Collectors.toList());
//                for (Participant participant : notSpeakerParts) {
//                    if (haveMajorStream) {
//                        passThruList.add(constructPartRecordInfo(demotionMinorStream(participant, kurentoSession), order));
//                    } else {
//                        haveMajorStream = true;
//                        passThruList.add(constructPartRecordInfo(participant, order));
//                    }
//                    order++;
//                }
//            } else {
//                recordingProperties.setLayoutMode(kurentoSession.getLayoutMode().getMode());
//                JsonArray majorShareMixLinkedArr = kurentoSession.getMajorShareMixLinkedArr();
//
//                for (JsonElement jsonElement : majorShareMixLinkedArr) {
//                    String publicId = jsonElement.getAsJsonObject().get("connectionId").getAsString();
//                    Optional<Participant> part = participants.stream()
//                            .filter(participant -> Objects.equals(publicId, participant.getParticipantPublicId())).findAny();
//                    if (part.isPresent()) {
//                        passThruList.add(constructPartRecordInfo(part.get(), order));
//                        order++;
//                    }
//                }
//            }
//        } else {
//            if (Objects.isNull(moderatorPart)) {
//                log.error("Moderator participant not found.");
//                return false;
//            }
//
//            // specific recording layout
//            passThruList.add(constructPartRecordInfo(sharingPart, 1));
//            if (Objects.isNull(speakerPart)) {
//                passThruList.add(constructPartRecordInfo(demotionMinorStream(moderatorPart, kurentoSession), 2));
//                recordingProperties.setLayoutMode(LayoutModeEnum.TWO.getMode());
//            } else {
//                Participant speakerPartMinorStream = demotionMinorStream(speakerPart, kurentoSession);
//                if (Objects.nonNull(curParticipant) &&
//                        (curParticipant.getUuid().equals(speakerPartMinorStream.getUuid()))) {
//                    log.warn("cur participant uuid:{} streamType:{} we need minor streamType(Maybe minor stream is not publish now)");
//                    return false;
//                }
//                passThruList.add(constructPartRecordInfo(speakerPartMinorStream, 2));
//                passThruList.add(constructPartRecordInfo(demotionMinorStream(moderatorPart, kurentoSession), 3));
//                recordingProperties.setLayoutMode(LayoutModeEnum.THREE.getMode());
//            }
//        }
//
//        if (passThruList.size() == 0) {
//            log.error("No passThru elements added.");
//            return false;
//        }
//        mediaSourceObj.add("passThruList", passThruList);
//        JsonArray mediaSources = new JsonArray();
//        mediaSources.add(mediaSourceObj);
//
//        recordingProperties.setMediaSources(mediaSources);
//        return true;
//    }

    // delete 2.0
//    /**
//     * ??????????????????
//     *
//     * @param part ?????????????????????????????????????????????
//     * @return ???????????????????????????????????????????????????
//     */
//    private Participant demotionMinorStream(Participant part, KurentoSession kurentoSession) {
//        if (part.getTerminalType() != TerminalTypeEnum.HDC) {
//            return part;
//        }
////        Participant minorPart = kurentoSession.getPartByPrivateIdAndStreamType(part.getParticipantPrivateId(), StreamType.MINOR);
////        if (minorPart != null) {
////            if (minorPart.isStreaming())
////                return minorPart;
////            log.info("minorPart is not null but not streaming");
////        }
//        return part;
//    }

    //delete 2.0
//    private JsonObject constructPartRecordInfo(Participant part, int order) {
//        KurentoParticipant kurentoParticipant = (KurentoParticipant) part;
//        log.info("record construct participant:{}, uuid:{}, osd:{}, order:{}, role:{}, handStatus:{},record info.",
//                part.getParticipantPublicId(), part.getUuid(), part.getUsername(), order, part.getRole().name(), part.getHandStatus().name());
//
//        PublisherEndpoint publisherEndpoint = kurentoParticipant.getPublisher(StreamType.MAJOR);
//        if (Objects.isNull(publisherEndpoint) || Objects.isNull(publisherEndpoint.getPassThru())) {
//            publisherEndpoint = new PublisherEndpoint(true, kurentoParticipant, part.getParticipantPublicId(),
//                    kurentoParticipant.getSession().getPipeline(), StreamType.MAJOR, this.openviduConfig);
//            publisherEndpoint.setCompositeService(kurentoParticipant.getSession().getCompositeService());
//            publisherEndpoint.setPassThru(new PassThrough.Builder(kurentoParticipant.getSession().getPipeline()).build());
//            kurentoParticipant.setPublisher(StreamType.MAJOR, publisherEndpoint);
//        }
//        JsonObject jsonObject = new JsonObject();
//        if (Objects.nonNull(publisherEndpoint.getPassThru())) {
//            jsonObject.addProperty("passThruId", publisherEndpoint.getPassThru().getId());
//            jsonObject.addProperty("order", order);
//            jsonObject.addProperty("uuid", part.getUuid());
//            //jsonObject.addProperty("streamType", part.getStreamType().name());
//            jsonObject.addProperty("osd", part.getUsername());
//
//        }
//        return jsonObject;
//    }

    @Override
    public void handleRecordErrorEvent(Object msg) {
        String ruid;
        log.info("recording-error {}", msg);
        JsonObject jsonObject = new Gson().fromJson(String.valueOf(msg), JsonObject.class);
        if (jsonObject.has("method") && jsonObject.has("params")
                && Objects.nonNull(ruid = jsonObject.get("params").getAsJsonObject().get("ruid").getAsString())) {
            Session session;
            Optional<Session> sessionOptional = getSessions().stream().filter(session1 -> Objects.equals(ruid, session1.getRuid())).findAny();
            if (sessionOptional.isPresent()) {
                session = sessionOptional.get();
                switch (jsonObject.get("method").getAsString()) {
                    case CommonConstants.RECORD_STOP_BY_FILL_IN_STORAGE:
                        stopRecordAndNotify(session, CommonConstants.RECORD_STOP_BY_FILL_IN_STORAGE);
                        break;
                    case CommonConstants.RECORD_STOP_BY_MODERATOR:
                        changeRoomRecordStatusAndNotify(session);
                        break;
                    case CommonConstants.RECORD_STORAGE_LESS_THAN_TEN_PERCENT:
                        sendStorageNotify(session);
                        break;
                    case CommonConstants.RECORD_REBUILD_TASK:
                        reStartRecord(session);
                        break;
                    default:
                        break;
                }
            } else {
                log.warn("Session that ruid:{} not found.", ruid);
            }
        } else {
            log.error("Invalid record error event:{}", msg);
        }
    }

    private void reStartRecord(Session session) {
        startRecording(session.getSessionId());
    }

    private void sendStorageNotify(Session session) {
        JsonObject notify = new JsonObject();
        notify.addProperty("reason", CommonConstants.RECORD_STORAGE_LESS_THAN_TEN_PERCENT);
        session.getParticipants()
                .stream()
                .filter(participant -> participant.getRole().isController())
                .forEach(participant ->
                        rpcNotificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.STOP_CONF_RECORD_METHOD, notify));

        sendChnMsg(session, "RecordStorage");
    }

    private void sendChnMsg(Session session, String usage) {
        // send sms to admin user
        String project = session.getConference().getProject();
        User adminUser = userManage.getAdminUserByProject(project);
        if (StringUtils.isEmpty(adminUser.getPhone())) {
            return;
        }

        JsonObject smsObj = new JsonObject();
        JsonObject contentObj = new JsonObject();
        contentObj.addProperty("project", project);

        smsObj.addProperty("phoneNumber", adminUser.getPhone());
        smsObj.add("content", contentObj);
        smsObj.addProperty("smsType", usage);

        if (!cacheManage.checkDuplicationSendPhone(adminUser.getPhone(), usage)) {
            log.info("duplication send phone msg smsObj = {}", smsObj.toString());
            return;
        }
        log.info("send phone msg smsObj = {}", smsObj.toString());
        redisPublisher.sendChnMsg(BrokerChannelConstans.SMS_DELIVERY_CHANNEL, smsObj.toString());
    }

    private void changeRoomRecordStatusAndNotify(Session session) {
        if (session.sessionAllowedToStopRecording()) {
            JsonObject notify = new JsonObject();
            notify.addProperty("reason", CommonConstants.RECORD_STOP_BY_MODERATOR);
            session.getParticipants().forEach(participant ->
                    rpcNotificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.STOP_CONF_RECORD_METHOD, notify));
        } else {
            log.warn("Fail to change the record status and roomId:{}, ruid:{}", session.getSessionId(), session.getRuid());
        }
    }

    private void stopRecordAndNotify(Session session, String reason) {
        if (session.sessionAllowedToStopRecording()) {
            // stop the recording
            stopRecording(session.getSessionId());
            // send the stopping recording notify
            JsonObject notify = new JsonObject();
            notify.addProperty("reason", reason);
            session.getParticipants().forEach(participant -> {
                rpcNotificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.STOP_CONF_RECORD_METHOD, notify);
            });
        } else {
            log.warn("Fail to stop the record and ruid:{}", session.getRuid());
        }
        sendChnMsg(session, "StorageFillIn");
    }

    // ??????????????????kill -9??????????????????????????????????????????????????????
    class RoomLeaseThread extends Thread {
        private final Session session;

        public RoomLeaseThread(Session session) {
            this.session = session;
        }

        @Override
        public void run() {
            log.info("room lease thead start,roomId={}, ruid={}", session.getSessionId(), session.getRuid());
            int idleCnt = 0;
            while (!session.isClosed()) {
                try {
                    cacheManage.roomLease(session.getSessionId(), session.getRuid());
                    TimeUnit.SECONDS.sleep(10);
                    if (!org.apache.commons.lang.StringUtils.startsWith(session.getRuid(), "appt-") && session.getPartSize() == 0) {
                        idleCnt = session.getPartSize() == 0 ? ++idleCnt : 0;
                    }
                    if (idleCnt > 6) {
                        log.info("room lease thead interrupt, roomId={}, ruid={} that close room", session.getSessionId(), session.getRuid());
                        closeRoom(session, EndReason.sessionIdleTimeout);
                        return;
                    }
                    if (new Random().nextInt(60) == 59) {
                        try {
                            if (rtcRoomClient.leaseRoom(session.getSessionId(), session.getPresetInfo().getInstanceId())) {
                                log.warn("room lease to rtc-user failure, roomId={}, ruid={}", session.getSessionId(), session.getRuid());
                            }
                        } catch (Exception e) {
                            log.warn("room lease to rtc-user exception, roomId={}, ruid={}", session.getSessionId(), session.getRuid(), e);
                        }
                    }
                } catch (InterruptedException e) {
                    return;
                }
            }
            log.info("room lease thead stop,roomId={}, ruid={}", session.getSessionId(), session.getRuid());
        }
    }
}
