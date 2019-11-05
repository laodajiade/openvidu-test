package io.openvidu.server.rpc;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.dao.ConferenceMapper;
import io.openvidu.server.common.dao.DeviceMapper;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferenceSearch;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author chosongi
 * @date 2019/11/5 14:21
 */
@Slf4j
@Component
public abstract class RpcAbstractHandler {

    @Resource
    protected SessionManager sessionManager;

    @Resource
    protected CacheManage cacheManage;

    @Resource
    protected DeviceMapper deviceMapper;

    @Resource
    protected ConferenceMapper conferenceMapper;

    @Resource
    protected RpcNotificationService notificationService;

    public abstract void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request);

    protected static String getStringParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            throw new RuntimeException("Request element '" + key + "' is missing in method '" + request.getMethod()
                    + "'. CHECK THAT 'openvidu-server' AND 'openvidu-browser' SHARE THE SAME VERSION NUMBER");
        }
        return request.getParams().get(key).getAsString();
    }

    protected static String getStringOptionalParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            return null;
        }

        return request.getParams().get(key).getAsString();
    }

    protected static boolean getBooleanParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            throw new RuntimeException("Request element '" + key + "' is missing in method '" + request.getMethod()
                    + "'. CHECK THAT 'openvidu-server' AND 'openvidu-browser' SHARE THE SAME VERSION NUMBER");
        }
        return request.getParams().get(key).getAsBoolean();
    }

    protected void leaveRoomAfterConnClosed(String participantPrivateId, EndReason reason) {
        try {
            sessionManager.evictParticipant(this.sessionManager.getParticipant(participantPrivateId), null, null,
                    reason);
            log.info("Evicted participant with privateId {}", participantPrivateId);
        } catch (OpenViduException e) {
            log.warn("Unable to evict: {}", e.getMessage());
            log.trace("Unable to evict user", e);
        }
    }

    protected static Integer getIntOptionalParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            return null;
        }

        return request.getParams().get(key).getAsInt();
    }

    protected static Float getFloatOptionalParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            return null;
        }

        return request.getParams().get(key).getAsFloat();
    }

    protected String generalRoomId() {
        String sessionId = "";
        int tryCnt = 10;
        while (tryCnt-- > 0) {
            sessionId = StringUtil.createSessionId();
            if (isExistingRoom(sessionId, "")) {
                log.warn("conference:{} already exist.", sessionId);
                continue;
            }
            log.info("general sessionId:{}", sessionId);
            break;
        }

        return sessionId;
    }

    protected boolean isExistingRoom(String sessionId, String userUuid) {
        // verify room id ever exists
        ConferenceSearch search = new ConferenceSearch();
        search.setRoomId(sessionId);
        // 会议状态：0 未开始(当前不存在该状态) 1 进行中 2 已结束
        search.setStatus(1);
        try {
            List<Conference> conferences = conferenceMapper.selectBySearchCondition(search);
            if (conferences != null && !conferences.isEmpty()) {
                if (sessionId.equals(userUuid)) {
                    // force close previous room when sessionId is userUuid.
                    log.warn("conference:{} will be force closed.", sessionId);
                    // TODO
                    conferences.forEach(conference -> sessionManager.endConferenceInfo(conference));
                    cleanSession(sessionId, "", false, EndReason.forceCloseSessionByUser);
                    return false;
                }

                log.warn("conference:{} already exist.", sessionId);
                return true;
            }
        } catch (Exception e) {
            log.info("conferenceMapper selectBySearchCondition(search) exception {}", e);
        }
        return false;
    }

    protected ErrorCodeEnum cleanSession(String sessionId, String privateId, boolean checkModerator, EndReason reason) {
        if (Objects.isNull(sessionManager.getSession(sessionId))) {
            return ErrorCodeEnum.CONFERENCE_NOT_EXIST;
        }

        if (sessionManager.getSession(sessionId).isClosed()) {
            return ErrorCodeEnum.CONFERENCE_ALREADY_CLOSED;
        }

        if (checkModerator && sessionManager.getParticipant(sessionId, privateId).getRole() != OpenViduRole.MODERATOR) {
            return ErrorCodeEnum.PERMISSION_LIMITED;
        }

        // 1. notify all participant stop publish and receive stream.
        // 2. close session but can not disconnect the connection.
        this.sessionManager.unpublishAllStream(sessionId, reason);
        this.sessionManager.closeSession(sessionId, reason);

        return ErrorCodeEnum.SUCCESS;
    }

}
