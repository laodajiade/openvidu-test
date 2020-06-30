package io.openvidu.server.common.cache;

import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.enums.UserOnlineStatusEnum;

import java.util.Map;

/**
 * @author geedow
 * @date 2019/9/12 14:50
 */
public interface CacheManage {

    Map getUserInfoByUUID(String userId);

    String getUserAuthorization(String userId);

    void updateUserOnlineStatus(String uuid, UserOnlineStatusEnum onlineStatusEnum);

    void updateReconnectInfo(String userUuid, String privateId);

    void updateDeviceName(String userUuid, String deviceName);

    void setDeviceStatus(String key, String version);

    String getDeviceStatus(String serialNumber);

    void saveLivingInfo(String sessionId, String entryKey, String entryValue);

    String getLivingInfo(String sessionId, String entryKey);

    void delLivingInfo(String sessionId);

    void updateTerminalStatus(String userUuid, UserOnlineStatusEnum offline, String serialNumber, DeviceStatus offline1);
}
