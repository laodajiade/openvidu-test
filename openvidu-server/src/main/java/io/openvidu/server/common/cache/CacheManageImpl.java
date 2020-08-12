package io.openvidu.server.common.cache;

import io.openvidu.server.common.constants.CacheKeyConstants;
import io.openvidu.server.common.enums.AccessTypeEnum;
import io.openvidu.server.common.enums.TerminalStatus;
import io.openvidu.server.common.enums.UserOnlineStatusEnum;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author geedow
 * @date 2019/9/12 14:50
 */
@Slf4j
@Component
public class CacheManageImpl implements CacheManage {

    @Resource(name = "tokenStringTemplate")
    private StringRedisTemplate tokenStringTemplate;

    @Resource(name = "roomRedisTemplate")
    private RedisTemplate<String, Object> roomRedisTemplate;


    @Override
    public Map getUserInfoByUUID(String uuid) {
        return tokenStringTemplate.opsForHash().entries(CacheKeyConstants.APP_TOKEN_PREFIX_KEY + uuid);
    }

    @Override
    public String getUserAuthorization(String userId) {
        return tokenStringTemplate.opsForHash().entries(CacheKeyConstants.APP_TOKEN_PREFIX_KEY + userId)
                .get("privilege").toString();
    }

    @Override
    public String getDeviceStatus(String serialNumber) {
        return tokenStringTemplate.opsForValue().get(CacheKeyConstants.DEV_PREFIX_KEY + serialNumber);
    }

    @Override
    public void updateUserOnlineStatus(String uuid, UserOnlineStatusEnum onlineStatusEnum) {
        if (StringUtils.isEmpty(uuid)) {
            log.info("###########uuid is null");
            return;
        }
        log.info("Update user online status in cache. uuid:{}, updateStatus:{}", uuid, onlineStatusEnum.name());
        tokenStringTemplate.opsForHash().put(CacheKeyConstants.APP_TOKEN_PREFIX_KEY + uuid, "status", onlineStatusEnum.name());
    }

    @Override
    public void updateReconnectInfo(String userUuid, String privateId) {
        if (StringUtils.isEmpty(userUuid)) return;
        tokenStringTemplate.opsForHash().put(CacheKeyConstants.APP_TOKEN_PREFIX_KEY + userUuid, "reconnect", privateId);
    }

    @Override
    public void updateDeviceName(String userUuid, String deviceName) {
        if (StringUtils.isEmpty(userUuid)) return;
        tokenStringTemplate.opsForHash().put(CacheKeyConstants.APP_TOKEN_PREFIX_KEY + userUuid, "deviceName", deviceName);
    }

    @Override
    public void setDeviceStatus(String serialNumber, String version) {
        String key = CacheKeyConstants.DEV_PREFIX_KEY + serialNumber;
        tokenStringTemplate.opsForValue().set(key, version);
    }

    @Override
    public void saveLivingInfo(String sessionId, String entryKey, String livingUrl) {
        String key = CacheKeyConstants.CONFERENCE_LIVING_INFO_KEY + sessionId;
        tokenStringTemplate.opsForHash().put(key, entryKey, livingUrl);
        tokenStringTemplate.expire(key, CacheKeyConstants.DEFAULT_CONFERENCE_EXPIRE, TimeUnit.SECONDS);
    }

    @Override
    public String getLivingInfo(String sessionId, String entryKey) {
        String key = CacheKeyConstants.CONFERENCE_LIVING_INFO_KEY + sessionId;
        Object obj = tokenStringTemplate.opsForHash().get(key, entryKey);
        return Objects.isNull(obj) ? null : (String) obj;
    }

    @Override
    public void delLivingInfo(String sessionId) {
        String key = CacheKeyConstants.CONFERENCE_LIVING_INFO_KEY + sessionId;
        tokenStringTemplate.delete(key);
    }

    @Override
    public void updateTerminalStatus(RpcConnection rpcConnection, TerminalStatus terminalStatus) {
        if (Objects.equals(AccessTypeEnum.web, rpcConnection.getAccessType())) {
            return;
        }

        if (!StringUtils.isEmpty(rpcConnection.getUserUuid())) {
            tokenStringTemplate.opsForHash().put(CacheKeyConstants.APP_TOKEN_PREFIX_KEY + rpcConnection.getUserUuid(), "status", terminalStatus.name());
            log.info("Update user online status in cache. uuid:{}, updateStatus:{}", rpcConnection.getUserUuid(), terminalStatus.name());
        }

        if (!StringUtils.isEmpty(rpcConnection.getSerialNumber())) {
            tokenStringTemplate.opsForValue().set(CacheKeyConstants.DEV_PREFIX_KEY + rpcConnection.getSerialNumber(), terminalStatus.name());
            log.info("Update device online status in cache. serialNumber:{}, updateStatus:{}", rpcConnection.getSerialNumber(), terminalStatus.name());
        }
    }

    @Override
    public void delUserToken(String uuid) {
        tokenStringTemplate.delete(CacheKeyConstants.APP_TOKEN_PREFIX_KEY + uuid);
    }

    @Override
    public void updateTokenInfo(String uuid, String key, String value) {
        tokenStringTemplate.opsForHash().put(CacheKeyConstants.APP_TOKEN_PREFIX_KEY + uuid, key, value);
    }

    @Override
    public void saveRoomInfo(String roomId, Map<String, Object> roomInfo) {
        log.info("save room info:{} and partInfo:{}", roomId, roomInfo.toString());
        String key = CacheKeyConstants.getConferencesKey(roomId);
        roomRedisTemplate.opsForHash().putAll(key, roomInfo);
        roomRedisTemplate.expire(key, CacheKeyConstants.DEFAULT_CONFERENCE_EXPIRE, TimeUnit.SECONDS);
    }

    @Override
    public void savePartInfo(String uuid, Map<String, Object> partInfo) {
        log.info("save part info:{} and partInfo:{}", uuid, partInfo.toString());
        String key = CacheKeyConstants.getParticipantKey(uuid);
        roomRedisTemplate.opsForHash().putAll(key, partInfo);
        roomRedisTemplate.expire(key, CacheKeyConstants.DEFAULT_CONFERENCE_EXPIRE, TimeUnit.SECONDS);
    }

    @Override
    public void delPartInfo(String uuid) {
        log.info("del part info:{}", uuid);
        roomRedisTemplate.delete(CacheKeyConstants.getParticipantKey(uuid));
    }

    @Override
    public void delRoomInfo(String sessionId) {
        log.info("del part info:{}", sessionId);
        roomRedisTemplate.delete(CacheKeyConstants.getConferencesKey(sessionId));
    }

    @Override
    public Map getPartInfo(String userUuid) {
        return roomRedisTemplate.opsForHash().entries(CacheKeyConstants.getParticipantKey(userUuid));
    }

    @Override
    public Map getRoomInfo(String roomId) {
        return roomRedisTemplate.opsForHash().entries(CacheKeyConstants.getConferencesKey(roomId));
    }

    @Override
    public void updatePartInfo(String uuid, String key, Object updateInfo) {
        log.info("update part info:{} and key:{}, status:{}", uuid, key, updateInfo.toString());
        roomRedisTemplate.opsForHash().put(CacheKeyConstants.getParticipantKey(uuid), key, updateInfo);
    }
}
