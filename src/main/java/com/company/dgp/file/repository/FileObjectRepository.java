package com.company.dgp.file.repository;

import com.company.dgp.file.domain.FileObject;

import java.util.Optional;

public interface FileObjectRepository {

    FileObject save(FileObject fileObject);

    Optional<FileObject> findById(Long id);

    Optional<FileObject> findByFileCode(String fileCode);

    void updateStatus(Long id, String status);
}
