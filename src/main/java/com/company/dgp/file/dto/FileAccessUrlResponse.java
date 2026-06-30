package com.company.dgp.file.dto;

public record FileAccessUrlResponse(
        String url,
        Integer expireSeconds,
        String usage
) {
}
