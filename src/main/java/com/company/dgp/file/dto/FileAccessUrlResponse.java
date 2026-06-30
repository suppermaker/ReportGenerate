package com.company.dgp.file.dto;

import java.time.OffsetDateTime;

public record FileAccessUrlResponse(
        String url,
        Integer expireSeconds,
        String usage,
        OffsetDateTime expireAt
) {
}
