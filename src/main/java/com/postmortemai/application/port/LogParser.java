package com.postmortemai.application.port;

import com.postmortemai.domain.enums.LogFormat;
import com.postmortemai.domain.model.ParsedLogContents;

/**
 * Strategy interface for log parsers.
 *
 * <p>Each concrete implementation handles exactly one {@link LogFormat}.
 * The {@code LogFormatDetector} selects the appropriate parser at runtime —
 * callers never instantiate parsers directly (ADR-001).
 *
 * <p>Implementations must be stateless and thread-safe.
 */
public interface LogParser {

    /**
     * Returns {@code true} if this parser can handle the given format.
     *
     * @param format the format detected by {@code LogFormatDetector}
     * @return whether this parser supports the format
     */
    boolean supports(LogFormat format);

    /**
     * Parses the raw log string into a structured {@link ParsedLogContents}.
     *
     * @param rawLog the raw log content as a single string; must not be null
     * @return the structured parse result; never null
     * @throws IllegalArgumentException if {@code rawLog} is null
     */
    ParsedLogContents parse(String rawLog);
}
