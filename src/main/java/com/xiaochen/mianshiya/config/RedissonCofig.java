package com.xiaochen.mianshiya.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JacksonCodec;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Redisson 配置
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonCofig {

    private Integer port;

    private String host;

    private String password;

    private Integer database;

    @Bean
    public RedissonClient redissonClient() {
//
        Config config = new Config();
        config.useSingleServer()
                .setPassword(password)
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database);
//        config.setCodec(new JsonJacksonCodec());
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }

}
