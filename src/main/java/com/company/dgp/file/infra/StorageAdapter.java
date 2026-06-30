package com.company.dgp.file.infra;

import java.time.Duration;

public interface StorageAdapter {

    String bucketName();

    void upload(StorageObject storageObject);

    void delete(String objectName);

    String generatePresignedGetUrl(String objectName, Duration expiry);
}
