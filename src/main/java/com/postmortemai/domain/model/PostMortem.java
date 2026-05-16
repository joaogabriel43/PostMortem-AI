package com.postmortemai.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pure domain model for a Post-Mortem report.
 * Decoupled from any persistence framework or annotations.
 */
public record PostMortem(
        UUID id,
        UUID incidentId,
        String title,
        String summary,
        String timeline,
        String rootCause,
        String impact,
        String detection,
        String contributingFactors,
        String actionItems,
        String lessonsLearned,
        String exportedMarkdown,
        LocalDateTime createdAt
) {}
