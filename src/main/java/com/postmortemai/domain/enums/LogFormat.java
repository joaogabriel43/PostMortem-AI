package com.postmortemai.domain.enums;

/**
 * Supported log format types for automatic detection and parsing.
 * Used by {@code LogFormatDetector} and the {@code LogParser} strategy hierarchy.
 */
public enum LogFormat {

    /** Structured JSON — one object per line or a JSON array. */
    JSON,

    /** Java exception block — signature line followed by {@code at } frames. */
    JAVA_STACK_TRACE,

    /** Unstructured plain text — fallback when no specific format is detected. */
    PLAIN_TEXT
}
