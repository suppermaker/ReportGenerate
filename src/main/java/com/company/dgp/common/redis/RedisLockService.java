package com.company.dgp.common.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class RedisLockService {

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    public RedisLockService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String newLockValue() {
        return UUID.randomUUID().toString();
    }

    public boolean tryLock(String key, String lockValue, Duration ttl) {
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(RedisKeys.lock(key), lockValue, ttl);
        return Boolean.TRUE.equals(success);
    }

    public boolean unlock(String key, String lockValue) {
        Long result = stringRedisTemplate.execute(RELEASE_SCRIPT, List.of(RedisKeys.lock(key)), lockValue);
        return Long.valueOf(1).equals(result);
    }
}
