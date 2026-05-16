package com.postmortemai.application.dto;

/**
 * Immutable record representing the structured facts extracted from logs by the LLM (Prompt 1).
 * Matches the required JSON schema defined in ADR-005.
 *
 * @param projectName         Name of the project
 * @param serviceName         Name of the service
 * @param title               Descriptive title of the incident
 * @param severity            Inferred severity (e.g., P1, P2, P3, P4)
 * @param status              Inferred status (e.g., INVESTIGATING, MONITORING, RESOLVED)
 * @param summary             Non-technical executive summary
 * @param timeline            Chronological timeline of events extracted from logs
 * @param rootCause           Root cause with direct causal evidence
 * @param impact              What was affected and for how long
 * @param detection           How/when the problem was detected
 * @param contributingFactors Aggravating factors (nullable if insufficient data)
 * @param actionItems         Suggested corrections with placeholders (nullable)
 * @param lessonsLearned      What the incident revealed about the system (nullable)
 */
public record ExtractedFacts(
        String projectName,
        String serviceName,
        String title,
        String severity,
        String status,
        String summary,
        String timeline,
        String rootCause,
        String impact,
        String detection,
        String contributingFactors,
        String actionItems,
        String lessonsLearned
) {}
