package com.company.dgp.template.controller;

import com.company.dgp.common.result.ApiResponse;
import com.company.dgp.common.result.PageResult;
import com.company.dgp.template.application.TemplateFacade;
import com.company.dgp.template.dto.TemplateCreateCommand;
import com.company.dgp.template.dto.TemplateResponse;
import com.company.dgp.template.dto.TemplateVariableUpdateRequest;
import com.company.dgp.template.dto.TemplateVariableResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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

    @PutMapping("/{templateId}/variables")
    public ApiResponse<List<TemplateVariableResponse>> updateVariables(
            @PathVariable Long templateId,
            @Valid @org.springframework.web.bind.annotation.RequestBody TemplateVariableUpdateRequest updateRequest,
            HttpServletRequest request
    ) {
        return ApiResponse.success(templateFacade.updateVariables(templateId, updateRequest), request.getRequestId());
    }

    @GetMapping("/{templateId}/download-url")
    public ApiResponse<Map<String, Object>> downloadUrl(
            @PathVariable Long templateId,
            @RequestParam(value = "expireSeconds", defaultValue = "600") @Min(1) @Max(3600) Integer expireSeconds,
            HttpServletRequest request
    ) {
        int normalizedExpireSeconds = expireSeconds == null ? 600 : expireSeconds;
        String url = templateFacade.generateDownloadUrl(templateId, normalizedExpireSeconds);
        return ApiResponse.success(Map.of(
                "url", url,
                "expireSeconds", normalizedExpireSeconds,
                "usage", "DOWNLOAD",
                "expireAt", OffsetDateTime.now().plusSeconds(normalizedExpireSeconds).toString()
        ), request.getRequestId());
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

    @DeleteMapping("/{templateId}")
    public ApiResponse<Void> delete(@PathVariable Long templateId, HttpServletRequest request) {
        templateFacade.delete(templateId, null);
        return ApiResponse.success(null, request.getRequestId());
    }
}
