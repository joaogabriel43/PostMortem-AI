package com.postmortemai.unit.parser;

import com.postmortemai.domain.enums.LogFormat;
import com.postmortemai.domain.model.ParsedLogContents;
import com.postmortemai.domain.model.ParsedLogLine;
import com.postmortemai.infrastructure.parser.JavaStackTraceParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JavaStackTraceParser")
class JavaStackTraceParserTest {

    private JavaStackTraceParser parser;

    @BeforeEach
    void setUp() {
        parser = new JavaStackTraceParser();
    }

    // ── supports() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("supports JAVA_STACK_TRACE format")
    void supports_java_stack_trace() {
        assertThat(parser.supports(LogFormat.JAVA_STACK_TRACE)).isTrue();
    }

    @Test
    @DisplayName("does not support JSON or PLAIN_TEXT")
    void does_not_support_other_formats() {
        assertThat(parser.supports(LogFormat.JSON)).isFalse();
        assertThat(parser.supports(LogFormat.PLAIN_TEXT)).isFalse();
    }

    // ── parse() — grouping ────────────────────────────────────────────────────

    @Test
    @DisplayName("groups exception declaration with all 'at' frames into a single ParsedLogLine")
    void groups_exception_block_into_single_line() {
        String raw = """
                java.lang.NullPointerException: Null ref
                \tat com.example.Service.doWork(Service.java:42)
                \tat com.example.Controller.handle(Controller.java:18)
                """;

        ParsedLogContents result = parser.parse(raw);

        assertThat(result.format()).isEqualTo(LogFormat.JAVA_STACK_TRACE);
        // The entire exception block must be a single entry
        assertThat(result.lines()).hasSize(1);
        assertThat(result.lines().get(0).message()).contains("NullPointerException");
        assertThat(result.lines().get(0).message()).contains("at com.example.Service.doWork");
    }

    @Test
    @DisplayName("handles Caused by chains as part of the same block")
    void groups_caused_by_chain() {
        String raw = """
                java.lang.RuntimeException: Outer error
                \tat com.example.A.method(A.java:10)
                Caused by: java.io.IOException: File not found
                \tat com.example.B.read(B.java:55)
                \t... 12 more
                """;

        ParsedLogContents result = parser.parse(raw);

        assertThat(result.lines()).hasSize(1);
        String message = result.lines().get(0).message();
        assertThat(message).contains("RuntimeException");
        assertThat(message).contains("Caused by");
        assertThat(message).contains("IOException");
    }

    @Test
    @DisplayName("treats non-stack-trace lines as individual entries")
    void treats_plain_lines_as_individual_entries() {
        String raw = """
                2026-05-16 INFO Application started
                java.lang.Exception: Something went wrong
                \tat com.example.Foo.bar(Foo.java:1)
                2026-05-16 WARN Recovery attempted
                """;

        ParsedLogContents result = parser.parse(raw);

        assertThat(result.lines()).hasSize(3);
        assertThat(result.lines().get(0).message()).contains("Application started");
        assertThat(result.lines().get(1).message()).contains("Exception");
        assertThat(result.lines().get(2).message()).contains("Recovery attempted");
    }

    @Test
    @DisplayName("extracts log level from bracketed prefix [ERROR]")
    void extracts_level_from_bracket_prefix() {
        String raw = "[ERROR] java.lang.IllegalStateException: bad state\n\tat com.example.A.b(A.java:1)";

        ParsedLogContents result = parser.parse(raw);

        assertThat(result.lines()).hasSize(1);
        assertThat(result.lines().get(0).level()).contains("ERROR");
    }

    @Test
    @DisplayName("handles empty input gracefully")
    void handles_empty_input() {
        ParsedLogContents result = parser.parse("");

        assertThat(result.format()).isEqualTo(LogFormat.JAVA_STACK_TRACE);
        assertThat(result.lines()).isEmpty();
    }

    @Test
    @DisplayName("throws IllegalArgumentException for null input")
    void throws_on_null_input() {
        assertThatIllegalArgumentException().isThrownBy(() -> parser.parse(null));
    }

    @Test
    @DisplayName("parses realistic stacktrace.log sample file")
    void parses_realistic_sample_file() throws Exception {
        String raw = new String(
                getClass().getResourceAsStream("/logs/stacktrace.log").readAllBytes()
        );

        ParsedLogContents result = parser.parse(raw);

        assertThat(result.format()).isEqualTo(LogFormat.JAVA_STACK_TRACE);
        // Must have at least one NPE block and some plain lines
        assertThat(result.lines().size()).isGreaterThanOrEqualTo(2);

        // At least one entry must contain the NPE — proving exception grouping occurred
        List<ParsedLogLine> exceptionEntries = result.lines().stream()
                .filter(l -> l.message().contains("NullPointerException"))
                .toList();
        assertThat(exceptionEntries).isNotEmpty();

        // After grouping, the 'at' frames must be INSIDE the exception block message,
        // not as standalone entries whose entire message is just an "at" frame
        boolean hasStandaloneAtFrame = result.lines().stream()
                .anyMatch(l -> l.message().strip().startsWith("at ")
                        && !l.message().contains("Exception")
                        && !l.message().contains("Error"));
        assertThat(hasStandaloneAtFrame)
                .as("'at' frames should be grouped inside exception blocks, not standalone")
                .isFalse();
    }
}
