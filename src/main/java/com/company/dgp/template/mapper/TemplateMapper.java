package com.company.dgp.template.mapper;

import com.company.dgp.common.mapper.DgpMapper;
import com.company.dgp.template.domain.Template;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TemplateMapper extends DgpMapper {

    int insert(Template template);

    Template selectById(@Param("id") Long id);

    List<Template> selectByCondition(
            @Param("reportType") String reportType,
            @Param("status") String status,
            @Param("keyword") String keyword
    );

    int updateFileId(@Param("id") Long id, @Param("fileId") Long fileId, @Param("updatedBy") Long updatedBy);

    int clearLatestByTemplateCode(@Param("templateCode") String templateCode, @Param("updatedBy") Long updatedBy);

    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("updatedBy") Long updatedBy);

    int logicalDelete(@Param("id") Long id, @Param("updatedBy") Long updatedBy);
}
