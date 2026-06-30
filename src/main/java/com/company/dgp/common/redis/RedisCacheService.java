package com.company.dgp.common.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.time.Duration;
import java.util.Optional;

@Service
public class RedisCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public void setString(String key, String value, Duration ttl) {
        stringRedisTemplate.opsForValue().set(RedisKeys.cache(key), value, ttl);
    }

    public Optional<String> getString(String key) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(RedisKeys.cache(key)));
    }

    public <T> void setJson(String key, T value, Duration ttl) {
        try {
            setString(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("serialize redis cache value failed", exception);
        }
    }

    public <T> Optional<T> getJson(String key, Class<T> valueType) {
        return getString(key).map(value -> {
            try {
                return objectMapper.readValue(value, valueType);
            } catch (JsonProcessingException exception) {
                throw new IllegalArgumentException("deserialize redis cache value failed", exception);
            }
        });
    }

    public void delete(String key) {
        stringRedisTemplate.delete(RedisKeys.cache(key));
    }

    public void deleteAll(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        stringRedisTemplate.delete(keys.stream()
                .map(RedisKeys::cache)
                .toList());
    }
}
