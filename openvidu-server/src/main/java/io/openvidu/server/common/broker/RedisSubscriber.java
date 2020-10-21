package io.openvidu.server.common.broker;

import io.openvidu.server.common.constants.BrokerChannelConstans;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisSubscriber {


    @Autowired
    CorpServiceExpiredNotifyHandler corpServiceExpiredNotifyHandler;

    @Autowired
    NotifyDeviceUploadLogHandler notifyDeviceUploadLogHandler;

    public void receiveMessage(String message, String channel) {
        switch (channel) {
            case BrokerChannelConstans.DEVICE_UPGRADE_CHANNEL:
                DeviceUpgradeHandler.notifyDevice2Upgrade(message);
                break;
            case BrokerChannelConstans.USER_DELETE_CHANNEL:
                UserDelHandler.accessOutDeletedUser(message);
                break;
            case BrokerChannelConstans.CORP_SERVICE_EXPIRED_CHANNEL:
                corpServiceExpiredNotifyHandler.notify(message);
                break;
            case BrokerChannelConstans.DEVICE_LOG_UPLOAD_CHANNEL:
                notifyDeviceUploadLogHandler.notifyDev2UploadLog(message);
                break;
            default:
                log.error("Unrecognized listening channel:{}", channel);
                break;

        }
    }

}
