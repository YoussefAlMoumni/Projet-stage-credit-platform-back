package com.talan.creditplatform.controller.rest;

import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.entity.Evaluation;
import com.talan.creditplatform.model.dto.EvaluationResultDto;
import com.talan.creditplatform.repository.DossierRepository;
import com.talan.creditplatform.repository.EvaluationRepository;
import com.talan.creditplatform.repository.StageResultRepository;
import com.talan.creditplatform.repository.UserRepository;
import com.talan.creditplatform.service.PipelineOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    public DossierController(DossierRepository dossierRepository, UserRepository userRepository,
                             EvaluationRepository evaluationRepository, StageResultRepository stageResultRepository,
                             PipelineOrchestrator pipelineOrchestrator) {
        this.dossierRepository = dossierRepository;
        this.userRepository = userRepository;
        this.evaluationRepository = evaluationRepository;
        this.stageResultRepository = stageResultRepository;
        this.pipelineOrchestrator = pipelineOrchestrator;
    }

    @GetMapping
    public ResponseEntity<List<Dossier>> getAllDossiers() {
        return ResponseEntity.ok(dossierRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Dossier> createDossier(@Valid @RequestBody Dossier dossier) {
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
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{value}")
    public ResponseEntity<Dossier> getDossier(@PathVariable String value) {
        Optional<Dossier> dossier = findBySirenOrId(value);
        return dossier.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDossier(@PathVariable Long id) {
        if (!dossierRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        dossierRepository.deleteById(id);
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
