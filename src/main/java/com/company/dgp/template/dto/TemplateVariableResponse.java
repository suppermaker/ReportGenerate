package com.company.dgp.template.dto;

import com.company.dgp.template.domain.TemplateVariable;

public record TemplateVariableResponse(
        Long id,
        Long templateId,
        String variableCode,
        String variableName,
        String variableType,
        Boolean required,
        String defaultValue,
        Integer sortNo
) {

    public static TemplateVariableResponse from(TemplateVariable variable) {
        return new TemplateVariableResponse(
                variable.getId(),
                variable.getTemplateId(),
                variable.getVariableCode(),
                variable.getVariableName(),
                variable.getVariableType(),
                variable.getRequired(),
                variable.getDefaultValue(),
                variable.getSortNo()
        );
    }
}
