package io.openvidu.server.common.broker;

import io.openvidu.server.common.constants.BrokerChannelConstans;
import io.openvidu.server.core.SessionManager;
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

    @Autowired
    ToOpenviduNotifyHandler toOpenviduNotifyHandler;
    @Autowired
    private SessionManager sessionManager;

    @Autowired
    NotifyDeviceInfoUpdateHandler deviceInfoUpdateHandler;

    @Autowired
    private DeviceUpgradeHandler deviceUpgradeHandler;

    public void receiveMessage(String message, String channel) {
        switch (channel) {
            case BrokerChannelConstans.DEVICE_UPGRADE_CHANNEL:
                deviceUpgradeHandler.notifyDevice2Upgrade(message);
                break;
            case BrokerChannelConstans.USER_DELETE_CHANNEL:
                UserDelHandler.accessOutDeletedUser(message);
                break;
            case BrokerChannelConstans.CORP_SERVICE_EXPIRED_CHANNEL:
            case BrokerChannelConstans.CORP_INFO_MODIFIED_CHANNEL:
                corpServiceExpiredNotifyHandler.notify(message);
                break;
            case BrokerChannelConstans.DEVICE_LOG_UPLOAD_CHANNEL:
                notifyDeviceUploadLogHandler.notifyDev2UploadLog(message);
                break;
            case BrokerChannelConstans.DEVICE_NAME_UPDATE_CHANNEL:
                deviceInfoUpdateHandler.notifyDeviceInfoUpdate(message);
                break;
            case BrokerChannelConstans.TOPIC_ROOM_RECORDER_ERROR:
                sessionManager.handleRecordErrorEvent(message);
                break;
            case BrokerChannelConstans.TO_OPENVIDU_CHANNEL:
                toOpenviduNotifyHandler.notify(message);
                break;
            default:
                log.error("Unrecognized listening channel:{}", channel);
                break;

        }
    }

}
