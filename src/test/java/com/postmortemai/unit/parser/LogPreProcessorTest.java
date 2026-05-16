package com.postmortemai.unit.parser;

import com.postmortemai.infrastructure.parser.LogPreProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LogPreProcessor")
class LogPreProcessorTest {

    private LogPreProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new LogPreProcessor();
    }

    // ── Critical window preservation ──────────────────────────────────────────

    @Test
    @DisplayName("returns input unchanged when total lines <= 100 (both windows fit)")
    void returns_unchanged_when_fits_in_windows() {
        String raw = buildLog(99, "INFO", "Regular line");

        String result = processor.process(raw);

        assertThat(result).isEqualTo(raw);
    }

    @Test
    @DisplayName("preserves all first 50 lines verbatim even if they are DEBUG/TRACE")
    void preserves_head_window_debug_lines() {
        // Build 150 lines: first 50 are DEBUG, middle 50 are INFO, last 50 are INFO
        String raw = buildMixedLog(50, "DEBUG", 50, "INFO", 50, "INFO");

        String result = processor.process(raw);
        List<String> resultLines = Arrays.asList(result.split("\n"));

        // The first 50 lines must all be present (DEBUG preserved in head window)
        long debugInHead = resultLines.subList(0, 50).stream()
                .filter(l -> l.contains("DEBUG"))
                .count();
        assertThat(debugInHead).isEqualTo(50);
    }

    @Test
    @DisplayName("preserves all last 50 lines verbatim even if they are DEBUG/TRACE")
    void preserves_tail_window_debug_lines() {
        // Build 150 lines: first 50 INFO, middle 50 INFO, last 50 DEBUG
        String raw = buildMixedLog(50, "INFO", 50, "INFO", 50, "DEBUG");

        String result = processor.process(raw);
        List<String> resultLines = Arrays.asList(result.split("\n"));

        // The last 50 lines in the result must all be DEBUG
        int resultSize = resultLines.size();
        long debugInTail = resultLines.subList(resultSize - 50, resultSize).stream()
                .filter(l -> l.contains("DEBUG"))
                .count();
        assertThat(debugInTail).isEqualTo(50);
    }

    @Test
    @DisplayName("head window is exactly 50 lines — line 51 onward is subject to filtering")
    void head_window_boundary_is_exact() {
        // 120 total: head=50 (INFO), middle=20 (DEBUG should be filtered), tail=50 (INFO)
        String raw = buildMixedLog(50, "INFO", 20, "DEBUG", 50, "INFO");
        String[] originalLines = raw.split("\n");

        String result = processor.process(raw);
        List<String> resultLines = Arrays.asList(result.split("\n"));

        // Head window lines 1-50 must still be present
        assertThat(resultLines.subList(0, 50))
                .containsExactlyElementsOf(Arrays.asList(originalLines).subList(0, 50));

        // DEBUG lines in the middle should have been filtered out
        long debugInMiddle = resultLines.subList(50, resultLines.size() - 50).stream()
                .filter(l -> l.contains("DEBUG"))
                .count();
        assertThat(debugInMiddle).isZero();
    }

    // ── Deduplication ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("removes consecutive duplicate lines from the middle section")
    void removes_consecutive_duplicates_in_middle() {
        // 120 total: head=50, middle=20 lines (10 unique duplicated), tail=50
        String headAndTail = buildLog(50, "INFO", "Head line");
        String middle = IntStream.range(0, 10)
                .mapToObj(i -> "ERROR Duplicate error\nERROR Duplicate error")
                .collect(Collectors.joining("\n"));
        String raw = headAndTail + "\n" + middle + "\n" + headAndTail;

        String result = processor.process(raw);
        List<String> resultLines = Arrays.asList(result.split("\n"));

        // In the middle section, duplicate should appear only once per pair
        long duplicateCount = resultLines.stream()
                .filter(l -> l.equals("ERROR Duplicate error"))
                .count();
        assertThat(duplicateCount).isLessThan(20); // Was 20, should be fewer after dedup
    }

    @Test
    @DisplayName("does NOT remove non-consecutive duplicates")
    void keeps_non_consecutive_duplicates() {
        // Build exactly 103 lines: 50 head + 3 middle + 50 tail
        // Middle: A, B, A — the two A's are NOT consecutive, so both must survive
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) sb.append("INFO Head line ").append(i).append("\n");
        sb.append("INFO line A\n");
        sb.append("INFO line B\n");
        sb.append("INFO line A\n"); // non-consecutive duplicate
        for (int i = 0; i < 50; i++) sb.append("INFO Tail line ").append(i).append("\n");
        String raw = sb.toString().stripTrailing();

        // Sanity check: raw has exactly 103 lines
        assertThat(raw.split("\n")).hasSize(103);

        String result = processor.process(raw);

        long countA = Arrays.stream(result.split("\n"))
                .filter(l -> l.equals("INFO line A"))
                .count();
        assertThat(countA).isEqualTo(2); // Both occurrences must survive
    }

    // ── Level filtering ───────────────────────────────────────────────────────

    @Test
    @DisplayName("removes DEBUG lines from the middle section")
    void removes_debug_from_middle() {
        String raw = buildMixedLog(50, "INFO", 10, "DEBUG", 50, "INFO");

        String result = processor.process(raw);
        List<String> middle = Arrays.asList(result.split("\n")).subList(50, result.split("\n").length - 50);

        assertThat(middle).noneMatch(l -> l.contains("DEBUG"));
    }

    @Test
    @DisplayName("removes TRACE lines from the middle section")
    void removes_trace_from_middle() {
        String raw = buildMixedLog(50, "INFO", 10, "TRACE", 50, "INFO");

        String result = processor.process(raw);
        List<String> allLines = Arrays.asList(result.split("\n"));
        List<String> middle = allLines.subList(50, allLines.size() - 50);

        assertThat(middle).noneMatch(l -> l.contains("TRACE"));
    }

    @Test
    @DisplayName("preserves exception lines that are part of a stack trace block")
    void preserves_exception_inside_middle_section() {
        // Build 102 lines: 50 head + 2 middle (exception block) + 50 tail
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) sb.append("INFO Head ").append(i).append("\n");
        sb.append("java.lang.Exception: something\n");
        sb.append("at com.example.Foo.bar(Foo.java:1)\n");
        for (int i = 0; i < 50; i++) sb.append("INFO Tail ").append(i).append("\n");
        String raw = sb.toString().stripTrailing();

        String result = processor.process(raw);

        assertThat(result).contains("java.lang.Exception");
        assertThat(result).contains("at com.example.Foo.bar");
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("handles empty input")
    void handles_empty_input() {
        assertThat(processor.process("")).isEqualTo("");
    }

    @Test
    @DisplayName("throws IllegalArgumentException for null input")
    void throws_on_null_input() {
        assertThatIllegalArgumentException().isThrownBy(() -> processor.process(null));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildLog(int count, String level, String message) {
        return IntStream.range(0, count)
                .mapToObj(i -> level + " " + message + " " + i)
                .collect(Collectors.joining("\n"));
    }

    private String buildMixedLog(int headCount, String headLevel,
                                  int midCount, String midLevel,
                                  int tailCount, String tailLevel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < headCount; i++) sb.append(headLevel).append(" Head line ").append(i).append("\n");
        for (int i = 0; i < midCount;  i++) sb.append(midLevel).append(" Mid line ").append(i).append("\n");
        for (int i = 0; i < tailCount; i++) sb.append(tailLevel).append(" Tail line ").append(i).append("\n");
        return sb.toString().stripTrailing();
    }
}
