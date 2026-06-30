package com.company.dgp.template.dto;

import com.company.dgp.template.domain.Template;

import java.util.List;

public record TemplateResponse(
        Long id,
        String templateCode,
        String templateName,
        String reportType,
        String versionNo,
        Long fileId,
        String description,
        String status,
        Boolean latest,
        List<TemplateVariableResponse> variables
) {

    public static TemplateResponse from(Template template, List<TemplateVariableResponse> variables) {
        return new TemplateResponse(
                template.getId(),
                template.getTemplateCode(),
                template.getTemplateName(),
                template.getReportType(),
                template.getVersionNo(),
                template.getFileId(),
                template.getDescription(),
                template.getStatus(),
                template.getLatest(),
                variables
        );
    }
}
