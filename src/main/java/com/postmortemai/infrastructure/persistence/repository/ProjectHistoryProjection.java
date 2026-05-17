package com.postmortemai.infrastructure.persistence.repository;

import com.postmortemai.domain.enums.IncidentSeverity;
import com.postmortemai.domain.enums.IncidentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ProjectHistoryProjection {
    UUID getId();
    String getTitle();
    IncidentSeverity getSeverity();
    IncidentStatus getStatus();
    LocalDateTime getCreatedAt();
}
