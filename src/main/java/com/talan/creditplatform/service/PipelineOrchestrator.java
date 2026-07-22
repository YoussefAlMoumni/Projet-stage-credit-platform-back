package com.talan.creditplatform.service;

import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.entity.Evaluation;
import com.talan.creditplatform.repository.AiModelRepository;
import com.talan.creditplatform.repository.EvaluationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PipelineOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(PipelineOrchestrator.class);

    private final SolvabiliteAgent solvabiliteAgent;
    private final HistoriqueAgent historiqueAgent;
    private final GarantiesAgent garantiesAgent;
    private final ConformiteAgent conformiteAgent;
    private final SupervisorAgent supervisorAgent;
    private final EvaluationRepository evaluationRepository;
    private final AiModelRepository aiModelRepository;

    public PipelineOrchestrator(SolvabiliteAgent solvabiliteAgent, HistoriqueAgent historiqueAgent, 
                                GarantiesAgent garantiesAgent, ConformiteAgent conformiteAgent, 
                                SupervisorAgent supervisorAgent,
                                EvaluationRepository evaluationRepository, AiModelRepository aiModelRepository) {
        this.solvabiliteAgent = solvabiliteAgent;
        this.historiqueAgent = historiqueAgent;
        this.garantiesAgent = garantiesAgent;
        this.conformiteAgent = conformiteAgent;
        this.supervisorAgent = supervisorAgent;
        this.evaluationRepository = evaluationRepository;
        this.aiModelRepository = aiModelRepository;
    }

    @Transactional
    public Evaluation evaluate(Dossier dossier, String mode) {
        int workerCtx = "FULL".equalsIgnoreCase(mode) ? 4096 : 2048;
        String workerKeepAlive = "FULL".equalsIgnoreCase(mode) ? "300s" : "0s";
        int supervisorCtx = "FULL".equalsIgnoreCase(mode) ? 8192 : 4096;
        String supervisorKeepAlive = "FULL".equalsIgnoreCase(mode) ? "300s" : "0s";

        Evaluation eval = new Evaluation();
        eval.setDossier(dossier);
        eval.setMode(mode);
        eval.setAiModel(aiModelRepository.findFirstByStageNameAndActiveTrue("supervisor")
                .orElseGet(() -> aiModelRepository.save(
                        new com.talan.creditplatform.model.entity.AiModel("supervisor", "deepseek-r1:14b", supervisorCtx, 0.4, supervisorKeepAlive, true)
                )));
        eval = evaluationRepository.save(eval);

        logger.info("Starting pipeline for dossier {} with mode {}", dossier.getId(), mode);

        String solvabiliteOut = runStage("solvency", eval, () -> solvabiliteAgent.run(dossier, workerCtx, workerKeepAlive));
        String historiqueOut = runStage("history", eval, () -> historiqueAgent.run(dossier, workerCtx, workerKeepAlive));
        String garantiesOut = runStage("guarantees", eval, () -> garantiesAgent.run(dossier, workerCtx, workerKeepAlive));
        String conformiteOut = runStage("compliance", eval, () -> conformiteAgent.run(dossier, workerCtx, workerKeepAlive));

        logger.info("Starting supervisor stage for dossier {}", dossier.getId());
        long start = System.currentTimeMillis();
        String finalReport = supervisorAgent.runSuperviseur(dossier, solvabiliteOut, historiqueOut, garantiesOut, conformiteOut, supervisorCtx, supervisorKeepAlive);
        long duration = System.currentTimeMillis() - start;

        eval.setFinalReport(finalReport);
        eval.setSupervisorDurationMs(toIntDuration(duration));
        evaluationRepository.save(eval);
        
        logger.info("Pipeline completed for dossier {} in {} ms", dossier.getId(), duration);
        return eval;
    }

    private String runStage(String stageName, Evaluation eval, StageRunner runner) {
        logger.info("Running stage: {}", stageName);
        long start = System.currentTimeMillis();
        String result = runner.run();
        long duration = System.currentTimeMillis() - start;

        switch (stageName) {
            case "solvency" -> {
                eval.setSolvencyStageOutput(result);
                eval.setSolvencyDurationMs(toIntDuration(duration));
            }
            case "history" -> {
                eval.setHistoryStageOutput(result);
                eval.setHistoryDurationMs(toIntDuration(duration));
            }
            case "guarantees" -> {
                eval.setGuaranteesStageOutput(result);
                eval.setGuaranteesDurationMs(toIntDuration(duration));
            }
            case "compliance" -> {
                eval.setComplianceStageOutput(result);
                eval.setComplianceDurationMs(toIntDuration(duration));
            }
            default -> throw new IllegalArgumentException("Unknown stage: " + stageName);
        }
        evaluationRepository.save(eval);

        return result;
    }

    private Integer toIntDuration(long duration) {
        return duration > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) duration;
    }

    @FunctionalInterface
    private interface StageRunner {
        String run();
    }
}
