package com.company.dgp.common.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class RedisQueueService {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisQueueService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void pushGenerateTask(String taskCode) {
        rightPush(RedisKeys.GENERATE_TASK_QUEUE, taskCode);
    }

    public Optional<String> popGenerateTask(Duration timeout) {
        return leftPop(RedisKeys.GENERATE_TASK_QUEUE, timeout);
    }

    public void rightPush(String queueKey, String value) {
        stringRedisTemplate.opsForList().rightPush(queueKey, value);
    }

    public Optional<String> leftPop(String queueKey, Duration timeout) {
        return Optional.ofNullable(stringRedisTemplate.opsForList().leftPop(queueKey, timeout));
    }
}
