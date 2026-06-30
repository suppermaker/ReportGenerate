package com.company.dgp.template.controller;

import com.company.dgp.common.result.PageResult;
import com.company.dgp.template.application.TemplateFacade;
import com.company.dgp.template.dto.TemplateResponse;
import com.company.dgp.template.dto.TemplateVariableResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TemplateController.class)
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TemplateFacade templateFacade;

    @Test
    void createTemplateReturnsApiResponse() throws Exception {
        when(templateFacade.createTemplate(any())).thenReturn(templateResponse());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "template.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/templates")
                        .file(file)
                        .param("templateCode", "TPL001")
                        .param("templateName", "demo")
                        .param("reportType", "CRYPTO")
                        .param("versionNo", "1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(20))
                .andExpect(jsonPath("$.data.variables[0].variableCode").value("projectName"));
    }

    @Test
    void listTemplatesReturnsPageResult() throws Exception {
        when(templateFacade.pageTemplates("CRYPTO", "ENABLED", "demo", 1, 10))
                .thenReturn(PageResult.of(List.of(templateResponse()), 1, 1, 10));

        mockMvc.perform(get("/api/templates")
                        .param("reportType", "CRYPTO")
                        .param("status", "ENABLED")
                        .param("keyword", "demo")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].templateCode").value("TPL001"));
    }

    @Test
    void getTemplateReturnsDetail() throws Exception {
        when(templateFacade.getTemplate(20L)).thenReturn(templateResponse());

        mockMvc.perform(get("/api/templates/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateName").value("demo"));
    }

    @Test
    void listVariablesReturnsVariables() throws Exception {
        when(templateFacade.listVariables(20L)).thenReturn(List.of(variableResponse()));

        mockMvc.perform(get("/api/templates/20/variables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].variableCode").value("projectName"));
    }

    @Test
    void updateVariablesReturnsVariables() throws Exception {
        when(templateFacade.updateVariables(any(), any())).thenReturn(List.of(variableResponse()));

        mockMvc.perform(put("/api/templates/20/variables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "variables": [
                                    {
                                      "variableCode": "projectName",
                                      "variableName": "项目名称",
                                      "variableType": "TEXT",
                                      "required": true,
                                      "sortNo": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].variableCode").value("projectName"));
    }

    @Test
    void downloadUrlReturnsUrl() throws Exception {
        when(templateFacade.generateDownloadUrl(20L, 600)).thenReturn("http://signed-url");

        mockMvc.perform(get("/api/templates/20/download-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("http://signed-url"))
                .andExpect(jsonPath("$.data.expireSeconds").value(600))
                .andExpect(jsonPath("$.data.usage").value("DOWNLOAD"));
    }

    @Test
    void enableAndDisableDelegateToApplicationService() throws Exception {
        mockMvc.perform(post("/api/templates/20/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/api/templates/20/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(templateFacade).enable(20L, null);
        verify(templateFacade).disable(20L, null);
    }

    @Test
    void deleteDelegatesToApplicationService() throws Exception {
        mockMvc.perform(delete("/api/templates/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(templateFacade).delete(20L, null);
    }

    private TemplateResponse templateResponse() {
        return new TemplateResponse(
                20L,
                "TPL001",
                "demo",
                "CRYPTO",
                "1.0",
                10L,
                "description",
                "ENABLED",
                true,
                List.of(variableResponse())
        );
    }

    private TemplateVariableResponse variableResponse() {
        return new TemplateVariableResponse(
                1L,
                20L,
                "projectName",
                "projectName",
                "TEXT",
                false,
                null,
                1
        );
    }
}
