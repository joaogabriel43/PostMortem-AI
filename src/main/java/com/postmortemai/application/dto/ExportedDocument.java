package com.postmortemai.application.dto;

public record ExportedDocument(
        byte[] content,
        String contentType
) {}
