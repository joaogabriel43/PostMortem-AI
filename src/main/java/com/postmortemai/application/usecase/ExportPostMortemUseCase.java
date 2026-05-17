package com.postmortemai.application.usecase;

import com.postmortemai.application.dto.ExportFormat;
import com.postmortemai.application.dto.ExportedDocument;
import com.postmortemai.application.exception.ResourceNotFoundException;
import com.postmortemai.application.port.PostMortemRepositoryPort;
import com.postmortemai.application.port.out.PostMortemExporter;
import com.postmortemai.domain.model.PostMortem;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class ExportPostMortemUseCase {

    private final PostMortemRepositoryPort postMortemRepositoryPort;
    private final List<PostMortemExporter> exporters;

    public ExportPostMortemUseCase(PostMortemRepositoryPort postMortemRepositoryPort, List<PostMortemExporter> exporters) {
        this.postMortemRepositoryPort = postMortemRepositoryPort;
        this.exporters = exporters;
    }

    public ExportedDocument execute(UUID incidentId, ExportFormat format) {
        PostMortem postMortem = postMortemRepositoryPort.findByIncidentId(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Post-Mortem not found for incident ID: " + incidentId));

        PostMortemExporter exporter = exporters.stream()
                .filter(exp -> exp.supports(format))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported export format: " + format));

        byte[] content = exporter.export(postMortem);
        String contentType = format == ExportFormat.PDF ? "application/pdf" : "text/markdown";

        if (format == ExportFormat.MARKDOWN) {
            String markdownText = new String(content, StandardCharsets.UTF_8);
            PostMortem updatedPostMortem = new PostMortem(
                    postMortem.id(),
                    postMortem.incidentId(),
                    postMortem.title(),
                    postMortem.summary(),
                    postMortem.timeline(),
                    postMortem.rootCause(),
                    postMortem.impact(),
                    postMortem.detection(),
                    postMortem.contributingFactors(),
                    postMortem.actionItems(),
                    postMortem.lessonsLearned(),
                    markdownText,
                    postMortem.createdAt()
            );
            postMortemRepositoryPort.save(updatedPostMortem);
        }

        return new ExportedDocument(content, contentType);
    }
}
