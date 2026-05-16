package com.postmortemai.infrastructure.persistence.entity;

import com.postmortemai.domain.enums.IncidentSeverity;
import com.postmortemai.domain.enums.IncidentStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the 'incidents' table.
 *
 * <p>This class belongs to the infrastructure layer and must NOT be exposed
 * to the domain or application layers. All cross-layer communication happens
 * through domain models and DTOs, mapped explicitly by repository implementations.
 *
 * <p>Enums are stored as STRING to avoid ordinal-based compatibility issues
 * if enum members are reordered or new members are added in the future.
 */
@Entity
@Table(
    name = "incidents",
    indexes = {
        @Index(name = "idx_incidents_project_name", columnList = "project_name")
    }
)
public class IncidentEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "project_name", nullable = false, length = 255)
    private String projectName;

    @Column(name = "service_name", nullable = false, length = 255)
    private String serviceName;

    /**
     * SHA-256 hash of the submitted raw log.
     * The raw log itself is never persisted — only its hash (for deduplication).
     */
    @Column(name = "raw_log_hash", nullable = false, length = 64)
    private String rawLogHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IncidentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    protected IncidentEntity() {
        // Required by JPA — not for direct use
    }

    public IncidentEntity(
            UUID id,
            String projectName,
            String serviceName,
            String rawLogHash,
            IncidentSeverity severity,
            IncidentStatus status,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.projectName = projectName;
        this.serviceName = serviceName;
        this.rawLogHash = rawLogHash;
        this.severity = severity;
        this.status = status;
        this.createdAt = createdAt;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getRawLogHash() {
        return rawLogHash;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ── Setters (only for mutable fields) ────────────────────────────────────

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }
}
