package com.postmortemai.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.postmortemai.application.exception.OpenAiResponseException;
import com.postmortemai.infrastructure.config.OpenAiProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

/**
 * Resilient client to communicate with the OpenAI API.
 * Uses RestClient and is protected by Resilience4j Retry and CircuitBreaker policies.
 */
@Component
public class OpenAIClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAIClient.class);
    private static final String RESILIENCE_INSTANCE = "openai";

    private final RestClient restClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAIClient(RestClient restClient, OpenAiProperties properties, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends a prompt to the OpenAI chat completions API.
     * Protected by Retry and CircuitBreaker.
     *
     * @param prompt The complete prompt to send.
     * @return The text content of the response.
     */
    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public String callChatCompletion(String prompt) {
        try {
            OpenAiRequest request = new OpenAiRequest(
                    "gpt-4o-mini",
                    List.of(new OpenAiMessage("user", prompt)),
                    0.1 // Low temperature for deterministic output
            );

            OpenAiResponse response = restClient.post()
                    .uri(properties.getBaseUrl() + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(OpenAiResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new OpenAiResponseException("OpenAI API returned an empty response body or no choices");
            }

            return response.choices().get(0).message().content();

        } catch (RestClientResponseException e) {
            log.error("Failed to communicate with OpenAI API. Status: {}", e.getStatusCode());
            throw new OpenAiResponseException("OpenAI API call failed with status " + e.getStatusCode().value(), e);
        } catch (Exception e) {
            if (e instanceof OpenAiResponseException) {
                throw e;
            }
            log.error("Unexpected error calling OpenAI API", e);
            throw new OpenAiResponseException("Unexpected error when communicating with OpenAI", e);
        }
    }

    // ── Internal DTOs for OpenAI API ──────────────────────────────────────────

    private record OpenAiRequest(String model, List<OpenAiMessage> messages, double temperature) {}
    private record OpenAiMessage(String role, String content) {}
    private record OpenAiResponse(List<OpenAiChoice> choices) {}
    private record OpenAiChoice(OpenAiMessage message) {}
}
