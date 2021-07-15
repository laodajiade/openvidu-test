package io.openvidu.server.rpc;

import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.rpc.handlers.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author geedow
 * @date 2019/11/5 14:27
 */
@Slf4j
@Component
public class RpcHandlerFactory {

    private static final Map<String, RpcAbstractHandler> handlersMap = new ConcurrentHashMap<>(100);

    @Resource
    private AccessInHandler accessInHandler;

    @Resource
    private ConfirmApplyForLoginHandler confirmApplyForLoginHandler;

    @Resource
    private AccessOutHandler accessOutHandler;

    @Resource
    private CreateRoomHandler createRoomHandler;

    @Resource
    private ShareScreenHandler shareScreenHandler;

    @Resource
    private StopShareScreenHandler stopShareScreenHandler;

    @Resource
    private GetParticipantsHandler getParticipantsHandler;

    @Resource
    private SetAudioStatusHandler setAudioStatusHandler;

    @Resource
    private SetVideoStatusHandler setVideoStatusHandler;

    @Resource
    private RaiseHandHandler raiseHandHandler;

    @Resource
    private PutDownHandHandler putDownHandHandler;

    @Resource
    private SetRollCallHandler setRollCallHandler;

    @Resource
    private EndRollCallHandler endRollCallHandler;

    @Resource
    private ReplaceRollCallHandler replaceRollCallHandler;

    @Resource
    private LockSessionHandler lockSessionHandler;

    @Resource
    private UnlockSessionHandler unlockSessionHandler;

    @Resource
    private JoinRoomHandler joinRoomHandler;

    @Resource
    private LeaveRoomHandler leaveRoomHandler;

    @Resource
    private PublishVideoHandler publishVideoHandler;

    @Resource
    private SubscribeVideoHandler subscribeVideoHandler;

    @Resource
    private UnsubscribeVideoHandler unsubscribeVideoHandler;

    @Resource
    private OnIceCandidateHandler onIceCandidateHandler;

    @Resource
    private SendMessageHandler sendMessageHandler;

    @Resource
    private UnpublishVideoHandler unpublishVideoHandler;

    @Resource
    private StreamPropertyChangedHandler streamPropertyChangedHandler;

    @Resource
    private ForceDisconnectHandler forceDisconnectHandler;

    @Resource
    private ForceUnpublishHandler forceUnpublishHandler;

    @Resource
    private ApplyFilterHandler applyFilterHandler;

    @Resource
    private RemoveFilterHandler removeFilterHandler;

    @Resource
    private ExecFilterMethodHandler execFilterMethodHandler;

    @Resource
    private AddFilterEventListenerHandler addFilterEventListenerHandler;

    @Resource
    private RemoveFilterEventListenerHandler removeFilterEventListenerHandler;

    @Resource
    private CloseRoomHandler closeRoomHandler;

    @Resource
    private InviteParticipantHandler inviteParticipantHandler;

    @Resource
    private RefuseInviteHandler refuseInviteHandler;

    @Resource
    private GetPresetInfoHandler getPresetInfoHandler;

    @Resource
    private SetAudioSpeakerStatusHandler setAudioSpeakerStatusHandler;

    @Resource
    private SetSharePowerHandler setSharePowerHandler;

    @Resource
    private TransferModeratorHandler transferModeratorHandler;

    @Resource
    private GetOrgListHandler getOrgListHandler;

    @Resource
    private GetUserDeviceListHandler getUserDeviceListHandler;

    @Resource
    private GetDeviceInfoHandler getDeviceInfoHandler;

    @Resource
    private UpdateDeviceInfoHandler updateDeviceInfoHandler;

    @Resource
    private RoomDelayHandler roomDelayHandler;

    @Resource
    private GetNotFinishedRoomHandler getNotFinishedRoomHandler;

    @Resource
    private GetRoomLayoutHandler getRoomLayoutHandler;

    @Resource
    private BroadcastMajorLayoutHandler broadcastMajorLayoutHandler;

    @Resource
    private SetConferenceLayoutHandler setConferenceLayoutHandler;

    @Resource
    private GetSubDevOrUserHandler getSubDevOrUserHandler;

    @Resource
    private StartPtzControlHandler startPtzControlHandler;

