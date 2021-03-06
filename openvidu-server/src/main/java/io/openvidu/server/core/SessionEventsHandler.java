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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.cdr.CallDetailRecord;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.config.InfoHandler;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.exception.NoSuchKmsException;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.kurento.endpoint.KurentoFilter;
import io.openvidu.server.kurento.endpoint.PublisherEndpoint;
import io.openvidu.server.kurento.kms.KmsManager;
import io.openvidu.server.recording.Recording;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcNotificationService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class SessionEventsHandler {

    private static final Logger log = LoggerFactory.getLogger(SessionEventsHandler.class);

    @Autowired
    protected RpcNotificationService rpcNotificationService;

    @Autowired
    protected InfoHandler infoHandler;

    @Autowired
    protected CallDetailRecord CDR;

    @Autowired
    protected OpenviduConfig openviduConfig;

    @Autowired
    public CacheManage cacheManage;

    @Autowired
    protected SessionManager sessionManager;

    @Autowired
    private KmsManager kmsManager;

    @Value("${conference.delivery.load.factor}")
    private int loadFactor = 999;

    Map<String, Recording> recordingsStarted = new ConcurrentHashMap<>();

    ReentrantLock lock = new ReentrantLock();

    public void onSessionCreated(Session session) {
        CDR.recordSessionCreated(session);
    }

    public void onSessionClosed(String sessionId, EndReason reason) {
        CDR.recordSessionDestroyed(sessionId, reason);
    }


    public void onParticipantJoined(Conference conference, Participant participant, String sessionId, Set<Participant> existingParticipants,
                                    Integer transactionId, OpenViduException error) {
        if (error != null) {
            rpcNotificationService.sendErrorResponse(participant.getParticipantPrivateId(), transactionId, null, error);
            return;
        }
        Session session = sessionManager.getSession(sessionId);
        JsonObject result = new JsonObject();
        JsonObject roomInfoJson = new JsonObject();
        //JsonArray resultArray = new JsonArray();
        UseTime.point("join room p5");
        participantJoined(participant, existingParticipants);
        UseTime.point("join room p6");

        // MCU start
        if (session.getConferenceMode() == ConferenceModeEnum.SFU && session.getPresetInfo().getMcuThreshold() < session.getPartSize()) {
            log.info("session {} ConferenceModeEnum change {} -> {}", sessionId, session.getConferenceMode().name(), ConferenceModeEnum.MCU.name());
            session.getCompositeService().createComposite();
        }

        notifyUpdateOrder(participant, session);
        roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_USER_PARAM, participant.getParticipantPublicId());
        roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_CREATEDAT_PARAM, participant.getCreatedAt());
        roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_METADATA_PARAM, participant.getFullMetadata());
        roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_MIC_STATUS_PARAM, participant.getMicStatus().name());
        roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_VIDEO_STATUS_PARAM, participant.getVideoStatus().name());
        roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_SHARE_POWER_PARAM, participant.getPreset().getSharePowerInRoom().name());
        roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_SUBJECT_PARAM, session.getPresetInfo().getRoomSubject());
        roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_CONFERENCE_MODE_PARAM, session.getConferenceMode().name());
        roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_ROOM_CAPACITY_PARAM, participant.getPreset().getRoomCapacity());
        roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_ROOM_CREATE_AT_PARAM, session.getStartTime());
        roomInfoJson.addProperty("subtitleConfig", session.getSubtitleConfig().name());
        roomInfoJson.addProperty("order", participant.getOrder());
        //roomInfoJson.addProperty("pushStreamStatus",participant.getPushStreamStatus().name());
        roomInfoJson.add("languageTypes", new Gson().fromJson(session.getLanguages().toString(), JsonArray.class));
        if (Objects.nonNull(session.getSubtitleExtraConfig())) {
            roomInfoJson.add("extraInfo", session.getSubtitleExtraConfig());
        }
        roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_ROOM_CREATE_AT_PARAM, session.getStartTime());
        roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_ALLOW_PART_OPER_SHARE_PARAM, participant.getPreset().getAllowPartOperShare().name());
        roomInfoJson.addProperty("allowRecord", participant.getPreset().getAllowRecord().name());
        roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_APP_SHOWNAME_PARAM, participant.getAppShowName());
        roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_APP_SHOWDESC_PARAM, participant.getAppShowDesc());
        //roomInfoJson.addProperty(ProtocolElements.JOINROOM_STREAM_TYPE_PARAM, participant.getStreamType().name());
        roomInfoJson.addProperty(ProtocolElements.SETPARTOPERSPEAKER_ALLOWPARTOPERSPEAKER_PARAM, participant.getPreset().getAllowPartOperSpeaker().name());
        roomInfoJson.addProperty("isVoiceMode", participant.getVoiceMode().equals(VoiceMode.on));
        roomInfoJson.addProperty("automatically", session.isAutomatically());
        roomInfoJson.addProperty("roomIdType", conference.getRoomIdType());
        roomInfoJson.addProperty("roomId", conference.getRoomId());
        roomInfoJson.addProperty("ruid", conference.getRuid());
        roomInfoJson.addProperty("moderatorAccount", conference.getModeratorUuid());
        roomInfoJson.addProperty("moderatorName", conference.getModeratorName());
        roomInfoJson.addProperty("moderatorName", conference.getModeratorName());
        roomInfoJson.addProperty("partSize", session.getPartSize());
        roomInfoJson.addProperty("sfuPublisherThreshold", session.getPresetInfo().getSfuPublisherThreshold());
        roomInfoJson.addProperty("mcuThreshold", session.getPresetInfo().getMcuThreshold());
        roomInfoJson.addProperty("unMcuThreshold", session.getPresetInfo().getUnMcuThreshold());
        roomInfoJson.addProperty("sharingUuid", session.getSharingPart().isPresent() ? session.getSharingPart().get().getUuid() : "");
        roomInfoJson.addProperty("speakerUuid", session.getSpeakerPart().isPresent() ? session.getSpeakerPart().get().getUuid() : "");
        roomInfoJson.addProperty("isRecording", session.getIsRecording());
        roomInfoJson.addProperty("quietStatusInRoom", session.getPresetInfo().getQuietStatusInRoom().name());
        roomInfoJson.addProperty("allowPartDismissMute", participant.getPreset().getAllowPartDismissMute().name());
        roomInfoJson.addProperty("imMode", session.getPresetInfo().getImMode());
        if (!session.isAutomatically()) {
            roomInfoJson.addProperty("mode", session.getLayoutMode().getMode());
        }
        if (participant.getRole().equals(OpenViduRole.MODERATOR)) {
            roomInfoJson.addProperty("moderatorPassword", conference.getModeratorPassword());
        }
        if (org.apache.commons.lang.StringUtils.isNotEmpty(conference.getPassword())) {
            roomInfoJson.addProperty("password", conference.getPassword());
        }

        //result.add("value", resultArray);

        if (Objects.equals(session.getConferenceMode(), ConferenceModeEnum.MCU)) {
            roomInfoJson.add(ProtocolElements.JOINROOM_MIXFLOWS_PARAM, session.getCompositeService().getMixFlowArr());

            JsonObject layoutInfoObj = new JsonObject();

            layoutInfoObj.add("linkedCoordinates", session.getCompositeService().getLayoutCoordinates());
            roomInfoJson.add("layoutInfo", layoutInfoObj);
        }
        result.add("roomInfo", roomInfoJson);

        //participant properties
        if (StringUtils.isNotEmpty(participant.getUsedRTCMode())) {
            JsonObject partInfo = new JsonObject();
            partInfo.addProperty("usedRTCMode", participant.getUsedRTCMode());
            result.add("partInfo", partInfo);
        }


        rpcNotificationService.sendResponse(participant.getParticipantPrivateId(), transactionId, result);

        new Thread(() -> deliveryOnParticipantJoined(session)).start();

        // ????????????MCU???MCU????????????
        if (session.getConferenceMode() == ConferenceModeEnum.MCU && participant.getRole().needToPublish()) {
            session.getCompositeService().asyncUpdateComposite();
        }

    }

    private void deliveryOnParticipantJoined(Session session) {
        // ????????????????????????????????????????????????
        KurentoSession ks = (KurentoSession) session;
        if (kmsManager.getKmss().size() < 2) {
            return;
        }
        loadFactor=1;//todo dev
        if (!ks.needMediaDeliveryKms(loadFactor) && session.getPresetInfo().getMcuThreshold() < 500) {
            return;
        }
        try {
            ks.createDeliveryKms(kmsManager.getLessLoadedKms(ks.getKms()), loadFactor);
        } catch (NoSuchKmsException e) {
            log.info("session {} delivery fail, no such kms", session.getSessionId());
        }
    }


    private void participantJoined(Participant participant, Set<Participant> existingParticipants) {
        JsonObject notifiedParams = new JsonObject();
        if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(participant.getParticipantPublicId())) {
            // Metadata associated to new participant
            RpcConnection rpcConnection = rpcNotificationService.getRpcConnection(participant.getParticipantPrivateId());
            if (rpcConnection == null) {
                log.info(participant.getParticipantPrivateId());
            }
            notifiedParams.addProperty(ProtocolElements.PARTICIPANTJOINED_USER_PARAM, participant.getParticipantPublicId());
            notifiedParams.addProperty(ProtocolElements.PARTICIPANTJOINED_UUID_PARAM, participant.getUuid());
            notifiedParams.addProperty(ProtocolElements.PARTICIPANTJOINED_CREATEDAT_PARAM, participant.getCreatedAt());
            notifiedParams.addProperty(ProtocolElements.PARTICIPANTJOINED_METADATA_PARAM, participant.getFullMetadata());
            notifiedParams.addProperty(ProtocolElements.PARTICIPANTJOINED_IS_RECONNECTED_PARAM, rpcConnection.isReconnected());
            notifiedParams.addProperty(ProtocolElements.PARTICIPANTJOINED_ABILITY_PARAM, rpcConnection.getAbility());
            notifiedParams.addProperty(ProtocolElements.PARTICIPANTJOINED_FUNCTIONALITY_PARAM, rpcConnection.getFunctionality());
            notifiedParams.addProperty(ProtocolElements.PARTICIPANTJOINED_TERMINAL_TYPE_PARAM, rpcConnection.getTerminalType().name());
            notifiedParams.addProperty(ProtocolElements.PARTICIPANTJOINED_ROLE_PARAM, participant.getRole().name());
            notifiedParams.addProperty("order", participant.getOrder());
            if (!Objects.isNull(rpcConnection.getTerminalConfig()))
                notifiedParams.add(ProtocolElements.PARTICIPANTJOINED_TERMINALCONFIG_PARAM, new Gson().fromJson(rpcConnection.getTerminalConfig(), JsonObject.class));
        }

        List<String> notifyList = new ArrayList<>();
        for (Participant existingParticipant : existingParticipants) {
            if (Objects.equals(existingParticipant.getParticipantPublicId(), participant.getParticipantPublicId())) {
                continue;
            }
            // If RECORDER participant has joined do NOT send 'participantJoined'
            // notification to existing participants. 'recordingStarted' will be sent to all
            // existing participants when recorder first subscribe to a stream
            if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(participant.getParticipantPublicId())) {
                if (!participant.getParticipantPrivateId().equals(existingParticipant.getParticipantPrivateId())) {
                    notifyList.add(existingParticipant.getParticipantPrivateId());
                }
            }
        }

        if (!notifyList.isEmpty()) {
            rpcNotificationService.sendBatchNotification(notifyList,
                    ProtocolElements.PARTICIPANTJOINED_METHOD, notifiedParams);
        }
    }

    private static final HashSet<String> notifyUpdateOrderLock = new HashSet<>();

    /**
     * ?????????????????????????????????
     * ??????0.2???????????????????????????????????????????????????
     */
    private void notifyUpdateOrder(Participant participant, Session session) {
        if (ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(participant.getParticipantPublicId())) {
            return;
        }
        boolean notifyFlag;
        synchronized (notifyUpdateOrderLock) {
            notifyFlag = notifyUpdateOrderLock.add(session.getSessionId());//???????????????????????????????????????????????????
        }
        if (notifyFlag) {
            log.info("notifyUpdateOrder pass");
            asyncNotifyUpdateOrder(session);
        } else {
            log.info("notifyUpdateOrder skip");
        }
    }

    /**
     * ?????????????????????????????????,
     */
    private void asyncNotifyUpdateOrder(Session session) {
        Thread thread = new Thread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(200);//??????0.2???
            } catch (InterruptedException e) {
                //
            }
            synchronized (notifyUpdateOrderLock) {
                notifyUpdateOrderLock.remove(session.getSessionId());
            }
            if (session.isClosing() || session.isClosed()) {
                log.info("session {} is closing or is closed,stop notifyUpdateOrder", session.getSessionId());
                return;
            }
            log.info("notifyUpdateOrder doing");
            session.notifyPartOrderOrRoleChanged(true, null, false,
                    OpenViduRole.PUBLISHER, OpenViduRole.SUBSCRIBER, rpcNotificationService);
