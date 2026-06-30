package com.company.dgp.template.parser;

import com.company.dgp.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WordTemplateVariableParserTest {

    private final WordTemplateVariableParser parser = new WordTemplateVariableParser();

    @Test
    void parseSupportsBothPlaceholderStylesAndDeduplicates() throws Exception {
        byte[] docx = docx("""
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body><w:p><w:r><w:t>${projectName} {{report_date}} ${projectName}</w:t></w:r></w:p></w:body>
                </w:document>
                """);

        List<String> variables = parser.parse(new ByteArrayInputStream(docx));

        assertThat(variables).containsExactly("projectName", "report_date");
    }

    @Test
    void parseReturnsEmptyListWhenNoPlaceholderExists() throws Exception {
        byte[] docx = docx("""
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body><w:p><w:r><w:t>plain text</w:t></w:r></w:p></w:body>
                </w:document>
                """);

        assertThat(parser.parse(new ByteArrayInputStream(docx))).isEmpty();
    }

    @Test
    void parseSupportsPlaceholderSplitAcrossRunsInSameParagraph() throws Exception {
        byte[] docx = docx("""
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                    <w:p>
                      <w:r><w:t>${pro</w:t></w:r>
                      <w:r><w:t>jectName}</w:t></w:r>
                      <w:r><w:t> {{report</w:t></w:r>
                      <w:r><w:t>Date}}</w:t></w:r>
                    </w:p>
                  </w:body>
                </w:document>
                """);

        List<String> variables = parser.parse(new ByteArrayInputStream(docx));

        assertThat(variables).containsExactly("projectName", "reportDate");
    }

    @Test
    void parseRejectsInvalidWordFile() {
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream("bad".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(BusinessException.class);
    }

    private byte[] docx(String documentXml) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            zipOutputStream.putNextEntry(new ZipEntry("word/document.xml"));
            zipOutputStream.write(documentXml.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return outputStream.toByteArray();
    }
}
