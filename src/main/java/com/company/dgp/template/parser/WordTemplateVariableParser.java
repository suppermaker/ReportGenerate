package com.company.dgp.template.parser;

import com.company.dgp.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class WordTemplateVariableParser implements TemplateVariableParser {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_.-]+)}|\\{\\{([A-Za-z0-9_.-]+)}}");
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("<w:p[\\s>][\\s\\S]*?</w:p>");
    private static final Pattern TEXT_PATTERN = Pattern.compile("<w:t(?:\\s[^>]*)?>([\\s\\S]*?)</w:t>");

    @Override
    public List<String> parse(InputStream inputStream) {
        if (inputStream == null) {
            throw new BusinessException(4001, "template file stream is required");
        }
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            Set<String> variables = new LinkedHashSet<>();
            boolean foundWordXml = false;
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (isWordXml(entry)) {
                    foundWordXml = true;
                    collectVariables(extractXml(zipInputStream), variables);
                }
                zipInputStream.closeEntry();
            }
            if (!foundWordXml) {
                throw new BusinessException(4002, "parse word template variables failed");
            }
            return new ArrayList<>(variables);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(4002, "parse word template variables failed");
        }
    }

    private boolean isWordXml(ZipEntry entry) {
        String name = entry.getName();
        return !entry.isDirectory()
                && name.startsWith("word/")
                && name.endsWith(".xml");
    }

    private String extractXml(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        inputStream.transferTo(outputStream);
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private void collectVariables(String xml, Set<String> variables) {
        boolean foundParagraph = false;
        Matcher paragraphMatcher = PARAGRAPH_PATTERN.matcher(xml);
        while (paragraphMatcher.find()) {
            foundParagraph = true;
            collectFromText(joinTextNodes(paragraphMatcher.group()), variables);
        }
        if (!foundParagraph) {
            collectFromText(stripXmlTags(xml), variables);
        }
    }

    private String joinTextNodes(String xml) {
        StringBuilder builder = new StringBuilder();
        Matcher textMatcher = TEXT_PATTERN.matcher(xml);
        while (textMatcher.find()) {
            builder.append(unescapeXml(textMatcher.group(1)));
        }
        return builder.toString();
    }

    private String stripXmlTags(String xml) {
        return unescapeXml(xml.replaceAll("<[^>]+>", ""));
    }

    private void collectFromText(String text, Set<String> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        while (matcher.find()) {
            String variable = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            variables.add(variable);
        }
    }

    private String unescapeXml(String value) {
        return value.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }
}
