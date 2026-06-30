package com.company.dgp.file.application;

import com.company.dgp.file.domain.FileObject;
import com.company.dgp.file.config.FileUploadProperties;
import com.company.dgp.file.dto.FileObjectResponse;
import com.company.dgp.file.dto.FileUploadCommand;
import com.company.dgp.file.infra.StorageAdapter;
import com.company.dgp.file.infra.StorageObject;
import com.company.dgp.file.repository.FileObjectRepository;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileApplicationServiceTest {

    @Test
    void uploadStoresObjectThenSavesMetadata() {
        FileObjectRepository repository = mock(FileObjectRepository.class);
        StorageAdapter storageAdapter = mock(StorageAdapter.class);
        when(storageAdapter.bucketName()).thenReturn("report-generate");
        when(repository.save(any(FileObject.class))).thenAnswer(invocation -> {
            FileObject fileObject = invocation.getArgument(0);
            fileObject.setId(10L);
            return fileObject;
        });

        FileApplicationService service = new FileApplicationService(repository, storageAdapter, new FileUploadProperties());

        FileObjectResponse response = service.upload(command());

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.fileExt()).isEqualTo("docx");
        assertThat(response.fileHash()).isEqualTo("sha256:039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81");
        verify(storageAdapter).upload(any(StorageObject.class));
        verify(repository).save(any(FileObject.class));
    }

    @Test
    void generateDownloadUrlDelegatesToStorageAdapter() {
        FileObject fileObject = new FileObject();
        fileObject.setId(10L);
        fileObject.setStatus("ACTIVE");
        fileObject.setObjectName("templates/demo.docx");

        FileObjectRepository repository = mock(FileObjectRepository.class);
        StorageAdapter storageAdapter = mock(StorageAdapter.class);
        when(repository.findById(10L)).thenReturn(Optional.of(fileObject));
        when(storageAdapter.generatePresignedGetUrl("templates/demo.docx", Duration.ofMinutes(10)))
                .thenReturn("http://signed-url");

        FileApplicationService service = new FileApplicationService(repository, storageAdapter, new FileUploadProperties());

        assertThat(service.generateDownloadUrl(10L, Duration.ofMinutes(10))).isEqualTo("http://signed-url");
    }

    @Test
    void uploadDeletesObjectWhenMetadataSaveFails() {
        FileObjectRepository repository = mock(FileObjectRepository.class);
        StorageAdapter storageAdapter = mock(StorageAdapter.class);
        when(storageAdapter.bucketName()).thenReturn("report-generate");
        when(repository.save(any(FileObject.class))).thenThrow(new IllegalStateException("db failed"));

        FileApplicationService service = new FileApplicationService(repository, storageAdapter, new FileUploadProperties());

        assertThatThrownBy(() -> service.upload(command()))
                .isInstanceOf(IllegalStateException.class);
        verify(storageAdapter).delete("templates/demo.docx");
    }

    @Test
    void uploadRejectsInvalidBizType() {
        FileApplicationService service = new FileApplicationService(
                mock(FileObjectRepository.class),
                mock(StorageAdapter.class),
                new FileUploadProperties()
        );

        assertThatThrownBy(() -> service.upload(new FileUploadCommand(
                "INVALID",
                null,
                "demo.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                3,
                new ByteArrayInputStream(new byte[]{1, 2, 3}),
                1L,
                "templates/demo.docx"
        ))).hasMessage("file biz type is invalid");
    }

    @Test
    void uploadRejectsEmptyFile() {
        FileApplicationService service = new FileApplicationService(
                mock(FileObjectRepository.class),
                mock(StorageAdapter.class),
                new FileUploadProperties()
        );

        assertThatThrownBy(() -> service.upload(new FileUploadCommand(
                "TEMPLATE",
                null,
                "demo.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                0,
                new ByteArrayInputStream(new byte[]{}),
                1L,
                "templates/demo.docx"
        ))).hasMessage("file size is invalid");
    }

    @Test
    void uploadRejectsDisallowedExtension() {
        FileApplicationService service = new FileApplicationService(
                mock(FileObjectRepository.class),
                mock(StorageAdapter.class),
                new FileUploadProperties()
        );

        assertThatThrownBy(() -> service.upload(new FileUploadCommand(
                "TEMPLATE",
                null,
                "demo.exe",
                "application/octet-stream",
                3,
                new ByteArrayInputStream(new byte[]{1, 2, 3}),
                1L,
                "templates/demo.exe"
        ))).hasMessage("file extension is not allowed");
    }

    @Test
    void deleteOnlyUpdatesMetadataStatus() {
        FileObject fileObject = new FileObject();
        fileObject.setId(10L);
        fileObject.setStatus("ACTIVE");
        fileObject.setObjectName("templates/demo.docx");

        FileObjectRepository repository = mock(FileObjectRepository.class);
        StorageAdapter storageAdapter = mock(StorageAdapter.class);
        when(repository.findById(10L)).thenReturn(Optional.of(fileObject));

        FileApplicationService service = new FileApplicationService(repository, storageAdapter, new FileUploadProperties());

        service.delete(10L);

        verify(repository).updateStatus(10L, "DELETED");
        verify(storageAdapter, never()).delete("templates/demo.docx");
    }

    @Test
    void deleteObjectRemovesStorageObjectForCompensation() {
        FileObject fileObject = new FileObject();
        fileObject.setId(10L);
        fileObject.setStatus("ACTIVE");
        fileObject.setObjectName("templates/demo.docx");

        FileObjectRepository repository = mock(FileObjectRepository.class);
        StorageAdapter storageAdapter = mock(StorageAdapter.class);
        when(repository.findById(10L)).thenReturn(Optional.of(fileObject));

        FileApplicationService service = new FileApplicationService(repository, storageAdapter, new FileUploadProperties());

        service.deleteObject(10L);

        verify(storageAdapter).delete("templates/demo.docx");
        verify(repository).updateStatus(10L, "DELETED");
    }

    private FileUploadCommand command() {
        return new FileUploadCommand(
                "TEMPLATE",
                null,
                "demo.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                3,
                new ByteArrayInputStream(new byte[]{1, 2, 3}),
                1L,
                "templates/demo.docx"
        );
    }
}
