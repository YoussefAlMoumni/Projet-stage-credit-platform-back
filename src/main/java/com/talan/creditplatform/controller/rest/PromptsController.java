package com.talan.creditplatform.controller.rest;

import com.talan.creditplatform.model.entity.AiModel;
import com.talan.creditplatform.model.entity.AiPrompt;
import com.talan.creditplatform.repository.AiModelRepository;
import com.talan.creditplatform.repository.AiPromptRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import com.talan.creditplatform.service.EventService;

@RestController
@RequestMapping("/api/prompts")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class PromptsController {

    private final AiModelRepository aiModelRepository;
    private final AiPromptRepository aiPromptRepository;
    private final EventService eventService;

    public PromptsController(AiModelRepository aiModelRepository, AiPromptRepository aiPromptRepository, EventService eventService) {
        this.aiModelRepository = aiModelRepository;
        this.aiPromptRepository = aiPromptRepository;
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getActivePrompts() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<String> stages = List.of("solvency", "history", "guarantees", "compliance", "supervisor");

        for (String stage : stages) {
            Optional<AiModel> modelOpt = aiModelRepository.findFirstByStageNameAndActiveTrue(stage);
            if (modelOpt.isPresent()) {
                AiModel model = modelOpt.get();
                Optional<AiPrompt> promptOpt = aiPromptRepository.findFirstByAiModelIdOrderByUpdatedAtDesc(model.getId());
                
                Map<String, Object> map = new HashMap<>();
                map.put("modelId", model.getId());
                map.put("stageName", model.getStageName());
                map.put("modelName", model.getModelName());
                map.put("contextWindowSize", model.getContextWindowSize());
                map.put("temperature", model.getTemperature());
                map.put("keepAliveSetting", model.getKeepAliveSetting());
                map.put("active", model.getActive());
                
                if (promptOpt.isPresent()) {
                    AiPrompt prompt = promptOpt.get();
                    map.put("promptId", prompt.getId());
                    map.put("promptText", prompt.getPromptText());
                    map.put("versionTag", prompt.getVersionTag());
                    map.put("updatedAt", prompt.getUpdatedAt());
                } else {
                    map.put("promptId", null);
                    map.put("promptText", "");
                    map.put("versionTag", "v1.0.0");
                    map.put("updatedAt", null);
                }
                result.add(map);
            }
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePromptAndModel(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Optional<AiModel> modelOpt = aiModelRepository.findById(id);
        if (modelOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AiModel model = modelOpt.get();

        // Update model settings
        if (payload.containsKey("modelName")) {
            model.setModelName((String) payload.get("modelName"));
        }
        if (payload.containsKey("contextWindowSize")) {
            model.setContextWindowSize(Integer.valueOf(payload.get("contextWindowSize").toString()));
        }
        if (payload.containsKey("temperature")) {
            model.setTemperature(Double.valueOf(payload.get("temperature").toString()));
        }
        if (payload.containsKey("keepAliveSetting")) {
            model.setKeepAliveSetting((String) payload.get("keepAliveSetting"));
        }
        aiModelRepository.save(model);

        // Update prompt text if provided
        if (payload.containsKey("promptText")) {
            String promptText = (String) payload.get("promptText");
            Optional<AiPrompt> latestPromptOpt = aiPromptRepository.findFirstByAiModelIdOrderByUpdatedAtDesc(model.getId());
            
            String nextVersion = "v1.0.0";
            if (latestPromptOpt.isPresent()) {
                String latestVersion = latestPromptOpt.get().getVersionTag();
                nextVersion = incrementVersionTag(latestVersion);
            }

            AiPrompt newPrompt = new AiPrompt();
            newPrompt.setAiModel(model);
            newPrompt.setPromptText(promptText);
            newPrompt.setVersionTag(nextVersion);
            newPrompt.setUpdatedAt(LocalDateTime.now());
            aiPromptRepository.save(newPrompt);
        }

        eventService.emitPromptsChanged();
        return ResponseEntity.ok(Map.of("message", "Prompt and model parameters updated."));
    }

    private String incrementVersionTag(String tag) {
        if (tag == null || !tag.startsWith("v")) {
            return "v1.0.0";
        }
        try {
            String[] parts = tag.substring(1).split("\\.");
            if (parts.length == 3) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                int patch = Integer.parseInt(parts[2]);
                patch++;
                return "v" + major + "." + minor + "." + patch;
            }
        } catch (Exception e) {
            // fall back
        }
        return "v1.0.0";
    }
}
