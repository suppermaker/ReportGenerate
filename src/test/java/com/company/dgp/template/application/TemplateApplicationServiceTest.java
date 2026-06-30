package com.company.dgp.template.application;

import com.company.dgp.file.application.FileFacade;
import com.company.dgp.file.dto.FileObjectResponse;
import com.company.dgp.file.dto.FileUploadCommand;
import com.company.dgp.common.exception.BusinessException;
import com.company.dgp.common.result.PageResult;
import com.company.dgp.template.domain.Template;
import com.company.dgp.template.domain.TemplateStatus;
import com.company.dgp.template.dto.TemplateCreateCommand;
import com.company.dgp.template.dto.TemplateResponse;
import com.company.dgp.template.dto.TemplateVariableUpdateRequest;
import com.company.dgp.template.parser.TemplateVariableParser;
import com.company.dgp.template.repository.TemplateRepository;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemplateApplicationServiceTest {

    @Test
    void createTemplateUploadsFileAndSavesParsedVariables() {
        TemplateRepository repository = mock(TemplateRepository.class);
        TemplateVariableParser parser = mock(TemplateVariableParser.class);
        FileFacade fileFacade = mock(FileFacade.class);
        when(fileFacade.upload(any(FileUploadCommand.class))).thenReturn(fileResponse());
        when(parser.parse(any())).thenReturn(List.of("projectName", "reportDate"));
        when(repository.save(any(Template.class))).thenAnswer(invocation -> {
            Template template = invocation.getArgument(0);
            template.setId(20L);
            return template;
        });

        TemplateApplicationService service = new TemplateApplicationService(repository, parser, fileFacade);

        TemplateResponse response = service.createTemplate(command());

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.status()).isEqualTo(TemplateStatus.DRAFT);
        assertThat(response.variables()).extracting("variableCode")
                .containsExactly("projectName", "reportDate");
        org.mockito.ArgumentCaptor<FileUploadCommand> uploadCommand = org.mockito.ArgumentCaptor.forClass(FileUploadCommand.class);
        verify(fileFacade).upload(uploadCommand.capture());
        assertThat(uploadCommand.getValue().fileSize()).isEqualTo(3L);
        verify(repository).updateFileId(20L, 10L, 1L);
        verify(repository).clearLatestByTemplateCode("TPL001", 1L);
        verify(repository).replaceVariables(org.mockito.ArgumentMatchers.eq(20L), any());
    }

    @Test
    void pageTemplatesReturnsPageResult() {
        Template template = new Template();
        template.setId(20L);
        template.setTemplateCode("TPL001");
        template.setTemplateName("demo");
        template.setReportType("CRYPTO");
        template.setVersionNo("1.0");
        template.setStatus(TemplateStatus.ENABLED);
        template.setLatest(true);

        TemplateRepository repository = mock(TemplateRepository.class);
        when(repository.findByCondition("CRYPTO", TemplateStatus.ENABLED, "demo")).thenReturn(List.of(template));

        TemplateApplicationService service = new TemplateApplicationService(
                repository,
                inputStream -> List.of(),
                mock(FileFacade.class)
        );

        PageResult<TemplateResponse> response = service.pageTemplates("CRYPTO", TemplateStatus.ENABLED, "demo", 1, 10);

        assertThat(response.records()).hasSize(1);
        assertThat(response.records().get(0).templateCode()).isEqualTo("TPL001");
        verify(repository).findByCondition("CRYPTO", TemplateStatus.ENABLED, "demo");
    }

    @Test
    void enableUpdatesTemplateStatus() {
        Template template = new Template();
        template.setId(20L);

        TemplateRepository repository = mock(TemplateRepository.class);
        when(repository.findById(20L)).thenReturn(Optional.of(template));

        TemplateApplicationService service = new TemplateApplicationService(
                repository,
                inputStream -> List.of(),
                mock(FileFacade.class)
        );

        service.enable(20L, 1L);

        verify(repository).updateStatus(20L, TemplateStatus.ENABLED, 1L);
    }

    @Test
    void updateVariablesReplacesTemplateVariables() {
        Template template = new Template();
        template.setId(20L);

        TemplateRepository repository = mock(TemplateRepository.class);
        when(repository.findById(20L)).thenReturn(Optional.of(template));

        TemplateApplicationService service = new TemplateApplicationService(
                repository,
                inputStream -> List.of(),
                mock(FileFacade.class)
        );

        List<TemplateVariableUpdateRequest.Item> items = List.of(new TemplateVariableUpdateRequest.Item(
                "projectName",
                "项目名称",
                "TEXT",
                true,
                null,
                null,
                "demo",
                1
        ));

        assertThat(service.updateVariables(20L, new TemplateVariableUpdateRequest(items)))
                .extracting("variableCode")
                .containsExactly("projectName");
        verify(repository).replaceVariables(org.mockito.ArgumentMatchers.eq(20L), any());
    }

    @Test
    void generateDownloadUrlDelegatesToFileFacade() {
        Template template = new Template();
        template.setId(20L);
        template.setFileId(10L);

        TemplateRepository repository = mock(TemplateRepository.class);
        FileFacade fileFacade = mock(FileFacade.class);
        when(repository.findById(20L)).thenReturn(Optional.of(template));
        when(fileFacade.generateDownloadUrl(10L, java.time.Duration.ofSeconds(600))).thenReturn("http://signed-url");

        TemplateApplicationService service = new TemplateApplicationService(
                repository,
                inputStream -> List.of(),
                fileFacade
        );

        assertThat(service.generateDownloadUrl(20L, 600)).isEqualTo("http://signed-url");
    }

    @Test
    void createTemplateRejectsNonDocxFile() {
        TemplateApplicationService service = new TemplateApplicationService(
                mock(TemplateRepository.class),
                inputStream -> List.of(),
                mock(FileFacade.class)
        );

        assertThatThrownBy(() -> service.createTemplate(new TemplateCreateCommand(
                "TPL001",
                "demo",
                "CRYPTO",
                "1.0",
                "demo",
                "template.pdf",
                "application/pdf",
                3,
                new ByteArrayInputStream(new byte[]{1, 2, 3}),
                1L
        ))).hasMessage("template file must be docx");
    }

    @Test
    void deleteLogicallyDeletesTemplateVariablesAndFileMetadata() {
        Template template = new Template();
        template.setId(20L);
        template.setFileId(10L);

        TemplateRepository repository = mock(TemplateRepository.class);
        FileFacade fileFacade = mock(FileFacade.class);
        when(repository.findById(20L)).thenReturn(Optional.of(template));

        TemplateApplicationService service = new TemplateApplicationService(
                repository,
                inputStream -> List.of(),
                fileFacade
        );

        service.delete(20L, 1L);

        verify(repository).delete(20L, 1L);
        verify(repository).deleteVariables(20L);
        verify(fileFacade).delete(10L);
    }

    @Test
    void deleteTemplateWithoutFileDoesNotDeleteFileMetadata() {
        Template template = new Template();
        template.setId(20L);

        TemplateRepository repository = mock(TemplateRepository.class);
        FileFacade fileFacade = mock(FileFacade.class);
        when(repository.findById(20L)).thenReturn(Optional.of(template));

        TemplateApplicationService service = new TemplateApplicationService(
                repository,
                inputStream -> List.of(),
                fileFacade
        );

        service.delete(20L, 1L);

        verify(repository).delete(20L, 1L);
        verify(repository).deleteVariables(20L);
        verify(fileFacade, never()).delete(any());
    }

    @Test
    void deleteMissingTemplateThrowsBusinessException() {
        TemplateRepository repository = mock(TemplateRepository.class);
        FileFacade fileFacade = mock(FileFacade.class);
        when(repository.findById(20L)).thenReturn(Optional.empty());

        TemplateApplicationService service = new TemplateApplicationService(
                repository,
                inputStream -> List.of(),
                fileFacade
        );

        assertThatThrownBy(() -> service.delete(20L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("template not found");
        verify(repository, never()).delete(any(), any());
        verify(repository, never()).deleteVariables(any());
        verify(fileFacade, never()).delete(any());
    }

    @Test
    void createTemplateDeletesUploadedFileWhenVariableParsingFails() {
        TemplateRepository repository = mock(TemplateRepository.class);
        TemplateVariableParser parser = mock(TemplateVariableParser.class);
        FileFacade fileFacade = mock(FileFacade.class);
        when(fileFacade.upload(any(FileUploadCommand.class))).thenReturn(fileResponse());
        when(parser.parse(any())).thenThrow(new IllegalStateException("parse failed"));
        when(repository.save(any(Template.class))).thenAnswer(invocation -> {
            Template template = invocation.getArgument(0);
            template.setId(20L);
            return template;
        });

        TemplateApplicationService service = new TemplateApplicationService(repository, parser, fileFacade);

        assertThatThrownBy(() -> service.createTemplate(command()))
                .isInstanceOf(IllegalStateException.class);
        verify(fileFacade).deleteObject(10L);
    }

    private TemplateCreateCommand command() {
        return new TemplateCreateCommand(
                "TPL001",
                "密码测评模板",
                "CRYPTO",
                "1.0",
                "demo",
                "template.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                3,
                new ByteArrayInputStream(new byte[]{1, 2, 3}),
                1L
        );
    }

    private FileObjectResponse fileResponse() {
        return new FileObjectResponse(
                10L,
                "F001",
                "TEMPLATE",
                null,
                "template.docx",
                "templates/CRYPTO/20/1.0/template.docx",
                "report-generate",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "docx",
                3L,
                null,
                "ACTIVE"
        );
    }
}
