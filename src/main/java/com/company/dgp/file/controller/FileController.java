package com.company.dgp.file.controller;

import com.company.dgp.common.result.ApiResponse;
import com.company.dgp.file.application.FileFacade;
import com.company.dgp.file.dto.FileAccessUrlResponse;
import com.company.dgp.file.dto.FileObjectResponse;
import com.company.dgp.file.dto.FileUploadCommand;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

@Validated
@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final int DEFAULT_EXPIRE_SECONDS = 600;
    private static final int MAX_EXPIRE_SECONDS = 3600;

    private final FileFacade fileFacade;

    public FileController(FileFacade fileFacade) {
        this.fileFacade = fileFacade;
    }

    @PostMapping
    public ApiResponse<FileObjectResponse> upload(
            @RequestParam("file") @NotNull MultipartFile file,
            @RequestParam("bizType") @NotBlank String bizType,
            @RequestParam(value = "bizId", required = false) Long bizId,
            @RequestParam(value = "objectName", required = false) String objectName,
            HttpServletRequest request
    ) throws IOException {
        FileObjectResponse response = fileFacade.upload(new FileUploadCommand(
                bizType,
                bizId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream(),
                null,
                objectName
        ));
        return ApiResponse.success(response, request.getRequestId());
    }

    @GetMapping("/{fileId}")
    public ApiResponse<FileObjectResponse> getFile(@PathVariable Long fileId, HttpServletRequest request) {
        return ApiResponse.success(fileFacade.getFile(fileId), request.getRequestId());
    }

    @GetMapping("/{fileId}/access-url")
    public ApiResponse<FileAccessUrlResponse> accessUrl(
            @PathVariable Long fileId,
            @RequestParam(value = "usage", defaultValue = "DOWNLOAD") String usage,
            @RequestParam(value = "expireSeconds", defaultValue = "600") @Min(1) @Max(MAX_EXPIRE_SECONDS) Integer expireSeconds,
            HttpServletRequest request
    ) {
        int normalizedExpireSeconds = expireSeconds == null ? DEFAULT_EXPIRE_SECONDS : expireSeconds;
        String url = fileFacade.generateDownloadUrl(fileId, Duration.ofSeconds(normalizedExpireSeconds));
        return ApiResponse.success(new FileAccessUrlResponse(url, normalizedExpireSeconds, usage), request.getRequestId());
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> delete(@PathVariable Long fileId, HttpServletRequest request) {
        fileFacade.delete(fileId);
        return ApiResponse.success(null, request.getRequestId());
    }
}
