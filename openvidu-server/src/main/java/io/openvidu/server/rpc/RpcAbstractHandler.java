package io.openvidu.server.rpc;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.dao.DeviceMapper;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

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

}
