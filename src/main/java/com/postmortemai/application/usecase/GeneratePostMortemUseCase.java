package com.postmortemai.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postmortemai.application.dto.ExtractedFacts;
import com.postmortemai.application.port.IncidentRepositoryPort;
import com.postmortemai.application.port.LogParser;
import com.postmortemai.application.port.PostMortemRepositoryPort;
import com.postmortemai.domain.enums.IncidentSeverity;
import com.postmortemai.domain.enums.IncidentStatus;
import com.postmortemai.domain.enums.LogFormat;
import com.postmortemai.domain.model.Incident;
import com.postmortemai.domain.model.ParsedLogContents;
import com.postmortemai.domain.model.ParsedLogLine;
import com.postmortemai.domain.model.PostMortem;
import com.postmortemai.infrastructure.ai.OpenAIClient;
import com.postmortemai.infrastructure.ai.prompt.ExtractionPromptBuilder;
import com.postmortemai.infrastructure.ai.prompt.RedactionPromptBuilder;
import com.postmortemai.infrastructure.parser.LogPreProcessor;
import com.postmortemai.infrastructure.parser.detector.LogFormatDetector;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GeneratePostMortemUseCase {

    private final IncidentRepositoryPort incidentRepositoryPort;
    private final PostMortemRepositoryPort postMortemRepositoryPort;
    private final LogFormatDetector logFormatDetector;
    private final List<LogParser> logParsers;
    private final LogPreProcessor logPreProcessor;
    private final ExtractionPromptBuilder extractionPromptBuilder;
    private final RedactionPromptBuilder redactionPromptBuilder;
    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;

    public GeneratePostMortemUseCase(
            IncidentRepositoryPort incidentRepositoryPort,
            PostMortemRepositoryPort postMortemRepositoryPort,
            LogFormatDetector logFormatDetector,
            List<LogParser> logParsers,
            LogPreProcessor logPreProcessor,
            ExtractionPromptBuilder extractionPromptBuilder,
            RedactionPromptBuilder redactionPromptBuilder,
            OpenAIClient openAIClient,
            ObjectMapper objectMapper
    ) {
        this.incidentRepositoryPort = incidentRepositoryPort;
        this.postMortemRepositoryPort = postMortemRepositoryPort;
        this.logFormatDetector = logFormatDetector;
        this.logParsers = logParsers;
        this.logPreProcessor = logPreProcessor;
        this.extractionPromptBuilder = extractionPromptBuilder;
        this.redactionPromptBuilder = redactionPromptBuilder;
        this.openAIClient = openAIClient;
        this.objectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public PostMortem execute(String projectName, String serviceName, String rawLog) {
        String logHash = calculateSha256(rawLog);

        // Idempotency: Check if incident already exists by hash
        Optional<Incident> existingIncident = incidentRepositoryPort.findByRawLogHash(logHash);
        if (existingIncident.isPresent()) {
            Optional<PostMortem> existingPostMortem = postMortemRepositoryPort.findByIncidentId(existingIncident.get().id());
            if (existingPostMortem.isPresent()) {
                return existingPostMortem.get();
            }
        }

        // 1. Detect Format
        LogFormat format = logFormatDetector.detect(rawLog);

        // 2. Parse Log
        LogParser selectedParser = logParsers.stream()
                .filter(p -> p.supports(format))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No parser found for format: " + format));

        ParsedLogContents parsedContents = selectedParser.parse(rawLog);
        
        // Assemble lines into a single string for preprocessing
        String parsedString = parsedContents.lines().stream()
                .map(ParsedLogLine::message)
                .collect(Collectors.joining("\n"));

        // 3. Pre-process (Critical Window & Noise Reduction)
        String preProcessedLogs = logPreProcessor.process(parsedString);

        // 4. Extraction (Prompt 1)
        String extractionPrompt = extractionPromptBuilder.buildPrompt(preProcessedLogs);
        String extractionResponse = openAIClient.callChatCompletion(extractionPrompt);
        
        ExtractedFacts facts = parseJsonSafely(extractionResponse);

        // 5. Redaction (Prompt 2)
        String redactionPrompt = redactionPromptBuilder.buildPrompt(facts);
        String markdownReport = openAIClient.callChatCompletion(redactionPrompt);

        // 6. Save Incident
        IncidentSeverity severity = parseSeverity(facts.severity());
        IncidentStatus status = parseStatus(facts.status());

        Incident incident = new Incident(
                existingIncident.map(Incident::id).orElseGet(UUID::randomUUID),
                projectName,
                serviceName,
                logHash,
                severity,
                status,
                LocalDateTime.now()
        );
        incident = incidentRepositoryPort.save(incident);

        // 7. Save PostMortem
        PostMortem postMortem = new PostMortem(
                UUID.randomUUID(),
                incident.id(),
                facts.title() != null ? facts.title() : "Incident Report",
                facts.summary(),
                facts.timeline(),
                facts.rootCause(),
                facts.impact(),
                facts.detection(),
                facts.contributingFactors(),
                facts.actionItems(),
                facts.lessonsLearned(),
                markdownReport,
                LocalDateTime.now()
        );
        return postMortemRepositoryPort.save(postMortem);
    }

    private ExtractedFacts parseJsonSafely(String jsonResponse) {
        try {
            // Remove markdown code fences if present
            String cleanedJson = jsonResponse.trim();
            if (cleanedJson.startsWith("```json")) {
                cleanedJson = cleanedJson.substring(7);
            } else if (cleanedJson.startsWith("```")) {
                cleanedJson = cleanedJson.substring(3);
            }
            if (cleanedJson.endsWith("```")) {
                cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
            }
            return objectMapper.readValue(cleanedJson.trim(), ExtractedFacts.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse extracted facts JSON from LLM response", e);
        }
    }

    private IncidentSeverity parseSeverity(String sev) {
        if (sev == null) return IncidentSeverity.P3; // default
        try {
            return IncidentSeverity.valueOf(sev.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return IncidentSeverity.P3;
        }
    }

    private IncidentStatus parseStatus(String st) {
        if (st == null) return IncidentStatus.INVESTIGATING; // default
        try {
            return IncidentStatus.valueOf(st.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return IncidentStatus.INVESTIGATING;
        }
    }

    private String calculateSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
