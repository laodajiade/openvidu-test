package io.openvidu.server.rpc;

import io.openvidu.server.rpc.handlers.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.openvidu.client.internal.ProtocolElements.*;

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

    @PostConstruct
    public void init() {
        handlersMap.put(ACCESS_IN_METHOD, accessInHandler);
        handlersMap.put(CONFIRM_APPLY_FOR_LOGIN_METHOD, confirmApplyForLoginHandler);
        handlersMap.put(ACCESS_OUT_METHOD, accessOutHandler);
        handlersMap.put(CREATE_ROOM_METHOD, createRoomHandler);
        handlersMap.put(SHARE_SCREEN_METHOD, shareScreenHandler);
        handlersMap.put(STOP_SHARE_SCREEN_METHOD, stopShareScreenHandler);
        handlersMap.put(GET_PARTICIPANTS_METHOD, getParticipantsHandler);
        handlersMap.put(SET_AUDIO_SPEAKER_STATUS_METHOD, setAudioStatusHandler);
        handlersMap.put(SET_VIDEO_STATUS_METHOD, setVideoStatusHandler);
        handlersMap.put(RAISE_HAND_METHOD, raiseHandHandler);
    }

    public RpcAbstractHandler getRpcHandler(String requestMethod) {
        return handlersMap.getOrDefault(requestMethod, null);
    }

}
