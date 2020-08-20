package io.openvidu.server.common.cache;

import io.openvidu.server.common.enums.TerminalStatus;
import io.openvidu.server.common.enums.UserOnlineStatusEnum;
import io.openvidu.server.rpc.RpcConnection;

import java.util.Date;
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

    void updateTerminalStatus(RpcConnection rpcConnection, TerminalStatus status);

    void delUserToken(String uuid);

    void updateTokenInfo(String uuid, String key, String value);

    void saveRoomInfo(String roomId, Map<String, Object> roomInfo);

    void savePartInfo(String uuid, Map<String, Object> partInfo);

    void delPartInfo(String uuid);

    void delRoomInfo(String sessionId);

    Map getPartInfo(String userUuid);

    Map getRoomInfo(String roomId);

    void updatePartInfo(String uuid, String key, Object updateInfo);

    boolean existsConferenceRelativeInfo(String key);

    Long getMaxConcurrentOfDayAndDel(String project, Date queryEndTime);

    void recordSubscriberSetRollCall(String sessionId, Long startTime, String uuid);

    void delConferenceRelativeKey(String key);

    void recordWsExceptionLink(RpcConnection rpc, boolean overKeepAlive);

    String getTerminalStatus(String uuid);
}
