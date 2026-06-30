package com.company.dgp.template.dto;

import java.io.InputStream;

public record TemplateCreateCommand(
        String templateCode,
        String templateName,
        String reportType,
        String versionNo,
        String description,
        String originalFilename,
        String contentType,
        long fileSize,
        InputStream inputStream,
        Long createdBy
) {
}
