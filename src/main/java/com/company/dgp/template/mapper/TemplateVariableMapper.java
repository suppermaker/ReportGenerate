package com.company.dgp.template.mapper;

import com.company.dgp.common.mapper.DgpMapper;
import com.company.dgp.template.domain.TemplateVariable;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TemplateVariableMapper extends DgpMapper {

    int batchInsert(@Param("variables") List<TemplateVariable> variables);

    List<TemplateVariable> selectByTemplateId(@Param("templateId") Long templateId);

    int logicalDeleteByTemplateId(@Param("templateId") Long templateId);
}
