package io.openvidu.server.common.broker;

import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import io.openvidu.server.common.constants.BrokerChannelConstans;
import io.openvidu.server.common.redis.RecordingRedisSubscribe;
import io.openvidu.server.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.concurrent.CountDownLatch;

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
        container.addMessageListener(listenerAdapter,new PatternTopic(BrokerChannelConstans.TOPIC_ROOM_RECORDER_ERROR));
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

    /**
     * 注册订阅者
     * @param latch
     * @return
     */
    @Bean
    RecordingRedisSubscribe receiver(CountDownLatch latch) {
        return new RecordingRedisSubscribe(latch);
    }

    /**
     * 计数器，用来控制线程
     * @return
     */
    @Bean
    public CountDownLatch latch(){
        //指定了计数的次数 1
        return new CountDownLatch(1);
    }

    /**
     * 实例化 RedisTemplate 对象
     *
     * @return
     */
    @Bean("RedisTemplateS")
    public RedisTemplate<String, Object> functionDomainRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        initDomainRedisTemplate(redisTemplate, redisConnectionFactory);
        return redisTemplate;
    }

    /**
     * 设置数据存入 redis 的序列化方式,并开启事务
     *
     * @param redisTemplate
     * @param factory
     */
    private void initDomainRedisTemplate(@Qualifier("RedisTemplateS") RedisTemplate<String, Object> redisTemplate, RedisConnectionFactory factory) {
        // 如果不配置Serializer，那么存储的时候缺省使用String，如果用User类型存储，那么会提示错误User can't cast to
        // String！
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        FastJsonRedisSerializer<Object> fastJsonRedisSerializer = new FastJsonRedisSerializer<Object>(Object.class);
        redisTemplate.setHashValueSerializer(fastJsonRedisSerializer);
        redisTemplate.setValueSerializer(fastJsonRedisSerializer);
        // 开启事务
        redisTemplate.setEnableTransactionSupport(true);
        redisTemplate.setConnectionFactory(factory);
    }

    /**
     * 注入封装RedisTemplate
     *
     */
    @Bean(name = "redisUtils")
    public RedisUtils redisUtil(@Qualifier("RedisTemplateS") RedisTemplate<String, Object> redisTemplate) {
        RedisUtils redisUtil = new RedisUtils();
        redisUtil.setRedisTemplate(redisTemplate);
        return redisUtil;
    }

}
