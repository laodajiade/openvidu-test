package io.openvidu.server.common.cache;

import io.openvidu.server.common.constants.BrokerChannelConstans;
import io.openvidu.server.common.constants.CacheKeyConstants;
import io.openvidu.server.common.enums.AccessTypeEnum;
import io.openvidu.server.common.enums.TerminalStatus;
import io.openvidu.server.common.enums.TerminalTypeEnum;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author geedow
 * @date 2019/9/12 14:50
 */
@Slf4j
@Component
public class CacheManageImpl implements CacheManage {

    @Value("${hdc.retain.in.room.interval}")
    private long hdcRetainInRoomInterval;

    @Value("${other.terminal.retain.in.room.interval}")
    private long otherTerminalRetainInRoomInterval;

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

        boolean updateDevStatus = false;
        if (!StringUtils.isEmpty(rpcConnection.getUserUuid())) {
            BoundHashOperations<String, String, Object> boundHashOperations =
                    tokenStringTemplate.boundHashOps(CacheKeyConstants.APP_TOKEN_PREFIX_KEY + rpcConnection.getUserUuid());

            Object preUpdateTime = boundHashOperations.get("updateTime");
            if (Objects.isNull(preUpdateTime) || rpcConnection.getCreateTime().compareTo(Long.valueOf(preUpdateTime.toString())) >= 0) {
                boundHashOperations.put("status", terminalStatus.name());
                boundHashOperations.put("updateTime", String.valueOf(rpcConnection.getCreateTime()));
                log.info("Update user online status in cache. uuid:{}, updateStatus:{}, updateTime:{}",
                        rpcConnection.getUserUuid(), terminalStatus.name(), rpcConnection.getCreateTime());

                updateDevStatus = true;
            } else {
                log.info("RpcConnection:{} is not the latest, its createTime:{} and preCreateTime:{}",
                        rpcConnection.getParticipantPrivateId(), rpcConnection.getCreateTime(), preUpdateTime);
            }
        }

