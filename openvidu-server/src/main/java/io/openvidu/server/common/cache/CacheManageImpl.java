package io.openvidu.server.common.cache;

import io.openvidu.server.common.constants.CacheKeyConstants;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.enums.UserOnlineStatusEnum;
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
    public void updateTerminalStatus(String userUuid, UserOnlineStatusEnum userOnlineStatus, String serialNumber, DeviceStatus deviceStatus) {
        if (!StringUtils.isEmpty(userUuid)) {
            log.info("Update user online status in cache. uuid:{}, updateStatus:{}", userUuid, userOnlineStatus.name());
            tokenStringTemplate.opsForHash().put(CacheKeyConstants.APP_TOKEN_PREFIX_KEY + userUuid, "status", userOnlineStatus.name());
        }

        if (!StringUtils.isEmpty(serialNumber)) {
            log.info("Update device online status in cache. serialNumber:{}, updateStatus:{}", serialNumber, deviceStatus.name());
            tokenStringTemplate.opsForValue().set(CacheKeyConstants.DEV_PREFIX_KEY + serialNumber, deviceStatus.name());
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
        String key = CacheKeyConstants.getConferencesKey(roomId);
        roomRedisTemplate.opsForHash().putAll(key, roomInfo);
        roomRedisTemplate.expire(key, CacheKeyConstants.DEFAULT_CONFERENCE_EXPIRE, TimeUnit.SECONDS);
    }

    @Override
    public void savePartInfo(String uuid, Map<String, Object> partInfo) {
        String key = CacheKeyConstants.getParticipantKey(uuid);
        roomRedisTemplate.opsForHash().putAll(key, partInfo);
        roomRedisTemplate.expire(key, CacheKeyConstants.DEFAULT_CONFERENCE_EXPIRE, TimeUnit.SECONDS);
    }

    @Override
    public void delPartInfo(String uuid) {
        roomRedisTemplate.delete(CacheKeyConstants.getParticipantKey(uuid));
    }

    @Override
    public void delRoomInfo(String sessionId) {
        roomRedisTemplate.delete(CacheKeyConstants.getConferencesKey(sessionId));
    }
}
