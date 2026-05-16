package com.postmortemai.presentation.dto;

public record IncidentRequest(
        String projectName,
        String serviceName,
        String rawLog
) {}
