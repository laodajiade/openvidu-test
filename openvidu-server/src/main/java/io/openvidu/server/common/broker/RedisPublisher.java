package io.openvidu.server.common.broker;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.common.Contants.BrokerChannelConstans;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

@Service
public class RedisPublisher {

    @Resource(name = "tokenStringTemplate")
    private StringRedisTemplate stringTemplate;

    private void sendChannelMsg(String channel, String message) {
        stringTemplate.convertAndSend(channel, message);
    }

    private static String composePubMsg(String sessionId, List<String> excludePrivateIds, String method, Object notifyObj) {
        JsonObject pubJson = new JsonObject();
        JsonArray excludeTargets = new JsonArray();
        if (!CollectionUtils.isEmpty(excludePrivateIds))
            excludePrivateIds.forEach(excludeTargets::add);
        pubJson.addProperty(BrokerChannelConstans.CLIENT_NOTIFY_SESSION_ID, sessionId);
        pubJson.add(BrokerChannelConstans.CLIENT_NOTIFY_EXCLUDE_TARGETS, excludeTargets);
        pubJson.addProperty(BrokerChannelConstans.CLIENT_NOTIFY_METHOD, method);
        pubJson.addProperty(BrokerChannelConstans.CLIENT_NOTIFY_PARAMS, notifyObj.toString());
        return pubJson.toString();
    }

    public void notifyParticipants(String sessionId, List<String> excludePrivateIds, String method, Object params) {
        sendChannelMsg(BrokerChannelConstans.CLIENT_NOTIFY_CHANNEL, composePubMsg(sessionId, excludePrivateIds, method, params));
    }
}
