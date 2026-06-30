package com.company.dgp.file.dto;

import com.company.dgp.file.domain.FileObject;

public record FileObjectResponse(
        Long id,
        String fileCode,
        String bizType,
        Long bizId,
        String originalFilename,
        String objectName,
        String bucketName,
        String contentType,
        String fileExt,
        Long fileSize,
        String fileHash,
        String status
) {

    public static FileObjectResponse from(FileObject fileObject) {
        return new FileObjectResponse(
                fileObject.getId(),
                fileObject.getFileCode(),
                fileObject.getBizType(),
                fileObject.getBizId(),
                fileObject.getOriginalFilename(),
                fileObject.getObjectName(),
                fileObject.getBucketName(),
                fileObject.getContentType(),
                fileObject.getFileExt(),
                fileObject.getFileSize(),
                fileObject.getFileHash(),
                fileObject.getStatus()
        );
    }
}
