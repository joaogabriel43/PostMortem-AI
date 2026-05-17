package com.postmortemai.application.dto;

import java.util.List;

public record PageResult<T>(
        List<T> data,
        long totalElements,
        int totalPages,
        int currentPage
) {}
