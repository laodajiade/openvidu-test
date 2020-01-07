package io.openvidu.server.common.cache;

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
}
