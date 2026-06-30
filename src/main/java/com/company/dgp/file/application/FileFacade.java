package com.company.dgp.file.application;

import com.company.dgp.file.dto.FileObjectResponse;
import com.company.dgp.file.dto.FileUploadCommand;

import java.time.Duration;

public interface FileFacade {

    FileObjectResponse upload(FileUploadCommand command);

    FileObjectResponse getFile(Long fileId);

    String generateDownloadUrl(Long fileId, Duration expiry);

    void delete(Long fileId);

    void deleteObject(Long fileId);
}
