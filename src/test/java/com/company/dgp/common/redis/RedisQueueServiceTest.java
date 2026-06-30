package com.company.dgp.common.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisQueueServiceTest {

    @Test
    void sizeReturnsZeroWhenRedisReturnsNull() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ListOperations<String, String> listOperations = mock(ListOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.size("queue")).thenReturn(null);

        RedisQueueService service = new RedisQueueService(redisTemplate);

        assertThat(service.size("queue")).isZero();
    }

    @Test
    void rightPushAllSkipsEmptyValues() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisQueueService service = new RedisQueueService(redisTemplate);

        service.rightPushAll("queue", List.of());

        verify(redisTemplate, org.mockito.Mockito.never()).opsForList();
    }

    @Test
    void leftPopWithoutTimeoutDelegatesToRedis() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ListOperations<String, String> listOperations = mock(ListOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop("queue")).thenReturn("v1");

        RedisQueueService service = new RedisQueueService(redisTemplate);

        assertThat(service.leftPop("queue")).contains("v1");
    }
}
