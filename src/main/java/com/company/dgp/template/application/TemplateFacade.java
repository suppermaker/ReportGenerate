package com.company.dgp.template.application;

import com.company.dgp.common.result.PageResult;
import com.company.dgp.template.dto.TemplateCreateCommand;
import com.company.dgp.template.dto.TemplateResponse;
import com.company.dgp.template.dto.TemplateVariableResponse;

import java.util.List;

public interface TemplateFacade {

    TemplateResponse createTemplate(TemplateCreateCommand command);

    TemplateResponse getTemplate(Long templateId);

    List<TemplateResponse> listTemplates(String reportType, String status);

    PageResult<TemplateResponse> pageTemplates(String reportType, String status, String keyword, int pageNum, int pageSize);

    List<TemplateVariableResponse> listVariables(Long templateId);

    void enable(Long templateId, Long updatedBy);

    void disable(Long templateId, Long updatedBy);
}
