package com.company.dgp.file.application;

import com.company.dgp.common.exception.BusinessException;
import com.company.dgp.file.config.FileUploadProperties;
import com.company.dgp.file.domain.FileBizType;
import com.company.dgp.file.domain.FileObject;
import com.company.dgp.file.domain.FileObjectStatus;
import com.company.dgp.file.dto.FileObjectResponse;
import com.company.dgp.file.dto.FileUploadCommand;
import com.company.dgp.file.infra.StorageAdapter;
import com.company.dgp.file.infra.StorageObject;
import com.company.dgp.file.repository.FileObjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Set;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileApplicationService implements FileFacade {

    private final FileObjectRepository fileObjectRepository;
    private final StorageAdapter storageAdapter;
    private final FileUploadProperties fileUploadProperties;

    public FileApplicationService(
            FileObjectRepository fileObjectRepository,
            StorageAdapter storageAdapter,
            FileUploadProperties fileUploadProperties
    ) {
        this.fileObjectRepository = fileObjectRepository;
        this.storageAdapter = storageAdapter;
        this.fileUploadProperties = fileUploadProperties;
    }

    @Override
    @Transactional
    public FileObjectResponse upload(FileUploadCommand command) {
        validateUploadCommand(command, false);
        byte[] content = readAllBytes(command);
        String fileCode = nextFileCode();
        String objectName = defaultIfBlank(command.objectName(), buildDefaultObjectName(command, fileCode));
        String fileHash = sha256(content);
        storageAdapter.upload(new StorageObject(
                objectName,
                command.contentType(),
                content.length,
                new ByteArrayInputStream(content)
        ));
        try {
            FileUploadCommand metadataCommand = new FileUploadCommand(
                    command.bizType(),
                    command.bizId(),
                    command.originalFilename(),
                    command.contentType(),
                    content.length,
                    new ByteArrayInputStream(content),
                    command.uploadedBy(),
                    objectName
            );
            return saveMetadata(metadataCommand, fileCode, storageAdapter.bucketName(), fileHash);
        } catch (RuntimeException exception) {
            storageAdapter.delete(objectName);
            throw exception;
        }
    }

    public FileObjectResponse saveMetadata(FileUploadCommand command, String fileCode, String bucketName, String fileHash) {
        validateUploadCommand(command, true);
        FileObject fileObject = new FileObject();
        fileObject.setFileCode(fileCode);
        fileObject.setBizType(command.bizType());
        fileObject.setBizId(command.bizId());
        fileObject.setOriginalFilename(command.originalFilename());
        fileObject.setObjectName(command.objectName());
        fileObject.setBucketName(bucketName);
        fileObject.setContentType(command.contentType());
        fileObject.setFileExt(resolveFileExt(command.originalFilename()));
        fileObject.setFileSize(command.fileSize());
        fileObject.setFileHash(fileHash);
        fileObject.setStorageProvider("MINIO");
        fileObject.setStatus(FileObjectStatus.ACTIVE);
        fileObject.setUploadedBy(command.uploadedBy());
        fileObject.setUploadedAt(LocalDateTime.now());
        return FileObjectResponse.from(fileObjectRepository.save(fileObject));
    }

    @Override
    public FileObjectResponse getFile(Long fileId) {
        FileObject fileObject = fileObjectRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(4041, "file object not found"));
        return FileObjectResponse.from(fileObject);
    }

    @Override
    public String generateDownloadUrl(Long fileId, Duration expiry) {
        FileObject fileObject = fileObjectRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(4041, "file object not found"));
        if (!FileObjectStatus.ACTIVE.equals(fileObject.getStatus())) {
            throw new BusinessException(4001, "file object is not active");
        }
        return storageAdapter.generatePresignedGetUrl(fileObject.getObjectName(), expiry);
    }

    @Override
    @Transactional
    public void delete(Long fileId) {
        FileObject fileObject = fileObjectRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(4041, "file object not found"));
        fileObjectRepository.updateStatus(fileObject.getId(), FileObjectStatus.DELETED);
    }

    @Override
    public void deleteObject(Long fileId) {
        FileObject fileObject = fileObjectRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(4041, "file object not found"));
        storageAdapter.delete(fileObject.getObjectName());
        fileObjectRepository.updateStatus(fileObject.getId(), FileObjectStatus.DELETED);
    }

    private void validateUploadCommand(FileUploadCommand command, boolean requireObjectName) {
        if (command == null || command.bizType() == null || command.bizType().isBlank()) {
            throw new BusinessException(4001, "file biz type is required");
        }
        if (command.originalFilename() == null || command.originalFilename().isBlank()) {
            throw new BusinessException(4001, "original filename is required");
        }
        if (requireObjectName && (command.objectName() == null || command.objectName().isBlank())) {
            throw new BusinessException(4001, "object name is required");
        }
        if (!validBizTypes().contains(command.bizType())) {
            throw new BusinessException(4001, "file biz type is invalid");
        }
        if (command.fileSize() <= 0) {
            throw new BusinessException(4001, "file size is invalid");
        }
        if (command.fileSize() > fileUploadProperties.getMaxSize()) {
            throw new BusinessException(4001, "file size exceeds limit");
        }
        String fileExt = resolveFileExt(command.originalFilename());
        if (!fileUploadProperties.getAllowedExtensions().contains(fileExt)) {
            throw new BusinessException(4001, "file extension is not allowed");
        }
    }

    private Set<String> validBizTypes() {
        return Set.of(
                FileBizType.TEMPLATE,
                FileBizType.REFERENCE,
                FileBizType.REPORT,
                FileBizType.PREVIEW,
                FileBizType.OTHER
        );
    }

    private byte[] readAllBytes(FileUploadCommand command) {
        try {
            return command.inputStream().readAllBytes();
        } catch (IOException exception) {
            throw new BusinessException(4002, "read upload file failed");
        }
    }

    private String buildDefaultObjectName(FileUploadCommand command, String fileCode) {
        String filename = command.originalFilename();
        if (FileBizType.REFERENCE.equals(command.bizType())) {
            return "references/%s/%s/%s/%s".formatted(
                    nullableSegment(command.uploadedBy()),
                    nullableSegment(command.bizId()),
                    fileCode,
                    filename
            );
        }
        if (FileBizType.REPORT.equals(command.bizType())) {
            return "reports/%s/%s/%s".formatted(nullableSegment(command.bizId()), fileCode, filename);
        }
        if (FileBizType.PREVIEW.equals(command.bizType())) {
            return "preview/%s/%s/%s".formatted(nullableSegment(command.bizId()), fileCode, filename);
        }
        if (FileBizType.TEMPLATE.equals(command.bizType())) {
            return "templates/%s/%s/%s".formatted(nullableSegment(command.bizId()), fileCode, filename);
        }
        return "files/%s/%s".formatted(fileCode, filename);
    }

    private String nullableSegment(Object value) {
        return value == null ? "unknown" : value.toString();
    }

    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(5004, "calculate file hash failed");
        }
    }

    private String nextFileCode() {
        return "F" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String resolveFileExt(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
