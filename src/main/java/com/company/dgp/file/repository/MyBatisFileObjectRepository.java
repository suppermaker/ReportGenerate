package com.company.dgp.file.repository;

import com.company.dgp.file.domain.FileObject;
import com.company.dgp.file.mapper.FileObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MyBatisFileObjectRepository implements FileObjectRepository {

    private final FileObjectMapper fileObjectMapper;

    public MyBatisFileObjectRepository(FileObjectMapper fileObjectMapper) {
        this.fileObjectMapper = fileObjectMapper;
    }

    @Override
    public FileObject save(FileObject fileObject) {
        fileObjectMapper.insert(fileObject);
        return fileObject;
    }

    @Override
    public Optional<FileObject> findById(Long id) {
        return Optional.ofNullable(fileObjectMapper.selectById(id));
    }

    @Override
    public Optional<FileObject> findByFileCode(String fileCode) {
        return Optional.ofNullable(fileObjectMapper.selectByFileCode(fileCode));
    }

    @Override
    public List<FileObject> findByBizTypeAndBizId(String bizType, Long bizId) {
        return fileObjectMapper.selectByBizTypeAndBizId(bizType, bizId);
    }

    @Override
    public Optional<FileObject> findByFileHash(String fileHash) {
        return Optional.ofNullable(fileObjectMapper.selectByFileHash(fileHash));
    }

    @Override
    public void updateStatus(Long id, String status) {
        fileObjectMapper.updateStatus(id, status);
    }
}
