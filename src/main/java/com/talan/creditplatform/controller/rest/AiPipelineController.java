package com.talan.creditplatform.controller.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talan.creditplatform.model.dto.EvaluationResultDto;
import com.talan.creditplatform.model.dto.PipelineStageEvent;
import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.entity.Evaluation;
import com.talan.creditplatform.repository.DossierRepository;
import com.talan.creditplatform.repository.EvaluationRepository;
import com.talan.creditplatform.repository.StageResultRepository;
import com.talan.creditplatform.service.PipelineOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * REST controller for AI pipeline operations on Dossiers.
 *
 * <p>Handles triggering, streaming (SSE), stopping, and retrieving AI pipeline evaluations.
 * CRUD operations for Dossiers remain in {@link DossierController}.
 */
@RestController
@RequestMapping("/api/dossiers")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class AiPipelineController {

    private static final Logger logger = LoggerFactory.getLogger(AiPipelineController.class);

    private final DossierRepository     dossierRepository;
    private final EvaluationRepository  evaluationRepository;
    private final StageResultRepository stageResultRepository;
    private final PipelineOrchestrator  pipelineOrchestrator;
    private final ObjectMapper          objectMapper;

    public AiPipelineController(DossierRepository dossierRepository,
                                EvaluationRepository evaluationRepository,
                                StageResultRepository stageResultRepository,
                                PipelineOrchestrator pipelineOrchestrator,
                                ObjectMapper objectMapper) {
        this.dossierRepository    = dossierRepository;
        this.evaluationRepository = evaluationRepository;
        this.stageResultRepository = stageResultRepository;
        this.pipelineOrchestrator = pipelineOrchestrator;
        this.objectMapper         = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Non-streaming trigger
    // -------------------------------------------------------------------------

    /**
     * Triggers the AI evaluation pipeline synchronously and returns the result once complete.
     */
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

    // -------------------------------------------------------------------------
    // SSE streaming
    // -------------------------------------------------------------------------

    /**
     * Streams AI evaluation progress as Server-Sent Events (SSE).
     * Each agent stage emits STARTED and COMPLETED events, followed by a final complete event.
     */
    @GetMapping(value = "/{value}/ai-decision/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAiDecision(@PathVariable String value,
                                       @RequestParam(defaultValue = "FAST") String mode) {
        Optional<Dossier> dossierOpt = findBySirenOrId(value);
        if (dossierOpt.isEmpty()) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new RuntimeException("Dossier not found"));
            return emitter;
        }

        SseEmitter emitter = new SseEmitter(900_000L); // 15-minute timeout
        AtomicBoolean completed = new AtomicBoolean(false);

        CompletableFuture.runAsync(() -> {
            try {
                Evaluation evaluation = pipelineOrchestrator.evaluateWithProgress(dossierOpt.get(), mode, event -> {
                    if (completed.get()) return;
                    try {
                        String json = objectMapper.writeValueAsString(event);
                        emitter.send(SseEmitter.event().name("message").data(json, MediaType.APPLICATION_JSON));
                    } catch (Exception e) {
                        if (completed.compareAndSet(false, true)) {
                            sendErrorEvent(emitter, "Event serialization failed: " + e.getMessage());
                        }
                    }
                });

                if (completed.get()) return;

                var stageResults = stageResultRepository.fromEvaluation(evaluation);
                String json = objectMapper.writeValueAsString(new EvaluationResultDto(evaluation, stageResults));
                emitter.send(SseEmitter.event().name("complete").data(json, MediaType.APPLICATION_JSON));

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

    // -------------------------------------------------------------------------
    // Stop
    // -------------------------------------------------------------------------

    /**
     * Cancels an active AI pipeline for the given dossier.
     */
    @PostMapping("/{value}/ai-decision/stop")
    public ResponseEntity<?> stopAiDecision(@PathVariable String value) {
        Optional<Dossier> dossierOpt = findBySirenOrId(value);
        if (dossierOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            pipelineOrchestrator.cancelEvaluation(dossierOpt.get().getId());
            return ResponseEntity.ok(Map.of("message", "Pipeline arrêté avec succès."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Read latest evaluation
    // -------------------------------------------------------------------------

    /**
     * Returns the most recent AI evaluation for the given dossier.
     */
    @GetMapping("/{value}/ai-decision")
    public ResponseEntity<?> getLatestAiDecision(@PathVariable String value) {
        Optional<Dossier> dossierOpt = findBySirenOrId(value);
        if (dossierOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<Evaluation> evaluations = evaluationRepository.findByDossierIdOrderByCreatedAtDesc(dossierOpt.get().getId());
        if (evaluations.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Aucune évaluation trouvée pour ce dossier."));
        }
        Evaluation latest = evaluations.get(0);
        var stageResults = stageResultRepository.fromEvaluation(latest);
        return ResponseEntity.ok(new EvaluationResultDto(latest, stageResults));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Optional<Dossier> findBySirenOrId(String value) {
        Optional<Dossier> bySiren = dossierRepository.findBySiren(value);
        if (bySiren.isPresent()) return bySiren;
        try {
            return dossierRepository.findById(Long.valueOf(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Sends a structured JSON error event then completes the emitter with an error.
     */
    private void sendErrorEvent(SseEmitter emitter, String message) {
        try {
            String errorJson = objectMapper.writeValueAsString(
                    Map.of("status", "ERROR", "message", message != null ? message : "Erreur inconnue"));
            emitter.send(SseEmitter.event().name("error").data(errorJson, MediaType.APPLICATION_JSON));
        } catch (Exception ignored) {
            // best-effort: if we can't send the error event, fall through to completeWithError
        }
        emitter.completeWithError(new RuntimeException(message));
    }
}
