package com.postmortemai.infrastructure.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postmortemai.application.port.LogParser;
import com.postmortemai.domain.enums.LogFormat;
import com.postmortemai.domain.model.ParsedLogContents;
import com.postmortemai.domain.model.ParsedLogLine;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parser for structured JSON logs.
 *
 * <p>Supports two JSON layouts:
 * <ul>
 *   <li>Newline-delimited JSON (NDJSON) — one JSON object per line.</li>
 *   <li>A top-level JSON array of log objects.</li>
 * </ul>
 *
 * <p>Expected fields per log object (all optional):
 * <ul>
 *   <li>{@code timestamp} or {@code time} or {@code @timestamp} — parsed as {@link Instant}</li>
 *   <li>{@code level} or {@code severity} or {@code log.level}</li>
 *   <li>{@code message} or {@code msg} or {@code log}</li>
 * </ul>
 *
 * <p>Lines that cannot be parsed as valid JSON are included as plain-text
 * {@link ParsedLogLine} entries to avoid silent data loss.
 */
@Component
public class JsonLogParser implements LogParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Known field names for each semantic field
    private static final List<String> TIMESTAMP_FIELDS = List.of("timestamp", "time", "@timestamp", "ts");
    private static final List<String> LEVEL_FIELDS     = List.of("level", "severity", "log.level", "loglevel");
    private static final List<String> MESSAGE_FIELDS   = List.of("message", "msg", "log", "body");

    @Override
    public boolean supports(LogFormat format) {
        return LogFormat.JSON == format;
    }

    @Override
    public ParsedLogContents parse(String rawLog) {
        if (rawLog == null) throw new IllegalArgumentException("rawLog must not be null");

        String trimmed = rawLog.trim();
        List<ParsedLogLine> lines = new ArrayList<>();

        if (trimmed.startsWith("[")) {
            // Try to parse as a JSON array
            lines.addAll(parseArray(trimmed));
        } else {
            // Parse line by line (NDJSON)
            for (String line : rawLog.split("\n", -1)) {
                String l = line.strip();
                if (l.isEmpty()) continue;
                lines.add(parseJsonLine(l));
            }
        }

        return new ParsedLogContents(LogFormat.JSON, lines);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<ParsedLogLine> parseArray(String json) {
        List<ParsedLogLine> result = new ArrayList<>();
        try {
            JsonNode array = MAPPER.readTree(json);
            if (array.isArray()) {
                for (JsonNode node : array) {
                    result.add(toLogLine(node));
                }
            } else {
                result.add(toLogLine(array));
            }
        } catch (JsonProcessingException e) {
            // Fallback — treat whole string as unstructured
            result.add(ParsedLogLine.of(json));
        }
        return result;
    }

    private ParsedLogLine parseJsonLine(String line) {
        try {
            JsonNode node = MAPPER.readTree(line);
            return toLogLine(node);
        } catch (JsonProcessingException e) {
            // Line is not valid JSON — preserve as plain text to avoid data loss
            return ParsedLogLine.of(line);
        }
    }

    private ParsedLogLine toLogLine(JsonNode node) {
        Optional<Instant> timestamp = extractTimestamp(node);
        Optional<String>  level     = extractField(node, LEVEL_FIELDS);
        String            message   = extractField(node, MESSAGE_FIELDS).orElse(node.toString());
        return new ParsedLogLine(timestamp, level, message);
    }

    private Optional<Instant> extractTimestamp(JsonNode node) {
        for (String field : TIMESTAMP_FIELDS) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                try {
                    return Optional.of(Instant.parse(value.asText()));
                } catch (Exception ignored) {
                    // Not a valid ISO-8601 instant — skip
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractField(JsonNode node, List<String> candidates) {
        for (String field : candidates) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return Optional.of(value.asText());
            }
        }
        return Optional.empty();
    }
}