        if (!StringUtils.isEmpty(rpcConnection.getSerialNumber()) && updateDevStatus) {
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
        log.info("del room info:{}", sessionId);
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

    @Override
    public boolean existsConferenceRelativeInfo(String key) {
        return roomRedisTemplate.hasKey(key);
    }

    @Override
    public Long getMaxConcurrentOfDayAndDel(String project, Date queryEndTime) {
        Boolean exists;
        String key = CacheKeyConstants.getMaxConcurrentStatisticKey(project, queryEndTime);
        if (Objects.nonNull(exists = roomRedisTemplate.hasKey(key)) && exists) {
            Long result = Long.valueOf(String.valueOf(roomRedisTemplate.opsForValue().get(key)));
            roomRedisTemplate.delete(key);
            return result;
        }
        return 0L;
    }

    @Override
    public void updateMaxConcurrentOfDay(int partSize, String project) {
        Boolean exists;
        String key = CacheKeyConstants.getMaxConcurrentStatisticKey(project, null);
        if (Objects.nonNull(exists = roomRedisTemplate.hasKey(key)) && exists) {
            // compare the original value and set the new value if necessary
            if (Integer.parseInt(String.valueOf(roomRedisTemplate.opsForValue().get(key))) < partSize) {
                roomRedisTemplate.opsForValue().set(key, partSize);
            }
        } else {
            // set the initial value of today
            roomRedisTemplate.opsForValue().set(key, partSize, 172800, TimeUnit.SECONDS);
        }
    }

    @Override
    public void recordSubscriberSetRollCall(String sessionId, Long startTime, String uuid) {
        log.info("record down wall part:{} set roll call in session:{} and session start time:{}", uuid, sessionId, startTime);
        String key;
        roomRedisTemplate.opsForValue().increment(key = CacheKeyConstants.getSubscriberSetRollCallKey(sessionId, startTime, uuid));
        roomRedisTemplate.expire(key, 30, TimeUnit.SECONDS);

    }

    @Override
    public void delConferenceRelativeKey(String key) {
        roomRedisTemplate.delete(key);
    }

    @Override
    public void recordWsExceptionLink(RpcConnection rpc, boolean overKeepAlive) {
        long expireTime = TerminalTypeEnum.HDC.equals(rpc.getTerminalType()) ? hdcRetainInRoomInterval : otherTerminalRetainInRoomInterval;
        expireTime = overKeepAlive ? expireTime - 20 : expireTime;
        roomRedisTemplate.opsForValue().set(String.format(BrokerChannelConstans.CLIENT_WS_EXCEPTION_KEY, rpc.getSessionId(), rpc.getParticipantPrivateId()),
                1, expireTime, TimeUnit.SECONDS);
    }

    @Override
    public String getTerminalStatus(String uuid) {
        Map tokenInfo = getUserInfoByUUID(uuid);
        Object status;
        return !tokenInfo.isEmpty() && !Objects.isNull(status = tokenInfo.get("status")) ?
                String.valueOf(status) : TerminalStatus.offline.name();
    }

    @Override
    public void setLogUploadToken(String uuid, String uploadToken) {
        String key;
        tokenStringTemplate.opsForValue().set((key = CacheKeyConstants.LOG_UPLOAD_TOKEN_PREFIX_KEY + uuid), uploadToken);
        // set expired time
        tokenStringTemplate.expire(key, 900, TimeUnit.SECONDS);
    }

    @Override
    public void batchUpdatePartInfo(String uuid, Map<String, Object> updateMap) {
        if (Objects.nonNull(updateMap) && !updateMap.isEmpty()) {
            BoundHashOperations<String, String, Object> boundHashOperations =
                    roomRedisTemplate.boundHashOps(CacheKeyConstants.getParticipantKey(uuid));
            updateMap.forEach(boundHashOperations::put);
        }
    }

    @Override
    public void saveInviteInfo(String sessionId, String entryValue) {
        String key = CacheKeyConstants.getConferencesInviteKey(sessionId);
        tokenStringTemplate.opsForHash().put(key, entryValue, entryValue);
        tokenStringTemplate.expire(key, 60, TimeUnit.SECONDS);
    }

    @Override
    public Map getInviteInfo(String sessionId) {
        String key = CacheKeyConstants.getConferencesInviteKey(sessionId);
        return tokenStringTemplate.opsForHash().entries(key);
    }

    @Override
    public void saveAccessInParticipantPrivateId(String uuid, String privateId) {
        String key = CacheKeyConstants.ACCESSIN_PRIVATEID_PREFIX_KEY + uuid;
        tokenStringTemplate.opsForValue().set(key, privateId);
        tokenStringTemplate.expire(key, CacheKeyConstants.DEFAULT_CONFERENCE_EXPIRE, TimeUnit.SECONDS);
    }

    @Override
    public String getAccessInParticipantPrivateId(String uuid) {
        String key = CacheKeyConstants.ACCESSIN_PRIVATEID_PREFIX_KEY + uuid;
        return tokenStringTemplate.opsForValue().get(key);
    }

    @Override
    public void delAccessInParticipantPrivateId(String uuid) {
        String key = CacheKeyConstants.ACCESSIN_PRIVATEID_PREFIX_KEY + uuid;
        tokenStringTemplate.delete(key);
    }


    public boolean getCorpExpired(String project) {
        return tokenStringTemplate.opsForSet().isMember("corp_expired_col", project);
    }

    public void setCorpExpired(Set<String> projects) {
        dropCorpExpiredCollect();
        String key = "corp_expired_col";
        tokenStringTemplate.opsForSet().add(key, projects.toArray(new String[0]));
    }

    public void dropCorpExpiredCollect(){
        String key = "corp_expired_col";
        tokenStringTemplate.delete(key);
    }

    @Override
    public void publish(String channel, String message) {
        tokenStringTemplate.convertAndSend(channel, message);
    }
}
