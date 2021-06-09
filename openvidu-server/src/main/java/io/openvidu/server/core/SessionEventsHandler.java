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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
	private int loadFactor =10;

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
		JsonArray resultArray = new JsonArray();
		UseTime.point("join room p5");
		participantJoined(participant, existingParticipants);
		UseTime.point("join room p6");
		for (Participant existingParticipant : existingParticipants) {
			if (Objects.equals(existingParticipant.getParticipantPublicId(), participant.getParticipantPublicId())) {
				continue;
			}
			if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(participant.getParticipantPublicId())) {
				if (Objects.equals(OpenViduRole.THOR, existingParticipant.getRole())) {
					continue;
				}
			}

			JsonObject participantJson = new JsonObject();
			participantJson.addProperty(ProtocolElements.JOINROOM_PEERID_PARAM,
					existingParticipant.getParticipantPublicId());
			participantJson.addProperty(ProtocolElements.JOINROOM_MICSTATUS_PARAM,
					existingParticipant.getMicStatus().name());
			participantJson.addProperty(ProtocolElements.JOINROOM_VIDEOSTATUS_PARAM,
					existingParticipant.getVideoStatus().name());
			participantJson.addProperty(ProtocolElements.JOINROOM_PEERCREATEDAT_PARAM,
					existingParticipant.getCreatedAt());
			participantJson.addProperty(ProtocolElements.JOINROOM_PEERSHARESTATUS_PARAM,
					existingParticipant.getShareStatus().name());
			participantJson.addProperty(ProtocolElements.JOINROOM_PEERSPEAKERSTATUS_PARAM,
					existingParticipant.getSpeakerStatus().name());
			participantJson.addProperty(ProtocolElements.JOINROOM_PEERHANDSTATUS_PARAM,
					existingParticipant.getHandStatus().name());
			participantJson.addProperty(ProtocolElements.JOINROOM_PEERAPPSHOWNAME_PARAM,
					existingParticipant.getAppShowName());
			participantJson.addProperty(ProtocolElements.JOINROOM_PEERAPPSHOWDESC_PARAM,
					existingParticipant.getAppShowDesc());
			participantJson.addProperty(ProtocolElements.JOINROOM_STREAM_TYPE_PARAM,
					existingParticipant.getStreamType().name());
			RpcConnection rpc = rpcNotificationService.getRpcConnection(existingParticipant.getParticipantPrivateId());
			if (Objects.isNull(rpc)) {
				participantJson.addProperty(ProtocolElements.JOINROOM_PEERONLINESTATUS_PARAM,
						UserOnlineStatusEnum.offline.name());
			} else {
				String status = UserOnlineStatusEnum.online.name();
				participantJson.addProperty(ProtocolElements.JOINROOM_PEERONLINESTATUS_PARAM, status);
                participantJson.addProperty(ProtocolElements.JOINROOM_ABILITY_PARAM, rpc.getAbility());
                participantJson.addProperty(ProtocolElements.JOINROOM_FUNCTIONALITY_PARAM, rpc.getFunctionality());
				if (!Objects.isNull(rpc.getTerminalConfig())) {
					participantJson.add(ProtocolElements.JOINROOM_TERMINALCONFIG_PARAM, new Gson().fromJson(rpc.getTerminalConfig(), JsonObject.class));
				}
				participantJson.addProperty("deviceVersion", rpc.getDeviceVersion());
            }
            participantJson.addProperty("isVoiceMode", existingParticipant.getVoiceMode().equals(VoiceMode.on));
			participantJson.addProperty("order",existingParticipant.getOrder());
			participantJson.addProperty("pushStreamStatus",existingParticipant.getPushStreamStatus().name());


			// Metadata associated to each existing participant
			participantJson.addProperty(ProtocolElements.JOINROOM_METADATA_PARAM,
					existingParticipant.getFullMetadata());

			if (existingParticipant.isStreaming()) {
				try {
					KurentoParticipant kParticipant = (KurentoParticipant) existingParticipant;

					JsonObject stream = new JsonObject();
					stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMID_PARAM,
							existingParticipant.getPublisherStreamId());
					stream.addProperty(ProtocolElements.JOINROOM_PEERCREATEDAT_PARAM,
							kParticipant.getPublisher().createdAt());
					stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMHASAUDIO_PARAM,
							kParticipant.getPublisherMediaOptions().hasAudio);
					stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMHASVIDEO_PARAM,
							kParticipant.getPublisherMediaOptions().hasVideo);
					stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMMIXINCLUDED_PARAM,
							kParticipant.isMixIncluded());
					stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMVIDEOACTIVE_PARAM, ParticipantVideoStatus.on.equals(kParticipant.getVideoStatus()));
					stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMAUDIOACTIVE_PARAM, ParticipantMicStatus.on.equals(kParticipant.getMicStatus()));
					stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMTYPEOFVIDEO_PARAM,
							kParticipant.getPublisherMediaOptions().typeOfVideo);
					stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMFRAMERATE_PARAM,
							kParticipant.getPublisherMediaOptions().frameRate);
					stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMVIDEODIMENSIONS_PARAM,
							kParticipant.getPublisherMediaOptions().videoDimensions);
					JsonElement filter = kParticipant.getPublisherMediaOptions().getFilter() != null
							? kParticipant.getPublisherMediaOptions().getFilter().toJson()
							: new JsonObject();
					stream.add(ProtocolElements.JOINROOM_PEERSTREAMFILTER_PARAM, filter);

					JsonArray streamsArray = new JsonArray();
					streamsArray.add(stream);
					participantJson.add(ProtocolElements.JOINROOM_PEERSTREAMS_PARAM, streamsArray);
				} catch (Exception e) {
					log.error("get participant {} stream info error", existingParticipant.getUuid(), e);
				}
			}

			// Avoid emitting 'connectionCreated' event of existing RECORDER participant in
			// openvidu-browser in newly joined participants
			if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(existingParticipant.getParticipantPublicId())) {
				resultArray.add(participantJson);
			}
		}
		UseTime.point("join room p7");
		notifyUpdateOrder(participant, session);
		UseTime.point("join room p8");
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
		roomInfoJson.addProperty("order",participant.getOrder());
		roomInfoJson.addProperty("pushStreamStatus",participant.getPushStreamStatus().name());
		roomInfoJson.add("languageTypes", new Gson().fromJson(session.getLanguages().toString(), JsonArray.class));
		if (Objects.nonNull(session.getSubtitleExtraConfig())) {
			roomInfoJson.add("extraInfo", session.getSubtitleExtraConfig());
		}
		roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_ROOM_CREATE_AT_PARAM, session.getStartTime());
		roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_ALLOW_PART_OPER_MIC_PARAM, participant.getPreset().getAllowPartOperMic().name());
		roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_ALLOW_PART_OPER_SHARE_PARAM, participant.getPreset().getAllowPartOperShare().name());
		roomInfoJson.addProperty("allowRecord" , participant.getPreset().getAllowRecord().name());
		roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_APP_SHOWNAME_PARAM, participant.getAppShowName());
		roomInfoJson.addProperty(ProtocolElements.PARTICIPANTJOINED_APP_SHOWDESC_PARAM, participant.getAppShowDesc());
		roomInfoJson.addProperty(ProtocolElements.JOINROOM_STREAM_TYPE_PARAM, participant.getStreamType().name());
		roomInfoJson.addProperty(ProtocolElements.SETPARTOPERSPEAKER_ALLOWPARTOPERSPEAKER_PARAM,participant.getPreset().getAllowPartOperSpeaker().name());
		roomInfoJson.addProperty("isVoiceMode", participant.getVoiceMode().equals(VoiceMode.on));
		roomInfoJson.addProperty("automatically", session.isAutomatically());
        roomInfoJson.addProperty("roomIdType", conference.getRoomIdType());
        roomInfoJson.addProperty("roomId", conference.getRoomId());
        roomInfoJson.addProperty("ruid", conference.getRuid());
        roomInfoJson.addProperty("moderatorAccount", conference.getModeratorUuid());
		roomInfoJson.addProperty("moderatorName", conference.getModeratorName());
		roomInfoJson.addProperty(ProtocolElements.CREATE_ROOM_QUIET_STATUS_PARAM,participant.getPreset().getQuietStatusInRoom().name());
        if (!session.isAutomatically()) {
			roomInfoJson.addProperty("mode", session.getLayoutMode().getMode());
		}
        if (participant.getRole().equals(OpenViduRole.MODERATOR)) {
			roomInfoJson.addProperty("moderatorPassword", conference.getModeratorPassword());
		}
        if (org.apache.commons.lang.StringUtils.isNotEmpty(conference.getPassword())) {
			roomInfoJson.addProperty("password", conference.getPassword());
		}

		result.add("value", resultArray);

		if (Objects.equals(session.getConferenceMode(), ConferenceModeEnum.MCU)) {
			roomInfoJson.add(ProtocolElements.JOINROOM_MIXFLOWS_PARAM, getMixFlowArr(sessionId));

            JsonObject layoutInfoObj = new JsonObject();
            layoutInfoObj.add("linkedCoordinates", session.getCurrentPartInMcuLayout());
			roomInfoJson.add("layoutInfo", layoutInfoObj);
        }
		result.add("roomInfo", roomInfoJson);
		rpcNotificationService.sendResponse(participant.getParticipantPrivateId(), transactionId, result);

		new Thread(() -> deliveryOnParticipantJoined(session)).start();

	}

    private void deliveryOnParticipantJoined(Session session) {
        // 人数超过阈值后开始往第二台分发。
        KurentoSession ks = (KurentoSession) session;

        if (!ks.needMediaDeliveryKms(loadFactor)) {
            return;
        }
        try {
            ks.createDeliveryKms(kmsManager.getLessLoadedKms(ks.getKms()), loadFactor);
		} catch (NoSuchKmsException e) {
			log.info("session {} delivery fail, no such kms", session.getSessionId());
		}
    }



	private void participantJoined(Participant participant, Set<Participant> existingParticipants) {
		JsonObject notifParams = new JsonObject();
		if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(participant.getParticipantPublicId())) {
			// Metadata associated to new participant
			RpcConnection rpcConnection = rpcNotificationService.getRpcConnection(participant.getParticipantPrivateId());
			notifParams.addProperty(ProtocolElements.PARTICIPANTJOINED_USER_PARAM, participant.getParticipantPublicId());
			notifParams.addProperty(ProtocolElements.PARTICIPANTJOINED_CREATEDAT_PARAM, participant.getCreatedAt());
			notifParams.addProperty(ProtocolElements.PARTICIPANTJOINED_METADATA_PARAM, participant.getFullMetadata());
			notifParams.addProperty(ProtocolElements.PARTICIPANTJOINED_IS_RECONNECTED_PARAM, rpcConnection.isReconnected());
			notifParams.addProperty(ProtocolElements.PARTICIPANTJOINED_STREAM_TYPE_PARAM, participant.getStreamType().name());
			notifParams.addProperty(ProtocolElements.PARTICIPANTJOINED_ABILITY_PARAM, rpcConnection.getAbility());
			notifParams.addProperty(ProtocolElements.PARTICIPANTJOINED_FUNCTIONALITY_PARAM, rpcConnection.getFunctionality());
			if (StreamType.MAJOR.equals(participant.getStreamType())) {
				notifParams.addProperty("order", participant.getOrder());
			}
			if (!Objects.isNull(rpcConnection.getTerminalConfig()))
				notifParams.add(ProtocolElements.PARTICIPANTJOINED_TERMINALCONFIG_PARAM, new Gson().fromJson(rpcConnection.getTerminalConfig(), JsonObject.class));
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
				if (!participant.getParticipantPrivateId().equals(existingParticipant.getParticipantPrivateId())
						&& Objects.equals(StreamType.MAJOR, existingParticipant.getStreamType())) {
					notifyList.add(existingParticipant.getParticipantPrivateId());
				}
			}
		}

		if (!notifyList.isEmpty()) {
			rpcNotificationService.sendBatchNotification(notifyList,
					ProtocolElements.PARTICIPANTJOINED_METHOD, notifParams);
		}
	}

	private static final HashSet<String> notifyUpdateOrderLock = new HashSet<>();

	/**
	 * 通知端上排序有发生改变
	 * 延迟0.2秒通知，并合并期间的所有相同的通知
	 */
	private void notifyUpdateOrder(Participant participant, Session session) {
		if (ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(participant.getParticipantPublicId())) {
			return;
		}
		boolean notifyFlag;
		synchronized (notifyUpdateOrderLock) {
			notifyFlag = notifyUpdateOrderLock.add(session.getSessionId());//在这期间只有第一个线程允许进行通知
		}
		if (notifyFlag) {
			log.info("notifyUpdateOrder pass");
			asyncNotifyUpdateOrder(session);
		} else {
			log.info("notifyUpdateOrder skip");
		}
	}

	/**
	 * 通知端上排序有发生改变,
	 */
	private void asyncNotifyUpdateOrder(Session session) {
	    new Thread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(200);//延迟0.2秒
            } catch (InterruptedException e) {
                //
            }
            synchronized (notifyUpdateOrderLock) {
                notifyUpdateOrderLock.remove(session.getSessionId());
            }
			log.info("notifyUpdateOrder doing");
            Set<Participant> existParticipants =  session.getMajorPartEachConnect();
            if (!CollectionUtils.isEmpty(existParticipants)) {
                JsonObject notifyParam = new JsonObject();
                JsonArray orderedParts = new JsonArray();
                for (Participant exist : existParticipants) {
                    if (exist.getStreamType() == StreamType.MAJOR) {
                        JsonObject order = new JsonObject();
                        order.addProperty("account", exist.getUuid());
                        order.addProperty("order", exist.getOrder());
                        orderedParts.add(order);
                    }
                }
                notifyParam.add("orderedParts", orderedParts);

				List<String> notifyList = existParticipants.stream().map(Participant::getParticipantPrivateId).collect(Collectors.toList());
				rpcNotificationService.sendBatchNotification(notifyList,
						ProtocolElements.UPDATE_PARTICIPANTS_ORDER_METHOD, notifyParam);
            }
        }).start();
	}

	public void onParticipantLeft(Participant participant, String sessionId, Set<Participant> remainingParticipants,
			Integer transactionId, OpenViduException error, EndReason reason) {
		if (error != null) {
			rpcNotificationService.sendErrorResponse(participant.getParticipantPrivateId(), transactionId, null, error);
			return;
		}

		if (ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(participant.getParticipantPublicId())) {
			// RECORDER participant
			return;
		}

		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.PARTICIPANTLEFT_NAME_PARAM, participant.getParticipantPublicId());
		params.addProperty(ProtocolElements.PARTICIPANTLEFT_REASON_PARAM, reason != null ? reason.name() : "");

		List<String> notifyPartList = new ArrayList<>();
		for (Participant p : remainingParticipants) {
			if (!p.getParticipantPrivateId().equals(participant.getParticipantPrivateId())
					&& Objects.equals(StreamType.MAJOR, p.getStreamType())) {
				notifyPartList.add(p.getParticipantPrivateId());
			}
		}
		rpcNotificationService.sendBatchNotification(notifyPartList, ProtocolElements.PARTICIPANTLEFT_METHOD, params);

		if (transactionId != null) {
			// No response when the participant is forcibly evicted instead of voluntarily
			// leaving the session
			rpcNotificationService.sendResponse(participant.getParticipantPrivateId(), transactionId, params);
		}

		/*if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(participant.getParticipantPublicId())) {
			CDR.recordParticipantLeft(participant, sessionId, reason);
		}*/
	}

	public void onPublishMedia(Participant participant, String streamId, Long createdAt, String sdpAnswer,
							   Integer transactionId, OpenViduException error) {
		if (error != null) {
			rpcNotificationService.sendErrorResponse(participant.getParticipantPrivateId(), transactionId, null, error);
			return;
		}

        KurentoParticipant kurentoParticipant = (KurentoParticipant) participant;
		JsonObject result = new JsonObject();
		result.addProperty(ProtocolElements.PUBLISHVIDEO_SDPANSWER_PARAM, sdpAnswer);
		result.addProperty(ProtocolElements.PUBLISHVIDEO_STREAMID_PARAM, streamId);
		result.addProperty(ProtocolElements.PUBLISHVIDEO_CREATEDAT_PARAM, createdAt);
		rpcNotificationService.sendResponse(participant.getParticipantPrivateId(), transactionId, result);

		kurentoParticipant.notifyPublishChannelPass(kurentoParticipant.getPublisher());
	}

	public void notifyPublishMedias(Participant participant, String streamId, Long createdAt, String sessionId,
									MediaOptions mediaOptions, Set<Participant> participants, boolean isIntermit,
									Integer transactionId, OpenViduException error) {
		if (error != null) {
			rpcNotificationService.sendErrorResponse(participant.getParticipantPrivateId(), transactionId, null, error);
			return;
		}
		// 处理硬终端的问题，由平台模拟并拦截推流通知，在2.0时重构，这是临时代码
        if (participant.getTerminalType() == TerminalTypeEnum.S) {
            log.info("come in sip notifyPublishMedias function");
            if (participant.getHandStatus() == ParticipantHandStatus.speaker) {
                log.info("notify PublishMedias because sip is speaker");
            } else if (participant.getRole() != OpenViduRole.SUBSCRIBER) {
                log.info("notify PublishMedias because sip is not subscriber");
            } else {
                log.info("interrupt sip notifyPublishMedias");
                return;
            }
        }

		KurentoParticipant kurentoParticipant = (KurentoParticipant) participant;

		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_USER_PARAM, participant.getParticipantPublicId());
		params.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_METADATA_PARAM, participant.getFullMetadata());
		params.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_APPSHOWNAME_PARAM, participant.getAppShowName());
		params.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_APPSHOWDESC_PARAM, participant.getAppShowDesc());
		JsonObject stream = new JsonObject();

		stream.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_STREAMID_PARAM, streamId);
		stream.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_STREAMTYPE_PARAM, participant.getStreamType().name());
		stream.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_CREATEDAT_PARAM, createdAt);
		stream.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_HASAUDIO_PARAM, mediaOptions.hasAudio);
		stream.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_HASVIDEO_PARAM, mediaOptions.hasVideo);
		stream.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_MIXINCLUDED_PARAM, kurentoParticipant.isMixIncluded());
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
		if (Objects.equals(ConferenceModeEnum.MCU, conferenceSession.getConferenceMode())) {
			params.add(ProtocolElements.JOINROOM_MIXFLOWS_PARAM, getMixFlowArr(sessionId));
		}

		Set<Participant> publisherParticipants = new HashSet<>();
		Set<Participant> subscribeParticipants = new HashSet<>();
		for (Participant p : participants) {
			if (p.getRole() != OpenViduRole.SUBSCRIBER) {
				publisherParticipants.add(p);
			} else {
				subscribeParticipants.add(p);
			}
		}

		int nNotifyParticipantNum = 0;
		for (Participant p : publisherParticipants) {
			if (StreamType.MAJOR.equals(p.getStreamType())) {
				rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
						ProtocolElements.PARTICIPANTPUBLISHED_METHOD, params);
				nNotifyParticipantNum++;

				// broadcast the changes of layout
				if (Objects.equals(conferenceSession.getConferenceMode(), ConferenceModeEnum.MCU)) {
					rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
							ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, conferenceSession.getLayoutNotifyInfo());
				}
			}
		}
		log.info("publisher participants num:{} subscriber participants num:{} nNotifyParticipantNum:{}",
				publisherParticipants.size(), subscribeParticipants.size(), nNotifyParticipantNum);

		int nParticipantIndex = 0;
		for (Participant p : subscribeParticipants) {
			if (isIntermit && nParticipantIndex % 10 == 0) {
				try {
					TimeUnit.MILLISECONDS.sleep(500);//延迟0.5秒
					log.info("notifyPublishMedias {}", nParticipantIndex);
				} catch (InterruptedException e) {
					//
				}
			}

			nParticipantIndex++;
			if (StreamType.MAJOR.equals(p.getStreamType())) {
				rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
						ProtocolElements.PARTICIPANTPUBLISHED_METHOD, params);

				// broadcast the changes of layout
				if (Objects.equals(conferenceSession.getConferenceMode(), ConferenceModeEnum.MCU)) {
					rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
							ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, conferenceSession.getLayoutNotifyInfo());
				}
			}
		}
	}

	private JsonArray getMixFlowArr(String sessionId) {
        Session session = sessionManager.getSession(sessionId);
        JsonArray mixFlowsArr = new JsonArray(2);
        KurentoSession kurentoSession = (KurentoSession) session;
        if (!StringUtils.isEmpty(kurentoSession.compositeService.getMixMajorShareStreamId())) {
            JsonObject mixJsonObj = new JsonObject();
            mixJsonObj.addProperty(ProtocolElements.JOINROOM_MIXFLOWS_STREAMID_PARAM,
                    kurentoSession.compositeService.getMixMajorShareStreamId());
            mixJsonObj.addProperty(ProtocolElements.JOINROOM_MIXFLOWS_STREAMMODE_PARAM, StreamModeEnum.MIX_MAJOR_AND_SHARING.name());
            mixFlowsArr.add(mixJsonObj);

            if (!StringUtils.isEmpty(kurentoSession.compositeService.getShareStreamId())) {
                JsonObject shareJsonObj = new JsonObject();
                shareJsonObj.addProperty(ProtocolElements.JOINROOM_MIXFLOWS_STREAMID_PARAM,
                        kurentoSession.compositeService.getShareStreamId());
                shareJsonObj.addProperty(ProtocolElements.JOINROOM_MIXFLOWS_STREAMMODE_PARAM, StreamModeEnum.SFU_SHARING.name());
                mixFlowsArr.add(shareJsonObj);
            }
        }
        return mixFlowsArr;
    }

	public void onUnpublishMedia(Participant participant, Set<Participant> participants, Participant moderator,
			Integer transactionId, OpenViduException error, EndReason reason) {
		boolean isRpcFromModerator = transactionId != null && moderator != null;
		boolean isRpcFromOwner = transactionId != null && moderator == null;

		if (isRpcFromModerator) {
			if (error != null) {
				rpcNotificationService.sendErrorResponse(moderator.getParticipantPrivateId(), transactionId, null,
						error);
				return;
			}
			rpcNotificationService.sendResponse(moderator.getParticipantPrivateId(), transactionId, new JsonObject());
		}

		if (!Objects.equals(EndReason.closeSessionByModerator, reason)) {
			JsonObject params = new JsonObject();
			params.addProperty(ProtocolElements.PARTICIPANTUNPUBLISHED_NAME_PARAM, participant.getParticipantPublicId());
			params.addProperty(ProtocolElements.PARTICIPANTUNPUBLISHED_REASON_PARAM, reason != null ? reason.name() : "");

			for (Participant p : participants) {
				if (!Objects.equals(StreamType.MAJOR, p.getStreamType())) continue;
				log.info("unPublish ParticipantPublicId {} p PublicId {}", participant.getParticipantPublicId(), p.getParticipantPublicId());
				if (p.getParticipantPrivateId().equals(participant.getParticipantPrivateId())) {
					// Send response to the affected participant
					if (!isRpcFromOwner) {
						rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
								ProtocolElements.PARTICIPANTUNPUBLISHED_METHOD, params);
					} else {
						if (error != null) {
							rpcNotificationService.sendErrorResponse(p.getParticipantPrivateId(), transactionId, null,
									error);
							return;
						}
						rpcNotificationService.sendResponse(p.getParticipantPrivateId(), transactionId, new JsonObject());
					}
				} else {
					if (error == null) {
						// Send response to every other user in the session different than the affected
						// participant
						rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
								ProtocolElements.PARTICIPANTUNPUBLISHED_METHOD, params);
					}
				}
			}
		}
	}

	public void onSubscribe(Participant participant, Session session, String sdpAnswer, Integer transactionId,
			OpenViduException error) {
		if (error != null) {
			rpcNotificationService.sendErrorResponse(participant.getParticipantPrivateId(), transactionId, null, error);
			return;
		}
		JsonObject result = new JsonObject();
		result.addProperty(ProtocolElements.RECEIVEVIDEO_SDPANSWER_PARAM, sdpAnswer);
		rpcNotificationService.sendResponse(participant.getParticipantPrivateId(), transactionId, result);

		if (ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(participant.getParticipantPublicId())) {
			lock.lock();
			try {
				Recording recording = this.recordingsStarted.remove(session.getSessionId());
				if (recording != null) {
					// RECORDER participant is now receiving video from the first publisher
					this.sendRecordingStartedNotification(session, recording);
				}
			} finally {
				lock.unlock();
			}
		}
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
				if (!Objects.equals(StreamType.MAJOR, p.getStreamType())) continue;
				rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
						ProtocolElements.PARTICIPANTSENDMESSAGE_METHOD, params);
			}
		} else {
			Set<String> participantPublicIds = participants.stream().map(Participant::getParticipantPublicId)
					.collect(Collectors.toSet());
			for (String to : toSet) {
				if (participantPublicIds.contains(to)) {
					Optional<Participant> p = participants.stream().filter(x -> to.equals(x.getParticipantPublicId())
							&& Objects.equals(StreamType.MAJOR, x.getStreamType())).findFirst();
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
			if (!Objects.equals(StreamType.MAJOR, p.getStreamType())) continue;
			if (p.getParticipantPrivateId().equals(participant.getParticipantPrivateId())) {
				rpcNotificationService.sendResponse(participant.getParticipantPrivateId(), transactionId,
						new JsonObject());
			} else {
				rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
						ProtocolElements.STREAMPROPERTYCHANGED_METHOD, params);
			}
		}
	}

	public void onRecvIceCandidate(Participant participant, Integer transactionId, OpenViduException error) {
		if (error != null) {
			rpcNotificationService.sendErrorResponse(participant.getParticipantPrivateId(), transactionId, null, error);
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
			params.addProperty(ProtocolElements.PARTICIPANTEVICTED_REASON_PARAM, reason != null ? reason.name() : "");

			if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(evictedParticipant.getParticipantPublicId())) {
				log.info("evictedParticipant ParticipantPublicId {}", evictedParticipant.getParticipantPublicId());
				// Do not send a message when evicting RECORDER participant
				try {
					rpcNotificationService.sendNotification(evictedParticipant.getParticipantPrivateId(),
							ProtocolElements.PARTICIPANTEVICTED_METHOD, params);
				} catch (Exception e) {
					log.error("Exception:\n", e);
				}
			}
			for (Participant p : participants) {
				if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(evictedParticipant.getParticipantPublicId())) {
					log.info("p ParticipantPublicId {}", p.getParticipantPublicId());
					if (!p.getParticipantPrivateId().equals(evictedParticipant.getParticipantPrivateId())
							&& Objects.equals(StreamType.MAJOR, p.getStreamType())) {
						rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
								ProtocolElements.PARTICIPANTEVICTED_METHOD, params);
					}
				}
			}
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
			if (!Objects.equals(StreamType.MAJOR, p.getStreamType())) continue;
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
			if (!Objects.equals(StreamType.MAJOR, p.getStreamType())) continue;
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
			if (!Objects.equals(StreamType.MAJOR, p.getStreamType())) continue;
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
			if (!Objects.equals(StreamType.MAJOR, p.getStreamType())) continue;
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