//			Set<Participant> existParticipants = session.getParticipants();
//			if (!CollectionUtils.isEmpty(existParticipants)) {
//				JsonObject notifyParam = new JsonObject();
//				JsonArray orderedParts = new JsonArray();
//				for (Participant exist : existParticipants) {
//					JsonObject order = new JsonObject();
//					order.addProperty("account", exist.getUuid());
//					order.addProperty("uuid", exist.getUuid());
//					order.addProperty("order", exist.getOrder());
//					orderedParts.add(order);
//				}
//				notifyParam.add("orderedParts", orderedParts);
//
//				List<String> notifyList = existParticipants.stream().map(Participant::getParticipantPrivateId).collect(Collectors.toList());
//				rpcNotificationService.sendBatchNotification(notifyList,
//						ProtocolElements.UPDATE_PARTICIPANTS_ORDER_METHOD, notifyParam);
//			}
        });
        thread.setName("asyncNotifyUpdateOrder-" + session.getSessionId() + "-thread");
        thread.start();
    }

    public void onParticipantLeft(Participant participant, String sessionId, Set<Participant> remainingParticipants,
                                  Integer transactionId, OpenViduException error, EndReason reason) {
        if (error != null) {
            rpcNotificationService.sendErrorResponse(participant.getParticipantPrivateId(), transactionId, null, error);
            return;
        }

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.PARTICIPANTLEFT_NAME_PARAM, participant.getParticipantPublicId());
        params.addProperty(ProtocolElements.PARTICIPANTLEFT_UUID_PARAM, participant.getUuid());
        params.addProperty(ProtocolElements.PARTICIPANTLEFT_REASON_PARAM, reason != null ? reason.name() : "");

        List<String> notifyPartList = new ArrayList<>();
        for (Participant p : remainingParticipants) {
            if (!p.getParticipantPrivateId().equals(participant.getParticipantPrivateId())) {
                notifyPartList.add(p.getParticipantPrivateId());
            }
        }
        rpcNotificationService.sendBatchNotification(notifyPartList, ProtocolElements.PARTICIPANTLEFT_METHOD, params);

        if (transactionId != null) {
            // No response when the participant is forcibly evicted instead of voluntarily
            // leaving the session
            rpcNotificationService.sendResponse(participant.getParticipantPrivateId(), transactionId, params);
        }
    }

    public void onPublishMedia(Participant participant, String streamId, Long createdAt, String sdpAnswer,
                               Integer transactionId, OpenViduException error, PublisherEndpoint publisherEndpoint, StreamType streamType) {
        if (error != null) {
            rpcNotificationService.sendErrorResponse(participant.getParticipantPrivateId(), transactionId, null, error);
            return;
        }

        KurentoParticipant kurentoParticipant = (KurentoParticipant) participant;
        JsonObject result = new JsonObject();
        result.addProperty(ProtocolElements.PUBLISHVIDEO_SDPANSWER_PARAM, sdpAnswer);
        result.addProperty(ProtocolElements.PUBLISHVIDEO_PUBLISHID_PARAM, streamId);
        result.addProperty(ProtocolElements.PUBLISHVIDEO_CREATEDAT_PARAM, createdAt);
        result.addProperty("streamType", streamType.name());
        rpcNotificationService.sendResponse(participant.getParticipantPrivateId(), transactionId, result);

        kurentoParticipant.notifyPublishChannelPass(publisherEndpoint);
    }

    public void notifyPublishMedias(Participant participant, PublisherEndpoint publisherEndpoint, String sessionId, Set<Participant> participants) {

        MediaOptions mediaOptions = publisherEndpoint.getMediaOptions();
        KurentoParticipant kurentoParticipant = (KurentoParticipant) participant;

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_UUID_PARAM, participant.getUuid());
        params.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_NAME_PARAM, participant.getParticipantPublicId());
        params.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_METADATA_PARAM, participant.getFullMetadata());
        params.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_APPSHOWNAME_PARAM, participant.getAppShowName());
        params.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_APPSHOWDESC_PARAM, participant.getAppShowDesc());
        params.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_STREAMTYPE_PARAM, publisherEndpoint.getStreamType().name());

        JsonObject stream = new JsonObject();
        stream.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_STREAMID_PARAM, publisherEndpoint.getStreamId());
        stream.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_STREAMTYPE_PARAM, publisherEndpoint.getStreamType().name());
        stream.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_CREATEDAT_PARAM, publisherEndpoint.createdAt());
        stream.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_HASAUDIO_PARAM, mediaOptions.hasAudio);
        stream.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_HASVIDEO_PARAM, mediaOptions.hasVideo);
        // stream.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_MIXINCLUDED_PARAM, kurentoParticipant.isMixIncluded());
        stream.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_AUDIOACTIVE_PARAM, mediaOptions.audioActive);
        stream.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_VIDEOACTIVE_PARAM, mediaOptions.videoActive);
        stream.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_TYPEOFVIDEO_PARAM, mediaOptions.typeOfVideo);
        stream.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_FRAMERATE_PARAM, mediaOptions.frameRate);
        stream.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_VIDEODIMENSIONS_PARAM, mediaOptions.videoDimensions);
        JsonElement filter = mediaOptions.getFilter() != null ? mediaOptions.getFilter().toJson() : new JsonObject();
        stream.add(ProtocolElements.JOINROOM_PEERSTREAMFILTER_PARAM, filter);

        JsonArray streamsArray = new JsonArray();
        streamsArray.add(stream);
        params.add(ProtocolElements.PARTICIPANTPUBLISHED_STREAMS_PARAM, streamsArray);

        Session conferenceSession = sessionManager.getSession(sessionId);
        // delete 2.0
