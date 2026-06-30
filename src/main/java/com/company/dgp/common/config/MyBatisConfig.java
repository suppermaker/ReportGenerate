package com.company.dgp.common.config;

import com.company.dgp.common.mapper.DgpMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.company.dgp", markerInterface = DgpMapper.class)
public class MyBatisConfig {
}
