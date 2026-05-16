package com.postmortemai.infrastructure.parser.detector;

import com.postmortemai.domain.enums.LogFormat;
import org.springframework.stereotype.Component;

/**
 * Heuristic detector that classifies a raw log string into a {@link LogFormat}.
 *
 * <p>Detection uses short-circuit rules in the following priority order:
 * <ol>
 *   <li>If the first non-blank line starts with {@code {}, classify as {@link LogFormat#JSON}.</li>
 *   <li>If any line contains an exception declaration AND the next non-blank line
 *       starts with {@code at }, classify as {@link LogFormat#JAVA_STACK_TRACE}.</li>
 *   <li>Otherwise fall back to {@link LogFormat#PLAIN_TEXT}.</li>
 * </ol>
 *
 * <p>This class is stateless and thread-safe. Instances may be shared freely.
 */
@Component
public class LogFormatDetector {

    /** Patterns that indicate the start of a Java exception declaration. */
    private static final String[] EXCEPTION_INDICATORS = {
            "Exception", "Error", "Throwable", "Caused by:"
    };

    /**
     * Detects the format of the given raw log content.
     *
     * @param rawLog the raw log string to inspect; must not be null
     * @return the detected {@link LogFormat}; never null
     * @throws IllegalArgumentException if {@code rawLog} is null
     */
    public LogFormat detect(String rawLog) {
        if (rawLog == null) throw new IllegalArgumentException("rawLog must not be null");

        String[] lines = rawLog.split("\n", -1);

        // ── Rule 1: JSON detection ────────────────────────────────────────────
        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) continue;
            if (trimmed.startsWith("{") || trimmed.startsWith("[{")) {
                return LogFormat.JSON;
            }
            break; // Only inspect the first non-blank line
        }

        // ── Rule 2: Java stack trace detection ────────────────────────────────
        for (int i = 0; i < lines.length; i++) {
            if (looksLikeExceptionDeclaration(lines[i])) {
                // Look ahead for an "at " frame within the next few lines
                for (int j = i + 1; j < Math.min(i + 5, lines.length); j++) {
                    String next = lines[j].strip();
                    if (next.startsWith("at ") || next.startsWith("\tat ")) {
                        return LogFormat.JAVA_STACK_TRACE;
                    }
                    if (!next.isEmpty()) break; // Non-blank, non-frame line — stop lookahead
                }
            }
        }

        // ── Rule 3: Fallback ──────────────────────────────────────────────────
        return LogFormat.PLAIN_TEXT;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean looksLikeExceptionDeclaration(String line) {
        for (String indicator : EXCEPTION_INDICATORS) {
            if (line.contains(indicator)) {
                return true;
            }
        }
        return false;
    }
}
