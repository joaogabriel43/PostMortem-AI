package com.postmortemai.infrastructure.parser;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pre-processes raw log strings to reduce noise before sending to the AI pipeline.
 *
 * <p>Applies the following transformations in order:
 * <ol>
 *   <li><b>Critical window preservation</b> — the first and last 50 lines of the
 *       original log are always retained verbatim, regardless of log level. This
 *       protects startup/shutdown context from being filtered away (ADR-003).</li>
 *   <li><b>Deduplication</b> — consecutive identical lines (inside the middle section)
 *       are collapsed to the first occurrence.</li>
 *   <li><b>Level filtering</b> — lines whose level is {@code DEBUG} or {@code TRACE}
 *       are removed from the middle section, unless they are part of an active
 *       exception/stack trace block.</li>
 * </ol>
 *
 * <p>Lines in the critical windows are NOT subject to deduplication or filtering.
 *
 * <p>This class is stateless and thread-safe.
 */
@Component
public class LogPreProcessor {

    /** Number of lines to preserve unconditionally at the head and tail of the log. */
    public static final int CRITICAL_WINDOW_SIZE = 50;

    private static final Set<String> NOISE_LEVELS = Set.of("DEBUG", "TRACE");

    /**
     * Pre-processes the raw log and returns a cleaned version ready for parsing.
     *
     * @param rawLog the raw log content as a single string; must not be null
     * @return the cleaned log string; never null, may be smaller than the input
     * @throws IllegalArgumentException if {@code rawLog} is null
     */
    public String process(String rawLog) {
        if (rawLog == null) throw new IllegalArgumentException("rawLog must not be null");

        String[] lines = rawLog.split("\n", -1);
        int total = lines.length;

        // Short-circuit: if the log fits entirely within one or both critical windows,
        // return it unchanged — no filtering needed.
        if (total <= CRITICAL_WINDOW_SIZE * 2) {
            return rawLog;
        }

        // Split into three segments
        List<String> head   = toList(lines, 0, CRITICAL_WINDOW_SIZE);
        List<String> middle = toList(lines, CRITICAL_WINDOW_SIZE, total - CRITICAL_WINDOW_SIZE);
        List<String> tail   = toList(lines, total - CRITICAL_WINDOW_SIZE, total);

        // Apply transformations only to the middle section
        List<String> processedMiddle = filterMiddle(middle);

        // Reassemble
        List<String> result = new ArrayList<>(head.size() + processedMiddle.size() + tail.size());
        result.addAll(head);
        result.addAll(processedMiddle);
        result.addAll(tail);

        return String.join("\n", result);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Applies deduplication and level-based filtering to the middle section.
     * Stack trace lines (starting with "at " or "Caused by:") are always kept.
     */
    private List<String> filterMiddle(List<String> lines) {
        List<String> filtered = new ArrayList<>();
        String lastLine = null;
        boolean inStackBlock = false;

        for (String line : lines) {
            String stripped = line.strip();

            // Track whether we are inside a stack trace block
            if (isExceptionStart(stripped)) {
                inStackBlock = true;
            } else if (isStackFrame(stripped)) {
                inStackBlock = true; // Continuation of stack block
            } else if (!stripped.isEmpty() && !isStackFrame(stripped) && !isExceptionStart(stripped)) {
                inStackBlock = false;
            }

            // Deduplication — skip consecutively identical lines
            if (stripped.equals(lastLine != null ? lastLine.strip() : null)) {
                continue;
            }

            // Level filtering — drop DEBUG/TRACE unless inside a stack block
            if (!inStackBlock && isNoisyLevel(stripped)) {
                lastLine = line;
                continue;
            }

            filtered.add(line);
            lastLine = line;
        }

        return filtered;
    }

    private boolean isNoisyLevel(String line) {
        String upper = line.toUpperCase();
        for (String level : NOISE_LEVELS) {
            if (upper.contains("[" + level + "]")
                    || upper.startsWith(level + " ")
                    || upper.startsWith(level + ":")) {
                return true;
            }
        }
        return false;
    }

    private boolean isExceptionStart(String line) {
        return line.contains("Exception")
                || line.contains("Error")
                || line.contains("Throwable")
                || line.startsWith("Caused by:");
    }

    private boolean isStackFrame(String line) {
        return line.startsWith("at ")
                || line.startsWith("\tat ")
                || line.startsWith("...");
    }

    private List<String> toList(String[] lines, int from, int to) {
        List<String> list = new ArrayList<>(to - from);
        for (int i = from; i < to; i++) {
            list.add(lines[i]);
        }
        return list;
    }
}
