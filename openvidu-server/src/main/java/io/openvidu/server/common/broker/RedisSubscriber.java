package io.openvidu.server.common.broker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.openvidu.server.common.contants.BrokerChannelConstans;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
            case BrokerChannelConstans.DEVICE_UPGRADE_CHANNEL:
                DeviceUpgradeHandler.notifyDevice2Upgrade(message);
                break;
            default:
                log.error("Unrecognized listening channel:{}", channel);
                break;

        }
    }

}
