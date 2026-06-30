package com.company.dgp;

import com.company.dgp.file.infra.StorageAdapter;
import com.company.dgp.file.mapper.FileObjectMapper;
import com.company.dgp.template.mapper.TemplateMapper;
import com.company.dgp.template.mapper.TemplateVariableMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class DgpApplicationTests {

    @MockBean
    private FileObjectMapper fileObjectMapper;

    @MockBean
    private TemplateMapper templateMapper;

    @MockBean
    private TemplateVariableMapper templateVariableMapper;

    @MockBean
    private StorageAdapter storageAdapter;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {
    }
}
