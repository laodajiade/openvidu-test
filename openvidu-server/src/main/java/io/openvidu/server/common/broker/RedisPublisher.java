package io.openvidu.server.common.broker;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class RedisPublisher {

    @Resource(name = "tokenStringTemplate")
    private StringRedisTemplate template;

    public void sendChnMsg(String channelName, String message) {
        template.convertAndSend(channelName, message);
    }
}
