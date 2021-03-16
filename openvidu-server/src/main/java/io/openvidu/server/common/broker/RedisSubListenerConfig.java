package io.openvidu.server.common.broker;

import io.openvidu.server.common.constants.BrokerChannelConstans;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Slf4j
@Configuration
public class RedisSubListenerConfig {

    @Bean
    @Primary
    RedisMessageListenerContainer container(RedisConnectionFactory tokenRedisConnectionFactory,
                                            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(tokenRedisConnectionFactory);

        container.addMessageListener(listenerAdapter, new PatternTopic(BrokerChannelConstans.DEVICE_UPGRADE_CHANNEL));
        container.addMessageListener(listenerAdapter, new PatternTopic(BrokerChannelConstans.USER_DELETE_CHANNEL));
        container.addMessageListener(listenerAdapter, new PatternTopic(BrokerChannelConstans.CORP_SERVICE_EXPIRED_CHANNEL));
        container.addMessageListener(listenerAdapter, new PatternTopic(BrokerChannelConstans.CORP_INFO_MODIFIED_CHANNEL));
        container.addMessageListener(listenerAdapter, new PatternTopic(BrokerChannelConstans.DEVICE_LOG_UPLOAD_CHANNEL));
        container.addMessageListener(listenerAdapter, new PatternTopic(BrokerChannelConstans.DEVICE_NAME_UPDATE_CHANNEL));
        log.info("Meeting Control Center now subscribe to the redis channel ==> {}", BrokerChannelConstans.DEVICE_UPGRADE_CHANNEL);
        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(RedisSubscriber redisSubscriber) {
        return new MessageListenerAdapter(redisSubscriber, "receiveMessage");
    }

    @Bean(name = "roomRedisMessageListenerContainer")
    RedisMessageListenerContainer roomContainer(@Qualifier("roomRedisConnectionFactory") RedisConnectionFactory roomRedisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(roomRedisConnectionFactory);
        return container;
    }

    @Bean
    RedisKeyExpirationListener roomRedisKeyExpirationListener(
            @Qualifier("roomRedisMessageListenerContainer") RedisMessageListenerContainer redisMessageListenerContainer) {
        return new RedisKeyExpirationListener(redisMessageListenerContainer);
    }

}
