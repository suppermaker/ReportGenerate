package com.company.dgp.template.application;

import com.company.dgp.common.exception.BusinessException;
import com.company.dgp.common.result.PageResult;
import com.company.dgp.file.application.FileFacade;
import com.company.dgp.file.domain.FileBizType;
import com.company.dgp.file.dto.FileObjectResponse;
import com.company.dgp.file.dto.FileUploadCommand;
import com.company.dgp.template.domain.Template;
import com.company.dgp.template.domain.TemplateStatus;
import com.company.dgp.template.domain.TemplateVariable;
import com.company.dgp.template.dto.TemplateCreateCommand;
import com.company.dgp.template.dto.TemplateResponse;
import com.company.dgp.template.dto.TemplateVariableUpdateRequest;
import com.company.dgp.template.dto.TemplateVariableResponse;
import com.company.dgp.template.parser.TemplateVariableParser;
import com.company.dgp.template.repository.TemplateRepository;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TemplateApplicationService implements TemplateFacade {

    private final TemplateRepository templateRepository;
    private final TemplateVariableParser templateVariableParser;
    private final FileFacade fileFacade;

    public TemplateApplicationService(
            TemplateRepository templateRepository,
            TemplateVariableParser templateVariableParser,
            FileFacade fileFacade
    ) {
        this.templateRepository = templateRepository;
        this.templateVariableParser = templateVariableParser;
        this.fileFacade = fileFacade;
    }

    @Override
    @Transactional
    public TemplateResponse createTemplate(TemplateCreateCommand command) {
        validateCreateCommand(command);
        byte[] content = readAllBytes(command.inputStream());
        String versionNo = defaultIfBlank(command.versionNo(), "1.0");
        String templateCode = defaultIfBlank(command.templateCode(), nextTemplateCode());
        templateRepository.clearLatestByTemplateCode(templateCode, command.createdBy());

        Template template = new Template();
        template.setTemplateCode(templateCode);
        template.setTemplateName(command.templateName());
        template.setReportType(command.reportType());
        template.setVersionNo(versionNo);
        template.setDescription(command.description());
        template.setStatus(TemplateStatus.DRAFT);
        template.setLatest(true);
        template.setCreatedBy(command.createdBy());
        template.setUpdatedBy(command.createdBy());
        templateRepository.save(template);

        String objectName = buildTemplateObjectName(command.reportType(), template.getId(), versionNo, command.originalFilename());
        FileObjectResponse file = fileFacade.upload(new FileUploadCommand(
                FileBizType.TEMPLATE,
                template.getId(),
                command.originalFilename(),
                command.contentType(),
                content.length,
                new ByteArrayInputStream(content),
                command.createdBy(),
                objectName
        ));
        try {
            template.setFileId(file.id());
            templateRepository.updateFileId(template.getId(), file.id(), command.createdBy());

            List<String> variableCodes = templateVariableParser.parse(new ByteArrayInputStream(content));
            List<TemplateVariable> variables = toVariables(template.getId(), variableCodes);
            templateRepository.replaceVariables(template.getId(), variables);
            return toResponse(template, variables);
        } catch (RuntimeException exception) {
            fileFacade.deleteObject(file.id());
            throw exception;
        }
    }

    @Override
    public TemplateResponse getTemplate(Long templateId) {
        Template template = findTemplate(templateId);
        return toResponse(template, templateRepository.findVariables(templateId));
    }

    @Override
    public List<TemplateResponse> listTemplates(String reportType, String status) {
        return templateRepository.findByCondition(reportType, status, null).stream()
                .map(template -> toResponse(template, templateRepository.findVariables(template.getId())))
                .toList();
    }

    @Override
    public PageResult<TemplateResponse> pageTemplates(String reportType, String status, String keyword, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Template> templates = templateRepository.findByCondition(reportType, status, keyword);
        PageInfo<Template> pageInfo = new PageInfo<>(templates);
        List<TemplateResponse> records = templates.stream()
                .map(template -> toResponse(template, templateRepository.findVariables(template.getId())))
                .toList();
        return PageResult.of(records, pageInfo.getTotal(), pageNum, pageSize);
    }

    @Override
    public List<TemplateVariableResponse> listVariables(Long templateId) {
        findTemplate(templateId);
        return templateRepository.findVariables(templateId).stream()
                .map(TemplateVariableResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public List<TemplateVariableResponse> updateVariables(Long templateId, TemplateVariableUpdateRequest request) {
        findTemplate(templateId);
        if (request == null || request.variables() == null || request.variables().isEmpty()) {
            throw new BusinessException(4001, "template variables are required");
        }
        List<TemplateVariable> variables = toVariablesFromRequest(templateId, request.variables());
        templateRepository.replaceVariables(templateId, variables);
        return variables.stream()
                .map(TemplateVariableResponse::from)
                .toList();
    }

    @Override
    public String generateDownloadUrl(Long templateId, int expireSeconds) {
        Template template = findTemplate(templateId);
        if (template.getFileId() == null) {
            throw new BusinessException(4043, "template file not found");
        }
        return fileFacade.generateDownloadUrl(template.getFileId(), Duration.ofSeconds(expireSeconds));
    }

    @Override
    public void enable(Long templateId, Long updatedBy) {
        findTemplate(templateId);
        templateRepository.updateStatus(templateId, TemplateStatus.ENABLED, updatedBy);
    }

    @Override
    public void disable(Long templateId, Long updatedBy) {
        findTemplate(templateId);
        templateRepository.updateStatus(templateId, TemplateStatus.DISABLED, updatedBy);
    }

    @Override
    @Transactional
    public void delete(Long templateId, Long deletedBy) {
        Template template = findTemplate(templateId);
        templateRepository.delete(templateId, deletedBy);
        templateRepository.deleteVariables(templateId);
        if (template.getFileId() != null) {
            fileFacade.delete(template.getFileId());
        }
    }

    private Template findTemplate(Long templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(4042, "template not found"));
    }

    private void validateCreateCommand(TemplateCreateCommand command) {
        if (command == null) {
            throw new BusinessException(4001, "template create command is required");
        }
        if (isBlank(command.templateName())) {
            throw new BusinessException(4001, "template name is required");
        }
        if (isBlank(command.reportType())) {
            throw new BusinessException(4001, "report type is required");
        }
        if (isBlank(command.originalFilename())) {
            throw new BusinessException(4001, "template filename is required");
        }
        if (command.inputStream() == null) {
            throw new BusinessException(4001, "template file stream is required");
        }
        if (!isDocx(command.originalFilename(), command.contentType())) {
            throw new BusinessException(4001, "template file must be docx");
        }
    }

    private byte[] readAllBytes(InputStream inputStream) {
        try {
            return inputStream.readAllBytes();
        } catch (IOException exception) {
            throw new BusinessException(4002, "read template file failed");
        }
    }

    private List<TemplateVariable> toVariables(Long templateId, List<String> variableCodes) {
        java.util.ArrayList<TemplateVariable> variables = new java.util.ArrayList<>();
        for (int i = 0; i < variableCodes.size(); i++) {
            String variableCode = variableCodes.get(i);
            TemplateVariable variable = new TemplateVariable();
            variable.setTemplateId(templateId);
            variable.setVariableCode(variableCode);
            variable.setVariableName(variableCode);
            variable.setVariableType("TEXT");
            variable.setRequired(false);
            variable.setSortNo(i + 1);
            variables.add(variable);
        }
        return variables;
    }

    private List<TemplateVariable> toVariablesFromRequest(Long templateId, List<TemplateVariableUpdateRequest.Item> items) {
        java.util.ArrayList<TemplateVariable> variables = new java.util.ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            TemplateVariableUpdateRequest.Item item = items.get(i);
            if (item.variableCode() == null || item.variableCode().isBlank()) {
                throw new BusinessException(4001, "template variable code is required");
            }
            TemplateVariable variable = new TemplateVariable();
            variable.setTemplateId(templateId);
            variable.setVariableCode(item.variableCode());
            variable.setVariableName(defaultIfBlank(item.variableName(), item.variableCode()));
            variable.setVariableType(defaultIfBlank(item.variableType(), "TEXT"));
            variable.setRequired(Boolean.TRUE.equals(item.required()));
            variable.setDefaultValue(item.defaultValue());
            variable.setOptionsJson(item.optionsJson());
            variable.setDescription(item.description());
            variable.setSortNo(item.sortNo() == null ? i + 1 : item.sortNo());
            variables.add(variable);
        }
        return variables;
    }

    private TemplateResponse toResponse(Template template, List<TemplateVariable> variables) {
        return TemplateResponse.from(template, variables.stream()
                .map(TemplateVariableResponse::from)
                .toList());
    }

    private String buildTemplateObjectName(String reportType, Long templateId, String versionNo, String filename) {
        return "templates/%s/%s/%s/%s".formatted(reportType, templateId, versionNo, filename);
    }

    private String nextTemplateCode() {
        return "TPL" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isDocx(String filename, String contentType) {
        boolean docxExt = filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".docx");
        boolean docxContentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType);
        return docxExt || docxContentType;
    }
}
