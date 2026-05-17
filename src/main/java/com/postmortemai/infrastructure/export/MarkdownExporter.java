package com.postmortemai.infrastructure.export;

import com.postmortemai.application.port.out.PostMortemExporter;
import com.postmortemai.domain.model.PostMortem;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MarkdownExporter implements PostMortemExporter {

    @Override
    public byte[] export(PostMortem postMortem) {
        StringBuilder sb = new StringBuilder();

        sb.append("# ").append(postMortem.title()).append("\n\n");
        sb.append("## Summary\n").append(postMortem.summary()).append("\n\n");
        sb.append("## Timeline\n").append(postMortem.timeline()).append("\n\n");
        sb.append("## Root Cause\n").append(postMortem.rootCause()).append("\n\n");
        sb.append("## Impact\n").append(postMortem.impact()).append("\n\n");
        sb.append("## Detection\n").append(postMortem.detection()).append("\n\n");

        if (postMortem.contributingFactors() != null && !postMortem.contributingFactors().isBlank()) {
            sb.append("## Contributing Factors\n").append(postMortem.contributingFactors()).append("\n\n");
        }

        if (postMortem.actionItems() != null && !postMortem.actionItems().isBlank()) {
            sb.append("## Action Items\n").append(postMortem.actionItems()).append("\n\n");
        }

        if (postMortem.lessonsLearned() != null && !postMortem.lessonsLearned().isBlank()) {
            sb.append("## Lessons Learned\n").append(postMortem.lessonsLearned()).append("\n\n");
        }

        return sb.toString().trim().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean supports(com.postmortemai.application.dto.ExportFormat format) {
        return format == com.postmortemai.application.dto.ExportFormat.MARKDOWN;
    }
}
