package io.openvidu.server.common.broker;

import com.google.gson.*;
import io.openvidu.server.common.Contants.BrokerChannelConstans;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RedisSubscriber {

    private static final Gson gson = new GsonBuilder().create();

    @Autowired
    SessionManager sessionManager;

    @Autowired
    RpcNotificationService notificationService;

    public void receiveMessage(String message, String channel) {
        switch (channel) {
            case BrokerChannelConstans.CLIENT_NOTIFY_CHANNEL:
                dealNotification(message);
                break;
            default:
                log.error("Unrecognized listening channel:{}", channel);
                break;

        }
    }

    private void dealNotification(String message) {
        JsonObject listenMsg = gson.fromJson(message, JsonObject.class);

        ArrayList excludeTargets = gson.fromJson(listenMsg.getAsJsonArray(BrokerChannelConstans.CLIENT_NOTIFY_EXCLUDE_TARGETS), ArrayList.class);
        String sessionId = listenMsg.get(BrokerChannelConstans.CLIENT_NOTIFY_SESSION_ID).getAsString();
        String method = listenMsg.get(BrokerChannelConstans.CLIENT_NOTIFY_METHOD).getAsString();
        String notifyMsg = listenMsg.getAsJsonObject(BrokerChannelConstans.CLIENT_NOTIFY_PARAMS).toString();

        List<String> notifyClientPrivateIds = sessionManager.getParticipants(sessionId).stream().map(Participant::getParticipantPrivateId)
                .filter(s -> !excludeTargets.contains(s)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(notifyClientPrivateIds)) return;
        notifyClientPrivateIds.forEach(privateId -> notificationService.sendNotification(privateId, method, notifyMsg));
    }
}
