package io.openvidu.server.common.cache;

import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.TerminalStatus;
import io.openvidu.server.common.pojo.DongleInfo;
import io.openvidu.server.rpc.RpcConnection;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Map;

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
     *
     * @param rpcConnection ws link
     * @param overKeepAlive judge the error link ever not receive ping from client
     */
    void recordWsExceptionLink(RpcConnection rpcConnection, boolean overKeepAlive);

    void setLogUploadToken(@NotNull String uuid, @NotNull String uploadToken);

    void batchUpdatePartInfo(String uuid, Map<String, Object> updateMap);

    void saveInviteInfo(String sessionId, String entryValue);

    Map getInviteInfo(String sessionId);

    void publish(String channel, String message);

    void setCorpRemainDuration(String project, int remainderDuration);

    int getCorpRemainDuration(String project);

    void setAdvanceCutDuration(String project, int advanceDuration);

    int getAdvanceCutDuration(String project);

    void delAdvanceCutDuration(String project);

    void setCorpRemainDurationLessTenHour(String project);

    String getCorpRemainDurationLessTenHour(String project);

    void delCorpRemainDurationLessTenHour(String project);

    void setCorpRemainDurationUsedUp(String project);

    String getCorpRemainDurationUsedUp(String project);

    void delCorpRemainDurationUsedUp(String project);

    JsonObject getMeetingQuality(String uuid);

    void setMeetingQuality(String uuid, JsonObject object);

    boolean checkDuplicationSendPhone(String phone, String usage);

    void roomLease(String sessionId, String ruid);

    boolean checkRoomLease(String sessionId, String ruid);

    void setCropDongleInfo(String dongleInfo);

    DongleInfo getCropDongleInfo();
}
