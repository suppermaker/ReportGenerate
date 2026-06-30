package com.company.dgp.template.repository;

import com.company.dgp.template.domain.Template;
import com.company.dgp.template.domain.TemplateVariable;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository {

    Template save(Template template);

    Optional<Template> findById(Long id);

    List<Template> findByCondition(String reportType, String status, String keyword);

    void updateFileId(Long id, Long fileId, Long updatedBy);

    void updateStatus(Long id, String status, Long updatedBy);

    void replaceVariables(Long templateId, List<TemplateVariable> variables);

    List<TemplateVariable> findVariables(Long templateId);
}