//		if (Objects.equals(ConferenceModeEnum.MCU, conferenceSession.getConferenceMode())) {
//			params.add(ProtocolElements.JOINROOM_MIXFLOWS_PARAM, getMixFlowArr(sessionId));
//		}

        Set<Participant> publisherParticipants = new HashSet<>();
        Set<Participant> subscribeParticipants = new HashSet<>();
        for (Participant p : participants) {
            if (p.getRole() != OpenViduRole.SUBSCRIBER) {
                publisherParticipants.add(p);
            } else {
                subscribeParticipants.add(p);
            }
        }

        rpcNotificationService.sendBatchNotificationConcurrent(publisherParticipants, ProtocolElements.PARTICIPANTPUBLISHED_METHOD, params);
        log.info("publisher participants num:{} subscriber participants num:{}",
                publisherParticipants.size(), subscribeParticipants.size());

        int nParticipantIndex = 0;
        for (Participant p : subscribeParticipants) {
            if (nParticipantIndex % 10 == 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(500);//??????0.5???
                    log.info("notifyPublishMedias {}", nParticipantIndex);
                } catch (InterruptedException e) {
                    //
                }
            }

            nParticipantIndex++;
            rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
                    ProtocolElements.PARTICIPANTPUBLISHED_METHOD, params);

            // broadcast the changes of layout
