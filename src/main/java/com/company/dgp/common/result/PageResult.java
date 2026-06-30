package com.company.dgp.common.result;

import java.util.List;

public record PageResult<T>(
        List<T> records,
        long total,
        int pageNum,
        int pageSize
) {

    public static <T> PageResult<T> of(List<T> records, long total, int pageNum, int pageSize) {
        return new PageResult<>(records, total, pageNum, pageSize);
    }
}
