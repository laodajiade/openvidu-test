package io.openvidu.server.common.cache;

import io.openvidu.server.common.enums.TerminalStatus;
import io.openvidu.server.rpc.RpcConnection;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * @author geedow
 * @date 2019/9/12 14:50
 */
public interface CacheManage {

    Map getUserInfoByUUID(String userId);

    String getUserAuthorization(String userId);

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

    void updateMaxConcurrentOfDay(int partSize, String project);

    void recordSubscriberSetRollCall(String sessionId, Long startTime, String uuid);

    void delConferenceRelativeKey(String key);

    String getTerminalStatus(String uuid);
    /**
     * set expire key that record the ws exception link
     * @param rpcConnection ws link
     * @param overKeepAlive judge the error link ever not receive ping from client
     */
    void recordWsExceptionLink(RpcConnection rpcConnection, boolean overKeepAlive);

    void setLogUploadToken(@NotNull String uuid, @NotNull String uploadToken);

    void batchUpdatePartInfo(String uuid, Map<String, Object> updateMap);

    boolean getCorpExpired(String project);

    void setCorpExpired(Set<String> projects);
}
