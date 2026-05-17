package com.postmortemai.application.dto;

public record PageQuery(int page, int size) {
    public PageQuery {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }
    }
}
