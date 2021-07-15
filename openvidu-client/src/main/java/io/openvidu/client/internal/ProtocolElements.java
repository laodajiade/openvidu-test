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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public static final String JOINROOM_MODERATORPASSWORD_PARAM = "moderatorPassword";
    public static final String JOINROOM_ISRECONNECTED_PARAM = "isReconnected";
    public static final String JOINROOM_RUID_PARAM = "ruid";

    public static final String JOINROOM_PEERID_PARAM = "id";
    public static final String JOINROOM_PEERCREATEDAT_PARAM = "createdAt";
    public static final String JOINROOM_PEERSTREAMS_PARAM = "streams";
    public static final String JOINROOM_PEERSTREAMID_PARAM = "id";
    public static final String JOINROOM_PEERPUBLISHID_PARAM = "publishId";
    public static final String JOINROOM_PEERSTREAMHASAUDIO_PARAM = "hasAudio";
    public static final String JOINROOM_PEERSTREAMHASVIDEO_PARAM = "hasVideo";
    public static final String JOINROOM_PEERSTREAMMIXINCLUDED_PARAM = "mixIncluded";
    public static final String JOINROOM_PEERSTREAMAUDIOACTIVE_PARAM = "audioActive";
    public static final String JOINROOM_PEERSTREAMVIDEOACTIVE_PARAM = "videoActive";
    public static final String JOINROOM_PEERSTREAMTYPEOFVIDEO_PARAM = "typeOfVideo";
    public static final String JOINROOM_PEERSTREAMFRAMERATE_PARAM = "frameRate";
    public static final String JOINROOM_PEERSTREAMVIDEODIMENSIONS_PARAM = "videoDimensions";
    public static final String JOINROOM_PEERSTREAMFILTER_PARAM = "filter";
    public static final String JOINROOM_MICSTATUS_PARAM = "micStatus";
    public static final String JOINROOM_VIDEOSTATUS_PARAM = "videoStatus";
    public static final String JOINROOM_PEERSHARESTATUS_PARAM = "shareStatus";
    public static final String JOINROOM_PEERSPEAKERSTATUS_PARAM = "speakerStatus";
    public static final String JOINROOM_PEERHANDSTATUS_PARAM = "handStatus";
    public static final String JOINROOM_PEERAPPSHOWNAME_PARAM = "appShowName";
    public static final String JOINROOM_PEERAPPSHOWDESC_PARAM = "appShowDesc";
    public static final String JOINROOM_ABILITY_PARAM = "ability";
    public static final String JOINROOM_FUNCTIONALITY_PARAM = "functionality";
    public static final String JOINROOM_TERMINALCONFIG_PARAM = "terminalConfig";
    public static final String JOINROOM_PEERONLINESTATUS_PARAM = "onlineStatus";
    public static final String JOINROOM_MIXFLOWS_PARAM = "mixFlows";
    public static final String JOINROOM_MIXFLOWS_STREAMID_PARAM = "streamId";
    public static final String JOINROOM_MIXFLOWS_STREAMMODE_PARAM = "streamMode";
    public static final String JOINROOM_NICKNAME_PARAM = "nickname";


    public static final String PUBLISHVIDEO_METHOD = "publishVideo";
    public static final String PUBLISHVIDEO_STREAM_TYPE_PARAM = "streamType";
    public static final String PUBLISHVIDEO_SDPOFFER_PARAM = "sdpOffer";
    public static final String PUBLISHVIDEO_DOLOOPBACK_PARAM = "doLoopback";
    public static final String PUBLISHVIDEO_SDPANSWER_PARAM = "sdpAnswer";
    //todo 2.0 删除
    @Deprecated
    public static final String PUBLISHVIDEO_STREAMID_PARAM = "id";
    public static final String PUBLISHVIDEO_PUBLISHID_PARAM = "publishId";
    public static final String PUBLISHVIDEO_CREATEDAT_PARAM = "createdAt";
    public static final String PUBLISHVIDEO_HASAUDIO_PARAM = "hasAudio";
    public static final String PUBLISHVIDEO_HASVIDEO_PARAM = "hasVideo";
    public static final String PUBLISHVIDEO_AUDIOACTIVE_PARAM = "audioActive";
    public static final String PUBLISHVIDEO_VIDEOACTIVE_PARAM = "videoActive";
    public static final String PUBLISHVIDEO_TYPEOFVIDEO_PARAM = "typeOfVideo";
    public static final String PUBLISHVIDEO_FRAMERATE_PARAM = "frameRate";
    public static final String PUBLISHVIDEO_VIDEODIMENSIONS_PARAM = "videoDimensions";
    public static final String PUBLISHVIDEO_KURENTOFILTER_PARAM = "filter";
    public static final String PUBLISHVIDEO_HAND_STATUS_PARAM = "handStatus";
    public static final String PUBLISHVIDEO_VIDEOSTATUS_PARAM = "videoStatus";
    public static final String PUBLISHVIDEO_MICSTATUS_PARAM = "micStatus";

    public static final String UNPUBLISHVIDEO_METHOD = "unpublishVideo";

    public static final String SUBSCRIBE_VIDEO_METHOD = "subscribeVideo";

    public static final String UNSUBSCRIBE_VIDEO_METHOD = "unsubscribeVideo";

    public static final String ONICECANDIDATE_METHOD = "onIceCandidate";
    public static final String ONICECANDIDATE_EPNAME_PARAM = "endpointName";
    public static final String ONICECANDIDATE_CANDIDATE_PARAM = "candidate";
    public static final String ONICECANDIDATE_SDPMIDPARAM = "sdpMid";
    public static final String ONICECANDIDATE_SDPMLINEINDEX_PARAM = "sdpMLineIndex";
    public static final String ONICECANDIDATE_STREAMMODE_PARAM = "streamMode";

    public static final String CUSTOMREQUEST_METHOD = "customRequest";

    public static final String STREAMPROPERTYCHANGED_METHOD = "streamPropertyChanged";
    public static final String STREAMPROPERTYCHANGED_CONNECTIONID_PARAM = "connectionId";
    public static final String STREAMPROPERTYCHANGED_STREAMID_PARAM = "streamId";
    public static final String STREAMPROPERTYCHANGED_PROPERTY_PARAM = "property";
    public static final String STREAMPROPERTYCHANGED_NEWVALUE_PARAM = "newValue";
    public static final String STREAMPROPERTYCHANGED_REASON_PARAM = "reason";

    public static final String FORCEDISCONNECT_METHOD = "forceDisconnect";
    public static final String FORCEDISCONNECT_CONNECTIONID_PARAM = "connectionId";
    public static final String FORCEDISCONNECT_UUID_PARAM = "uuid";

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
    public static final String ACCESS_IN_UUID_PARAM = "account";
    public static final String ACCESS_IN_TOKEN_PARAM = "token";
    public static final String ACCESS_IN_UDID_PARAM = "udid";
    public static final String ACCESS_IN_SERIAL_NUMBER_PARAM = "serialNumber";
    public static final String ACCESS_IN_MAC_PARAM = "mac";
    public static final String ACCESS_IN_FORCE_LOGIN_PARAM = "forceLogin";
    public static final String ACCESS_IN_ACCESSTYPE_PARAM = "accessType";
    public static final String ACCESS_IN_DEVICE_ID_PARAM = "deviceId";
    public static final String ACCESS_IN_DEVICE_NAME_PARAM = "deviceName";
    public static final String ACCESS_IN_USER_NAME_PARAM = "userName";
    public static final String ACCESS_IN_DEVICEMODEL_PARAM = "deviceModel";
    public static final String ACCESS_IN_DEVICEVERSION_PARAM = "deviceVersion";
    public static final String ACCESS_IN_ABILITY_PARAM = "ability";
    public static final String ACCESS_IN_FUNCTIONALITY_PARAM = "functionality";
    public static final String ACCESS_IN_TERMINALCONFIG_PARAM = "terminalConfig";
    public static final String ACCESS_IN_CLIENTTIMESTAMP_PARAM = "clientTimestamp";
    public static final String ACCESS_IN_SERVERTIMESTAMP_PARAM = "serverTimestamp";
    public static final String ACCESS_IN_USERTYPE_PARAM = "userType";
    public static final String ACCESS_IN_CLIENT_TYPE = "type";
    public static final String ACCESS_IN_REGISTRATION_ID_TYPE = "registrationId";
    public static final String ACCESS_IN_NICKNAME_PARAM = "nickName";

    public static final String RECONNECTPART_STOP_PUBLISH_SHARING_METHOD = "reconnectPartStopPublishSharing";
    public static final String RECONNECTPART_STOP_PUBLISH_SHARING_CONNECTIONID_PARAM = "connectionId";

    public static final String REMOTE_LOGIN_NOTIFY_METHOD = "remoteLoginNotify";

    public static final String APPLY_FOR_LOGIN_METHOD = "applyForLogin";
    public static final String APPLY_FOR_LOGIN_TOKEN_PARAM = "token";
    public static final String APPLY_FOR_LOGIN_DEVICE_NAME_PARAM = "deviceName";
    public static final String APPLY_FOR_LOGIN_APPLICANT_SESSION_ID_PARAM = "applicantSessionId";

    public static final String CONFIRM_APPLY_FOR_LOGIN_METHOD = "confirmApplyForLogin";
    public static final String CONFIRM_APPLY_FOR_LOGIN_ACCEPT_PARAM = "accept";
    public static final String CONFIRM_APPLY_FOR_LOGIN_APPLICANT_SESSION_ID_PARAM = "applicantSessionId";

    public static final String RESULT_OF_LOGIN_APPLY_NOTIFY = "resultOfLoginApplyNotify";

    public static final String GETROOMLAYOUT_METHOD = "getRoomLayout";
    public static final String GETROOMLAYOUT_ROOM_ID_PARAM = "roomId";
    public static final String GETROOMLAYOUT_MODE_PARAM = "mode";
    public static final String GETROOMLAYOUT_TYPE_PARAM = "type";
    public static final String GETROOMLAYOUT_LAYOUT_PARAM = "layout";
    public static final String GETROOMLAYOUT_MODERATOR_INDEX_PARAM_PARAM = "moderatorIndex";

    public static final String BROADCASTMAJORLAYOUT_METHOD = "broadcastMajorLayout";
    public static final String BROADCASTMAJORLAYOUT_MODE_PARAM = "mode";
    public static final String BROADCASTMAJORLAYOUT_TYPE_PARAM = "type";
    public static final String BROADCASTMAJORLAYOUT_LAYOUT_PARAM = "layout";

    public static final String MAJORLAYOUTNOTIFY_METHOD = "majorLayoutNotify";
    public static final String MAJORLAYOUTNOTIFY_MODE_PARAM = "mode";
    public static final String MAJORLAYOUTNOTIFY_TYPE_PARAM = "type";
    public static final String MAJORLAYOUTNOTIFY_LAYOUT_PARAM = "layout";


    public static final String SETCONFERENCELAYOUT_MODE_METHOD = "setConferenceLayout";
    public static final String SETCONFERENCELAYOUT_AUTOMATICAlly_PARAM = "automatically";
    public static final String SETCONFERENCELAYOUT_MODE_PARAM = "mode";
    public static final String SETCONFERENCELAYOUT_REPLACEINFO_PARAM = "replaceInfo";
    public static final String SETCONFERENCELAYOUT_TARGET_PARAM = "target";
    public static final String SETCONFERENCELAYOUT_REPLACEMENT_PARAM = "replacement";
    public static final String SETCONFERENCELAYOUT_SHAREINCLUDE_PARAM = "shareInclude";

    public static final String CONFERENCELAYOUTCHANGED_NOTIFY = "conferenceLayoutChanged";
    public static final String CONFERENCELAYOUTCHANGED_NOTIFY_MODE_PARAM = "mode";
    public static final String CONFERENCELAYOUTCHANGED_PARTLINKEDLIST_PARAM = "partLinkedList";
    public static final String CONFERENCELAYOUTCHANGED_AUTOMATICALLY_PARAM = "automatically";
    public static final String CONFERENCELAYOUTCHANGED_PARTLINKEDLISTSHAREINCLUDE_PARAM = "partLinkedListShareInclude";


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
    public static final String CREATE_ROOM_QUIET_STATUS_PARAM = "quietStatusInRoom";
    public static final String CREATE_ROOM_CONFERENCE_MODE_PARAM = "conferenceMode";
    public static final String CREATE_ROOM_RUID_PARAM = "ruid";
    public static final String CREATE_ROOM_ID_TYPE_PARAM = "roomIdType";

    public static final String SHARE_SCREEN_METHOD = "shareScreen";
    public static final String SHARE_ROOM_ID_PARAM = "roomId";
    public static final String SHARE_SOURCE_ID_PARAM = "sourceId";

    public static final String STOP_SHARE_SCREEN_METHOD = "stopShareScreen";
    public static final String STOP_SHARE_ROOM_ID_PARAM = "roomId";
    public static final String STOP_SHARE_SOURCE_ID_PARAM = "sourceId";

    public static final String GET_PARTICIPANTS_METHOD = "getParticipants";
    public static final String GET_PARTICIPANTS_ROOM_ID_PARAM = "roomId";
    public static final String GET_PARTICIPANTS_TARGETID_PARAM = "targetId";

    public static final String SET_AUDIO_STATUS_METHOD = "setAudioStatus";
    public static final String SET_AUDIO_ROOM_ID_PARAM = "roomId";
    public static final String SET_AUDIO_SOURCE_ID_PARAM = "sourceId";
    public static final String SET_AUDIO_TARGET_IDS_PARAM = "targetIds";
    public static final String SET_AUDIO_SOURCE_PARAM = "source";
    public static final String SET_AUDIO_TARGETS_PARAM = "targets";
    public static final String SET_AUDIO_STATUS_PARAM = "status";

    public static final String SET_VIDEO_STATUS_METHOD = "setVideoStatus";
    public static final String SET_VIDEO_ROOM_ID_PARAM = "roomId";
    public static final String SET_VIDEO_SOURCE_ID_PARAM = "sourceId";
    public static final String SET_VIDEO_TARGET_IDS_PARAM = "targetIds";
    public static final String SET_VIDEO_SOURCE_PARAM = "source";
    public static final String SET_VIDEO_TARGETS_PARAM = "targets";
    public static final String SET_VIDEO_STATUS_PARAM = "status";

    public static final String RAISE_HAND_METHOD = "raiseHand";
    public static final String RAISE_HAND_ROOM_ID_PARAM = "roomId";
    public static final String RAISE_HAND_SOURCE_ID_PARAM = "sourceId";

    public static final String PUT_DOWN_HAND_METHOD = "putDownHand";
    public static final String PUT_DOWN_HAND_ROOM_ID_PARAM = "roomId";
    public static final String PUT_DOWN_HAND_SOURCE_ID_PARAM = "sourceId";
    public static final String PUT_DOWN_HAND_TARGET_ID_PARAM = "targetId";


    public static final String SET_ROLL_CALL_METHOD = "setRollCall";
    public static final String SET_ROLL_CALL_NOTIFY_METHOD = "setRollCallNotify";
    public static final String SET_ROLL_CALL_ROOM_ID_PARAM = "roomId";
    public static final String SET_ROLL_CALL_SOURCE_ID_PARAM = "sourceId";
    public static final String SET_ROLL_CALL_TARGET_ID_PARAM = "targetId";

    public static final String END_ROLL_CALL_METHOD = "endRollCall";
    public static final String END_ROLL_CALL_NOTIFY_METHOD = "endRollCallNotify";
    public static final String END_ROLL_CALL_ROOM_ID_PARAM = "roomId";
    public static final String END_ROLL_CALL_SOURCE_ID_PARAM = "sourceId";
    public static final String END_ROLL_CALL_ORIGINATOR_ID_PARAM = "originator";
    public static final String END_ROLL_CALL_TARGET_ID_PARAM = "targetId";

    public static final String REPLACE_ROLL_CALL_METHOD = "replaceRollCall";
    public static final String REPLACE_ROLL_CALL_NOTIFY_METHOD = "replaceRollCallNotify";
    public static final String REPLACE_ROLL_CALL_ROOM_ID_PARAM = "roomId";
    public static final String REPLACE_ROLL_CALL_SOURCE_ID_PARAM = "sourceId";
    public static final String REPLACE_ROLL_CALL_END_TARGET_ID_PARAM = "endTargetId";
    public static final String REPLACE_ROLL_CALL_START_TARGET_ID_PARAM = "startTargetId";

    public static final String CHANGE_PART_ROLE_METHOD = "changePartRole";
    public static final String CHANGE_PART_ROLE_CHANGED_TO_ON_THE_WALL_PARAM = "toOnTheWall";
    public static final String CHANGE_PART_ROLE_CHANGED_TO_DOWN_THE_WALL_PARAM = "toDownTheWall";
    public static final String CHANGE_PART_ROLE_CHANGED_CONNECTION_ID_PARAM = "connectionId";
    public static final String CHANGE_PART_ROLE_ORIGINAL_ROLE_PARAM = "originalRole";
    public static final String CHANGE_PART_ROLE_PRESENT_ROLE_PARAM = "presentRole";


    public static final String NOTIFY_PART_ROLE_CHANGED_METHOD = "notifyPartRoleChanged";
    public static final String PART_ORDER_OR_ROLE_CHANGE_NOTIFY_METHOD = "partOrderOrRoleChangeNotify";



    // 2.0 废弃 public static final String NOTIFY_PART_ROLE_CHANGED_CONNECTION_ID_PARAM = "connectionId";
    public static final String NOTIFY_PART_ROLE_CHANGED_UUID_PARAM = "uuid";
    public static final String NOTIFY_PART_ROLE_CHANGED_ORIGINAL_ROLE_PARAM = "originalRole";
    public static final String NOTIFY_PART_ROLE_CHANGED_PRESENT_ROLE_PARAM = "presentRole";
    public static final String NOTIFY_PART_ROLE_CHANGED_HAND_STATUS_PARAM = "handStatus";

    public static final String LOCK_SESSION_METHOD = "lockSession";
    public static final String LOCK_SESSION_ROOM_ID_PARAM = "roomId";
    public static final String LOCK_SESSION_SOURCE_ID_PARAM = "sourceId";

    public static final String UNLOCK_SESSION_METHOD = "unlockSession";
    public static final String UNLOCK_SESSION_ROOM_ID_PARAM = "roomId";
    public static final String UNLOCK_SESSION_SOURCE_ID_PARAM = "sourceId";

    public static final String CLOSE_ROOM_METHOD = "closeRoom";
    public static final String CLOSE_ROOM_ID_PARAM = "roomId";

    public static final String CLOSE_ROOM_NOTIFY_METHOD = "closeRoomNotify";

    public static final String INVITE_PARTICIPANT_METHOD = "inviteParticipant";
    public static final String INVITE_PARTICIPANT_ID_PARAM = "roomId";
    public static final String INVITE_PARTICIPANT_SOURCE_ID_PARAM = "sourceId";
    public static final String INVITE_PARTICIPANT_TARGET_ID_PARAM = "targetId";
    public static final String INVITE_PARTICIPANT_USERNAME_PARAM = "username";
    public static final String INVITE_PARTICIPANT_DEVICE_NAME_PARAM = "deviceName";
    public static final String INVITE_PARTICIPANT_EXPIRETIME_PARAM = "expireTime";
    public static final String INVITE_PARTICIPANT_SUBJECT_PARAM = "subject";
    public static final String INVITE_PARTICIPANT_USERICON_PARAM = "userIcon";
    public static final String INVITE_PARTICIPANT_AUTO_INVITE_PARAM = "autoInvite";
    public static final String INVITE_PARTICIPANT_PASSWORD_PARAM = "password";

    public static final String REFUSE_INVITE_METHOD = "refuseInvite";
    public static final String REFUSE_INVITE_ID_PARAM = "roomId";
    public static final String REFUSE_INVITE_SOURCE_ID_PARAM = "sourceId";
    public static final String REFUSE_INVITE_TARGET_ID_PARAM = "targetId";
    public static final String REFUSE_INVITE_REASON_PARAM = "reason";

    public static final String CANCELINVITE_METHOD = "canceInvite";
    public static final String CANCELINVITE_ROOMID_PARAM = "roomId";
    public static final String CANCELINVITE_SOURCEID_PARAM = "sourceId";
    public static final String CANCELINVITE_TARGETIDS_PARAM = "targetIds";

    public static final String CANCELINVITE_NOTIFY_METHOD = "canceInviteNotify";

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
    public static final String GET_ACCOUNT_PARAM = "account";


    public static final String GET_DEVICE_INFO_METHOD = "getDeviceInfo";
    public static final String GET_DEVICE_STATUS_PARAM = "status";
    public static final String GET_DEVICE_NANE_PARAM = "deviceName";
    public static final String GET_DEVICE_DEVCURVERSION_PARAM = "devCurVersion";
    public static final String GET_DEVICE_VERSION_PARAM = "version";
    public static final String GET_DEVICE_DESC_PARAM = "desc";

    public static final String GET_DEVICE_VERUPGRADEAVAILABLE_PARAM = "verUpgradeAvailable";

    public static final String UPDATE_DEVICE_INFO_METHOD = "updateDeviceInfo";
    public static final String UPDATE_DEVICE_ID_PARAM = "deviceId";
    public static final String UPDATE_DEVICE_NANE_PARAM = "deviceName";


    public static final String SET_AUDIO_SPEAKER_STATUS_METHOD = "setAudioSpeakerStatus";
    public static final String SET_AUDIO_SPEAKER_ID_PARAM = "roomId";
    public static final String SET_AUDIO_SPEAKER_SOURCE_ID_PARAM = "sourceId";
    public static final String SET_AUDIO_SPEAKER_TARGET_ID_PARAM = "targetIds";
    public static final String SET_AUDIO_SPEAKER_STATUS_PARAM = "status";
    public static final String SET_AUDIO_SPEAKER_USERNAME_PARAM = "username";

    public static final String SET_SHARE_POWER_METHOD = "setSharePower";
    public static final String SET_SHARE_POWER_ID_PARAM = "roomId";
    public static final String SET_SHARE_POWER_SOURCE_ID_PARAM = "sourceId";
    public static final String SET_SHARE_POWER_TARGET_IDS_PARAM = "targetIds";
    public static final String SET_SHARE_POWER_STATUS_PARAM = "status";

    public static final String SHARING_CONTROL_METHOD = "sharingControl";
    public static final String SHARING_CONTROL_ROOMID_PARAM = "roomId";
    public static final String SHARING_CONTROL_SOURCEID_PARAM = "sourceId";
    public static final String SHARING_CONTROL_TARGETID_PARAM = "targetId";
    public static final String SHARING_CONTROL_OPERATION_PARAM = "operation";
    public static final String SHARING_CONTROL_MODE_PARAM = "mode";

    public static final String SHARING_CONTROL_NOTIFY = "sharingControlNotify";

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
    public static final String GET_NOT_FINISHED_ROOM_PASSWORD_PARAM = "password";
    public static final String GET_NOT_FINISHED_ROOM_ROLE_PARAM = "role";
    public static final String GET_NOT_FINISHED_ROOM_AUDIOACTIVE_PARAM = "micStatus";
    public static final String GET_NOT_FINISHED_ROOM_VIDEOACTIVE_PARAM = "videoStatus";
    public static final String GET_NOT_FINISHED_ROOM_SHARESTATUS_PARAM = "shareStatus";
    public static final String GET_NOT_FINISHED_ROOM_SPEAKERSTATUS_PARAM = "speakerStatus";
    public static final String GET_NOT_FINISHED_ROOM_HANDSTATUS_PARAM = "handStatus";
    public static final String GET_NOT_FINISHED_ROOM_MODERATOR_UUID_PARAM = "moderatorUuid";

    public static final String USER_BREAK_LINE_METHOD = "userBreakLine";
    public static final String USER_BREAK_LINE_CONNECTION_ID_PARAM = "connectionId";

    public static final String GET_SUB_DEVORUSER_METHOD = "getSubDevOrUser";
    public static final String GET_SUB_DEVORUSER_ORG_ID = "orgId";
    public static final String GET_SUB_DEVORUSER_SERIAL_NUMBER_PARAM = "serialNumber";
    public static final String GET_SUB_DEVORUSER_DEVICE_NAME_PARAM = "deviceName";
    public static final String GET_SUB_DEVORUSER_USER_NAME_PARAM = "username";

    public static final String GET_SUB_DEVORUSER_ACCOUNT_PARAM = "account";
    public static final String GET_SUB_DEVORUSER_USERID_PARAM = "userId";
    public static final String GET_SUB_DEVORUSER_DEVICESTATUS_PARAM = "deviceStatus";

    public static final String GET_SUB_DEVORUSER_DEVICE_LIST_PARAM = "deviceList";
    public static final String GET_SUB_DEVORUSER_ACCOUNT_LIST_PARAM = "accountList";

    public static final String START_PTZ_CONTROL_METHOD = "startPtzControl";
    public static final String START_PTZ_CONTROL_SERIAL_NUMBER_PARM = "serialNumber";
    public static final String START_PTZ_CONTROL_OPERATE_CODE_PARM = "operateCode";
    public static final String START_PTZ_CONTROL_MAX_DURATION_PARM = "maxDuration";
    public static final String START_PTZ_CONTROL_CONNECTIONID_PARM = "connectionId";

    public static final String START_PTZ_CONTROL_STARTPTZCONTROLNOTIFY_METHOD = "startPtzControlNotify";

    public static final String STOP_PTZ_CONTROL_METHOD = "stopPtzControl";
    public static final String STOP_PTZ_CONTROL_SERIAL_NUMBER_PARM = "serialNumber";
    public static final String STOP_PTZ_CONTROL_CONNECTIONID_PARM = "connectionId";

    public static final String STOP_PTZ_CONTROL_STOPPTZCONTROLNOTIFY_METHOD = "stopPtzControlNotify";

    public static final String SWAP_PART_WINDOW_METHOD = "swapPartWindow";
    public static final String SWAP_PART_WINDOW_SOURCE_CONNECTION_ID_PAPM = "sourceConnectionId";
    public static final String SWAP_PART_WINDOW_TARGET_CONNECTION_ID_PAPM = "targetConnectionId";

    public static final String GET_DEPARTMENT_TREE_METHOD = "getDepartmentTree";
    public static final String GET_DEPARTMENT_TREE_CORP_NAME_PAPM = "corpName";
    public static final String GET_DEPARTMENT_TREE_ORG_ID_PAPM = "orgId";
    public static final String GET_DEPARTMENT_TREE_ORGANIZATION_NAME_PAPM = "organizationName";
    public static final String GET_DEPARTMENT_TREE_ORGANIZATION_LIST_PAPM = "organizationList";
    public static final String GET_DEPARTMENT_TREE_PARENT_ID_PAPM = "parentId";

    public static final String COMMAND_UOGRADE_METHOD = "commandUpgrade";
    public static final String COMMAND_UOGRADE_SERIALNUMBER_PAPM = "serialNumber";
    public static final String COMMAND_UOGRADE_VERSION_PAPM = "version";
    public static final String COMMAND_UOGRADE_DOWNLOADURL_PAPM = "downloadUrl";

    public static final String UPGRADE_NOTIFY_METHOD = "upgradeNotify";
    public static final String CORP_INFO_MODIFIED_NOTIFY_METHOD = "corpInfoModifiedNotify";

    public static final String ADJUST_RESOLUTION_METHOD = "adjustResolution";
    public static final String ADJUST_RESOLUTION_CONNECTIONID_PAPM = "connectionId";
    public static final String ADJUST_RESOLUTION_SERIALNUMBER_PAPM = "serialNumber";
    public static final String ADJUST_RESOLUTION_RESOLUTION_PAPM = "resolution";

    public static final String GET_UNFINISHED_MEETINGS_METHOD = "getUnfinishedMeetings";
    public static final String GET_UNFINISHED_MEETINGS_ISADMIN_PAPM = "isAdmin";
    public static final String GET_UNFINISHED_MEETINGS_ROOMID_PAPM = "roomId";
    public static final String GET_UNFINISHED_MEETINGS_SUBJECT_PAPM = "subject";
    public static final String GET_UNFINISHED_MEETINGS_PASSWORD_PAPM = "password";
    public static final String GET_UNFINISHED_MEETINGS_ACCOUNT_PAPM = "account";
    public static final String GET_UNFINISHED_MEETINGS_ROOMCREAEAT_PAPM = "roomCreateAt";

    public static final String ADJUST_RESOLUTION_NOTIFY_METHOD = "adjustResolutionNotify";

    public static final String GET_GROUP_LIST_METHOD = "getGroupList";
    public static final String GET_GROUP_LIST_USERID_PARAM = "userId";
    public static final String GET_GROUP_LIST_GROUPLIST_PARAM = "list";
    public static final String GET_GROUP_LIST_GROUPID_PARAM = "groupId";
    public static final String GET_GROUP_LIST_GROUPNAME_PARAM = "groupName";
    public static final String GET_GROUP_LIST_NUMOFPEOPLE_PARAM = "numOfPeople";

    public static final String GET_GROUP_INFO_METHOD = "getGroupMember";
    public static final String GET_GROUP_INFO_TOTAL = "total";
    public static final String GET_GROUP_INFO_ACCOUNT_LIST = "list";
    public static final String GET_GROUP_INFO_GROUPID_PARAM = "groupId";
    public static final String GET_GROUP_INFO_GROUPINFO_PARAM = "groupInfo";
    public static final String GET_GROUP_INFO_DEVIDE_NAME_PARAM = "deviceName";
    public static final String GET_GROUP_INFO_SERIAL_NUMBER_PARAM = "serialNumber";
    public static final String GET_GROUP_INFO_ACCOUNT_PARAM = "account";
    public static final String GET_GROUP_INFO_USERID_PARAM = "userId";
    public static final String GET_GROUP_INFO_DEVICE_STATUS_PARAM = "deviceStatus";

    public static final String MODIFY_PASSWORD_METHOD = "modifyPassword";
    public static final String ORIGINAL_PASSWORD_PARAM = "originalPassword";
    public static final String NEW_PASSWORD_PARAM = "newPassword";


    public static final String CREATE_APPOINTMENT_ROOM_METHOD = "createAppointmentRoom";
    public static final String UPDATE_APPOINTMENT_ROOM_METHOD = "updateAppointmentRoom";
    public static final String CANCEL_APPOINTMENT_ROOM_METHOD = "cancelAppointmentRoom";
    public static final String GET_APPOINTMENT_ROOM_DETAILS_METHOD = "getAppointmentRoomDetails";
    public static final String CHANGE_MEETING_ROOM_METHOD = "changeMeetingRoom";

    public static final String DELETE_CONFERENCE_HISTORY_METHOD = "deleteConferenceHistory";

    public static final String SEND_MSG_METHOD = "sendMsg";
    public static final String NOTIFY_SEND_MSG_METHOD = "notifySendMsg";
    public static final String GET_MSG_HISTORY_METHOD = "getMsgHistory";
    public static final String SET_IM_MODE_METHOD = "setImMode";
    public static final String NOTIFY_SET_IM_MODE_METHOD = "notifySetImMode";

    // ---------------------------- SERVER RESPONSES & EVENTS -----------------

    public static final String PARTICIPANTJOINED_METHOD = "participantJoined";
    public static final String PARTICIPANTJOINED_USER_PARAM = "id";
    public static final String PARTICIPANTJOINED_CREATEDAT_PARAM = "createdAt";
    public static final String PARTICIPANTJOINED_METADATA_PARAM = "metadata";
    public static final String PARTICIPANTJOINED_MIC_STATUS_PARAM = "micStatusInRoom";
    public static final String PARTICIPANTJOINED_VIDEO_STATUS_PARAM = "videoStatusInRoom";
    public static final String PARTICIPANTJOINED_SHARE_POWER_PARAM = "sharePowerInRoom";
    public static final String PARTICIPANTJOINED_SUBJECT_PARAM = "subject";
    public static final String PARTICIPANTJOINED_CONFERENCE_MODE_PARAM = "conferenceMode";
    public static final String PARTICIPANTJOINED_ROOM_CAPACITY_PARAM = "roomCapacity";
    public static final String PARTICIPANTJOINED_ROOM_CREATE_AT_PARAM = "roomCreateAt";
    public static final String PARTICIPANTJOINED_APP_SHOWNAME_PARAM = "appShowName";
    public static final String PARTICIPANTJOINED_APP_SHOWDESC_PARAM = "appShowDesc";
    public static final String PARTICIPANTJOINED_ABILITY_PARAM = "ability";
    public static final String PARTICIPANTJOINED_FUNCTIONALITY_PARAM = "functionality";
    public static final String PARTICIPANTJOINED_TERMINALCONFIG_PARAM = "terminalConfig";
    public static final String PARTICIPANTJOINED_ALLOW_PART_OPER_MIC_PARAM = "allowPartOperMic";
    public static final String PARTICIPANTJOINED_ALLOW_PART_OPER_SHARE_PARAM = "allowPartOperShare";
    public static final String PARTICIPANTJOINED_IS_RECONNECTED_PARAM = "isReconnected";
    public static final String PARTICIPANTJOINED_STREAM_TYPE_PARAM = "streamType";

    public static final String PARTICIPANTLEFT_METHOD = "participantLeft";
    public static final String PARTICIPANTLEFT_NAME_PARAM = "connectionId";
    public static final String PARTICIPANTLEFT_UUID_PARAM = "uuid";
    public static final String PARTICIPANTLEFT_REASON_PARAM = "reason";

    public static final String PARTICIPANTEVICTED_METHOD = "participantEvicted";
    public static final String PARTICIPANTEVICTED_CONNECTIONID_PARAM = "connectionId";
    public static final String PARTICIPANTEVICTED_REASON_PARAM = "reason";

    public static final String PARTICIPANTPUBLISHED_METHOD = "participantPublished";
    public static final String PARTICIPANTPUBLISHED_UUID_PARAM = "uuid";
    public static final String PARTICIPANTPUBLISHED_NAME_PARAM = "connectionId";
    public static final String PARTICIPANTPUBLISHED_METADATA_PARAM = "metadata";
    public static final String PARTICIPANTPUBLISHED_STREAMS_PARAM = "streams";
    public static final String PARTICIPANTPUBLISHED_STREAMID_PARAM = "publishId";
    public static final String PARTICIPANTPUBLISHED_STREAMTYPE_PARAM = "streamType";
    public static final String PARTICIPANTPUBLISHED_CREATEDAT_PARAM = "createdAt";
    public static final String PARTICIPANTPUBLISHED_HASAUDIO_PARAM = "hasAudio";
    public static final String PARTICIPANTPUBLISHED_HASVIDEO_PARAM = "hasVideo";
    public static final String PARTICIPANTPUBLISHED_MIXINCLUDED_PARAM = "mixIncluded";
    public static final String PARTICIPANTPUBLISHED_AUDIOACTIVE_PARAM = "audioActive";
    public static final String PARTICIPANTPUBLISHED_VIDEOACTIVE_PARAM = "videoActive";
    public static final String PARTICIPANTPUBLISHED_TYPEOFVIDEO_PARAM = "typeOfVideo";
    public static final String PARTICIPANTPUBLISHED_FRAMERATE_PARAM = "frameRate";
    public static final String PARTICIPANTPUBLISHED_VIDEODIMENSIONS_PARAM = "videoDimensions";
    public static final String PARTICIPANTPUBLISHED_FILTER_PARAM = "filter";
    public static final String PARTICIPANTPUBLISHED_APPSHOWNAME_PARAM = "appShowName";
    public static final String PARTICIPANTPUBLISHED_APPSHOWDESC_PARAM = "appShowDesc";

    public static final String PARTICIPANT_UNPUBLISHED_METHOD = "participantUnpublished";
    public static final String PARTICIPANT_UNPUBLISHED_NAME_PARAM = "connectionId";
    public static final String PARTICIPANT_UNPUBLISHED_REASON_PARAM = "reason";
    public static final String PARTICIPANT_UNPUBLISHED_UUID_PARAM = "uuid";
    public static final String PARTICIPANT_UNPUBLISHED_STREAM_TYPE_PARAM = "streamType";
    public static final String PARTICIPANT_UNPUBLISHED_ID_PARAM = "publishId";

    public static final String PARTICIPANTSENDMESSAGE_METHOD = "sendMessage";
    public static final String PARTICIPANTSENDMESSAGE_DATA_PARAM = "data";
    public static final String PARTICIPANTSENDMESSAGE_FROM_PARAM = "from";
    public static final String PARTICIPANTSENDMESSAGE_TYPE_PARAM = "type";

    public static final String ROOMCLOSED_METHOD = "roomClosed";
    public static final String ROOMCLOSED_ROOM_PARAM = "sessionId";

    public static final String MEDIAERROR_METHOD = "mediaError";
    public static final String MEDIAERROR_ERROR_PARAM = "error";

    public static final String ICECANDIDATE_METHOD = "iceCandidate";
    public static final String ICECANDIDATE_SENDER_UUID_PARAM = "senderUuid";
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

    public static final String DISTRIBUTESHARECASTPLAYSTRATEGY_METHOD = "distributeShareCastPlayStrategy";
    public static final String DISTRIBUTESHARECASTPLAYSTRATEGY_CONNECTIONID_PARAM = "connectionId";
    public static final String DISTRIBUTESHARECASTPLAYSTRATEGY_STRATEGY_PARAM = "shareCastPlayStrategy";
    public static final String DISTRIBUTESHARECASTPLAYSTRATEGY_NOTIFY = "distributeShareCastPlayStrategyNotify";

    public static final String UPLOADSHARECASTPLAYSTRATEGY_METHOD = "uploadShareCastPlayStrategy";
    public static final String UPLOADSHARECASTPLAYSTRATEGY_CONNECTIONID_PARAM = "connectionId";
    public static final String UPLOADSHARECASTPLAYSTRATEGY_STRATEGY_PARAM = "shareCastPlayStrategy";
    public static final String UPLOADSHARECASTPLAYSTRATEGY_NOTIFY = "uploadShareCastPlayStrategyNotify";

    public static final String UPLOADTERMINALINFO_METHOD = "uploadTerminalInfo";
    public static final String UPLOADTERMINALINFO_ABILITY_PARAM = "ability";
    public static final String UPLOADTERMINALINFO_FUNCTIONALITY_PARAM = "functionality";

    public static final String TERMINAL_INFO_MODIFIED_NOTIFY_METHOD = "terminalInfoModifiedNotify";

    public static final String GETSUBDEVORUSERBYDEPTIDS_METHOD = "getSubDevOrUserByDeptIds";
    public static final String GETSUBDEVORUSERBYDEPTIDS_ORGIDS_PARAM = "orgIds";

    public static final String START_CONF_RECORD_METHOD = "startConferenceRecord";
    public static final String START_CONF_RECORD_ROOMID_PARAM = "roomId";

    public static final String STOP_CONF_RECORD_METHOD = "stopConferenceRecord";
    public static final String STOP_CONF_RECORD_ROOMID_PARAM = "roomId";

    public static final String GET_CONF_RECORD_METHOD = "getConferenceRecord";
    public static final String GET_CONF_RECORD__PAGENUM_PARAM = "pageNum";
    public static final String GET_CONF_RECORD__SIZE_PARAM = "size";
    public static final String GET_CONF_RECORD__TOTAL_PARAM = "total";
    public static final String GET_CONF_RECORD__RECORDS_PARAM = "records";

    public static final String GET_CONF_RECORD_STATUS_METHOD = "getConferenceRecordStatus";
    public static final String GET_CONF_RECORD_ROOMID_PARAM = "roomId";

    public static final String PLAYBACK_CONF_RECORD_METHOD = "playbackConferenceRecord";
    public static final String PLAYBACK_CONF_RECORD_ID_PARAM = "id";

    public static final String DOWNLOAD_CONF_RECORD_METHOD = "downloadConferenceRecord";
    public static final String DOWNLOAD_CONF_RECORD_ID_PARAM = "id";

    public static final String DEL_CONF_RECORD_METHOD = "delConferenceRecord";
    public static final String DEL_CONF_RECORD_ID_PARAM = "ids";

    public static final String CLEAR_CONFERENCE_RECORD = "clearConferenceRecord";

    public static final String GET_ROOMS_RECORD_INFO = "getRoomsRecordInfo";

    public static final String START_LIVE_METHOD = "startLive";
    public static final String START_LIVE_ROOMID_PARAM = "roomId";

    public static final String STOP_LIVE_METHOD = "stopLive";
    public static final String STOP_LIVE_ROOMID_PARAM = "roomId";

    public static final String GET_LIVE_STATUS_METHOD = "getLiveStatus";
    public static final String GET_LIVE_STATUS_ROOMID_PARAM = "roomId";

    public static final String GETALLROOMSOFCORP_METHOD = "getAllRoomsOfCorp";
    public static final String GETALLROOMSOFCORP_ROOM_ID = "roomId";

    public static final String GET_MEETING_HIS_DETAIL_METHOD = "getMeetingHisDetail";

    public static final String GETMEETINGSRECORDDETAIL_METHOD = "getMeetingRecordDetail";
    public static final String GET_MEETING_RECORDS_METHOD = "getMeetingRecords";
    public static final String GETMEETINGSRECORDDETAIL_RUID_PARAM = "ruid";
    public static final String GETMEETINGSRECORDDETAIL_MIX_PARAM = "mixParam";

    public static final String SETSUBTITLECONFIG_METHOD = "setSubtitleConfig";
    public static final String SETSUBTITLECONFIG_ROOMID_PARAM = "roomId";
    public static final String SETSUBTITLECONFIG_SOURCEID_PARAM = "sourceId";
    public static final String SETSUBTITLECONFIG_OPERATION_PARAM = "operation";
    public static final String SETSUBTITLECONFIG_SOURCELANGUAGE_PARAM = "sourceLanguage";
    public static final String SETSUBTITLECONFIG_EXTRAINFO_PARAM = "extraInfo";
    public static final String SETSUBTITLECONFIG_NOTIFY = "setSubtitleConfigNotify";

    public static final String SENDSUBTITLE_METHOD = "sendSubtitle";
    public static final String SENDSUBTITLE_STATUS_PARAM = "status";
    public static final String SENDSUBTITLE_SUBTITLES_PARAM = "subtitles";
    public static final String SENDDISPLAYSUBTITLE_NOTIFY = "sendDisplaySubtitle";

    public static final String UPDATEUSERNAME_METHOD = "updateUsername";
    public static final String UPDATEUSERNAME_USERNAME_PARAM = "username";

    public static final String GETH5PAGES_METHOD = "getH5Pages";
    public static final String GETH5PAGES_TYPE_PARAM = "type";

    public static final String SWITCHVOICEMODE_METHOD = "switchVoiceMode";
    public static final String SWITCHVOICEMODE_OPERATION_PARAM = "operation";
    public static final String SWITCHVOICEMODE_NOTIFY_METHOD = "switchVoiceModeNotify";
    public static final String SWITCHVOICEMODE_SENDERCONNECTIONID_PARAM = "senderConnectionId";

    public static final String SET_PART_OPER_SPEAKER_METHOD = "setPartOperSpeaker";
    public static final String SET_PART_OPER_SPEAKER_ID_PARAM = "roomId";
    public static final String SET_PART_OPER_SPEAKER_SOURCE_PARAM = "source";
    public static final String SETPARTOPERSPEAKER_ALLOWPARTOPERSPEAKER_PARAM = "allowPartOperSpeaker";

    public static final String APPLY_OPEN_SPEAKER_STATUS_METHOD = "applyOpenSpeakerStatus";
    public static final String APPLY_OPEN_SPEAKER_STATUS_ID_PARAM = "roomId";
    public static final String APPLY_OPEN_SPEAKER_STATUS_SOURCE_ID_PARAM = "sourceId";
    public static final String APPLY_OPEN_SPEAKER_STATUS_TARGET_ID_PARAM = "targetId";
    public static final String APPLY_OPEN_SPEAKER_STATUS_USERNAME_PARAM = "username";

    public static final String RINGRING_METHOD = "ringring";
    public static final String RINGRING_SOURCE_ID_PARAM = "sourceId";
    public static final String RINGRING_TARGET_ID_PARAM = "targetId";
    public static final String RINGRINGNOTIFY_METHOD = "ringringNotify";

    public static final String UPLOADDEVICELOG_NOTIFY = "uploadDeviceLog";

    public static final String GET_UPLOAD_TOKEN_METHOD = "getUploadToken";
    public static final String GET_UPLOAD_TOKEN_TYPE_PARAM = "type";

    public static final String PAGENUM = "pageNum";
    public static final String PAGESIZE = "pageSize";
    public static final String TOTAL = "total";
    public static final String PAGES = "pages";
    public static final String PAGELIST = "list";

    public static final String UPDATE_PARTICIPANTS_ORDER_METHOD = "updateParticipantsOrder";
    public static final String UPDATE_PARTICIPANTS_ORDER_ORDEREDPARTS_PARAM = "orderedParts";

    public static final String REPLACE_PARTICIPANTS_ORDER_METHOD = "replaceParticipantsOrder";
    public static final String REPLACE_PARTICIPANTS_ORDER_NOTIFY_METHOD = "replaceParticipantsOrderNotify";

    public static final String PAUSEANDRESUMESTREAM_METHOD = "pauseAndResumeStream";
    public static final String PAUSEANDRESUMESTREAM_OPERATION_PARAM = "operation";
    public static final String PAUSEANDRESUMESTREAM_STREAMS_PARAM = "streams";

    public static final String SETPUSHSTREAMSTATUS_METHOD = "setPushStreamStatus";
    public static final String SETPUSHSTREAMSTATUS_ROOMID_PARAM = "roomId";
    public static final String SETPUSHSTREAMSTATUS_UUID_PARAM = "uuid";
    public static final String SETPUSHSTREAMSTATUS_CONNECTIONID_PARAM = "connectionId";
    public static final String SETPUSHSTREAMSTATUS_STATUS_PARAM = "status";

    public static final String GETSPECIFICPAGEOFDEPT_METHOD = "getSpecificPageOfDept";
    public static final String GETSPECIFICPAGEOFMEMBER_METHOD = "getSpecificPageOfMember";
    public static final String GETMEMBERDETAILS_METHOD = "getMemberDetails";
    public static final String RECURSIVEQUERYUSER_METHOD = "recursiveQueryUser";

    public static final String SET_PRESET_POSITION_METHOD = "setPresetPosition";
    public static final String SET_PRESET_POSITION_SERIALNUMBER_PARAM = "serialNumber";
    public static final String SET_PRESET_POSITION_INDEX_PARAM = "index";
    public static final String SET_PRESET_POSITION_CONFIGINFO_PARAM = "configInfo";
    public static final String SET_PRESET_POSITION_THUMBNAIL_PARAM = "thumbnail";

    public static final String DISCARDPRESETPOSITIONS_METHOD = "discardPresetPositions";
    public static final String DISCARDPRESETPOSITIONS_SERIALNUMBER_PARAM = "serialNumber";
    public static final String DISCARDPRESETPOSITIONS_INDEXARRAY_PARAM = "indexArray";

    public static final String GETPRESETPOSITIONS_METHOD = "getPresetPositions";
    public static final String GETPRESETPOSITIONS_SERIALNUMBER_PARAM = "serialNumber";

    public static final String ADJUSTTOPRESETPOSITION_METHOD = "adjustToPresetPosition";
    public static final String ADJUSTTOPRESETPOSITION_SERIALNUMBER_PARAM = "serialNumber";
    public static final String ADJUSTTOPRESETPOSITION_INDEX_PARAM = "index";
    public static final String ADJUSTTOPRESETPOSITION_CONFIGINFO_PARAM = "configInfo";
    public static final String ADJUSTTOPRESETPOSITIONNOTIFY_METHOD = "adjustToPresetPositionNotify";

    public static final String CREATEAPPOINTMENTROOM_SUBJECT_PARAM = "subject";
    public static final String CREATEAPPOINTMENTROOM_STARTTIME_PARAM = "startTime";
    public static final String CREATEAPPOINTMENTROOM_ENDTIME_PARAM = "endTime";
    public static final String CREATEAPPOINTMENTROOM_CREATOR_PARAM = "creator";

    public static final String CONFERENCE_TO_BEGIN_METHOD = "conferenceToBegin";
    public static final String XXL_JOB_ID = "id";
    public static final String XXL_JOB_PARAM = "executorParam";
    public static final String APPOINTMENT_CONFERENCE_CREATED_METHOD = "appointmentConferenceCreated";
    public static final String GET_CONFERENCE_SCHEDULE_METHOD = "getConferenceSchedule";
    public static final String GET_CONFERENCE_SCHEDULE_ADMIN_METHOD = "getConferenceScheduleAdmin";
    public static final String GET_PARTICIPATE_HISTORY_METHOD = "getParticipateHistory";

    public static final String GETMEETINGSRECORDS_METHOD = "getMeetingRecords";
    public static final String GETMEETINGSRECORDS_SIZE_PARAM = "size";
    public static final String GETMEETINGSRECORDS_PAGENUM_PARAM = "pageNum";
    public static final String GETMEETINGSRECORDS_ROOMID_PARAM = "roomId";
    public static final String GETMEETINGSRECORDS_FROM_PARAM = "from";
    public static final String GETMEETINGSRECORDS_TO_PARAM = "to";

    public static final String START_POLLING_METHOD = "startPolling";
    public static final String START_POLLING_ROOMID_PARAM = "roomId";
    public static final String START_POLLING_INTERVAL_TIME_PARAM = "time";

    public static final String START_POLLING_NOTIFY_METHOD = "startPollingNotify";
    public static final String POLLING_TO_NOTIFY_METHOD = "pollingToNotify";
    public static final String POLLING_CHECK_NOTIFY_METHOD = "pollingCheckNotify";
    public static final String POLLING_CONNECTIONID_PARAM = "connectionId";
    public static final String POLLING_ISCHECK_PARAM = "isCheck";

    public static final String STOP_POLLING_METHOD = "stopPolling";
    public static final String STOP_POLLING_ROOMID_PARAM = "roomId";
    public static final String STOP_POLLING_NODIFY_METHOD = "stopPollingNotify";

    public static final String GET_POLLING_STATUS_METHOD = "getPollingStatus";
    public static final String GETPOLLINGSTATUS_ROOMID_PARAM = "roomId";
    public static final String GETPOLLINGSTATUS_STATUS_PARAM = "status";
    public static final String GETPOLLINGSTATUS_INTERVALTIME_PARAM = "time";

    public static final String GET_ICE_SERVER_LIST = "getIceServerList";
    public static final String GET_CORP_INFO = "getCorpInfo";

    public static final String STATISTICS_DURATION_METHOD = "statisticsDuration";
    public static final String ROOM_AUTO_DELAY_METHOD = "roomAutoDelay";


    public static final String GET_MEETING_QUALITY_METHOD = "getMeetingQuality";
    public static final String UPLOAD_MEETING_QUALITY_METHOD = "uploadMeetingQuality";
    public static final String UPLOAD_MEETING_QUALITY_NOTIFY_METHOD = "uploadMeetingQualityNotify";

    public static final String GET_JPUSH_MESSAGE_METHOD = "getJpushMessage";
    public static final String GET_JPUSH_MESSAGE_UUID_PARAM = "uuid";

    public static final String CLEAN_JPUSH_MESSAGE_METHOD = "cleanJpushMessage";
    public static final String CLEAN_JPUSH_MESSAGE_UUID_PARAM = "uuid";

    public static final String READ_JPUSH_MESSAGE_METHOD = "readJpushMessage";
    public static final String READ_JPUSH_MESSAGE_MSGID_PARAM = "msgId";
    public static final String READ_JPUSH_MESSAGE_UUID_PARAM = "uuid";

    public static final String GETNOTREADJPUSHMESSAGE_METHOD = "getNotReadJpushMessage";
    public static final String GETNOTREADJPUSHMESSAGE_UUID_PARAM = "uuid";

    public static final String GET_INVITE_INFO_METHOD = "getInviteInfo";
    public static final String GET_INVITE_INFO_ROOMID_PARAM = "roomId";

    public static final String GET_CALL_LIST_METHOD = "getCallList";
    public static final String GET_CALL_LIST_RUID_PARAM = "ruid";

    public static final String CALL_REMOVE_METHOD = "callRemove";
    public static final String CALL_REMOVE_UUID_PARAM = "uuid";
    public static final String CALL_REMOVE_RUID_PARAM = "ruid";

    public static final String EVICT_CORPORATION_METHOD = "evictCorporation";

    public static final String SET_SCROLLING_BANNERS_METHOD = "setScrollingBanners";
    public static final String SET_SCROLLING_BANNERS_ROOMID_PARAM = "roomId";
    public static final String SET_SCROLLING_BANNERS_OPERATION_PARAM = "operation";
    public static final String SET_SCROLLING_BANNERS_CONFIG_PARAM = "config";

    public static final String GET_SCROLLING_BANNERS_METHOD = "getScrollingBanners";
    public static final String GET_SCROLLING_BANNERS_ROOMID_PARAM = "roomId";

    public static final String SAVE_JPUSH_METHOD = "saveJpush";
    public static final String SAVE_JPUSH_UUID_PARAM = "uuid";
    public static final String SAVE_JPUSH_REGISTRATIONID_PARAM = "registrationId";

    public static final String GET_FREQUENT_CONTACTS = "getFrequentContacts";

    public static final String SET_FREQUENT_CONTACTS = "setFrequentContacts";

    public static final String DEVICE_INFO_UPDATE_NOTIFY_METHOD = "deviceInfoUpdateNotify";

    public static final String GET_FIXED_ROOM_LIST_METHOD = "getFixedRoomList";
    public static final String GET_ROOM_INFO_METHOD = "getRoomInfo";

    public static final String URGED_PEOPLE_TO_END_METHOD = "urgedPeopleToEnd";
    public static final String URGED_PEOPLE_TO_END_NOTIFY_METHOD = "urgedPeopleToEndNotify";

    public static final String DELAY_CONFERENCE_METHOD = "delayConference";

    public static final String APPOINTMENT_CONFERENCE_CANCEL_NOTIFY_METHOD = "appointmentConferenceCancelNotify";

    public static final String QUERY_OPERATION_PERMISSION = "queryOperationPermission";

    public static final String APPLY_SHARE_METHOD = "applyShare";
    public static final String APPLY_SHARE_NOTIFY_METHOD = "applyShareNotify";
    public static final String END_SHARE_METHOD = "endShare";
    public static final String END_SHARE_NOTIFY_METHOD = "endShareNotify";



    public static final Set<String> FILTERS = new HashSet<>(Arrays.asList(ACCESS_IN_METHOD, CONFIRM_APPLY_FOR_LOGIN_METHOD,
            GET_NOT_FINISHED_ROOM_METHOD, CREATE_ROOM_METHOD, JOINROOM_METHOD, ACCESS_OUT_METHOD,
            GET_DEVICE_INFO_METHOD, UPDATE_DEVICE_INFO_METHOD, LEAVEROOM_METHOD, CLOSE_ROOM_METHOD,
            GETROOMLAYOUT_METHOD, GET_ORG_METHOD, GET_USER_DEVICE_METHOD, GET_SUB_DEVORUSER_METHOD, GET_DEPARTMENT_TREE_METHOD,
            COMMAND_UOGRADE_METHOD, GET_UNFINISHED_MEETINGS_METHOD, GET_GROUP_LIST_METHOD, GET_GROUP_INFO_METHOD,
            UPLOADTERMINALINFO_METHOD, GETSUBDEVORUSERBYDEPTIDS_METHOD, START_CONF_RECORD_METHOD, GET_CONF_RECORD_METHOD,
            PLAYBACK_CONF_RECORD_METHOD, DOWNLOAD_CONF_RECORD_METHOD, DEL_CONF_RECORD_METHOD, GET_CONF_RECORD_STATUS_METHOD,
            GET_LIVE_STATUS_METHOD, GETALLROOMSOFCORP_METHOD, GETMEETINGSRECORDDETAIL_METHOD, GET_MEETING_RECORDS_METHOD, UPDATEUSERNAME_METHOD,
            MODIFY_PASSWORD_METHOD, GETH5PAGES_METHOD, RINGRING_METHOD, REFUSE_INVITE_METHOD, GET_UPLOAD_TOKEN_METHOD,
            GETSPECIFICPAGEOFDEPT_METHOD, GETSPECIFICPAGEOFMEMBER_METHOD, GETMEMBERDETAILS_METHOD, RECURSIVEQUERYUSER_METHOD,
            GET_GROUP_LIST_METHOD, GET_GROUP_INFO_METHOD, CREATE_APPOINTMENT_ROOM_METHOD, UPDATE_APPOINTMENT_ROOM_METHOD,
            CANCEL_APPOINTMENT_ROOM_METHOD, SET_PRESET_POSITION_METHOD, DISCARDPRESETPOSITIONS_METHOD,
            GETPRESETPOSITIONS_METHOD, ADJUSTTOPRESETPOSITION_METHOD, GET_CONFERENCE_SCHEDULE_METHOD, GET_ROOMS_RECORD_INFO,
            CLEAR_CONFERENCE_RECORD, GET_APPOINTMENT_ROOM_DETAILS_METHOD, START_POLLING_METHOD, STOP_POLLING_METHOD, GET_POLLING_STATUS_METHOD,
            GET_ICE_SERVER_LIST, GET_CONFERENCE_SCHEDULE_ADMIN_METHOD, GET_CORP_INFO, GET_MEETING_HIS_DETAIL_METHOD, GET_PARTICIPATE_HISTORY_METHOD,
            STATISTICS_DURATION_METHOD, DELETE_CONFERENCE_HISTORY_METHOD, GET_MEETING_QUALITY_METHOD, UPLOAD_MEETING_QUALITY_METHOD,
            GET_JPUSH_MESSAGE_METHOD, CLEAN_JPUSH_MESSAGE_METHOD, READ_JPUSH_MESSAGE_METHOD, GETNOTREADJPUSHMESSAGE_METHOD, SAVE_JPUSH_METHOD,
            GET_FREQUENT_CONTACTS, SET_FREQUENT_CONTACTS, GET_FIXED_ROOM_LIST_METHOD, GET_ROOM_INFO_METHOD, QUERY_OPERATION_PERMISSION,
            URGED_PEOPLE_TO_END_METHOD, CHANGE_MEETING_ROOM_METHOD)
    );

    /**
     * 如果企业到期，以下接口无法使用
     */
