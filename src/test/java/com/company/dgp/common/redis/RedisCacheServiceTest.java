package com.company.dgp.common.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisCacheServiceTest {

    @Test
    void deleteAllUsesCacheKeyPrefix() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisCacheService service = new RedisCacheService(redisTemplate, new ObjectMapper());

        service.deleteAll(List.of("k1", "k2"));

        verify(redisTemplate).delete(List.of(RedisKeys.cache("k1"), RedisKeys.cache("k2")));
    }
}
