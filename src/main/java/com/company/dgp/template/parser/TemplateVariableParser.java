package com.company.dgp.template.parser;

import java.io.InputStream;
import java.util.List;

public interface TemplateVariableParser {

    List<String> parse(InputStream inputStream);
}
