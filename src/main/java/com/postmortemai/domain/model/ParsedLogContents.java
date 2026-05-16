package com.postmortemai.domain.model;

import com.postmortemai.domain.enums.LogFormat;

import java.util.List;

/**
 * Immutable container for the full result of a log parsing operation.
 *
 * <p>This is the output contract of every {@code LogParser} implementation
 * and the input for the pre-processor and the AI pipeline.
 *
 * @param format  The format that was detected or explicitly used for parsing.
 * @param lines   The ordered list of parsed log lines. Never null, may be empty.
 */
public record ParsedLogContents(
        LogFormat format,
        List<ParsedLogLine> lines
) {

    /**
     * Compact constructor — enforces non-null invariants and immutability.
     */
    public ParsedLogContents {
        if (format == null) throw new IllegalArgumentException("format must not be null");
        if (lines == null)  throw new IllegalArgumentException("lines must not be null");
        lines = List.copyOf(lines); // defensive immutable copy
    }
}
