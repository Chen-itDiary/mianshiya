package com.xiaochen.mianshiya.manager;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CounterManager {
    @Resource
    private RedissonClient redissonClient;

    public long incrAndGetCounter(String key) {
        return incrAndGetCounter(key, 1, TimeUnit.MINUTES);
    }

    public long incrAndGetCounter(String key, int timeInterval, TimeUnit timeUnit) {
        long timeFactor;
        switch (timeUnit) {
            case SECONDS:
                timeFactor = timeInterval;
                break;
            case MINUTES:
                timeFactor = timeInterval * 60;
                break;
            case HOURS:
                timeFactor = timeInterval * 3600;
                break;
            default:
                throw new IllegalArgumentException("不支持的单位");
        }
        return incrAndGetCounter(key, timeInterval, timeUnit, timeFactor);
    }

    public long incrAndGetCounter(String key, int timeInterval, TimeUnit timeUnit, long expirationTime) {
        long timeFactor;
        if (StrUtil.isBlank(key)) {
            return 0;
        }
        switch (timeUnit) {
            case SECONDS:
                timeFactor = Instant.now().getEpochSecond() / timeInterval;
                break;
            case MINUTES:
                timeFactor = Instant.now().getEpochSecond() / timeInterval / 60;
                break;
            case HOURS:
                timeFactor = Instant.now().getEpochSecond() / timeInterval / 3600;
                break;
            default:
                throw new IllegalArgumentException("不支持的单位");
        }
        String redisKey = key + ":" + timeFactor;
        String luaScript =
                "if redis.call('exists', KEYS[1]) == 1 then " +
                        "  return redis.call('incr', KEYS[1]); " +
                        "else " +
                        "  redis.call('set', KEYS[1], 1); " +
                        "  redis.call('expire', KEYS[1], 180); " +  // 设置 180 秒过期时间
                        "  return 1; " +
                        "end";
        RScript script = redissonClient.getScript();
        Object countObj = script.eval(
                RScript.Mode.READ_WRITE,
                luaScript,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(redisKey), expirationTime
        );
        return (Long) countObj;
    }
}
