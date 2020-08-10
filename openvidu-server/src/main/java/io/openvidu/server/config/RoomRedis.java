package io.openvidu.server.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RoomRedis extends RedisCommonConfig {

    @Value("${room.redis.host}")
    private String hostName;

    @Value("${room.redis.port}")
    private int port;

    @Value("${room.redis.password}")
    private String password;

    @Value("${room.redis.database}")
    private int database;

    @Bean(name={"roomRedisConnectionFactory"})
    public RedisConnectionFactory roomRedisConnectionFactory(@Qualifier("jedisPoolConfig") JedisPoolConfig jedisPoolConfig) {
        return createRedisConnectionFactory(this.hostName, this.port, this.password, this.database, jedisPoolConfig);
    }

    @Bean(name={"roomStringTemplate"})
    public StringRedisTemplate roomStringTemplate(@Qualifier("roomRedisConnectionFactory") RedisConnectionFactory roomRedisConnectionFactory) {
        return getStringRedisTemplate(roomRedisConnectionFactory);
    }

    @Bean(name={"roomRedisTemplate"})
    public RedisTemplate<String, Object> roomRedisTemplate(@Qualifier("roomRedisConnectionFactory") RedisConnectionFactory roomRedisConnectionFactory) {
        return getRedisTemplate(roomRedisConnectionFactory);
    }

}
