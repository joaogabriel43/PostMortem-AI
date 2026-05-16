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
 * Fallback parser for unstructured plain-text logs.
 *
 * <p>Each non-blank line is converted to an individual {@link ParsedLogLine}.
 * A best-effort extraction of the log level is attempted via common patterns:
 * {@code [INFO]}, {@code WARN:}, or bare level tokens at common positions.
 *
 * <p>This parser always supports {@link LogFormat#PLAIN_TEXT} and should be
 * the last resort when no other parser matches.
 */
@Component
public class PlainTextParser implements LogParser {

    /**
     * Matches common log level notations:
     * {@code [INFO]}, {@code [WARN]}, {@code ERROR:}, {@code DEBUG }.
     */
    private static final Pattern LEVEL_PATTERN =
            Pattern.compile(
                    "\\[(?<l1>DEBUG|TRACE|INFO|WARN|WARNING|ERROR|FATAL|SEVERE)\\]" +
                    "|(?<l2>DEBUG|TRACE|INFO|WARN|WARNING|ERROR|FATAL|SEVERE)(?=\\s*[:\\s])"
            );

    @Override
    public boolean supports(LogFormat format) {
        return LogFormat.PLAIN_TEXT == format;
    }

    @Override
    public ParsedLogContents parse(String rawLog) {
        if (rawLog == null) throw new IllegalArgumentException("rawLog must not be null");

        List<ParsedLogLine> lines = new ArrayList<>();
        for (String line : rawLog.split("\n", -1)) {
            if (line.isBlank()) continue;
            Optional<String> level = extractLevel(line);
            lines.add(new ParsedLogLine(Optional.empty(), level, line.strip()));
        }

        return new ParsedLogContents(LogFormat.PLAIN_TEXT, lines);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Optional<String> extractLevel(String line) {
        Matcher m = LEVEL_PATTERN.matcher(line);
        if (!m.find()) return Optional.empty();
        String l1 = m.group("l1");
        String l2 = m.group("l2");
        return Optional.ofNullable(l1 != null ? l1 : l2);
    }
}