    @Resource
    private StopPtzControlHandler stopPtzControlHandler;

    @Resource
    private SwapPartWindowHandler swapPartWindowHandler;

    @Resource
    private GetDepartmentTreeHandler getDepartmentTreeHandler;

    @Resource
    private CommandUpgradeHandler commandUpgradeHandler;

    @Resource
    private AdjustResolutionHandler adjustResolutionHandler;

    @Resource
    private GetUnfinishedMeetingsHandler getUnfinishedMeetingsHandler;

    @Resource
    private SharingControlHandler sharingControlHandler;

    @Resource
    private DistributeShareCastPlayStrategyHandler distributeShareCastPlayStrategyHandler;

    @Resource
    private UploadShareCastPlayStrategyHandler uploadShareCastPlayStrategyHandler;

    @Resource
    private GetGroupListHandler getGroupListHandler;

    @Resource
    private GetGroupInfoHandler getGroupInfoHandler;

    @Resource
    private UploadTerminalInfoHandler uploadTerminalInfoHandler;

    @Resource
    private GetSubDevOrUserByDeptIdsHandler getSubDevOrUserByDeptIdsHandler;

    @Resource
    private ChangePartRoleHandler changePartRoleHandler;

    @Resource
    private StartConferenceRecordHandler startConferenceRecordHandler;

    @Resource
    private StopConferenceRecordHandler stopConferenceRecordHandler;

    @Resource
    private GetConferenceRecordHandler getConferenceRecordHandler;

    @Resource
    private PlaybackConferenceRecordHandler playbackConferenceRecordHandler;

    @Resource
    private DownloadConferenceRecordHandler downloadConferenceRecordHandler;

    @Resource
    private DelConferenceRecordHandler delConferenceRecordHandler;

    @Resource
    private StartLiveHandler startLiveHandler;

    @Resource
    private StopLiveHandler stopLiveHandler;

    @Resource
    private GetLiveStatusHandler getLiveStatusHandler;

    @Resource
    private GetConferenceRecordStatusHandler getConferenceRecordStatusHandler;
    @Resource
    private GetAllRoomsOfCorpHandler getAllRoomsOfCorpHandler;
    @Resource
    private GetMeetingRecordDetailHandler getMeetingRecordDetailHandler;
    @Resource
    private GetMeetingRecordsHandler getMeetingRecordsHandler;
    @Resource
    private SetSubtitleConfigHandler setSubtitleConfigHandler;
    @Resource
    private SendSubtitleHandler sendSubtitleHandler;
    @Resource
    private ModifyPasswordHandler modifyPasswordHandler;
    @Resource
    private UpdateUsernameHandler updateUsernameHandler;
    @Resource
    private GetH5PagesHandler getH5PagesHandler;
    @Resource
    private SwitchVoiceModeHandler switchVoiceModeHandler;
    @Resource
    private SetPartOperSpeakerHandler setPartOperSpeakerHandler;
    @Resource
    private ApplyOpenSpeakerStatusHandler applyOpenSpeakerStatusHandler;
    @Resource
    private RingRingHandler ringRingHandler;
    @Resource
    private CanceInviteHandler canceInviteHandler;
    @Resource
    private GetUploadTokenHandler getUploadTokenHandler;
    @Resource
    private UpdateParticipantsOrderHandler updateParticipantsOrderHandler;
    @Resource
    private PauseAndResumeStreamHandler pauseAndResumeStreamHandler;
    @Resource
    private SetPushStreamStatusHandler setPushStreamStatusHandler;
    @Resource
    private GetSpecificPageOfDeptHandler getSpecificPageOfDeptHandler;
    @Resource
    private GetSpecificPageOfMemberHandler getSpecificPageOfMemberHandler;
    @Resource
    private GetMemberDetailsHandler getMemberDetailsHandler;
    @Resource
    private RecursiveQueryUserHandler recursiveQueryUserHandler;
    @Resource
    private StartPollingHandler startPollingHandler;
    @Resource
    private StopPollingHandler stopPollingHandler;
    @Resource
    private GetPollingStatusHandler getPollingStatusHandler;
    @Resource
    private GetRoomsRecordInfoHandler getRoomsRecordInfoHandler;
    @Resource
    private ClearConferenceRecordHandler clearConferenceRecordHandler;
    @Resource
    private GetIceServerListHandler getIceServerListHandler;
    @Resource
    private GetCorpInfoHandler getCorpInfoHandler;
    @Resource
    private GetMeetingHisDetailHandler getMeetingHisDetailHandler;
    @Resource
    private SetPresetPositionHandler setPresetPositionHandler;
    @Resource
    private DiscardPresetPositionsHandler discardPresetPositionsHandler;
    @Resource
    private GetPresetPositionsHandler getPresetPositionsHandler;
    @Resource
    private AdjustToPresetPositionHandler adjustToPresetPositionHandler;
    @Resource
    private StatisticsDurationHandler statisticsDurationHandler;
    @Resource
    private GetJpushMessageHandler getJpushMessageHandler;
    @Resource
    private CleanJpushMessageHandler cleanJpushMessageHandler;
    @Resource
    private ReadJpushMessageHandler readJpushMessageHandler;
    @Resource
    private GetNotReadJpushMessageHandler getNotReadJpushMessageHandler;
    @Resource
    private GetInviteInfoHandler getInviteInfoHandler;
    @Resource
    private GetCallListHandler getCallListHandler;
    @Resource
    private CallRemoveHandler callRemoveHandler;
    @Resource
    private SetScrollingBannersHandler setScrollingBannersHandler;
    @Resource
    private GetScrollingBannersHandler getScrollingBannersHandler;
    @Resource
    private SaveJpushHandler saveJpushHandler;
    @Resource
    private GetFrequentContactsHandler getFrequentContactsHandler;
    @Resource
    private SetFrequentContactsHandler setFrequentContactsHandler;
    @Resource
    private QueryOperationPermissionHandler queryOperationPermissionHandler;
    @Resource
    private Map<String, ExRpcAbstractHandler> exHandlersMap;


