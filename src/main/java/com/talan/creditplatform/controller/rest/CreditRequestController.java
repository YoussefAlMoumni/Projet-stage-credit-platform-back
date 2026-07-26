package com.talan.creditplatform.controller.rest;

import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.entity.Evaluation;
import com.talan.creditplatform.model.dto.EvaluationResultDto;
import com.talan.creditplatform.repository.DossierRepository;
import com.talan.creditplatform.repository.EvaluationRepository;
import com.talan.creditplatform.repository.StageResultRepository;
import com.talan.creditplatform.service.PipelineOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/credit-requests")
public class CreditRequestController {

    private final PipelineOrchestrator pipelineOrchestrator;
    private final DossierRepository dossierRepository;
    private final EvaluationRepository evaluationRepository;
    private final StageResultRepository stageResultRepository;

    @Autowired
    public CreditRequestController(PipelineOrchestrator pipelineOrchestrator, DossierRepository dossierRepository,
                                   EvaluationRepository evaluationRepository, StageResultRepository stageResultRepository) {
        this.pipelineOrchestrator = pipelineOrchestrator;
        this.dossierRepository = dossierRepository;
        this.evaluationRepository = evaluationRepository;
        this.stageResultRepository = stageResultRepository;
    }

    @PostMapping("/{siren}/evaluate")
    public ResponseEntity<?> evaluate(@PathVariable String siren, @RequestParam(defaultValue = "FAST") String mode) {
        Optional<Dossier> dossierOpt = findBySirenOrId(siren);
        if (dossierOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            Evaluation evaluation = pipelineOrchestrator.evaluate(dossierOpt.get(), mode);
            var stageResults = stageResultRepository.fromEvaluation(evaluation);
            return ResponseEntity.ok(new EvaluationResultDto(evaluation, stageResults));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/{siren}/history")
    public ResponseEntity<List<Evaluation>> getHistory(@PathVariable String siren) {
        List<Evaluation> history = findBySirenOrId(siren)
                .map(dossier -> evaluationRepository.findByDossierIdOrderByCreatedAtDesc(dossier.getId()))
                .orElseGet(() -> evaluationRepository.findByDossierSirenOrderByCreatedAtDesc(siren));
        return ResponseEntity.ok(history);
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
}