//				if (Objects.equals(conferenceSession.getConferenceMode(), ConferenceModeEnum.MCU)) {
//					rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
//							ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, conferenceSession.getLayoutNotifyInfo());
//				}
        }
    }


    public void onUnpublishMedia(Participant participant, Set<Participant> participants, PublisherEndpoint publisherEndpoint,
                                 Integer transactionId, OpenViduException error, EndReason reason) {
        if (error != null) {
            rpcNotificationService.sendErrorResponse(participant.getParticipantPrivateId(), transactionId, null, error);
            return;
        }
        if (!Objects.equals(EndReason.closeSessionByModerator, reason)) {
            JsonObject params = new JsonObject();
            params.addProperty(ProtocolElements.PARTICIPANT_UNPUBLISHED_NAME_PARAM, participant.getParticipantPublicId());
            params.addProperty(ProtocolElements.PARTICIPANT_UNPUBLISHED_UUID_PARAM, participant.getUuid());
            params.addProperty(ProtocolElements.PARTICIPANT_UNPUBLISHED_REASON_PARAM, reason != null ? reason.name() : "");
            params.addProperty(ProtocolElements.PARTICIPANT_UNPUBLISHED_STREAM_TYPE_PARAM, publisherEndpoint.getStreamType().name());
            params.addProperty(ProtocolElements.PARTICIPANT_UNPUBLISHED_ID_PARAM, publisherEndpoint.getStreamId());

            rpcNotificationService.sendResponse(participant.getParticipantPrivateId(), transactionId, new JsonObject());
            rpcNotificationService.sendBatchNotificationConcurrent(participants,
                    ProtocolElements.PARTICIPANT_UNPUBLISHED_METHOD, params);
        }
    }

    public void onSubscribe(Participant participant, Session session, String sdpAnswer, Map<String, Object> resultObj, Integer transactionId,
                            OpenViduException error) {
        if (error != null) {
            rpcNotificationService.sendErrorResponse(participant.getParticipantPrivateId(), transactionId, null, error);
            return;
        }
        String subscribeId = resultObj.get("subscribeId").toString();
        JsonObject result = new JsonObject();
        result.addProperty("sdpAnswer", sdpAnswer);
        result.addProperty("subscribeId", subscribeId);
        rpcNotificationService.sendResponse(participant.getParticipantPrivateId(), transactionId, result);

        ((KurentoParticipant) participant).getSubscribers().get(subscribeId).gatherCandidates();
    }

    public void onUnsubscribe(Participant participant, Integer transactionId, OpenViduException error) {
        if (error != null) {
            rpcNotificationService.sendErrorResponse(participant.getParticipantPrivateId(), transactionId, null, error);
            return;
        }
        rpcNotificationService.sendResponse(participant.getParticipantPrivateId(), transactionId, new JsonObject());
    }

    public void onSendMessage(Participant participant, JsonObject message, Set<Participant> participants,
                              Integer transactionId, OpenViduException error) {
        if (error != null) {
            rpcNotificationService.sendErrorResponse(participant.getParticipantPrivateId(), transactionId, null, error);
            return;
        }

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.PARTICIPANTSENDMESSAGE_DATA_PARAM, message.get("data").getAsString());
        params.addProperty(ProtocolElements.PARTICIPANTSENDMESSAGE_FROM_PARAM, participant.getParticipantPublicId());
        params.addProperty(ProtocolElements.PARTICIPANTSENDMESSAGE_TYPE_PARAM, message.get("type").getAsString());

        Set<String> toSet = new HashSet<String>();

        if (message.has("to")) {
            JsonArray toJson = message.get("to").getAsJsonArray();
            for (int i = 0; i < toJson.size(); i++) {
                JsonElement el = toJson.get(i);
                if (el.isJsonNull()) {
                    throw new OpenViduException(Code.SIGNAL_TO_INVALID_ERROR_CODE,
                            "Signal \"to\" field invalid format: null");
                }
                toSet.add(el.getAsString());
            }
        }

        if (toSet.isEmpty()) {
            for (Participant p : participants) {
                rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
                        ProtocolElements.PARTICIPANTSENDMESSAGE_METHOD, params);
            }
        } else {
            Set<String> participantPublicIds = participants.stream().map(Participant::getParticipantPublicId)
                    .collect(Collectors.toSet());
            for (String to : toSet) {
                if (participantPublicIds.contains(to)) {
                    Optional<Participant> p = participants.stream().filter(x -> to.equals(x.getParticipantPublicId())).findFirst();
                    rpcNotificationService.sendNotification(p.get().getParticipantPrivateId(),
                            ProtocolElements.PARTICIPANTSENDMESSAGE_METHOD, params);
                } else {
                    throw new OpenViduException(Code.SIGNAL_TO_INVALID_ERROR_CODE,
                            "Signal \"to\" field invalid format: Connection [" + to + "] does not exist");
                }
            }
        }

        rpcNotificationService.sendResponse(participant.getParticipantPrivateId(), transactionId, new JsonObject());
    }

    public void onStreamPropertyChanged(Participant participant, Integer transactionId, Set<Participant> participants,
                                        String streamId, String property, JsonElement newValue, String reason) {

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.STREAMPROPERTYCHANGED_CONNECTIONID_PARAM,
                participant.getParticipantPublicId());
        params.addProperty(ProtocolElements.STREAMPROPERTYCHANGED_STREAMID_PARAM, streamId);
        params.addProperty(ProtocolElements.STREAMPROPERTYCHANGED_PROPERTY_PARAM, property);
        params.addProperty(ProtocolElements.STREAMPROPERTYCHANGED_NEWVALUE_PARAM, newValue.toString());
        params.addProperty(ProtocolElements.STREAMPROPERTYCHANGED_REASON_PARAM, reason);

        for (Participant p : participants) {
            if (p.getParticipantPrivateId().equals(participant.getParticipantPrivateId())) {
                rpcNotificationService.sendResponse(participant.getParticipantPrivateId(), transactionId,
                        new JsonObject());
            } else {
                rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
                        ProtocolElements.STREAMPROPERTYCHANGED_METHOD, params);
            }
        }
    }

    public void onRecvIceCandidate(Participant participant, Integer transactionId, ErrorCodeEnum error) {
        if (error != null && error != ErrorCodeEnum.SUCCESS) {
            rpcNotificationService.sendErrorResponseWithDesc(participant.getParticipantPrivateId(), transactionId, null, error);
            return;
        }
        rpcNotificationService.sendResponse(participant.getParticipantPrivateId(), transactionId, new JsonObject());
    }

    public void onForceDisconnect(Participant moderator, Participant evictedParticipant, Set<Participant> participants,
                                  Integer transactionId, OpenViduException error, EndReason reason) {

        boolean isRpcCall = transactionId != null;
        if (isRpcCall) {
            if (error != null) {
                rpcNotificationService.sendErrorResponse(moderator.getParticipantPrivateId(), transactionId, null,
                        error);
                return;
            }
            rpcNotificationService.sendResponse(moderator.getParticipantPrivateId(), transactionId, new JsonObject());
        }

        if (!Objects.equals(EndReason.closeSessionByModerator, reason)) {
            JsonObject params = new JsonObject();
            params.addProperty(ProtocolElements.PARTICIPANTEVICTED_CONNECTIONID_PARAM,
                    evictedParticipant.getParticipantPublicId());
            params.addProperty(ProtocolElements.PARTICIPANTE_VICTED_UUID_PARAM,
                    evictedParticipant.getUuid());
            params.addProperty(ProtocolElements.PARTICIPANTEVICTED_REASON_PARAM, reason != null ? reason.name() : "");

//			if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(evictedParticipant.getParticipantPublicId())) {
//				log.info("evictedParticipant ParticipantPublicId {}", evictedParticipant.getParticipantPublicId());
//				// Do not send a message when evicting RECORDER participant
//				try {
//					rpcNotificationService.sendNotification(evictedParticipant.getParticipantPrivateId(),
//							ProtocolElements.PARTICIPANTEVICTED_METHOD, params);
//				} catch (Exception e) {
//					log.error("Exception:\n", e);
//				}
//			}
//			for (Participant p : participants) {
//				if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(evictedParticipant.getParticipantPublicId())) {
//					log.info("p ParticipantPublicId {}", p.getParticipantPublicId());
//					if (!p.getParticipantPrivateId().equals(evictedParticipant.getParticipantPrivateId())
//							&& Objects.equals(StreamType.MAJOR, p.getStreamType())) {
//						rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
//								ProtocolElements.PARTICIPANTEVICTED_METHOD, params);
//					}
//				}
//			}
            rpcNotificationService.sendBatchNotificationConcurrent(participants, ProtocolElements.PARTICIPANTEVICTED_METHOD, params);
        }
    }

    public void sendRecordingStartedNotification(Session session, Recording recording) {

        // Filter participants by roles according to "openvidu.recording.notification"
        Set<Participant> filteredParticipants = this.filterParticipantsByRole(
                this.openviduConfig.getRolesFromRecordingNotification(), session.getParticipants());

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.RECORDINGSTARTED_ID_PARAM, recording.getId());
        params.addProperty(ProtocolElements.RECORDINGSTARTED_NAME_PARAM, recording.getName());

        for (Participant p : filteredParticipants) {
            rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
                    ProtocolElements.RECORDINGSTARTED_METHOD, params);
        }
    }

    public void sendRecordingStoppedNotification(Session session, Recording recording, EndReason reason) {

        // Be sure to clean this map (this should return null)
        this.recordingsStarted.remove(session.getSessionId());

        // Filter participants by roles according to "openvidu.recording.notification"
        Set<Participant> existingParticipants;
        try {
            existingParticipants = session.getParticipants();
        } catch (OpenViduException exception) {
            // Session is already closed. This happens when RecordingMode.ALWAYS and last
            // participant has left the session. No notification needs to be sent
            log.warn("Session already closed when trying to send 'recordingStopped' notification");
            return;
        }
        Set<Participant> filteredParticipants = this.filterParticipantsByRole(
                this.openviduConfig.getRolesFromRecordingNotification(), existingParticipants);

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.RECORDINGSTOPPED_ID_PARAM, recording.getId());
        params.addProperty(ProtocolElements.RECORDINGSTARTED_NAME_PARAM, recording.getName());
        params.addProperty(ProtocolElements.RECORDINGSTOPPED_REASON_PARAM, reason != null ? reason.name() : "");

        for (Participant p : filteredParticipants) {
            rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
                    ProtocolElements.RECORDINGSTOPPED_METHOD, params);
        }
    }

    public void onFilterChanged(Participant participant, Participant moderator, Integer transactionId,
                                Set<Participant> participants, String streamId, KurentoFilter filter, OpenViduException error,
                                String filterReason) {
        boolean isRpcFromModerator = transactionId != null && moderator != null;

        if (isRpcFromModerator) {
            // A moderator forced the application of the filter
            if (error != null) {
                rpcNotificationService.sendErrorResponse(moderator.getParticipantPrivateId(), transactionId, null,
                        error);
                return;
            }
            rpcNotificationService.sendResponse(moderator.getParticipantPrivateId(), transactionId, new JsonObject());
        }

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.STREAMPROPERTYCHANGED_CONNECTIONID_PARAM,
                participant.getParticipantPublicId());
        params.addProperty(ProtocolElements.STREAMPROPERTYCHANGED_STREAMID_PARAM, streamId);
        params.addProperty(ProtocolElements.STREAMPROPERTYCHANGED_PROPERTY_PARAM, "filter");
        JsonObject filterJson = new JsonObject();
        if (filter != null) {
            filterJson.addProperty(ProtocolElements.FILTER_TYPE_PARAM, filter.getType());
            filterJson.add(ProtocolElements.FILTER_OPTIONS_PARAM, filter.getOptions());
            if (filter.getLastExecMethod() != null) {
                filterJson.add(ProtocolElements.EXECFILTERMETHOD_LASTEXECMETHOD_PARAM,
                        filter.getLastExecMethod().toJson());
            }
        }
        params.add(ProtocolElements.STREAMPROPERTYCHANGED_NEWVALUE_PARAM, filterJson);
        params.addProperty(ProtocolElements.STREAMPROPERTYCHANGED_REASON_PARAM, filterReason);

        for (Participant p : participants) {
            if (p.getParticipantPrivateId().equals(participant.getParticipantPrivateId())) {
                // Affected participant
                if (isRpcFromModerator) {
                    // Force by moderator. Send notification to affected participant
                    rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
                            ProtocolElements.STREAMPROPERTYCHANGED_METHOD, params);
                } else {
                    // Send response to participant
                    if (error != null) {
                        rpcNotificationService.sendErrorResponse(p.getParticipantPrivateId(), transactionId, null,
                                error);
                        return;
                    }
                    rpcNotificationService.sendResponse(p.getParticipantPrivateId(), transactionId, new JsonObject());
                }
            } else {
                // Send response to every other user in the session different than the affected
                // participant or the moderator
                if (error == null && (moderator == null
                        || !p.getParticipantPrivateId().equals(moderator.getParticipantPrivateId()))) {
                    rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
                            ProtocolElements.STREAMPROPERTYCHANGED_METHOD, params);
                }
            }
        }
    }

    public void onFilterEventDispatched(String connectionId, String streamId, String filterType, String eventType,
                                        Object data, Set<Participant> participants, Set<String> subscribedParticipants) {
        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.FILTEREVENTLISTENER_CONNECTIONID_PARAM, connectionId);
        params.addProperty(ProtocolElements.FILTEREVENTLISTENER_STREAMID_PARAM, streamId);
        params.addProperty(ProtocolElements.FILTEREVENTLISTENER_FILTERTYPE_PARAM, filterType);
        params.addProperty(ProtocolElements.FILTEREVENTLISTENER_EVENTTYPE_PARAM, eventType);
        params.addProperty(ProtocolElements.FILTEREVENTLISTENER_DATA_PARAM, data.toString());
        for (Participant p : participants) {
            if (subscribedParticipants.contains(p.getParticipantPublicId())) {
                rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
                        ProtocolElements.FILTEREVENTDISPATCHED_METHOD, params);
            }
        }
    }

    public void closeRpcSession(String participantPrivateId) {
        // update user online status in cache
        RpcConnection rpcConnection;
        if (!Objects.isNull(rpcConnection = rpcNotificationService.getRpcConnection(participantPrivateId))) {
            cacheManage.updateTerminalStatus(rpcConnection, TerminalStatus.offline);
            this.rpcNotificationService.closeRpcSession(participantPrivateId);
        }
    }

    public void setRecordingStarted(String sessionId, Recording recording) {
        this.recordingsStarted.put(sessionId, recording);
    }

    private Set<Participant> filterParticipantsByRole(OpenViduRole[] roles, Set<Participant> participants) {
        return participants.stream().filter(part -> {
            if (ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(part.getParticipantPublicId())) {
                return false;
            }
            boolean isRole = false;
            for (OpenViduRole role : roles) {
                isRole = role.equals(part.getRole());
                if (isRole)
                    break;
            }
            return isRole;
        }).collect(Collectors.toSet());
    }

    public void sendSuccessResp(String participantPrivateId, Integer transactionId) {
        rpcNotificationService.sendResponse(participantPrivateId, transactionId, new JsonObject());
    }

}
