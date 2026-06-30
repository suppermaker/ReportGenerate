package com.company.dgp.file.mapper;

import com.company.dgp.common.mapper.DgpMapper;
import com.company.dgp.file.domain.FileObject;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FileObjectMapper extends DgpMapper {

    int insert(FileObject fileObject);

    FileObject selectById(@Param("id") Long id);

    FileObject selectByFileCode(@Param("fileCode") String fileCode);

    List<FileObject> selectByBizTypeAndBizId(@Param("bizType") String bizType, @Param("bizId") Long bizId);

    FileObject selectByFileHash(@Param("fileHash") String fileHash);

    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
