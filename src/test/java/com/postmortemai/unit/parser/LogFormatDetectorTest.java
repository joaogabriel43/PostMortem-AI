package com.postmortemai.unit.parser;

import com.postmortemai.domain.enums.LogFormat;
import com.postmortemai.infrastructure.parser.detector.LogFormatDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LogFormatDetector")
class LogFormatDetectorTest {

    private LogFormatDetector detector;

    @BeforeEach
    void setUp() {
        detector = new LogFormatDetector();
    }

    // ── JSON detection ────────────────────────────────────────────────────────

    @Test
    @DisplayName("detects JSON when first non-blank line starts with '{'")
    void detects_json_from_opening_brace() {
        String raw = """
                {"level":"INFO","message":"App started"}
                {"level":"ERROR","message":"DB failed"}
                """;

        assertThat(detector.detect(raw)).isEqualTo(LogFormat.JSON);
    }

    @Test
    @DisplayName("detects JSON when first non-blank line starts with '[{'")
    void detects_json_array_format() {
        String raw = "[{\"level\":\"INFO\",\"message\":\"Ok\"}]";

        assertThat(detector.detect(raw)).isEqualTo(LogFormat.JSON);
    }

    @Test
    @DisplayName("ignores leading blank lines when detecting JSON")
    void ignores_leading_blank_lines_for_json() {
        String raw = "\n\n{\"level\":\"WARN\",\"message\":\"slow\"}";

        assertThat(detector.detect(raw)).isEqualTo(LogFormat.JSON);
    }

    // ── Java stack trace detection ────────────────────────────────────────────

    @Test
    @DisplayName("detects JAVA_STACK_TRACE when exception line is followed by 'at ' frame")
    void detects_java_stack_trace() {
        String raw = """
                java.lang.NullPointerException: null ref
                \tat com.example.Service.run(Service.java:42)
                """;

        assertThat(detector.detect(raw)).isEqualTo(LogFormat.JAVA_STACK_TRACE);
    }

    @Test
    @DisplayName("detects JAVA_STACK_TRACE for 'Caused by' pattern")
    void detects_caused_by_pattern() {
        String raw = """
                2026-05-16 ERROR Request failed
                Caused by: java.io.IOException: timeout
                \tat com.example.Client.connect(Client.java:10)
                """;

        assertThat(detector.detect(raw)).isEqualTo(LogFormat.JAVA_STACK_TRACE);
    }

    @Test
    @DisplayName("does NOT detect JAVA_STACK_TRACE if 'Exception' appears without following 'at' frame")
    void does_not_detect_stack_trace_without_at_frame() {
        String raw = """
                An Exception was logged in the monitoring system.
                No stack frame follows.
                Normal log line here.
                """;

        // No "at " after the Exception mention → should not classify as stack trace
        assertThat(detector.detect(raw)).isEqualTo(LogFormat.PLAIN_TEXT);
    }

    // ── Plain text fallback ───────────────────────────────────────────────────

    @Test
    @DisplayName("falls back to PLAIN_TEXT for unstructured logs")
    void falls_back_to_plain_text() {
        String raw = """
                2026-05-16 INFO  Application started
                2026-05-16 WARN  Memory usage high
                2026-05-16 ERROR Disk full
                """;

        assertThat(detector.detect(raw)).isEqualTo(LogFormat.PLAIN_TEXT);
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("returns PLAIN_TEXT for empty string")
    void returns_plain_text_for_empty_string() {
        assertThat(detector.detect("")).isEqualTo(LogFormat.PLAIN_TEXT);
    }

    @Test
    @DisplayName("returns PLAIN_TEXT for whitespace-only input")
    void returns_plain_text_for_whitespace_only() {
        assertThat(detector.detect("   \n  \n  ")).isEqualTo(LogFormat.PLAIN_TEXT);
    }

    @Test
    @DisplayName("throws IllegalArgumentException for null input")
    void throws_on_null_input() {
        assertThatIllegalArgumentException().isThrownBy(() -> detector.detect(null));
    }

    @Test
    @DisplayName("correctly detects JSON even when later lines contain exception keywords")
    void json_takes_priority_over_exception_keywords() {
        String raw = """
                {"level":"ERROR","message":"NullPointerException occurred"}
                {"level":"ERROR","message":"at com.example.Foo.bar(Foo.java:1)"}
                """;

        // JSON is checked first — the "Exception" inside a JSON string must not override
        assertThat(detector.detect(raw)).isEqualTo(LogFormat.JSON);
    }
}