//    public static final Set<String> CORP_SERVICE_EXPIRED_FILTERS = new HashSet<>(Arrays.asList(
//            CREATE_ROOM_METHOD, JOINROOM_METHOD, START_CONF_RECORD_METHOD, GET_CONF_RECORD_STATUS_METHOD,
//            MODIFY_PASSWORD_METHOD, CREATE_APPOINTMENT_ROOM_METHOD, UPDATE_APPOINTMENT_ROOM_METHOD
//    ));

    /**
     * 会控主持人不在的情况下，接口黑名单
     */
    public static final Set<String> THOR_IF_MODERATOR_NOT_FOUND_FILTERS = new HashSet<>(Arrays.asList(
            SET_AUDIO_STATUS_METHOD, SET_ROLL_CALL_METHOD, SET_AUDIO_SPEAKER_STATUS_METHOD, SETCONFERENCELAYOUT_MODE_METHOD,
            SET_PART_OPER_SPEAKER_METHOD, START_CONF_RECORD_METHOD, STOP_CONF_RECORD_METHOD, UPDATE_PARTICIPANTS_ORDER_METHOD,
            PUT_DOWN_HAND_METHOD, END_ROLL_CALL_METHOD, REPLACE_ROLL_CALL_METHOD, FORCEDISCONNECT_METHOD, START_POLLING_METHOD,
            STOP_POLLING_METHOD, SEND_MSG_METHOD, GET_MSG_HISTORY_METHOD, SET_IM_MODE_METHOD

    ));

}
