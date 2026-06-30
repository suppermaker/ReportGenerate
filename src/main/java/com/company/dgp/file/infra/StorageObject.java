package com.company.dgp.file.infra;

import java.io.InputStream;

public record StorageObject(
        String objectName,
        String contentType,
        long fileSize,
        InputStream inputStream
) {
}
