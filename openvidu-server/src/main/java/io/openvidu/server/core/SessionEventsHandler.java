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
import io.openvidu.server.config.InfoHandler;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.kurento.endpoint.KurentoFilter;
import io.openvidu.server.recording.Recording;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
	protected CacheManage cacheManage;

	@Autowired
	protected SessionManager sessionManager;

	Map<String, Recording> recordingsStarted = new ConcurrentHashMap<>();

	ReentrantLock lock = new ReentrantLock();

	public void onSessionCreated(Session session) {
		CDR.recordSessionCreated(session);
	}

	public void onSessionClosed(String sessionId, EndReason reason) {
		CDR.recordSessionDestroyed(sessionId, reason);
	}

	public void onParticipantJoined(Participant participant, String sessionId, Set<Participant> existingParticipants,
			Integer transactionId, OpenViduException error) {
		if (error != null) {
			rpcNotificationService.sendErrorResponse(participant.getParticipantPrivateId(), transactionId, null, error);
			return;
		}
		Session session = sessionManager.getSession(sessionId);
		JsonObject result = new JsonObject();
		JsonArray resultArray = new JsonArray();
		for (Participant existingParticipant : existingParticipants) {
			if (Objects.equals(existingParticipant.getParticipantPublicId(), participant.getParticipantPublicId())) {
				continue;
			}

			// If RECORDER participant has joined do NOT send 'participantJoined'
			// notification to existing participants. 'recordingStarted' will be sent to all
			// existing participants when recorder first subscribe to a stream
			if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(participant.getParticipantPublicId())) {
				JsonObject notifParams = new JsonObject();

				// Metadata associated to new participant
                RpcConnection rpcConnection = rpcNotificationService.getRpcConnection(participant.getParticipantPrivateId());
				notifParams.addProperty(ProtocolElements.PARTICIPANTJOINED_USER_PARAM, participant.getParticipantPublicId());
				notifParams.addProperty(ProtocolElements.PARTICIPANTJOINED_CREATEDAT_PARAM, participant.getCreatedAt());
				notifParams.addProperty(ProtocolElements.PARTICIPANTJOINED_METADATA_PARAM, participant.getFullMetadata());
				notifParams.addProperty(ProtocolElements.PARTICIPANTJOINED_IS_RECONNECTED_PARAM, rpcConnection.isReconnected());
				notifParams.addProperty(ProtocolElements.PARTICIPANTJOINED_STREAM_TYPE_PARAM, participant.getStreamType().name());
                notifParams.addProperty(ProtocolElements.PARTICIPANTJOINED_ABILITY_PARAM, rpcConnection.getAbility());
                if (!Objects.isNull(rpcConnection.getTerminalConfig()))
                	notifParams.add(ProtocolElements.PARTICIPANTJOINED_TERMINALCONFIG_PARAM, rpcConnection.getTerminalConfig());

				if (!participant.getParticipantPrivateId().equals(existingParticipant.getParticipantPrivateId())
						&& Objects.equals(StreamType.MAJOR, existingParticipant.getStreamType())) {
					rpcNotificationService.sendNotification(existingParticipant.getParticipantPrivateId(),
							ProtocolElements.PARTICIPANTJOINED_METHOD, notifParams);
				}

				if (Objects.equals(OpenViduRole.THOR, existingParticipant.getRole())) {
					continue;
				}
			}

			JsonObject participantJson = new JsonObject();
			participantJson.addProperty(ProtocolElements.JOINROOM_PEERID_PARAM,
					existingParticipant.getParticipantPublicId());
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
				Map userInfo = cacheManage.getUserInfoByUUID(rpc.getUserUuid());
				String status = Objects.isNull(userInfo) ? UserOnlineStatusEnum.offline.name() :
						UserOnlineStatusEnum.offline.name().equals(String.valueOf(userInfo.get("status"))) ?
								UserOnlineStatusEnum.offline.name() : UserOnlineStatusEnum.online.name();
				participantJson.addProperty(ProtocolElements.JOINROOM_PEERONLINESTATUS_PARAM, status);
                participantJson.addProperty(ProtocolElements.JOINROOM_ABILITY_PARAM, rpc.getAbility());
				if (!Objects.isNull(rpc.getTerminalConfig())) {
					participantJson.add(ProtocolElements.JOINROOM_TERMINALCONFIG_PARAM, rpc.getTerminalConfig());
				}
            }
            participantJson.addProperty("isVoiceMode", existingParticipant.getVoiceMode().equals(VoiceMode.on));


			// Metadata associated to each existing participant
			participantJson.addProperty(ProtocolElements.JOINROOM_METADATA_PARAM,
					existingParticipant.getFullMetadata());

			if (existingParticipant.isStreaming()) {

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
				stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMVIDEOACTIVE_PARAM,
						kParticipant.getPublisherMediaOptions().videoActive);
				stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMAUDIOACTIVE_PARAM,
						kParticipant.isStreaming() && kParticipant.getPublisherMediaOptions().audioActive);
				stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMVIDEOACTIVE_PARAM,
						kParticipant.isStreaming() && kParticipant.getPublisherMediaOptions().videoActive);
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
			}

			// Avoid emitting 'connectionCreated' event of existing RECORDER participant in
			// openvidu-browser in newly joined participants
			if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(existingParticipant.getParticipantPublicId())) {
				resultArray.add(participantJson);
			}
		}

		result.addProperty(ProtocolElements.PARTICIPANTJOINED_USER_PARAM, participant.getParticipantPublicId());
		result.addProperty(ProtocolElements.PARTICIPANTJOINED_CREATEDAT_PARAM, participant.getCreatedAt());
		result.addProperty(ProtocolElements.PARTICIPANTJOINED_METADATA_PARAM, participant.getFullMetadata());
		result.addProperty(ProtocolElements.PARTICIPANTJOINED_MIC_STATUS_PARAM, participant.getMicStatus().name());
		result.addProperty(ProtocolElements.PARTICIPANTJOINED_VIDEO_STATUS_PARAM, participant.getVideoStatus().name());
		result.addProperty(ProtocolElements.PARTICIPANTJOINED_SHARE_POWER_PARAM, participant.getPreset().getSharePowerInRoom().name());
		result.addProperty(ProtocolElements.PARTICIPANTJOINED_SUBJECT_PARAM, participant.getPreset().getRoomSubject());
		result.addProperty(ProtocolElements.PARTICIPANTJOINED_CONFERENCE_MODE_PARAM, session.getConferenceMode().name());
		result.addProperty(ProtocolElements.PARTICIPANTJOINED_ROOM_CAPACITY_PARAM, participant.getPreset().getRoomCapacity());
		result.addProperty(ProtocolElements.PARTICIPANTJOINED_ROOM_CREATE_AT_PARAM, session.getStartTime());
		result.addProperty("subtitleConfig", session.getSubtitleConfig().name());
		result.add("languageTypes", new Gson().fromJson(session.getLanguages().toString(), JsonArray.class));
		if (Objects.nonNull(session.getSubtitleExtraConfig())) {
			result.add("extraInfo", session.getSubtitleExtraConfig());
		}
		result.addProperty(ProtocolElements.PARTICIPANTJOINED_ROOM_CREATE_AT_PARAM, session.getStartTime());
		result.addProperty(ProtocolElements.PARTICIPANTJOINED_ALLOW_PART_OPER_MIC_PARAM, participant.getPreset().getAllowPartOperMic().name());
		result.addProperty(ProtocolElements.PARTICIPANTJOINED_ALLOW_PART_OPER_SHARE_PARAM, participant.getPreset().getAllowPartOperShare().name());
		result.addProperty(ProtocolElements.PARTICIPANTJOINED_APP_SHOWNAME_PARAM, participant.getAppShowName());
		result.addProperty(ProtocolElements.PARTICIPANTJOINED_APP_SHOWDESC_PARAM, participant.getAppShowDesc());
		result.addProperty(ProtocolElements.JOINROOM_STREAM_TYPE_PARAM, participant.getStreamType().name());
		result.addProperty(ProtocolElements.SETPARTOPERSPEAKER_ALLOWPARTOPERSPEAKER_PARAM,participant.getPreset().getAllowPartOperSpeaker().name());
        result.addProperty("isVoiceMode", participant.getVoiceMode().equals(VoiceMode.on));
		result.add("value", resultArray);

		if (Objects.equals(session.getConferenceMode(), ConferenceModeEnum.MCU)) {
            result.add(ProtocolElements.JOINROOM_MIXFLOWS_PARAM, getMixFlowArr(sessionId));

            JsonObject layoutInfoObj = new JsonObject();
            layoutInfoObj.addProperty("automatically", session.isAutomatically());
            layoutInfoObj.addProperty("mode", session.getLayoutMode().getMode());
            layoutInfoObj.add("linkedCoordinates", session.getCurrentPartInMcuLayout());
            result.add("layoutInfo", layoutInfoObj);
        }

		rpcNotificationService.sendResponse(participant.getParticipantPrivateId(), transactionId, result);
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

		for (Participant p : remainingParticipants) {
			if (!p.getParticipantPrivateId().equals(participant.getParticipantPrivateId())
					&& Objects.equals(StreamType.MAJOR, p.getStreamType())) {
				rpcNotificationService.sendNotification(p.getParticipantPrivateId(),
						ProtocolElements.PARTICIPANTLEFT_METHOD, params);
			}
		}

		if (transactionId != null) {
			// No response when the participant is forcibly evicted instead of voluntarily
			// leaving the session
			rpcNotificationService.sendResponse(participant.getParticipantPrivateId(), transactionId, params);
		}

		/*if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(participant.getParticipantPublicId())) {
			CDR.recordParticipantLeft(participant, sessionId, reason);
		}*/
	}

	public void onPublishMedia(Participant participant, String streamId, Long createdAt, String sessionId,
			MediaOptions mediaOptions, String sdpAnswer, Set<Participant> participants, Integer transactionId,
			OpenViduException error) {
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

		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_USER_PARAM, participant.getParticipantPublicId());
		params.addProperty(ProtocolElements.PARTICIPANTPUBLISHED_METADATA_PARAM, participant.getClientMetadata());
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

        for (Participant p : participants) {
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
