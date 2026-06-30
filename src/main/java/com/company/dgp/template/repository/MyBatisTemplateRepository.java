package com.company.dgp.template.repository;

import com.company.dgp.template.domain.Template;
import com.company.dgp.template.domain.TemplateVariable;
import com.company.dgp.template.mapper.TemplateMapper;
import com.company.dgp.template.mapper.TemplateVariableMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MyBatisTemplateRepository implements TemplateRepository {

    private final TemplateMapper templateMapper;
    private final TemplateVariableMapper templateVariableMapper;

    public MyBatisTemplateRepository(TemplateMapper templateMapper, TemplateVariableMapper templateVariableMapper) {
        this.templateMapper = templateMapper;
        this.templateVariableMapper = templateVariableMapper;
    }

    @Override
    public Template save(Template template) {
        templateMapper.insert(template);
        return template;
    }

    @Override
    public Optional<Template> findById(Long id) {
        return Optional.ofNullable(templateMapper.selectById(id));
    }

    @Override
    public List<Template> findByCondition(String reportType, String status, String keyword) {
        return templateMapper.selectByCondition(reportType, status, keyword);
    }

    @Override
    public void updateFileId(Long id, Long fileId, Long updatedBy) {
        templateMapper.updateFileId(id, fileId, updatedBy);
    }

    @Override
    public void updateStatus(Long id, String status, Long updatedBy) {
        templateMapper.updateStatus(id, status, updatedBy);
    }

    @Override
    public void replaceVariables(Long templateId, List<TemplateVariable> variables) {
        templateVariableMapper.logicalDeleteByTemplateId(templateId);
        if (!variables.isEmpty()) {
            templateVariableMapper.batchInsert(variables);
        }
    }

    @Override
    public List<TemplateVariable> findVariables(Long templateId) {
        return templateVariableMapper.selectByTemplateId(templateId);
    }
}
