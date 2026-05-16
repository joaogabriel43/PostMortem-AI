package com.postmortemai.unit.export;

import com.postmortemai.domain.model.PostMortem;
import com.postmortemai.infrastructure.export.MarkdownExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownExporterTest {

    private MarkdownExporter markdownExporter;

    @BeforeEach
    void setUp() {
        markdownExporter = new MarkdownExporter();
    }

    @Test
    @DisplayName("Deve renderizar Markdown completo quando todos os campos estiverem preenchidos")
    void shouldRenderFullMarkdown() {
        PostMortem postMortem = new PostMortem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Outage Title",
                "Summary Text",
                "Timeline Details",
                "Root Cause Info",
                "Impact Description",
                "Detection Strategy",
                "Some contributing factors",
                "Fix the bug",
                "Test better",
                null,
                LocalDateTime.now()
        );

        byte[] resultBytes = markdownExporter.export(postMortem);
        String result = new String(resultBytes, StandardCharsets.UTF_8);

        assertThat(result).contains("# Outage Title");
        assertThat(result).contains("## Summary\nSummary Text");
        assertThat(result).contains("## Timeline\nTimeline Details");
        assertThat(result).contains("## Root Cause\nRoot Cause Info");
        assertThat(result).contains("## Impact\nImpact Description");
        assertThat(result).contains("## Detection\nDetection Strategy");
        assertThat(result).contains("## Contributing Factors\nSome contributing factors");
        assertThat(result).contains("## Action Items\nFix the bug");
        assertThat(result).contains("## Lessons Learned\nTest better");
    }

    @Test
    @DisplayName("Deve omitir completamente secoes opcionais quando os campos estiverem nulos")
    void shouldOmitNullOptionalSections() {
        PostMortem postMortem = new PostMortem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Outage Title",
                "Summary Text",
                "Timeline Details",
                "Root Cause Info",
                "Impact Description",
                "Detection Strategy",
                null,
                "   ", // empty string should be omitted
                null,
                null,
                LocalDateTime.now()
        );

        byte[] resultBytes = markdownExporter.export(postMortem);
        String result = new String(resultBytes, StandardCharsets.UTF_8);

        assertThat(result).contains("# Outage Title");
        assertThat(result).contains("## Detection\nDetection Strategy");
        
        assertThat(result).doesNotContain("Contributing Factors");
        assertThat(result).doesNotContain("Action Items");
        assertThat(result).doesNotContain("Lessons Learned");
    }
}
