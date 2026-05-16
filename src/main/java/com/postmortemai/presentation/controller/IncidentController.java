package com.postmortemai.presentation.controller;

import com.postmortemai.application.exception.ResourceNotFoundException;
import com.postmortemai.application.service.PostMortemService;
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

    public IncidentController(PostMortemService postMortemService) {
        this.postMortemService = postMortemService;
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

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PostMortemResponse.fromDomain(postMortem));
    }

    @GetMapping("/{id}/postmortem")
    public ResponseEntity<PostMortemResponse> getPostMortem(@PathVariable UUID id) {
        PostMortem postMortem = postMortemService.getPostMortemByIncidentId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post-Mortem not found for incident ID: " + id));
        
        return ResponseEntity.ok(PostMortemResponse.fromDomain(postMortem));
    }
}
