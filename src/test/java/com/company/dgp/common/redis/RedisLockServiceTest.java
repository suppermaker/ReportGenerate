package com.company.dgp.common.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisLockServiceTest {

    @Test
    void tryLockReturnsTrueWhenRedisSetIfAbsentSucceeds() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(RedisKeys.lock("task:1")), eq("v1"), eq(Duration.ofSeconds(5))))
                .thenReturn(true);

        RedisLockService lockService = new RedisLockService(redisTemplate);

        assertThat(lockService.tryLock("task:1", "v1", Duration.ofSeconds(5))).isTrue();
    }

    @Test
    void unlockReturnsFalseWhenLuaScriptDoesNotDeleteKey() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<DefaultRedisScript<Long>>any(),
                eq(List.of(RedisKeys.lock("task:1"))),
                eq("other")
        )).thenReturn(0L);

        RedisLockService lockService = new RedisLockService(redisTemplate);

        assertThat(lockService.unlock("task:1", "other")).isFalse();
    }
}
