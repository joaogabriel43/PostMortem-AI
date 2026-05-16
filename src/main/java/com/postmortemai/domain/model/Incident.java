package com.postmortemai.domain.model;

import com.postmortemai.domain.enums.IncidentSeverity;
import com.postmortemai.domain.enums.IncidentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pure domain model for an Incident.
 * Decoupled from any persistence framework or annotations.
 */
public record Incident(
        UUID id,
        String projectName,
        String serviceName,
        String rawLogHash,
        IncidentSeverity severity,
        IncidentStatus status,
        LocalDateTime createdAt
) {}
