package com.company.dgp.template.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TemplateVariableUpdateRequest(
        @Valid
        @NotEmpty
        List<Item> variables
) {

    public record Item(
            @NotBlank
            String variableCode,
            String variableName,
            String variableType,
            Boolean required,
            String defaultValue,
            String optionsJson,
            String description,
            Integer sortNo
    ) {
    }
}
