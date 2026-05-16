package com.postmortemai.unit.parser;

import com.postmortemai.domain.enums.LogFormat;
import com.postmortemai.domain.model.ParsedLogContents;
import com.postmortemai.infrastructure.parser.JsonLogParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JsonLogParser")
class JsonLogParserTest {

    private JsonLogParser parser;

    @BeforeEach
    void setUp() {
        parser = new JsonLogParser();
    }

    // ── supports() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("supports JSON format")
    void supports_json() {
        assertThat(parser.supports(LogFormat.JSON)).isTrue();
    }

    @Test
    @DisplayName("does not support PLAIN_TEXT or JAVA_STACK_TRACE")
    void does_not_support_other_formats() {
        assertThat(parser.supports(LogFormat.PLAIN_TEXT)).isFalse();
        assertThat(parser.supports(LogFormat.JAVA_STACK_TRACE)).isFalse();
    }

    // ── parse() — NDJSON ──────────────────────────────────────────────────────

    @Test
    @DisplayName("parses NDJSON log with timestamp, level, message")
    void parses_ndjson_with_all_fields() {
        String raw = """
                {"timestamp":"2026-05-16T10:00:00.000Z","level":"INFO","message":"App started"}
                {"timestamp":"2026-05-16T10:00:01.000Z","level":"ERROR","message":"DB failed"}
                """;

        ParsedLogContents result = parser.parse(raw);

        assertThat(result.format()).isEqualTo(LogFormat.JSON);
        assertThat(result.lines()).hasSize(2);
        assertThat(result.lines().get(0).message()).isEqualTo("App started");
        assertThat(result.lines().get(0).level()).contains("INFO");
        assertThat(result.lines().get(1).message()).isEqualTo("DB failed");
        assertThat(result.lines().get(1).level()).contains("ERROR");
    }

    @Test
    @DisplayName("extracts timestamp as Instant when ISO-8601 format is used")
    void parses_timestamp_as_instant() {
        String raw = """
                {"timestamp":"2026-05-16T10:00:00.000Z","level":"WARN","message":"High memory"}
                """;

        ParsedLogContents result = parser.parse(raw);

        assertThat(result.lines().get(0).timestamp()).isPresent();
    }

    @Test
    @DisplayName("falls back to plain text when a line is not valid JSON")
    void fallback_plain_text_for_invalid_json_line() {
        String raw = "this is not json at all\n{\"level\":\"INFO\",\"message\":\"OK\"}";

        ParsedLogContents result = parser.parse(raw);

        assertThat(result.format()).isEqualTo(LogFormat.JSON);
        assertThat(result.lines()).hasSize(2);
        assertThat(result.lines().get(0).message()).isEqualTo("this is not json at all");
        assertThat(result.lines().get(0).level()).isEmpty();
    }

    @Test
    @DisplayName("parses JSON array layout")
    void parses_json_array() {
        String raw = """
                [
                  {"level":"INFO","message":"First"},
                  {"level":"WARN","message":"Second"}
                ]
                """;

        ParsedLogContents result = parser.parse(raw.trim());

        assertThat(result.lines()).hasSize(2);
        assertThat(result.lines().get(0).message()).isEqualTo("First");
        assertThat(result.lines().get(1).message()).isEqualTo("Second");
    }

    @Test
    @DisplayName("handles empty input gracefully")
    void handles_empty_input() {
        ParsedLogContents result = parser.parse("");

        assertThat(result.format()).isEqualTo(LogFormat.JSON);
        assertThat(result.lines()).isEmpty();
    }

    @Test
    @DisplayName("throws IllegalArgumentException for null input")
    void throws_on_null_input() {
        assertThatIllegalArgumentException().isThrownBy(() -> parser.parse(null));
    }

    @Test
    @DisplayName("supports alternative field names: msg, severity, @timestamp")
    void supports_alternative_field_names() {
        String raw = "{\"@timestamp\":\"2026-05-16T10:00:00.000Z\",\"severity\":\"ERROR\",\"msg\":\"Crash\"}";

        ParsedLogContents result = parser.parse(raw);

        assertThat(result.lines().get(0).level()).contains("ERROR");
        assertThat(result.lines().get(0).message()).isEqualTo("Crash");
        assertThat(result.lines().get(0).timestamp()).isPresent();
    }
}