    @PostConstruct
    public void init() {
        handlersMap.putAll(exHandlersMap);

        handlersMap.put(ProtocolElements.ACCESS_IN_METHOD, accessInHandler);
        handlersMap.put(ProtocolElements.CONFIRM_APPLY_FOR_LOGIN_METHOD, confirmApplyForLoginHandler);
        handlersMap.put(ProtocolElements.ACCESS_OUT_METHOD, accessOutHandler);
        handlersMap.put(ProtocolElements.CREATE_ROOM_METHOD, createRoomHandler);
        handlersMap.put(ProtocolElements.SHARE_SCREEN_METHOD, shareScreenHandler);
        handlersMap.put(ProtocolElements.STOP_SHARE_SCREEN_METHOD, stopShareScreenHandler);
        handlersMap.put(ProtocolElements.GET_PARTICIPANTS_METHOD, getParticipantsHandler);
        handlersMap.put(ProtocolElements.SET_AUDIO_STATUS_METHOD, setAudioStatusHandler);
        handlersMap.put(ProtocolElements.SET_VIDEO_STATUS_METHOD, setVideoStatusHandler);
        handlersMap.put(ProtocolElements.RAISE_HAND_METHOD, raiseHandHandler);
        handlersMap.put(ProtocolElements.PUT_DOWN_HAND_METHOD, putDownHandHandler);
        handlersMap.put(ProtocolElements.SET_ROLL_CALL_METHOD, setRollCallHandler);
        handlersMap.put(ProtocolElements.END_ROLL_CALL_METHOD, endRollCallHandler);
        handlersMap.put(ProtocolElements.REPLACE_ROLL_CALL_METHOD, replaceRollCallHandler);
        handlersMap.put(ProtocolElements.LOCK_SESSION_METHOD, lockSessionHandler);
        handlersMap.put(ProtocolElements.UNLOCK_SESSION_METHOD, unlockSessionHandler);
        handlersMap.put(ProtocolElements.JOINROOM_METHOD, joinRoomHandler);
        handlersMap.put(ProtocolElements.LEAVEROOM_METHOD, leaveRoomHandler);
        handlersMap.put(ProtocolElements.PUBLISHVIDEO_METHOD, publishVideoHandler);
        handlersMap.put(ProtocolElements.SUBSCRIBE_VIDEO_METHOD, subscribeVideoHandler);
        handlersMap.put(ProtocolElements.UNSUBSCRIBE_VIDEO_METHOD, unsubscribeVideoHandler);
        handlersMap.put(ProtocolElements.ONICECANDIDATE_METHOD, onIceCandidateHandler);
        handlersMap.put(ProtocolElements.SENDMESSAGE_ROOM_METHOD, sendMessageHandler);
        handlersMap.put(ProtocolElements.UNPUBLISHVIDEO_METHOD, unpublishVideoHandler);
        handlersMap.put(ProtocolElements.STREAMPROPERTYCHANGED_METHOD, streamPropertyChangedHandler);
        handlersMap.put(ProtocolElements.FORCEDISCONNECT_METHOD, forceDisconnectHandler);
        handlersMap.put(ProtocolElements.FORCEUNPUBLISH_METHOD, forceUnpublishHandler);
        handlersMap.put(ProtocolElements.APPLYFILTER_METHOD, applyFilterHandler);
        handlersMap.put(ProtocolElements.REMOVEFILTER_METHOD, removeFilterHandler);
        handlersMap.put(ProtocolElements.EXECFILTERMETHOD_METHOD, execFilterMethodHandler);
        handlersMap.put(ProtocolElements.ADDFILTEREVENTLISTENER_METHOD, addFilterEventListenerHandler);
        handlersMap.put(ProtocolElements.REMOVEFILTEREVENTLISTENER_METHOD, removeFilterEventListenerHandler);
        handlersMap.put(ProtocolElements.CLOSE_ROOM_METHOD, closeRoomHandler);
        handlersMap.put(ProtocolElements.INVITE_PARTICIPANT_METHOD, inviteParticipantHandler);
        handlersMap.put(ProtocolElements.REFUSE_INVITE_METHOD, refuseInviteHandler);
        handlersMap.put(ProtocolElements.GET_PRESET_INFO_METHOD, getPresetInfoHandler);
        handlersMap.put(ProtocolElements.SET_AUDIO_SPEAKER_STATUS_METHOD, setAudioSpeakerStatusHandler);
        handlersMap.put(ProtocolElements.SET_SHARE_POWER_METHOD, setSharePowerHandler);
        handlersMap.put(ProtocolElements.TRANSFER_MODERATOR_METHOD, transferModeratorHandler);
        handlersMap.put(ProtocolElements.GET_ORG_METHOD, getOrgListHandler);
        handlersMap.put(ProtocolElements.GET_USER_DEVICE_METHOD, getUserDeviceListHandler);
        handlersMap.put(ProtocolElements.GET_DEVICE_INFO_METHOD, getDeviceInfoHandler);
        handlersMap.put(ProtocolElements.UPDATE_DEVICE_INFO_METHOD, updateDeviceInfoHandler);
        handlersMap.put(ProtocolElements.ROOM_DELAY_METHOD, roomDelayHandler);
        handlersMap.put(ProtocolElements.GET_NOT_FINISHED_ROOM_METHOD, getNotFinishedRoomHandler);
        handlersMap.put(ProtocolElements.GETROOMLAYOUT_METHOD, getRoomLayoutHandler);
        handlersMap.put(ProtocolElements.BROADCASTMAJORLAYOUT_METHOD, broadcastMajorLayoutHandler);
        handlersMap.put(ProtocolElements.SETCONFERENCELAYOUT_MODE_METHOD, setConferenceLayoutHandler);
        handlersMap.put(ProtocolElements.GET_SUB_DEVORUSER_METHOD, getSubDevOrUserHandler);
        handlersMap.put(ProtocolElements.START_PTZ_CONTROL_METHOD, startPtzControlHandler);
        handlersMap.put(ProtocolElements.STOP_PTZ_CONTROL_METHOD, stopPtzControlHandler);
        handlersMap.put(ProtocolElements.SWAP_PART_WINDOW_METHOD, swapPartWindowHandler);
        handlersMap.put(ProtocolElements.GET_DEPARTMENT_TREE_METHOD, getDepartmentTreeHandler);
        handlersMap.put(ProtocolElements.COMMAND_UOGRADE_METHOD, commandUpgradeHandler);
        handlersMap.put(ProtocolElements.ADJUST_RESOLUTION_METHOD, adjustResolutionHandler);
        handlersMap.put(ProtocolElements.GET_UNFINISHED_MEETINGS_METHOD, getUnfinishedMeetingsHandler);
        handlersMap.put(ProtocolElements.SHARING_CONTROL_METHOD, sharingControlHandler);
        handlersMap.put(ProtocolElements.DISTRIBUTESHARECASTPLAYSTRATEGY_METHOD, distributeShareCastPlayStrategyHandler);
        handlersMap.put(ProtocolElements.UPLOADSHARECASTPLAYSTRATEGY_METHOD, uploadShareCastPlayStrategyHandler);
        handlersMap.put(ProtocolElements.GET_GROUP_LIST_METHOD, getGroupListHandler);
        handlersMap.put(ProtocolElements.GET_GROUP_INFO_METHOD, getGroupInfoHandler);
        handlersMap.put(ProtocolElements.UPLOADTERMINALINFO_METHOD, uploadTerminalInfoHandler);
        handlersMap.put(ProtocolElements.GETSUBDEVORUSERBYDEPTIDS_METHOD, getSubDevOrUserByDeptIdsHandler);
        handlersMap.put(ProtocolElements.CHANGE_PART_ROLE_METHOD, changePartRoleHandler);
        handlersMap.put(ProtocolElements.START_CONF_RECORD_METHOD, startConferenceRecordHandler);
        handlersMap.put(ProtocolElements.STOP_CONF_RECORD_METHOD, stopConferenceRecordHandler);
        handlersMap.put(ProtocolElements.GET_CONF_RECORD_METHOD, getConferenceRecordHandler);
        handlersMap.put(ProtocolElements.PLAYBACK_CONF_RECORD_METHOD, playbackConferenceRecordHandler);
        handlersMap.put(ProtocolElements.DOWNLOAD_CONF_RECORD_METHOD, downloadConferenceRecordHandler);
        handlersMap.put(ProtocolElements.DEL_CONF_RECORD_METHOD, delConferenceRecordHandler);
        handlersMap.put(ProtocolElements.GET_CONF_RECORD_STATUS_METHOD, getConferenceRecordStatusHandler);
        handlersMap.put(ProtocolElements.START_LIVE_METHOD, startLiveHandler);
        handlersMap.put(ProtocolElements.STOP_LIVE_METHOD, stopLiveHandler);
        handlersMap.put(ProtocolElements.GET_LIVE_STATUS_METHOD, getLiveStatusHandler);
        handlersMap.put(ProtocolElements.GETALLROOMSOFCORP_METHOD, getAllRoomsOfCorpHandler);
        handlersMap.put(ProtocolElements.GETMEETINGSRECORDDETAIL_METHOD, getMeetingRecordDetailHandler);
        handlersMap.put(ProtocolElements.GET_MEETING_RECORDS_METHOD, getMeetingRecordsHandler);
        handlersMap.put(ProtocolElements.SETSUBTITLECONFIG_METHOD, setSubtitleConfigHandler);
        handlersMap.put(ProtocolElements.SENDSUBTITLE_METHOD, sendSubtitleHandler);
        handlersMap.put(ProtocolElements.MODIFY_PASSWORD_METHOD, modifyPasswordHandler);
        handlersMap.put(ProtocolElements.UPDATEUSERNAME_METHOD, updateUsernameHandler);
        handlersMap.put(ProtocolElements.GETH5PAGES_METHOD, getH5PagesHandler);
        handlersMap.put(ProtocolElements.SWITCHVOICEMODE_METHOD, switchVoiceModeHandler);
        handlersMap.put(ProtocolElements.SET_PART_OPER_SPEAKER_METHOD, setPartOperSpeakerHandler);
        handlersMap.put(ProtocolElements.APPLY_OPEN_SPEAKER_STATUS_METHOD, applyOpenSpeakerStatusHandler);
        handlersMap.put(ProtocolElements.RINGRING_METHOD, ringRingHandler);
        handlersMap.put(ProtocolElements.CANCELINVITE_METHOD, canceInviteHandler);
        handlersMap.put(ProtocolElements.GET_UPLOAD_TOKEN_METHOD, getUploadTokenHandler);
        handlersMap.put(ProtocolElements.UPDATE_PARTICIPANTS_ORDER_METHOD, updateParticipantsOrderHandler);
        handlersMap.put(ProtocolElements.SETPUSHSTREAMSTATUS_METHOD, setPushStreamStatusHandler);
        handlersMap.put(ProtocolElements.PAUSEANDRESUMESTREAM_METHOD, pauseAndResumeStreamHandler);
        handlersMap.put(ProtocolElements.GETSPECIFICPAGEOFDEPT_METHOD, getSpecificPageOfDeptHandler);
        handlersMap.put(ProtocolElements.GETSPECIFICPAGEOFMEMBER_METHOD, getSpecificPageOfMemberHandler);
        handlersMap.put(ProtocolElements.GETMEMBERDETAILS_METHOD, getMemberDetailsHandler);
        handlersMap.put(ProtocolElements.RECURSIVEQUERYUSER_METHOD, recursiveQueryUserHandler);
        handlersMap.put(ProtocolElements.START_POLLING_METHOD, startPollingHandler);
        handlersMap.put(ProtocolElements.STOP_POLLING_METHOD, stopPollingHandler);
        handlersMap.put(ProtocolElements.GET_POLLING_STATUS_METHOD, getPollingStatusHandler);
        handlersMap.put(ProtocolElements.GET_ROOMS_RECORD_INFO, getRoomsRecordInfoHandler);
        handlersMap.put(ProtocolElements.CLEAR_CONFERENCE_RECORD, clearConferenceRecordHandler);
        handlersMap.put(ProtocolElements.GET_ICE_SERVER_LIST, getIceServerListHandler);
        handlersMap.put(ProtocolElements.GET_CORP_INFO, getCorpInfoHandler);
        handlersMap.put(ProtocolElements.GET_MEETING_HIS_DETAIL_METHOD, getMeetingHisDetailHandler);
        handlersMap.put(ProtocolElements.SET_PRESET_POSITION_METHOD, setPresetPositionHandler);
        handlersMap.put(ProtocolElements.DISCARDPRESETPOSITIONS_METHOD, discardPresetPositionsHandler);
        handlersMap.put(ProtocolElements.GETPRESETPOSITIONS_METHOD, getPresetPositionsHandler);
        handlersMap.put(ProtocolElements.ADJUSTTOPRESETPOSITION_METHOD, adjustToPresetPositionHandler);
        handlersMap.put(ProtocolElements.STATISTICS_DURATION_METHOD, statisticsDurationHandler);
        handlersMap.put(ProtocolElements.GET_JPUSH_MESSAGE_METHOD, getJpushMessageHandler);
        handlersMap.put(ProtocolElements.CLEAN_JPUSH_MESSAGE_METHOD, cleanJpushMessageHandler);
        handlersMap.put(ProtocolElements.READ_JPUSH_MESSAGE_METHOD, readJpushMessageHandler);
        handlersMap.put(ProtocolElements.GETNOTREADJPUSHMESSAGE_METHOD, getNotReadJpushMessageHandler);
        handlersMap.put(ProtocolElements.GET_INVITE_INFO_METHOD, getInviteInfoHandler);
        handlersMap.put(ProtocolElements.GET_CALL_LIST_METHOD, getCallListHandler);
        handlersMap.put(ProtocolElements.CALL_REMOVE_METHOD, callRemoveHandler);
        handlersMap.put(ProtocolElements.SET_SCROLLING_BANNERS_METHOD, setScrollingBannersHandler);
        handlersMap.put(ProtocolElements.GET_SCROLLING_BANNERS_METHOD, getScrollingBannersHandler);
        handlersMap.put(ProtocolElements.SAVE_JPUSH_METHOD, saveJpushHandler);
        handlersMap.put(ProtocolElements.SAVE_JPUSH_METHOD, saveJpushHandler);
        handlersMap.put(ProtocolElements.GET_FREQUENT_CONTACTS, getFrequentContactsHandler);
        handlersMap.put(ProtocolElements.SET_FREQUENT_CONTACTS, setFrequentContactsHandler);
        handlersMap.put(ProtocolElements.QUERY_OPERATION_PERMISSION, queryOperationPermissionHandler);

    }

    public RpcAbstractHandler getRpcHandler(String requestMethod) {
        return handlersMap.getOrDefault(requestMethod, null);
    }

}
