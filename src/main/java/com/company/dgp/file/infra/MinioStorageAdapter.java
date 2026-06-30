package com.company.dgp.file.infra;

import com.company.dgp.common.exception.BusinessException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@EnableConfigurationProperties(MinioStorageProperties.class)
public class MinioStorageAdapter implements StorageAdapter {

    private final MinioStorageProperties properties;
    private final MinioClient minioClient;

    public MinioStorageAdapter(MinioStorageProperties properties) {
        this.properties = properties;
        this.minioClient = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Override
    public String bucketName() {
        return properties.getBucket();
    }

    @Override
    public void upload(StorageObject storageObject) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(storageObject.objectName())
                    .stream(storageObject.inputStream(), storageObject.fileSize(), -1)
                    .contentType(storageObject.contentType())
                    .build());
        } catch (Exception exception) {
            throw new BusinessException(5001, "upload file to object storage failed");
        }
    }

    @Override
    public void delete(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .build());
        } catch (Exception exception) {
            throw new BusinessException(5002, "delete file from object storage failed");
        }
    }

    @Override
    public String generatePresignedGetUrl(String objectName, Duration expiry) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .expiry((int) expiry.toSeconds())
                    .build());
        } catch (Exception exception) {
            throw new BusinessException(5003, "generate presigned file url failed");
        }
    }
}
