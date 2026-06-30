package com.company.dgp.template.controller;

import com.company.dgp.common.result.ApiResponse;
import com.company.dgp.common.result.PageResult;
import com.company.dgp.template.application.TemplateFacade;
import com.company.dgp.template.dto.TemplateCreateCommand;
import com.company.dgp.template.dto.TemplateResponse;
import com.company.dgp.template.dto.TemplateVariableResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateFacade templateFacade;

    public TemplateController(TemplateFacade templateFacade) {
        this.templateFacade = templateFacade;
    }

    @PostMapping
    public ApiResponse<TemplateResponse> createTemplate(
            @RequestParam("file") @NotNull MultipartFile file,
            @RequestParam(value = "templateCode", required = false) String templateCode,
            @RequestParam("templateName") @NotBlank String templateName,
            @RequestParam("reportType") @NotBlank String reportType,
            @RequestParam(value = "versionNo", required = false) String versionNo,
            @RequestParam(value = "description", required = false) String description,
            HttpServletRequest request
    ) throws IOException {
        TemplateResponse response = templateFacade.createTemplate(new TemplateCreateCommand(
                templateCode,
                templateName,
                reportType,
                versionNo,
                description,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream(),
                null
        ));
        return ApiResponse.success(response, request.getRequestId());
    }

    @GetMapping
    public ApiResponse<PageResult<TemplateResponse>> listTemplates(
            @RequestParam(value = "reportType", required = false) String reportType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") @Min(1) @Max(100) Integer pageSize,
            HttpServletRequest request
    ) {
        PageResult<TemplateResponse> response = templateFacade.pageTemplates(
                reportType,
                status,
                keyword,
                pageNum,
                pageSize
        );
        return ApiResponse.success(response, request.getRequestId());
    }

    @GetMapping("/{templateId}")
    public ApiResponse<TemplateResponse> getTemplate(@PathVariable Long templateId, HttpServletRequest request) {
        return ApiResponse.success(templateFacade.getTemplate(templateId), request.getRequestId());
    }

    @GetMapping("/{templateId}/variables")
    public ApiResponse<List<TemplateVariableResponse>> listVariables(
            @PathVariable Long templateId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(templateFacade.listVariables(templateId), request.getRequestId());
    }

    @PostMapping("/{templateId}/enable")
    public ApiResponse<Void> enable(@PathVariable Long templateId, HttpServletRequest request) {
        templateFacade.enable(templateId, null);
        return ApiResponse.success(null, request.getRequestId());
    }

    @PostMapping("/{templateId}/disable")
    public ApiResponse<Void> disable(@PathVariable Long templateId, HttpServletRequest request) {
        templateFacade.disable(templateId, null);
        return ApiResponse.success(null, request.getRequestId());
    }
}
