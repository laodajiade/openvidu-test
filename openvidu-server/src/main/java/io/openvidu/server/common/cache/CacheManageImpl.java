package io.openvidu.server.common.cache;

import io.openvidu.server.common.contants.CacheKeyConstants;
import io.openvidu.server.common.enums.UserOnlineStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author chosongi
 * @date 2019/9/12 14:50
 */
@Slf4j
@Component
public class CacheManageImpl implements CacheManage {

    @Resource(name = "tokenStringTemplate")
    private StringRedisTemplate tokenStringTemplate;

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
    public void updateUserOnlineStatus(String uuid, UserOnlineStatusEnum onlineStatusEnum) {
        if (StringUtils.isEmpty(uuid)) return;
        tokenStringTemplate.opsForHash().put(CacheKeyConstants.APP_TOKEN_PREFIX_KEY + uuid, "status", onlineStatusEnum.name());
    }

    @Override
    public void updateReconnectInfo(String userUuid, String privateId) {
        tokenStringTemplate.opsForHash().put(CacheKeyConstants.APP_TOKEN_PREFIX_KEY + userUuid, "reconnect", privateId);
    }

}
