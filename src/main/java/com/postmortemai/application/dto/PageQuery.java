package com.postmortemai.application.dto;

public record PageQuery(int page, int size) {
    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must not be less than or equal to zero");
        }
        if (size > 100) {
            throw new IllegalArgumentException("Page size must not be greater than 100");
        }
    }
}
