package io.openvidu.server.common.cache;

import io.openvidu.server.common.Contants.CacheKeyConstants;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author chosongi
 * @date 2019/9/12 14:50
 */

@Component
public class CacheManageImpl implements CacheManage {

    @Resource(name = "tokenRedisTemplate")
    private RedisTemplate<Object, Object> tokenRedisTemplate;


    @Override
    public boolean accessTokenEverValid(String userId, String token) {
        return Objects.equals(tokenRedisTemplate.opsForHash().entries(CacheKeyConstants.APP_TOKEN_PREFIX_KEY + userId).get("token").toString(), token);
    }
}
