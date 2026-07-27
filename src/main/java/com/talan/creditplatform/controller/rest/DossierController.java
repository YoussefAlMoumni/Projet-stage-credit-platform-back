package com.talan.creditplatform.controller.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.entity.Evaluation;
import com.talan.creditplatform.model.entity.User;
import com.talan.creditplatform.model.dto.EvaluationResultDto;
import com.talan.creditplatform.repository.DossierRepository;
import com.talan.creditplatform.repository.EvaluationRepository;
import com.talan.creditplatform.repository.StageResultRepository;
import com.talan.creditplatform.repository.UserRepository;
import com.talan.creditplatform.security.CustomUserDetails;
import com.talan.creditplatform.service.EventService;
import com.talan.creditplatform.service.PipelineOrchestrator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/dossiers")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class DossierController {

    private final DossierRepository dossierRepository;
    private final UserRepository userRepository;
    private final EvaluationRepository evaluationRepository;
    private final StageResultRepository stageResultRepository;
    private final PipelineOrchestrator pipelineOrchestrator;
    private final ObjectMapper objectMapper;
    private final EventService eventService;

    public DossierController(DossierRepository dossierRepository, UserRepository userRepository,
                             EvaluationRepository evaluationRepository, StageResultRepository stageResultRepository,
                             PipelineOrchestrator pipelineOrchestrator, ObjectMapper objectMapper, EventService eventService) {
        this.dossierRepository = dossierRepository;
        this.userRepository = userRepository;
        this.evaluationRepository = evaluationRepository;
        this.stageResultRepository = stageResultRepository;
        this.pipelineOrchestrator = pipelineOrchestrator;
        this.objectMapper = objectMapper;
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<Dossier>> getAllDossiers(@AuthenticationPrincipal CustomUserDetails principal) {
        User caller = principal != null ? principal.getUser() : null;
        // Analysts can only see their own dossiers; managers and admins see all.
        if (caller != null && "analyst".equalsIgnoreCase(caller.getRole())) {
            return ResponseEntity.ok(dossierRepository.findByAssignedAnalyst(caller));
        }
        return ResponseEntity.ok(dossierRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Dossier> createDossier(@Valid @RequestBody Dossier dossier,
                                                 @AuthenticationPrincipal CustomUserDetails principal) {
        // Automatically assign the authenticated user as analyst when creating
        if (dossier.getAssignedAnalyst() == null && principal != null) {
            dossier.setAssignedAnalyst(principal.getUser());
        }
        assignDefaultAnalystIfMissing(dossier);
        // Ensure bi-directional relationships
        if (dossier.getLoans() != null) {
            dossier.getLoans().forEach(loan -> {
                loan.setDossier(dossier);
                if (loan.getCollaterals() != null) {
                    loan.getCollaterals().forEach(c -> c.setLoan(loan));
                }
            });
        }
        if (dossier.getCreditHistories() != null) {
            dossier.getCreditHistories().forEach(h -> h.setDossier(dossier));
        }
        if (dossier.getIndividual() != null) {
            dossier.getIndividual().setDossier(dossier);
        }
        if (dossier.getCorporate() != null) {
            dossier.getCorporate().setDossier(dossier);
        }
        Dossier saved = dossierRepository.save(dossier);
        eventService.emitDossiersChanged();
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Dossier> updateDossier(@PathVariable Long id, @Valid @RequestBody Dossier dossier) {
        Optional<Dossier> existingDossierOpt = dossierRepository.findById(id);
        if (existingDossierOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Dossier existing = existingDossierOpt.get();
        // Preserve un-modifiable fields if necessary
        dossier.setId(id);
        dossier.setCreationDate(existing.getCreationDate());
        if (dossier.getAssignedAnalyst() == null) {
            dossier.setAssignedAnalyst(existing.getAssignedAnalyst());
        }
        if (dossier.getStatus() == null) {
            dossier.setStatus(existing.getStatus());
        }

        assignDefaultAnalystIfMissing(dossier);
        // Ensure bi-directional relationships
        if (dossier.getLoans() != null) {
            dossier.getLoans().forEach(loan -> {
                loan.setDossier(dossier);
                if (loan.getCollaterals() != null) {
                    loan.getCollaterals().forEach(c -> c.setLoan(loan));
                }
            });
        }
        if (dossier.getCreditHistories() != null) {
            dossier.getCreditHistories().forEach(h -> h.setDossier(dossier));
        }
        if (dossier.getIndividual() != null) {
            dossier.getIndividual().setDossier(dossier);
        }
        if (dossier.getCorporate() != null) {
            dossier.getCorporate().setDossier(dossier);
        }
        
        // We have to clear the existing collections and addAll to ensure orphanRemoval works gracefully,
        // or just let Spring Data JPA do its magic with save().
        // Calling save() with the updated object will merge it properly assuming IDs are correctly passed.
        Dossier saved = dossierRepository.save(dossier);
        eventService.emitDossiersChanged();
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{value}")
    public ResponseEntity<Dossier> getDossier(@PathVariable String value,
                                              @AuthenticationPrincipal CustomUserDetails principal) {
        Optional<Dossier> dossier = findBySirenOrId(value);
        if (dossier.isEmpty()) return ResponseEntity.notFound().build();
        if (!canAccess(dossier.get(), principal)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(dossier.get());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDossier(@PathVariable Long id) {
        if (!dossierRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        dossierRepository.deleteById(id);
        eventService.emitDossiersChanged();
        return ResponseEntity.ok(Map.of("message", "Dossier deleted."));
    }

    @PostMapping("/{value}/ai-decision")
    public ResponseEntity<?> triggerAiDecision(@PathVariable String value,
                                               @RequestParam(defaultValue = "FAST") String mode) {
        Optional<Dossier> dossierOpt = findBySirenOrId(value);
        if (dossierOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            Evaluation evaluation = pipelineOrchestrator.evaluate(dossierOpt.get(), mode);
            var stageResults = stageResultRepository.fromEvaluation(evaluation);
            return ResponseEntity.ok(new EvaluationResultDto(evaluation, stageResults));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping(value = "/{value}/ai-decision/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamAiDecision(@PathVariable String value,
                                                                                             @RequestParam(defaultValue = "FAST") String mode) {
        Optional<Dossier> dossierOpt = findBySirenOrId(value);
        if (dossierOpt.isEmpty()) {
            org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter();
            emitter.completeWithError(new RuntimeException("Dossier not found"));
            return emitter;
        }

        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(300000L); // 5 minutes timeout
        // Guard against double-complete (listener thread vs. outer catch race).
        java.util.concurrent.atomic.AtomicBoolean completed = new java.util.concurrent.atomic.AtomicBoolean(false);

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Evaluation evaluation = pipelineOrchestrator.evaluateWithProgress(dossierOpt.get(), mode, event -> {
                    if (completed.get()) return; // emitter already closed
                    try {
                        // Serialize to JSON string explicitly — passing a raw object to
                        // emitter.send() relies on a message converter that may not be
                        // registered for text/event-stream (Issue 3 fix).
                        String json = objectMapper.writeValueAsString(event);
                        emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                .name("message")
                                .data(json, MediaType.APPLICATION_JSON));
                    } catch (Exception e) {
                        if (completed.compareAndSet(false, true)) {
                            sendErrorEvent(emitter, "Event serialization failed: " + e.getMessage());
                        }
                    }
                });

                if (completed.get()) return;

                var stageResults = stageResultRepository.fromEvaluation(evaluation);
                EvaluationResultDto resultDto = new EvaluationResultDto(evaluation, stageResults);

                // Serialize result DTO to JSON string before sending — same Issue 3 fix.
                String json = objectMapper.writeValueAsString(resultDto);
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                        .name("complete")
                        .data(json, MediaType.APPLICATION_JSON));
                if (completed.compareAndSet(false, true)) {
                    emitter.complete();
                }
            } catch (Exception e) {
                if (completed.compareAndSet(false, true)) {
                    sendErrorEvent(emitter, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                }
            }
        });

        return emitter;
    }

    /**
     * Sends a structured JSON error event and then completes the SSE emitter.
     * Using objectMapper ensures the payload is a valid JSON string, not a raw
     * LinkedHashMap or exception toString().
     */
    private void sendErrorEvent(org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter, String message) {
        try {
            String errorJson = objectMapper.writeValueAsString(
                    java.util.Map.of("status", "ERROR", "message", message != null ? message : "Unknown error"));
            emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                    .name("error")
                    .data(errorJson, MediaType.APPLICATION_JSON));
        } catch (Exception ignored) {
            // If we can't even send the error event, just complete with error below.
        }
        emitter.completeWithError(new RuntimeException(message));
    }

    @GetMapping("/{value}/ai-decision")
    public ResponseEntity<?> getLatestAiDecision(@PathVariable String value) {
        Optional<Dossier> dossierOpt = findBySirenOrId(value);
        if (dossierOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Dossier dossier = dossierOpt.get();
        List<Evaluation> evaluations = evaluationRepository.findByDossierIdOrderByCreatedAtDesc(dossier.getId());
        if (evaluations.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "No evaluations found for this dossier."));
        }
        Evaluation latest = evaluations.get(0);
        var stageResults = stageResultRepository.fromEvaluation(latest);
        return ResponseEntity.ok(new EvaluationResultDto(latest, stageResults));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateDossierStatus(@PathVariable Long id, @RequestBody Map<String, String> body,
                                                 @AuthenticationPrincipal CustomUserDetails principal) {
        Optional<Dossier> dossierOpt = dossierRepository.findById(id);
        if (dossierOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!canAccess(dossierOpt.get(), principal)) return ResponseEntity.status(403).build();
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Status is required"));
        }
        Dossier dossier = dossierOpt.get();
        dossier.setStatus(status);
        dossierRepository.save(dossier);
        eventService.emitDossiersChanged();
        return ResponseEntity.ok(dossier);
    }

    private Optional<Dossier> findBySirenOrId(String value) {
        Optional<Dossier> bySiren = dossierRepository.findBySiren(value);
        if (bySiren.isPresent()) {
            return bySiren;
        }
        try {
            return dossierRepository.findById(Long.valueOf(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns true if the caller may access the given dossier.
     * Analysts can only access dossiers they were assigned to;
     * managers and admins can access any dossier.
     */
    private boolean canAccess(Dossier dossier, CustomUserDetails principal) {
        if (principal == null) return false;
        User caller = principal.getUser();
        if (!"analyst".equalsIgnoreCase(caller.getRole())) return true; // manager / admin
        User assigned = dossier.getAssignedAnalyst();
        return assigned != null && assigned.getId().equals(caller.getId());
    }

    private void assignDefaultAnalystIfMissing(Dossier dossier) {
        if (dossier.getAssignedAnalyst() != null) {
            return;
        }
        userRepository.findByUsername("analyst")
                .or(() -> userRepository.findByUsername("banker"))
                .or(() -> userRepository.findByUsername("admin"))
                .ifPresent(dossier::setAssignedAnalyst);
    }
}
