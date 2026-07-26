package com.talan.creditplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class EventService {
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    public EventService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // Infinite timeout for long-lived connection
        
        this.emitters.add(emitter);

        emitter.onCompletion(() -> this.emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            this.emitters.remove(emitter);
        });
        emitter.onError((e) -> {
            emitter.completeWithError(e);
            this.emitters.remove(emitter);
        });

        // Send an initial connection event
        try {
            emitter.send(SseEmitter.event().name("connected").data("true"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void broadcast(String eventName, Object data) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(Map.of(
                "type", eventName,
                "data", data != null ? data : "none",
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            logger.error("Failed to serialize event payload", e);
            return;
        }

        this.emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("message") // Custom event name can be set, but "message" is default for EventSource
                        .data(jsonPayload, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        });

        this.emitters.removeAll(deadEmitters);
    }

    public void emitDossiersChanged() {
        broadcast("DOSSIERS_CHANGED", null);
    }

    public void emitUsersChanged() {
        broadcast("USERS_CHANGED", null);
    }

    public void emitAnalystsChanged() {
        broadcast("ANALYSTS_CHANGED", null);
    }

    public void emitPromptsChanged() {
        broadcast("PROMPTS_CHANGED", null);
    }
}
