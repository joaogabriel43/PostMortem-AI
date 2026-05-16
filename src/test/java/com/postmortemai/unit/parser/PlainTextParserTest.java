package com.postmortemai.unit.parser;

import com.postmortemai.domain.enums.LogFormat;
import com.postmortemai.domain.model.ParsedLogContents;
import com.postmortemai.infrastructure.parser.PlainTextParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PlainTextParser")
class PlainTextParserTest {

    private PlainTextParser parser;

    @BeforeEach
    void setUp() {
        parser = new PlainTextParser();
    }

    // ── supports() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("supports PLAIN_TEXT format")
    void supports_plain_text() {
        assertThat(parser.supports(LogFormat.PLAIN_TEXT)).isTrue();
    }

    @Test
    @DisplayName("does not support JSON or JAVA_STACK_TRACE")
    void does_not_support_other_formats() {
        assertThat(parser.supports(LogFormat.JSON)).isFalse();
        assertThat(parser.supports(LogFormat.JAVA_STACK_TRACE)).isFalse();
    }

    // ── parse() ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("parses each non-blank line into a separate ParsedLogLine")
    void parses_each_line_individually() {
        String raw = """
                2026-05-16 INFO  App started
                2026-05-16 ERROR DB connection failed
                2026-05-16 WARN  Retry attempt 1
                """;

        ParsedLogContents result = parser.parse(raw);

        assertThat(result.format()).isEqualTo(LogFormat.PLAIN_TEXT);
        assertThat(result.lines()).hasSize(3);
        assertThat(result.lines().get(0).message()).contains("App started");
        assertThat(result.lines().get(1).message()).contains("DB connection failed");
    }

    @Test
    @DisplayName("extracts log level from [LEVEL] bracket notation")
    void extracts_level_from_bracket_notation() {
        String raw = "[ERROR] Something went wrong\n[INFO] All good";

        ParsedLogContents result = parser.parse(raw);

        assertThat(result.lines().get(0).level()).contains("ERROR");
        assertThat(result.lines().get(1).level()).contains("INFO");
    }

    @Test
    @DisplayName("extracts log level from bare level token followed by colon or space")
    void extracts_level_from_bare_token() {
        String raw = "WARN: High memory usage\nDEBUG Loaded 500 entries";

        ParsedLogContents result = parser.parse(raw);

        assertThat(result.lines().get(0).level()).contains("WARN");
        assertThat(result.lines().get(1).level()).contains("DEBUG");
    }

    @Test
    @DisplayName("skips blank lines")
    void skips_blank_lines() {
        String raw = "First line\n\n\nSecond line\n";

        ParsedLogContents result = parser.parse(raw);

        assertThat(result.lines()).hasSize(2);
    }

    @Test
    @DisplayName("returns empty result for blank input")
    void returns_empty_for_blank_input() {
        ParsedLogContents result = parser.parse("   \n  \n");

        assertThat(result.lines()).isEmpty();
    }

    @Test
    @DisplayName("throws IllegalArgumentException for null input")
    void throws_on_null_input() {
        assertThatIllegalArgumentException().isThrownBy(() -> parser.parse(null));
    }

    @Test
    @DisplayName("parses realistic standard.log file without errors")
    void parses_realistic_standard_log() throws Exception {
        String raw = new String(
                getClass().getResourceAsStream("/logs/standard.log").readAllBytes()
        );

        ParsedLogContents result = parser.parse(raw);

        assertThat(result.format()).isEqualTo(LogFormat.PLAIN_TEXT);
        assertThat(result.lines()).isNotEmpty();

        // All lines that have a known level should have it populated
        long linesWithLevel = result.lines().stream()
                .filter(l -> l.level().isPresent())
                .count();
        assertThat(linesWithLevel).isGreaterThan(0);
    }

    @Test
    @DisplayName("level is absent when line has no recognisable level token")
    void level_is_absent_when_unrecognised() {
        String raw = "just some free text with no level";

        ParsedLogContents result = parser.parse(raw);

        assertThat(result.lines().get(0).level()).isEmpty();
    }
}
