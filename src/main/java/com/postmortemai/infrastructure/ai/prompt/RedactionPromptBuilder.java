package com.postmortemai.infrastructure.ai.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postmortemai.application.dto.ExtractedFacts;
import org.springframework.stereotype.Component;

/**
 * Utility component to build the redaction prompt (Prompt 2).
 * Injects ONLY the structured ExtractedFacts JSON (never raw logs) to generate
 * the final Markdown post-mortem document.
 */
@Component
public class RedactionPromptBuilder {

    private final ObjectMapper objectMapper;

    private static final String REDACTION_SYSTEM_PROMPT = """
            You are an expert technical writer and Site Reliability Engineer.
            Your task is to write a professional post-mortem report in Markdown format based SOLELY on the provided JSON facts.
            
            CRITICAL INSTRUCTIONS:
            - Do NOT invent or hallucinate any details that are not present in the JSON.
            - If a field is `null` in the JSON, completely omit that section from the Markdown report.
            - Format the report using standard Markdown (e.g., # for main title, ## for sections).
            - Do not wrap the response in a markdown code block (do not start with ```markdown).
            
            The structure should generally follow this outline (omitting missing sections):
            # [Title]
            **Severity:** [Severity] | **Status:** [Status]
            
            ## Summary
            [Summary]
            
            ## Impact
            [Impact]
            
            ## Detection
            [Detection]
            
            ## Timeline
            [Timeline]
            
            ## Root Cause
            [Root Cause]
            
            ## Contributing Factors
            [Contributing Factors]
            
            ## Action Items
            [Action Items]
            
            ## Lessons Learned
            [Lessons Learned]
            """;

    public RedactionPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Builds the prompt instructing the LLM to write the Markdown report from facts.
     *
     * @param facts The extracted facts JSON object.
     * @return The complete prompt ready to be sent to the LLM.
     * @throws IllegalArgumentException if facts is null or cannot be serialized.
     */
    public String buildPrompt(ExtractedFacts facts) {
        if (facts == null) {
            throw new IllegalArgumentException("ExtractedFacts cannot be null");
        }
        try {
            String jsonFacts = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(facts);
            return REDACTION_SYSTEM_PROMPT + "\n\n--- EXTRACTED FACTS (JSON) START ---\n" + jsonFacts + "\n--- EXTRACTED FACTS (JSON) END ---\n";
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize ExtractedFacts to JSON", e);
        }
    }
}
