package io.openvidu.server.common.cache;

import io.openvidu.server.common.Contants.CacheKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

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
    public boolean accessTokenEverValid(String userId, String token) {
        boolean result;
        try {
            result = token.equals(tokenStringTemplate.opsForHash().entries(CacheKeyConstants.APP_TOKEN_PREFIX_KEY + userId).get("token").toString());
        } catch (Exception e) {
            log.error("Exception:", e);
            return false;
        }
        return result;
    }

}
