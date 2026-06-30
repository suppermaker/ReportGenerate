package com.company.dgp.file.dto;

import java.io.InputStream;

public record FileUploadCommand(
        String bizType,
        Long bizId,
        String originalFilename,
        String contentType,
        long fileSize,
        InputStream inputStream,
        Long uploadedBy,
        String objectName
) {
}
