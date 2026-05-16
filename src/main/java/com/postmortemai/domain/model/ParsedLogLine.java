package com.postmortemai.domain.model;

import java.time.Instant;
import java.util.Optional;

/**
 * Immutable representation of a single parsed log line.
 *
 * <p>All fields except {@code message} are optional because not every log
 * format carries a structured timestamp or explicit log level.
 *
 * @param timestamp  The parsed instant of the log event, if present.
 * @param level      The log level (e.g., INFO, WARN, ERROR), if present.
 * @param message    The full log message text. Never null, may be empty.
 */
public record ParsedLogLine(
        Optional<Instant> timestamp,
        Optional<String> level,
        String message
) {

    /**
     * Compact constructor — enforces non-null invariants.
     */
    public ParsedLogLine {
        if (timestamp == null) throw new IllegalArgumentException("timestamp must not be null (use Optional.empty())");
        if (level == null)     throw new IllegalArgumentException("level must not be null (use Optional.empty())");
        if (message == null)   throw new IllegalArgumentException("message must not be null");
    }

    /**
     * Convenience factory for lines with no timestamp and no explicit level.
     */
    public static ParsedLogLine of(String message) {
        return new ParsedLogLine(Optional.empty(), Optional.empty(), message);
    }

    /**
     * Convenience factory for lines with an explicit level but no timestamp.
     */
    public static ParsedLogLine of(String level, String message) {
        return new ParsedLogLine(Optional.empty(), Optional.of(level), message);
    }
}
