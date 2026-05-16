package com.postmortemai.infrastructure.ai.prompt;

import org.springframework.stereotype.Component;

/**
 * Utility component to build the extraction prompt (Prompt 1).
 * Injects the pre-processed logs and adds strict instructions to avoid the
 * Surface Attribution Error.
 */
@Component
public class ExtractionPromptBuilder {

    private static final String EXTRACTION_SYSTEM_PROMPT = """
            You are a Site Reliability Engineering (SRE) expert system.
            Your task is to analyze the provided production logs and extract structured facts about an incident.
            
            CRITICAL ANTI-SURFACE ATTRIBUTION INSTRUCTION:
            Only attribute root cause to components with direct causal evidence in the log sequence.
            Mentions alone are not causal evidence. If causality is ambiguous, set rootCause to null.
            Do NOT blame individuals or specific users. Focus on the system failure.
            
            You must return a strict JSON object that exactly matches the following schema.
            If there is not enough data for a field, return explicitly `null` for that field. Do not invent data.
            Do not wrap the JSON in Markdown code blocks (e.g., do not use ```json). Return ONLY the raw JSON string.
            
            {
              "projectName": "string",
              "serviceName": "string",
              "title": "string (descriptive name of the incident)",
              "severity": "string (one of: P1, P2, P3, P4)",
              "status": "string (one of: INVESTIGATING, MONITORING, RESOLVED)",
              "summary": "string (non-technical executive summary)",
              "timeline": "string (chronological timeline of events)",
              "rootCause": "string (must have direct causal evidence, or null)",
              "impact": "string (what was affected and for how long)",
              "detection": "string (how/when the problem was detected)",
              "contributingFactors": "string (or null)",
              "actionItems": "string (suggested corrections, or null)",
              "lessonsLearned": "string (what this revealed about the system, or null)"
            }
            """;

    /**
     * Builds the prompt instructing the LLM to extract facts from the given logs.
     *
     * @param preProcessedLogs The cleaned and pre-processed log string.
     * @return The complete prompt ready to be sent to the LLM.
     */
    public String buildPrompt(String preProcessedLogs) {
        if (preProcessedLogs == null || preProcessedLogs.isBlank()) {
            throw new IllegalArgumentException("Logs cannot be empty for extraction");
        }
        return EXTRACTION_SYSTEM_PROMPT + "\n\n--- LOGS START ---\n" + preProcessedLogs + "\n--- LOGS END ---\n";
    }
}
