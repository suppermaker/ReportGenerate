package com.company.dgp.common.redis;

public final class RedisKeys {

    public static final String PREFIX = "dgp:";
    public static final String GENERATE_TASK_QUEUE = PREFIX + "queue:generate-task";

    private RedisKeys() {
    }

    public static String cache(String key) {
        return PREFIX + "cache:" + key;
    }

    public static String lock(String key) {
        return PREFIX + "lock:" + key;
    }

    public static String template(Long templateId) {
        return "template:" + templateId;
    }

    public static String file(Long fileId) {
        return "file:" + fileId;
    }

    public static String taskLock(String taskCode) {
        return "task:" + taskCode;
    }
}
