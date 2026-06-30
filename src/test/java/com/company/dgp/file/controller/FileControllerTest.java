package com.company.dgp.file.controller;

import com.company.dgp.file.application.FileFacade;
import com.company.dgp.file.dto.FileObjectResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileController.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileFacade fileFacade;

    @Test
    void uploadReturnsApiResponse() throws Exception {
        when(fileFacade.upload(any())).thenReturn(fileResponse());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/files")
                        .file(file)
                        .param("bizType", "REFERENCE")
                        .param("bizId", "20")
                        .header("X-Request-Id", "REQ-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.fileHash").value("sha256:abc"));
    }

    @Test
    void getFileReturnsMetadata() throws Exception {
        when(fileFacade.getFile(10L)).thenReturn(fileResponse());

        mockMvc.perform(get("/api/files/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileCode").value("F001"));
    }

    @Test
    void accessUrlUsesDefaultsAndReturnsUrl() throws Exception {
        when(fileFacade.generateDownloadUrl(eq(10L), eq(Duration.ofSeconds(600))))
                .thenReturn("http://signed-url");

        mockMvc.perform(get("/api/files/10/access-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("http://signed-url"))
                .andExpect(jsonPath("$.data.expireSeconds").value(600))
                .andExpect(jsonPath("$.data.usage").value("DOWNLOAD"))
                .andExpect(jsonPath("$.data.expireAt").exists());
    }

    @Test
    void deleteDelegatesToApplicationService() throws Exception {
        mockMvc.perform(delete("/api/files/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(fileFacade).delete(10L);
    }

    private FileObjectResponse fileResponse() {
        return new FileObjectResponse(
                10L,
                "F001",
                "REFERENCE",
                20L,
                "demo.pdf",
                "references/unknown/20/F001/demo.pdf",
                "report-generate",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf",
                3L,
                "sha256:abc",
                "ACTIVE"
        );
    }
}
