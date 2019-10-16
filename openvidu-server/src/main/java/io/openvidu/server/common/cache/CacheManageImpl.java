package io.openvidu.server.common.cache;

import io.openvidu.server.common.Contants.CacheKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

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

}
