package io.openvidu.server.common.broker;

import io.openvidu.server.common.Contants.BrokerChannelConstans;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisSubListenerConfig {

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory tokenRedisConnectionFactory,
                                            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(tokenRedisConnectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic(BrokerChannelConstans.CLIENT_REQUEST_CHANNEL));
        container.addMessageListener(listenerAdapter, new PatternTopic(BrokerChannelConstans.CLIENT_NOTIFY_CHANNEL));
        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(RedisSubscriber redisSubscriber) {
        return new MessageListenerAdapter(redisSubscriber, "receiveMessage");
    }

}
