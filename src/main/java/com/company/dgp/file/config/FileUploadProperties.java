package com.company.dgp.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@ConfigurationProperties(prefix = "app.file")
@Component
public class FileUploadProperties {

    private long maxSize = 50L * 1024 * 1024;

    private Set<String> allowedExtensions = new LinkedHashSet<>(
            Set.of("docx", "pdf", "txt", "md", "doc", "markdown")
    );

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    public Set<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(Set<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }
}
