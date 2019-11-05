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
 * @author chosongi
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
    private ReceiveVideoFromHandler receiveVideoFromHandler;

    @Resource
    private UnsubscribeFromVideoHandler unsubscribeFromVideoHandler;

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




    @PostConstruct
    public void init() {
        handlersMap.put(ProtocolElements.ACCESS_IN_METHOD,                  accessInHandler);
        handlersMap.put(ProtocolElements.CONFIRM_APPLY_FOR_LOGIN_METHOD,    confirmApplyForLoginHandler);
        handlersMap.put(ProtocolElements.ACCESS_OUT_METHOD,                 accessOutHandler);
        handlersMap.put(ProtocolElements.CREATE_ROOM_METHOD,                createRoomHandler);
        handlersMap.put(ProtocolElements.SHARE_SCREEN_METHOD,               shareScreenHandler);
        handlersMap.put(ProtocolElements.STOP_SHARE_SCREEN_METHOD,          stopShareScreenHandler);
        handlersMap.put(ProtocolElements.GET_PARTICIPANTS_METHOD,           getParticipantsHandler);
        handlersMap.put(ProtocolElements.SET_AUDIO_SPEAKER_STATUS_METHOD,   setAudioStatusHandler);
        handlersMap.put(ProtocolElements.SET_VIDEO_STATUS_METHOD,           setVideoStatusHandler);
        handlersMap.put(ProtocolElements.RAISE_HAND_METHOD,                 raiseHandHandler);
        handlersMap.put(ProtocolElements.PUT_DOWN_HAND_METHOD,              putDownHandHandler);
        handlersMap.put(ProtocolElements.SET_ROLL_CALL_METHOD,              setRollCallHandler);
        handlersMap.put(ProtocolElements.END_ROLL_CALL_METHOD,              endRollCallHandler);
        handlersMap.put(ProtocolElements.REPLACE_ROLL_CALL_METHOD,          replaceRollCallHandler);
        handlersMap.put(ProtocolElements.LOCK_SESSION_METHOD,               lockSessionHandler);
        handlersMap.put(ProtocolElements.UNLOCK_SESSION_METHOD,             unlockSessionHandler);
        handlersMap.put(ProtocolElements.JOINROOM_METHOD,                   joinRoomHandler);
        handlersMap.put(ProtocolElements.LEAVEROOM_METHOD,                  leaveRoomHandler);
        handlersMap.put(ProtocolElements.PUBLISHVIDEO_METHOD,               publishVideoHandler);
        handlersMap.put(ProtocolElements.RECEIVEVIDEO_METHOD,               receiveVideoFromHandler);
        handlersMap.put(ProtocolElements.UNSUBSCRIBEFROMVIDEO_METHOD,       unsubscribeFromVideoHandler);
        handlersMap.put(ProtocolElements.ONICECANDIDATE_METHOD,             onIceCandidateHandler);
        handlersMap.put(ProtocolElements.SENDMESSAGE_ROOM_METHOD,           sendMessageHandler);
        handlersMap.put(ProtocolElements.UNPUBLISHVIDEO_METHOD,             unpublishVideoHandler);
        handlersMap.put(ProtocolElements.STREAMPROPERTYCHANGED_METHOD,      streamPropertyChangedHandler);
        handlersMap.put(ProtocolElements.FORCEDISCONNECT_METHOD,            forceDisconnectHandler);
        handlersMap.put(ProtocolElements.FORCEUNPUBLISH_METHOD,             forceUnpublishHandler);
        handlersMap.put(ProtocolElements.APPLYFILTER_METHOD,                applyFilterHandler);
        handlersMap.put(ProtocolElements.REMOVEFILTER_METHOD,               removeFilterHandler);
        handlersMap.put(ProtocolElements.EXECFILTERMETHOD_METHOD,           execFilterMethodHandler);
        handlersMap.put(ProtocolElements.ADDFILTEREVENTLISTENER_METHOD,     addFilterEventListenerHandler);
        handlersMap.put(ProtocolElements.REMOVEFILTEREVENTLISTENER_METHOD,  removeFilterEventListenerHandler);
        handlersMap.put(ProtocolElements.CLOSE_ROOM_METHOD,                 closeRoomHandler);
        handlersMap.put(ProtocolElements.INVITE_PARTICIPANT_METHOD,         inviteParticipantHandler);
        handlersMap.put(ProtocolElements.REFUSE_INVITE_METHOD,              refuseInviteHandler);
        handlersMap.put(ProtocolElements.GET_PRESET_INFO_METHOD,            getPresetInfoHandler);
    }

    public RpcAbstractHandler getRpcHandler(String requestMethod) {
        return handlersMap.getOrDefault(requestMethod, null);
    }

}
