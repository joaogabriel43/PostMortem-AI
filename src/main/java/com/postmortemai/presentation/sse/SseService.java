package com.postmortemai.presentation.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

    private static final Logger logger = LoggerFactory.getLogger(SseService.class);
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(UUID taskId) {
        SseEmitter emitter = new SseEmitter(120000L); // 2 minutes timeout
        emitters.put(taskId, emitter);

        emitter.onCompletion(() -> emitters.remove(taskId));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(taskId);
        });
        emitter.onError((e) -> {
            emitter.completeWithError(e);
            emitters.remove(taskId);
        });

        return emitter;
    }

    public void sendEvent(UUID taskId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(taskId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                logger.error("Failed to send SSE event for task {}", taskId, e);
                emitter.completeWithError(e);
                emitters.remove(taskId);
            }
        } else {
            logger.warn("No SSE emitter found for task {}", taskId);
        }
    }

    public void complete(UUID taskId) {
        SseEmitter emitter = emitters.get(taskId);
        if (emitter != null) {
            emitter.complete();
            emitters.remove(taskId);
        }
    }

    public void completeWithError(UUID taskId, Throwable error) {
        SseEmitter emitter = emitters.get(taskId);
        if (emitter != null) {
            emitter.completeWithError(error);
            emitters.remove(taskId);
        }
    }
}
