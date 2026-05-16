package com.postmortemai.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the 'postmortems' table.
 *
 * <p>This class belongs to the infrastructure layer and must NOT be exposed
 * to the domain or application layers. All cross-layer communication happens
 * through domain models and DTOs, mapped explicitly by repository implementations.
 *
 * <p>The relationship to {@link IncidentEntity} is LAZY to avoid loading the
 * full incident graph whenever a post-mortem is queried in isolation.
 *
 * <p>Optional sections (contributingFactors, actionItems, lessonsLearned,
 * exportedMarkdown) are nullable per ADR-005: the LLM returns null for sections
 * with insufficient evidence, and the export layer omits them from the final document.
 */
@Entity
@Table(
    name = "postmortems",
    indexes = {
        @Index(name = "idx_postmortems_incident_id", columnList = "incident_id")
    }
)
public class PostMortemEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * LAZY fetch — do not load the full incident when only post-mortem data is needed.
     * Use a join query explicitly when the incident context is required.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false, updatable = false)
    private IncidentEntity incident;

    // ── Mandatory sections (always generated) ─────────────────────────────────

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "timeline", nullable = false, columnDefinition = "TEXT")
    private String timeline;

    @Column(name = "root_cause", nullable = false, columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "impact", nullable = false, columnDefinition = "TEXT")
    private String impact;

    @Column(name = "detection", nullable = false, columnDefinition = "TEXT")
    private String detection;

    // ── Optional sections (null when data is insufficient — ADR-005) ──────────

    @Column(name = "contributing_factors", columnDefinition = "TEXT")
    private String contributingFactors;

    @Column(name = "action_items", columnDefinition = "TEXT")
    private String actionItems;

    @Column(name = "lessons_learned", columnDefinition = "TEXT")
    private String lessonsLearned;

    @Column(name = "exported_markdown", columnDefinition = "TEXT")
    private String exportedMarkdown;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    protected PostMortemEntity() {
        // Required by JPA — not for direct use
    }

    public PostMortemEntity(
            UUID id,
            IncidentEntity incident,
            String title,
            String summary,
            String timeline,
            String rootCause,
            String impact,
            String detection,
            String contributingFactors,
            String actionItems,
            String lessonsLearned,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.incident = incident;
        this.title = title;
        this.summary = summary;
        this.timeline = timeline;
        this.rootCause = rootCause;
        this.impact = impact;
        this.detection = detection;
        this.contributingFactors = contributingFactors;
        this.actionItems = actionItems;
        this.lessonsLearned = lessonsLearned;
        this.createdAt = createdAt;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public IncidentEntity getIncident() {
        return incident;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getTimeline() {
        return timeline;
    }

    public String getRootCause() {
        return rootCause;
    }

    public String getImpact() {
        return impact;
    }

    public String getDetection() {
        return detection;
    }

    public String getContributingFactors() {
        return contributingFactors;
    }

    public String getActionItems() {
        return actionItems;
    }

    public String getLessonsLearned() {
        return lessonsLearned;
    }

    public String getExportedMarkdown() {
        return exportedMarkdown;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ── Setters (only for mutable fields) ────────────────────────────────────

    public void setExportedMarkdown(String exportedMarkdown) {
        this.exportedMarkdown = exportedMarkdown;
    }
}
