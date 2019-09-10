package io.openvidu.server.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class TokenRedis extends RedisCommonConfig {

    @Value("${token.redis.host}")
    private String hostName;

    @Value("${token.redis.port}")
    private int port;

    @Value("${token.redis.password}")
    private String password;

    @Value("${token.redis.database}")
    private int database;

    @Bean(name = "jedisPoolConfig")
    @ConfigurationProperties(prefix = "redis.pool")
    public JedisPoolConfig jedisPoolConfig() {
        return new JedisPoolConfig();
    }

    @Primary
    @Bean(name={"tokenRedisConnectionFactory"})
    public RedisConnectionFactory tokenRedisConnectionFactory(@Qualifier("jedisPoolConfig") JedisPoolConfig jedisPoolConfig) {
        return createRedisConnectionFactory(this.hostName, this.port, this.password, this.database, jedisPoolConfig);
    }

    @Bean(name={"tokenStringTemplate"})
    public StringRedisTemplate tokenStringTemplate
            (@Qualifier("tokenRedisConnectionFactory") RedisConnectionFactory tokenRedisConnectionFactory) {
        return getStringRedisTemplate(tokenRedisConnectionFactory);
    }


    @Bean(name={"tokenRedisTemplate"})
    public RedisTemplate<Object, Object> tokenRedisTemplate
            (@Qualifier("tokenRedisConnectionFactory") RedisConnectionFactory tokenRedisConnectionFactory) {
        return getRedisTemplate(tokenRedisConnectionFactory);
    }
}
