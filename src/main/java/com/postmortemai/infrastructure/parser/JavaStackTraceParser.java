package com.postmortemai.infrastructure.parser;

import com.postmortemai.application.port.LogParser;
import com.postmortemai.domain.enums.LogFormat;
import com.postmortemai.domain.model.ParsedLogContents;
import com.postmortemai.domain.model.ParsedLogLine;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Java exception stack traces.
 *
 * <p>Groups log content into cohesive blocks:
 * <ul>
 *   <li>A block starts at any line that looks like a Java exception declaration
 *       (contains "Exception", "Error", or "Throwable" at the class level,
 *       or begins with "Caused by:").</li>
 *   <li>Subsequent lines that are JVM stack frames ({@code at ...}, {@code \tat ...},
 *       {@code ... N more}) or chained exceptions are appended to the same block.</li>
 *   <li>Lines outside exception blocks are treated as individual entries.</li>
 * </ul>
 *
 * <p>The optional {@code level} field is extracted from common log prefixes such as:
 * {@code [ERROR]} or {@code ERROR:} at the start of the line.
 */
@Component
public class JavaStackTraceParser implements LogParser {

    /**
     * Matches a leading log level token in brackets or followed by a colon/space.
     */
    private static final Pattern LEVEL_PREFIX =
            Pattern.compile(
                    "^(?:\\[(?<l1>[A-Z]+)\\]|(?<l2>DEBUG|TRACE|INFO|WARN|WARNING|ERROR|FATAL|SEVERE)[\\s:])"
            );

    @Override
    public boolean supports(LogFormat format) {
        return LogFormat.JAVA_STACK_TRACE == format;
    }

    @Override
    public ParsedLogContents parse(String rawLog) {
        if (rawLog == null) throw new IllegalArgumentException("rawLog must not be null");

        // Platform-safe split — handles \r\n (Windows) and \n (Unix)
        String[] rawLines = rawLog.split("\r?\n", -1);
        List<ParsedLogLine> result = new ArrayList<>();

        int i = 0;
        while (i < rawLines.length) {
            String line = rawLines[i];
            String stripped = line.strip();

            if (stripped.isEmpty()) {
                i++;
                continue;
            }

            if (isExceptionOrCausedBy(stripped)) {
                // Start collecting the full exception block
                StringBuilder block = new StringBuilder(line);
                int j = i + 1;
                while (j < rawLines.length && isStackBlockContinuation(rawLines[j].strip())) {
                    block.append('\n').append(rawLines[j]);
                    j++;
                }
                Optional<String> level = extractLevel(stripped);
                result.add(new ParsedLogLine(Optional.empty(), level, block.toString()));
                i = j;
            } else {
                Optional<String> level = extractLevel(stripped);
                result.add(new ParsedLogLine(Optional.empty(), level, stripped));
                i++;
            }
        }

        return new ParsedLogContents(LogFormat.JAVA_STACK_TRACE, result);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Returns true if the (already stripped) line declares the start of an
     * exception block: a fully-qualified Java throwable class name, or "Caused by:".
     */
    private boolean isExceptionOrCausedBy(String stripped) {
        if (stripped.contains("Caused by:")) return true;
        // Matches an exception class name preceded by optional log prefixes
        return stripped.matches(".*(?:[a-zA-Z_$][\\w$.]*\\.)+\\w*(?:Exception|Error|Throwable).*");
    }

    /**
     * Returns true if the (already stripped) line should be kept as part of an
     * ongoing exception block: JVM frames, "... N more", or another exception chain.
     */
    private boolean isStackBlockContinuation(String stripped) {
        if (stripped.isEmpty()) return false;
        if (stripped.startsWith("at "))   return true;
        if (stripped.startsWith("... "))  return true;
        if (stripped.startsWith("Caused by:")) return true;
        if (isExceptionOrCausedBy(stripped)) return true;
        return false;
    }

    private Optional<String> extractLevel(String line) {
        Matcher m = LEVEL_PREFIX.matcher(line);
        if (!m.find()) return Optional.empty();
        String l1 = m.group("l1");
        String l2 = m.group("l2");
        return Optional.ofNullable(l1 != null ? l1 : l2);
    }
}
