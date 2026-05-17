package com.postmortemai.presentation.controller;

import com.postmortemai.application.port.IncidentRepositoryPort;
import com.postmortemai.application.exception.ResourceNotFoundException;
import com.postmortemai.application.service.PostMortemService;
import com.postmortemai.application.usecase.ExportPostMortemUseCase;
import com.postmortemai.application.dto.ExportFormat;
import com.postmortemai.application.dto.ExportedDocument;
import com.postmortemai.domain.model.PostMortem;
import com.postmortemai.presentation.dto.IncidentRequest;
import com.postmortemai.presentation.dto.PostMortemResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final PostMortemService postMortemService;
    private final ExportPostMortemUseCase exportPostMortemUseCase;
    private final IncidentRepositoryPort incidentRepositoryPort;

    public IncidentController(
            PostMortemService postMortemService,
            ExportPostMortemUseCase exportPostMortemUseCase,
            IncidentRepositoryPort incidentRepositoryPort
    ) {
        this.postMortemService = postMortemService;
        this.exportPostMortemUseCase = exportPostMortemUseCase;
        this.incidentRepositoryPort = incidentRepositoryPort;
    }

    @PostMapping
    public ResponseEntity<PostMortemResponse> createIncident(@RequestBody IncidentRequest request) {
        if (request.projectName() == null || request.projectName().isBlank()) {
            throw new IllegalArgumentException("projectName is required");
        }
        if (request.serviceName() == null || request.serviceName().isBlank()) {
            throw new IllegalArgumentException("serviceName is required");
        }
        if (request.rawLog() == null || request.rawLog().isBlank()) {
            throw new IllegalArgumentException("rawLog is required");
        }

        PostMortem postMortem = postMortemService.generatePostMortem(
                request.projectName(),
                request.serviceName(),
                request.rawLog()
        );

        String severity = incidentRepositoryPort.findById(postMortem.incidentId())
                .map(incident -> incident.severity() != null ? incident.severity().name() : "P3")
                .orElse("P3");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PostMortemResponse.fromDomain(postMortem, severity));
    }

    @GetMapping("/{id}/postmortem")
    public ResponseEntity<PostMortemResponse> getPostMortem(@PathVariable UUID id) {
        PostMortem postMortem = postMortemService.getPostMortemByIncidentId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post-Mortem not found for incident ID: " + id));

        String severity = incidentRepositoryPort.findById(id)
                .map(incident -> incident.severity() != null ? incident.severity().name() : "P3")
                .orElse("P3");
        
        return ResponseEntity.ok(PostMortemResponse.fromDomain(postMortem, severity));
    }

    @GetMapping("/{id}/postmortem/export")
    public ResponseEntity<byte[]> exportPostMortem(
            @PathVariable UUID id,
            @RequestParam String format) {
        
        ExportFormat exportFormat;
        try {
            exportFormat = ExportFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid format. Supported: markdown, pdf");
        }

        ExportedDocument doc = exportPostMortemUseCase.execute(id, exportFormat);

        String ext = exportFormat == ExportFormat.PDF ? "pdf" : "md";
        String filename = "postmortem-" + id + "." + ext;

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, doc.contentType())
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(doc.content());
    }
}
