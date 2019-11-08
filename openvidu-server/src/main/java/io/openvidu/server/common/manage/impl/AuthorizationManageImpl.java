package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.enums.AuthorizationEnum;
import io.openvidu.server.common.manage.AuthorizationManage;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

import static io.openvidu.client.internal.ProtocolElements.*;

/**
 * @author geedow
 * @date 2019/10/16 17:12
 */

@Slf4j
@Component
public class AuthorizationManageImpl implements AuthorizationManage {

    @Resource
    private CacheManage cacheManage;

    private final static Set<String> AUTHORIZATION_EXCLUDE_SET = new HashSet<String>(1) {{
        add(ACCESS_IN_METHOD);add(SET_AUDIO_STATUS_METHOD);add(SET_VIDEO_STATUS_METHOD);add(SET_AUDIO_SPEAKER_STATUS_METHOD);}};

    /**
     * setAudioStatus setVideoStatus setAudioSpeakerStatus接口较为特殊，需根据请求参数进行权限的相应校验
     */
    private final static Map<String, String> METHOD_AUTHORIZATION_MAP = new HashMap<>(50);

    static {
        // createConference
        METHOD_AUTHORIZATION_MAP.put(CREATE_ROOM_METHOD, AuthorizationEnum.CREATE_CONFERENCE.getAuthorization());

        // conferenceControl
        METHOD_AUTHORIZATION_MAP.put(LOCK_SESSION_METHOD, AuthorizationEnum.CONFERENCE_CONTROL.getAuthorization());
        METHOD_AUTHORIZATION_MAP.put(UNLOCK_SESSION_METHOD, AuthorizationEnum.CONFERENCE_CONTROL.getAuthorization());
        METHOD_AUTHORIZATION_MAP.put(FORCEUNPUBLISH_METHOD, AuthorizationEnum.CONFERENCE_CONTROL.getAuthorization());
        METHOD_AUTHORIZATION_MAP.put(FORCEDISCONNECT_METHOD, AuthorizationEnum.CONFERENCE_CONTROL.getAuthorization());
        METHOD_AUTHORIZATION_MAP.put(EVICTED_PARTICIPANT_BY_USER_ID_METHOD, AuthorizationEnum.CONFERENCE_CONTROL.getAuthorization());
        METHOD_AUTHORIZATION_MAP.put(CLOSE_ROOM_METHOD, AuthorizationEnum.CONFERENCE_CONTROL.getAuthorization());
        METHOD_AUTHORIZATION_MAP.put(SET_AUDIO_SPEAKER_STATUS_METHOD, AuthorizationEnum.CONFERENCE_CONTROL.getAuthorization());
        METHOD_AUTHORIZATION_MAP.put(SET_SHARE_POWER_METHOD, AuthorizationEnum.CONFERENCE_CONTROL.getAuthorization());
        METHOD_AUTHORIZATION_MAP.put(TRANSFER_MODERATOR_METHOD, AuthorizationEnum.CONFERENCE_CONTROL.getAuthorization());

        // participantOnly
        METHOD_AUTHORIZATION_MAP.put(CONFIRM_APPLY_FOR_LOGIN_METHOD, AuthorizationEnum.PARTICIPANT_ONLY.getAuthorization());
        METHOD_AUTHORIZATION_MAP.put(JOINROOM_METHOD, AuthorizationEnum.PARTICIPANT_ONLY.getAuthorization());
        METHOD_AUTHORIZATION_MAP.put(PUBLISHVIDEO_METHOD, AuthorizationEnum.PARTICIPANT_ONLY.getAuthorization());
        METHOD_AUTHORIZATION_MAP.put(RECEIVEVIDEO_METHOD, AuthorizationEnum.PARTICIPANT_ONLY.getAuthorization());
        METHOD_AUTHORIZATION_MAP.put(LEAVEROOM_METHOD, AuthorizationEnum.PARTICIPANT_ONLY.getAuthorization());
        METHOD_AUTHORIZATION_MAP.put(RAISE_HAND_METHOD, AuthorizationEnum.PARTICIPANT_ONLY.getAuthorization());
        METHOD_AUTHORIZATION_MAP.put(PUT_DOWN_HAND_METHOD, AuthorizationEnum.PARTICIPANT_ONLY.getAuthorization());
        METHOD_AUTHORIZATION_MAP.put(GET_PARTICIPANTS_METHOD, AuthorizationEnum.PARTICIPANT_ONLY.getAuthorization());
        METHOD_AUTHORIZATION_MAP.put(GET_PRESET_INFO_METHOD, AuthorizationEnum.PARTICIPANT_ONLY.getAuthorization());
    }


    @Override
    public boolean checkIfOperationPermitted(String method, RpcConnection rpcConnection) {
        if (AUTHORIZATION_EXCLUDE_SET.contains(method)) return true;
        String methodMapAuth = METHOD_AUTHORIZATION_MAP.getOrDefault(method, AuthorizationEnum.PARTICIPANT_ONLY.getAuthorization());
        String userAuthorization = cacheManage.getUserAuthorization(rpcConnection.getUserUuid());
        return !Objects.isNull(userAuthorization) && Arrays.asList(userAuthorization.split(",")).contains(methodMapAuth);
    }
}
