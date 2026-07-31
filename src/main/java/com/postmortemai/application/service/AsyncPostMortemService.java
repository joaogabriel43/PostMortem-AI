package com.postmortemai.application.service;

import com.postmortemai.domain.model.PostMortem;
import com.postmortemai.presentation.sse.SseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@Service
public class AsyncPostMortemService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncPostMortemService.class);
    private final PostMortemService postMortemService;
    private final SseService sseService;

    public AsyncPostMortemService(PostMortemService postMortemService, SseService sseService) {
        this.postMortemService = postMortemService;
        this.sseService = sseService;
    }

    @Async("postMortemTaskExecutor")
    public void generateAsync(UUID taskId, String projectName, String serviceName, String rawLog) {
        try {
            logger.info("Starting async post-mortem generation for task {}", taskId);
            sseService.sendEvent(taskId, "PROCESSING", "Iniciando processamento do log com IA...");
            
            PostMortem postMortem = postMortemService.generatePostMortem(projectName, serviceName, rawLog);
            
            logger.info("Async post-mortem generation completed for task {}", taskId);
            
            Map<String, String> response = new HashMap<>();
            response.put("incidentId", postMortem.incidentId().toString());
            response.put("message", "Geração concluída!");
            
            sseService.sendEvent(taskId, "COMPLETED", response);
            sseService.complete(taskId);
        } catch (Exception e) {
            logger.error("Error during async generation for task {}", taskId, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            sseService.sendEvent(taskId, "FAILED", errorResponse);
            sseService.completeWithError(taskId, e);
        }
    }
}
