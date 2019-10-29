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
 */

package io.openvidu.client.internal;

import java.util.Arrays;
import java.util.List;

/**
 * This class defines constant values of client-server messages and their
 * parameters.
 *
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class ProtocolElements {

	// ---------------------------- CLIENT REQUESTS -----------------------

	public static final String SENDMESSAGE_ROOM_METHOD = "sendMessage";
	public static final String SENDMESSAGE_MESSAGE_PARAM = "message";

	public static final String LEAVEROOM_METHOD = "leaveRoom";
	public static final String LEAVEROOM_ROOM_ID_PARAM = "roomId";
	public static final String LEAVEROOM_SOURCE_ID_PARAM = "sourceId";
	public static final String LEAVEROOM_STREAM_TYPE_PARAM = "streamType";

	public static final String JOINROOM_METHOD = "joinRoom";
    public static final String JOINROOM_TYPE_PARAM = "joinType";
	public static final String JOINROOM_USER_PARAM = "user";
//	public static final String JOINROOM_TOKEN_PARAM = "token";
	public static final String JOINROOM_ROOM_PARAM = "session";
	public static final String JOINROOM_METADATA_PARAM = "metadata";
	public static final String JOINROOM_SECRET_PARAM = "secret";
	public static final String JOINROOM_PLATFORM_PARAM = "platform";
	public static final String JOINROOM_RECORDER_PARAM = "recorder";
	public static final String JOINROOM_ROLE_PARAM = "role";
	public static final String JOINROOM_STREAM_TYPE_PARAM = "streamType";
	public static final String JOINROOM_PASSWORD_PARAM = "password";

	public static final String JOINROOM_PEERID_PARAM = "id";
	public static final String JOINROOM_PEERCREATEDAT_PARAM = "createdAt";
	public static final String JOINROOM_PEERSTREAMS_PARAM = "streams";
	public static final String JOINROOM_PEERSTREAMID_PARAM = "id";
	public static final String JOINROOM_PEERSTREAMHASAUDIO_PARAM = "hasAudio";
	public static final String JOINROOM_PEERSTREAMHASVIDEO_PARAM = "hasVideo";
	public static final String JOINROOM_PEERSTREAMAUDIOACTIVE_PARAM = "audioActive";
	public static final String JOINROOM_PEERSTREAMVIDEOACTIVE_PARAM = "videoActive";
	public static final String JOINROOM_PEERSTREAMTYPEOFVIDEO_PARAM = "typeOfVideo";
	public static final String JOINROOM_PEERSTREAMFRAMERATE_PARAM = "frameRate";
	public static final String JOINROOM_PEERSTREAMVIDEODIMENSIONS_PARAM = "videoDimensions";
	public static final String JOINROOM_PEERSTREAMFILTER_PARAM = "filter";
    public static final String JOINROOM_PEERSHARESTATUS_PARAM = "shareStatus";
	public static final String JOINROOM_PEERSPEAKERSTATUS_PARAM = "speakerStatus";
	public static final String JOINROOM_PEERHANDSTATUS_PARAM = "handStatus";
	public static final String JOINROOM_PEERAPPSHOWNAME_PARAM = "appShowName";
	public static final String JOINROOM_PEERAPPSHOWDESC_PARAM = "appShowDesc";


	public static final String PUBLISHVIDEO_METHOD = "publishVideo";
	public static final String PUBLISHVIDEO_STREAM_TYPE_PARAM = "streamType";
	public static final String PUBLISHVIDEO_SDPOFFER_PARAM = "sdpOffer";
	public static final String PUBLISHVIDEO_DOLOOPBACK_PARAM = "doLoopback";
	public static final String PUBLISHVIDEO_SDPANSWER_PARAM = "sdpAnswer";
	public static final String PUBLISHVIDEO_STREAMID_PARAM = "id";
	public static final String PUBLISHVIDEO_CREATEDAT_PARAM = "createdAt";
	public static final String PUBLISHVIDEO_HASAUDIO_PARAM = "hasAudio";
	public static final String PUBLISHVIDEO_HASVIDEO_PARAM = "hasVideo";
	public static final String PUBLISHVIDEO_AUDIOACTIVE_PARAM = "audioActive";
	public static final String PUBLISHVIDEO_VIDEOACTIVE_PARAM = "videoActive";
	public static final String PUBLISHVIDEO_TYPEOFVIDEO_PARAM = "typeOfVideo";
	public static final String PUBLISHVIDEO_FRAMERATE_PARAM = "frameRate";
	public static final String PUBLISHVIDEO_VIDEODIMENSIONS_PARAM = "videoDimensions";
	public static final String PUBLISHVIDEO_KURENTOFILTER_PARAM = "filter";

	public static final String UNPUBLISHVIDEO_METHOD = "unpublishVideo";

	public static final String RECEIVEVIDEO_METHOD = "receiveVideoFrom";
	public static final String RECEIVEVIDEO_SDPOFFER_PARAM = "sdpOffer";
	public static final String RECEIVEVIDEO_SENDER_PARAM = "sender";
	public static final String RECEIVEVIDEO_SDPANSWER_PARAM = "sdpAnswer";

	public static final String UNSUBSCRIBEFROMVIDEO_METHOD = "unsubscribeFromVideo";
	public static final String UNSUBSCRIBEFROMVIDEO_SENDER_PARAM = "sender";

	public static final String ONICECANDIDATE_METHOD = "onIceCandidate";
	public static final String ONICECANDIDATE_EPNAME_PARAM = "endpointName";
	public static final String ONICECANDIDATE_CANDIDATE_PARAM = "candidate";
	public static final String ONICECANDIDATE_SDPMIDPARAM = "sdpMid";
	public static final String ONICECANDIDATE_SDPMLINEINDEX_PARAM = "sdpMLineIndex";

	public static final String CUSTOMREQUEST_METHOD = "customRequest";

	public static final String STREAMPROPERTYCHANGED_METHOD = "streamPropertyChanged";
	public static final String STREAMPROPERTYCHANGED_CONNECTIONID_PARAM = "connectionId";
	public static final String STREAMPROPERTYCHANGED_STREAMID_PARAM = "streamId";
	public static final String STREAMPROPERTYCHANGED_PROPERTY_PARAM = "property";
	public static final String STREAMPROPERTYCHANGED_NEWVALUE_PARAM = "newValue";
	public static final String STREAMPROPERTYCHANGED_REASON_PARAM = "reason";

	public static final String FORCEDISCONNECT_METHOD = "forceDisconnect";
	public static final String FORCEDISCONNECT_CONNECTIONID_PARAM = "connectionId";

	public static final String FORCEUNPUBLISH_METHOD = "forceUnpublish";
	public static final String FORCEUNPUBLISH_STREAMID_PARAM = "streamId";

	public static final String APPLYFILTER_METHOD = "applyFilter";
	public static final String FILTER_STREAMID_PARAM = "streamId";
	public static final String FILTER_TYPE_PARAM = "type";
	public static final String FILTER_OPTIONS_PARAM = "options";
	public static final String FILTER_METHOD_PARAM = "method";
	public static final String FILTER_PARAMS_PARAM = "params";
	public static final String EXECFILTERMETHOD_METHOD = "execFilterMethod";
	public static final String EXECFILTERMETHOD_LASTEXECMETHOD_PARAM = "lastExecMethod";
	public static final String REMOVEFILTER_METHOD = "removeFilter";
	public static final String ADDFILTEREVENTLISTENER_METHOD = "addFilterEventListener";
	public static final String REMOVEFILTEREVENTLISTENER_METHOD = "removeFilterEventListener";

	public static final String FILTEREVENTDISPATCHED_METHOD = "filterEventDispatched";
	public static final String FILTEREVENTLISTENER_CONNECTIONID_PARAM = "connectionId";
	public static final String FILTEREVENTLISTENER_STREAMID_PARAM = "streamId";
	public static final String FILTEREVENTLISTENER_FILTERTYPE_PARAM = "filterType";
	public static final String FILTEREVENTLISTENER_EVENTTYPE_PARAM = "eventType";
	public static final String FILTEREVENTLISTENER_DATA_PARAM = "data";

	// ---------------------------- add by chosongi at 2019-09-12
	public static final String ACCESS_IN_METHOD = "accessIn";
	public static final String ACCESS_IN_UUID_PARAM = "userId";
	public static final String ACCESS_IN_TOKEN_PARAM = "token";
    public static final String ACCESS_IN_SERIAL_NUMBER_PARAM = "serialNumber";
    public static final String ACCESS_IN_MAC_PARAM = "mac";
	public static final String ACCESS_IN_DEVICE_ID_PARAM = "deviceId";
	public static final String ACCESS_IN_DEVICE_NAME_PARAM = "deviceName";

	public static final String APPLY_FOR_LOGIN_METHOD = "applyForLogin";
	public static final String APPLY_FOR_LOGIN_TOKEN_PARAM = "token";
	public static final String APPLY_FOR_LOGIN_DEVICE_NAME_PARAM = "deviceName";
	public static final String APPLY_FOR_LOGIN_APPLICANT_SESSION_ID_PARAM = "applicantSessionId";

	public static final String CONFIRM_APPLY_FOR_LOGIN_METHOD = "confirmApplyForLogin";
	public static final String CONFIRM_APPLY_FOR_LOGIN_ACCEPT_PARAM = "accept";
	public static final String CONFIRM_APPLY_FOR_LOGIN_APPLICANT_SESSION_ID_PARAM = "applicantSessionId";

	public static final String RESULT_OF_LOGIN_APPLY_NOTIFY = "resultOfLoginApplyNotify";

	public static final String ACCESS_OUT_METHOD = "accessOut";

	public static final String CREATE_ROOM_METHOD = "createRoom";
	public static final String CREATE_ROOM_ID_PARAM = "roomId";
	public static final String CREATE_ROOM_PASSWORD_PARAM = "password";
	public static final String CREATE_ROOM_MIC_STATUS_PARAM = "micStatusInRoom";
	public static final String CREATE_ROOM_VIDEO_STATUS_PARAM = "videoStatusInRoom";
	public static final String CREATE_ROOM_SHARE_POWER_PARAM = "sharePowerInRoom";
	public static final String CREATE_ROOM_SUBJECT_PARAM = "subject";
	public static final String CREATE_ROOM_ROOM_CAPACITY_PARAM = "roomCapacity";
	public static final String CREATE_ROOM_DURATION_PARAM = "duration";
	public static final String CREATE_ROOM_USE_ID_PARAM = "useIdInRoom";
    public static final String CREATE_ROOM_ALLOW_PART_OPER_MIC_PARAM = "allowPartOperMic";
    public static final String CREATE_ROOM_ALLOW_PART_OPER_SHARE_PARAM = "allowPartOperShare";


	public static final String SHARE_SCREEN_METHOD = "shareScreen";
	public static final String SHARE_ROOM_ID_PARAM = "roomId";
	public static final String SHARE_SOURCE_ID_PARAM = "sourceId";

	public static final String STOP_SHARE_SCREEN_METHOD = "stopShareScreen";
	public static final String STOP_SHARE_ROOM_ID_PARAM = "roomId";
	public static final String STOP_SHARE_SOURCE_ID_PARAM = "sourceId";

	public static final String GET_PARTICIPANTS_METHOD = "getParticipants";
	public static final String GET_PARTICIPANTS_ROOM_ID_PARAM = "roomId";

	public static final String SET_AUDIO_STATUS_METHOD = "setAudioStatus";
	public static final String SET_AUDIO_ROOM_ID_PARAM = "roomId";
	public static final String SET_AUDIO_SOURCE_ID_PARAM = "sourceId";
	public static final String SET_AUDIO_TARGET_IDS_PARAM = "targetIds";
	public static final String SET_AUDIO_STATUS_PARAM = "status";

	public static final String SET_VIDEO_STATUS_METHOD = "setVideoStatus";
	public static final String SET_VIDEO_ROOM_ID_PARAM = "roomId";
	public static final String SET_VIDEO_SOURCE_ID_PARAM = "sourceId";
	public static final String SET_VIDEO_TARGET_IDS_PARAM = "targetIds";
	public static final String SET_VIDEO_STATUS_PARAM = "status";

	public static final String RAISE_HAND_METHOD = "raiseHand";
	public static final String RAISE_HAND_ROOM_ID_PARAM = "roomId";
	public static final String RAISE_HAND_SOURCE_ID_PARAM = "sourceId";
	public static final String RAISE_HAND_NUMBER_PARAM = "raiseHandNum";

	public static final String PUT_DOWN_HAND_METHOD = "putDownHand";
	public static final String PUT_DOWN_HAND_ROOM_ID_PARAM = "roomId";
	public static final String PUT_DOWN_HAND_SOURCE_ID_PARAM = "sourceId";
	public static final String PUT_DOWN_HAND_TARGET_ID_PARAM = "targetId";
	public static final String PUT_DOWN_HAND_RAISEHAND_NUMBER_PARAM = "raiseHandNum";

	public static final String SET_ROLL_CALL_METHOD = "setRollCall";
	public static final String SET_ROLL_CALL_ROOM_ID_PARAM = "roomId";
	public static final String SET_ROLL_CALL_SOURCE_ID_PARAM = "sourceId";
	public static final String SET_ROLL_CALL_TARGET_ID_PARAM = "targetId";
	public static final String SET_ROLL_CALL_RAISEHAND_NUMBER_PARAM = "raiseHandNum";

	public static final String END_ROLL_CALL_METHOD = "endRollCall";
	public static final String END_ROLL_CALL_ROOM_ID_PARAM = "roomId";
	public static final String END_ROLL_CALL_SOURCE_ID_PARAM = "sourceId";
	public static final String END_ROLL_CALL_TARGET_ID_PARAM = "targetId";
	public static final String END_ROLL_CALL_RAISEHAND_NUMBER_PARAM = "raiseHandNum";

	public static final String REPLACE_ROLL_CALL_METHOD = "replaceRollCall";
	public static final String REPLACE_ROLL_CALL_ROOM_ID_PARAM = "roomId";
	public static final String REPLACE_ROLL_CALL_SOURCE_ID_PARAM = "sourceId";
	public static final String REPLACE_ROLL_CALL_END_TARGET_ID_PARAM = "endTargetId";
	public static final String REPLACE_ROLL_CALL_START_TARGET_ID_PARAM = "startTargetId";
	public static final String REPLACE_ROLL_CALL_RAISEHAND_NUMBER_PARAM = "raiseHandNum";

	public static final String LOCK_SESSION_METHOD = "lockSession";
	public static final String LOCK_SESSION_ROOM_ID_PARAM = "roomId";
	public static final String LOCK_SESSION_SOURCE_ID_PARAM = "sourceId";

	public static final String UNLOCK_SESSION_METHOD = "unlockSession";
	public static final String UNLOCK_SESSION_ROOM_ID_PARAM = "roomId";
	public static final String UNLOCK_SESSION_SOURCE_ID_PARAM = "sourceId";

	public static final String CLOSE_ROOM_METHOD = "closeRoom";
	public static final String CLOSE_ROOM_ID_PARAM = "roomId";

	public static final String INVITE_PARTICIPANT_METHOD = "inviteParticipant";
	public static final String INVITE_PARTICIPANT_ID_PARAM = "roomId";
	public static final String INVITE_PARTICIPANT_SOURCE_ID_PARAM = "sourceId";
	public static final String INVITE_PARTICIPANT_TARGET_ID_PARAM = "targetId";

	public static final String GET_ORG_METHOD = "getOrgList";
	public static final String GET_ORG_NAME_PARAM = "organizationName";
	public static final String GET_ORG_ID_PARAM = "orgId";
	public static final String GET_ORG_LIST_PARAM = "organizationList";

	public static final String GET_USER_DEVICE_METHOD = "getUserDeviceList";
	public static final String GET_USER_DEVICE_ORGID_PARAM = "orgId";
	public static final String GET_USER_DEVICE_LIST_PARAM = "list";
	public static final String GET_USER_DEVICE_DEVICE_SERIAL_NUMBER_PARAM = "deviceSerialNumber";
	public static final String GET_USER_DEVICE_DEVICE_NAME_PARAM = "deviceName";
	public static final String GET_USER_DEVICE_STATUS_PARAM = "status";
	public static final String GET_USER_DEVICE_USER_NAME_PARAM = "username";
	public static final String GET_USER_DEVICE_USER_ID_PARAM = "userId";

	public static final String GET_DEVICE_INFO_METHOD = "getDeviceInfo";
	public static final String GET_DEVICE_ID_PARAM = "deviceId";
	public static final String GET_DEVICE_NANE_PARAM = "deviceName";

	public static final String UPDATE_DEVICE_INFO_METHOD = "updateDeviceInfo";
	public static final String UPDATE_DEVICE_ID_PARAM = "deviceId";
	public static final String UPDATE_DEVICE_NANE_PARAM = "deviceName";


	public static final String SET_AUDIO_SPEAKER_STATUS_METHOD = "setAudioSpeakerStatus";
	public static final String SET_AUDIO_SPEAKER_ID_PARAM = "roomId";
	public static final String SET_AUDIO_SPEAKER_SOURCE_ID_PARAM = "sourceId";
	public static final String SET_AUDIO_SPEAKER_TARGET_ID_PARAM = "targetId";
	public static final String SET_AUDIO_SPEAKER_STATUS_PARAM = "status";

	public static final String SET_SHARE_POWER_METHOD = "setSharePower";
	public static final String SET_SHARE_POWER_ID_PARAM = "roomId";
	public static final String SET_SHARE_POWER_SOURCE_ID_PARAM = "sourceId";
	public static final String SET_SHARE_POWER_TARGET_IDS_PARAM = "targetIds";
	public static final String SET_SHARE_POWER_STATUS_PARAM = "status";

	public static final String TRANSFER_MODERATOR_METHOD = "transferModerator";
	public static final String TRANSFER_MODERATOR_ID_PARAM = "roomId";
	public static final String TRANSFER_MODERATOR_SOURCE_ID_PARAM = "sourceId";
	public static final String TRANSFER_MODERATOR_TARGET_ID_PARAM = "targetId";

	public static final String GET_PRESET_INFO_METHOD = "getPresetInfo";
	public static final String GET_PRESET_INFO_ID_PARAM = "roomId";
	public static final String GET_PRESET_INFO_MIC_STATUS_PARAM = "micStatusInRoom";
	public static final String GET_PRESET_INFO_VIDEO_STATUS_PARAM = "videoStatusInRoom";
	public static final String GET_PRESET_INFO_SHARE_POWER_PARAM = "sharePowerInRoom";
	public static final String GET_PRESET_INFO_SUBJECT_PARAM = "subject";

	public static final String EVICTED_PARTICIPANT_BY_USER_ID_METHOD = "evictedParticipantByUserId";

	public static final String ROOM_COUNTDOWN_METHOD = "roomCountDown";
	public static final String ROOM_COUNTDOWN_INFO_ID_PARAM = "roomId";
	public static final String ROOM_COUNTDOWN_TIME_PARAM = "remainTime";

	public static final String ROOM_DELAY_METHOD = "roomDelay";
	public static final String ROOM_DELAY_ID_PARAM = "roomId";

	public static final String GET_NOT_FINISHED_ROOM_METHOD = "getNotFinishedRoom";
	public static final String GET_NOT_FINISHED_ROOM_ID_PARAM = "roomId";
	public static final String GET_NOT_FINISHED_ROOM_SUBJECT_PARAM = "subject";
	public static final String GET_NOT_FINISHED_ROOM_REMAINTIME_PARAM = "remainTime";
	public static final String GET_NOT_FINISHED_ROOM_ROLE_PARAM = "role";
	public static final String GET_NOT_FINISHED_ROOM_AUDIOACTIVE_PARAM = "audioActive";
	public static final String GET_NOT_FINISHED_ROOM_VIDEOACTIVE_PARAM = "videoActive";
	public static final String GET_NOT_FINISHED_ROOM_SHARESTATUS_PARAM = "shareStatus";
	public static final String GET_NOT_FINISHED_ROOM_SPEAKERSTATUS_PARAM = "speakerStatus";
	public static final String GET_NOT_FINISHED_ROOM_HANDSTATUS_PARAM = "handStatus";

	public static final String USER_BREAK_LINE_METHOD = "userBreakLine";
	public static final String USER_BREAK_LINE_ID_PARAM = "userId";

	public static final List<String> FILTERS = Arrays.asList(ACCESS_IN_METHOD, CONFIRM_APPLY_FOR_LOGIN_METHOD,
			GET_NOT_FINISHED_ROOM_METHOD, CREATE_ROOM_METHOD, JOINROOM_METHOD, ACCESS_OUT_METHOD,
			GET_DEVICE_INFO_METHOD, UPDATE_DEVICE_INFO_METHOD);

	// ---------------------------- SERVER RESPONSES & EVENTS -----------------

	public static final String PARTICIPANTJOINED_METHOD = "participantJoined";
	public static final String PARTICIPANTJOINED_USER_PARAM = "id";
	public static final String PARTICIPANTJOINED_CREATEDAT_PARAM = "createdAt";
	public static final String PARTICIPANTJOINED_METADATA_PARAM = "metadata";
    public static final String PARTICIPANTJOINED_MIC_STATUS_PARAM = "micStatusInRoom";
    public static final String PARTICIPANTJOINED_VIDEO_STATUS_PARAM = "videoStatusInRoom";
    public static final String PARTICIPANTJOINED_SHARE_POWER_PARAM = "sharePowerInRoom";
    public static final String PARTICIPANTJOINED_SUBJECT_PARAM = "subject";
	public static final String PARTICIPANTJOINED_ROOM_CAPACITY_PARAM = "roomCapacity";
    public static final String PARTICIPANTJOINED_APP_SHOWNAME_PARAM = "appShowName";
    public static final String PARTICIPANTJOINED_APP_SHOWDESC_PARAM = "appShowDesc";
	public static final String PARTICIPANTJOINED_ALLOW_PART_OPER_MIC_PARAM = "allowPartOperMic";
	public static final String PARTICIPANTJOINED_ALLOW_PART_OPER_SHARE_PARAM = "allowPartOperShare";


	public static final String PARTICIPANTLEFT_METHOD = "participantLeft";
	public static final String PARTICIPANTLEFT_NAME_PARAM = "connectionId";
	public static final String PARTICIPANTLEFT_REASON_PARAM = "reason";

	public static final String PARTICIPANTEVICTED_METHOD = "participantEvicted";
	public static final String PARTICIPANTEVICTED_CONNECTIONID_PARAM = "connectionId";
	public static final String PARTICIPANTEVICTED_REASON_PARAM = "reason";

	public static final String PARTICIPANTPUBLISHED_METHOD = "participantPublished";
	public static final String PARTICIPANTPUBLISHED_USER_PARAM = "id";
	public static final String PARTICIPANTPUBLISHED_STREAMS_PARAM = "streams";
	public static final String PARTICIPANTPUBLISHED_STREAMID_PARAM = "id";
	public static final String PARTICIPANTPUBLISHED_STREAMTYPE_PARAM = "streamType";
	public static final String PARTICIPANTPUBLISHED_CREATEDAT_PARAM = "createdAt";
	public static final String PARTICIPANTPUBLISHED_HASAUDIO_PARAM = "hasAudio";
	public static final String PARTICIPANTPUBLISHED_HASVIDEO_PARAM = "hasVideo";
	public static final String PARTICIPANTPUBLISHED_AUDIOACTIVE_PARAM = "audioActive";
	public static final String PARTICIPANTPUBLISHED_VIDEOACTIVE_PARAM = "videoActive";
	public static final String PARTICIPANTPUBLISHED_TYPEOFVIDEO_PARAM = "typeOfVideo";
	public static final String PARTICIPANTPUBLISHED_FRAMERATE_PARAM = "frameRate";
	public static final String PARTICIPANTPUBLISHED_VIDEODIMENSIONS_PARAM = "videoDimensions";
	public static final String PARTICIPANTPUBLISHED_FILTER_PARAM = "filter";

	public static final String PARTICIPANTUNPUBLISHED_METHOD = "participantUnpublished";
	public static final String PARTICIPANTUNPUBLISHED_NAME_PARAM = "connectionId";
	public static final String PARTICIPANTUNPUBLISHED_REASON_PARAM = "reason";

	public static final String PARTICIPANTSENDMESSAGE_METHOD = "sendMessage";
	public static final String PARTICIPANTSENDMESSAGE_DATA_PARAM = "data";
	public static final String PARTICIPANTSENDMESSAGE_FROM_PARAM = "from";
	public static final String PARTICIPANTSENDMESSAGE_TYPE_PARAM = "type";

	public static final String ROOMCLOSED_METHOD = "roomClosed";
	public static final String ROOMCLOSED_ROOM_PARAM = "sessionId";

	public static final String MEDIAERROR_METHOD = "mediaError";
	public static final String MEDIAERROR_ERROR_PARAM = "error";

	public static final String ICECANDIDATE_METHOD = "iceCandidate";
	public static final String ICECANDIDATE_SENDERCONNECTIONID_PARAM = "senderConnectionId";
	public static final String ICECANDIDATE_EPNAME_PARAM = "endpointName";
	public static final String ICECANDIDATE_CANDIDATE_PARAM = "candidate";
	public static final String ICECANDIDATE_SDPMID_PARAM = "sdpMid";
	public static final String ICECANDIDATE_SDPMLINEINDEX_PARAM = "sdpMLineIndex";

	public static final String RECORDINGSTARTED_METHOD = "recordingStarted";
	public static final String RECORDINGSTARTED_ID_PARAM = "id";
	public static final String RECORDINGSTARTED_NAME_PARAM = "name";
	public static final String RECORDINGSTOPPED_REASON_PARAM = "reason";

	public static final String RECORDINGSTOPPED_METHOD = "recordingStopped";
	public static final String RECORDINGSTOPPED_ID_PARAM = "id";

	public static final String CUSTOM_NOTIFICATION = "custonNotification";

	public static final String RECORDER_PARTICIPANT_PUBLICID = "RECORDER";
}
