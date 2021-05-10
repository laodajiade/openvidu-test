package io.openvidu.server.utils;

import org.springframework.data.redis.core.RedisTemplate;

public class RedisUtils {

    private RedisTemplate<String, Object> redisTemplate;

    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(String channal ,Object obj) {
        redisTemplate.convertAndSend(channal,obj );
    }
}
