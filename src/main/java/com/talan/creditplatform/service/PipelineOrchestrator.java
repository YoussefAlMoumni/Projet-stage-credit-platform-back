package com.talan.creditplatform.service;

import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.entity.Evaluation;
import com.talan.creditplatform.repository.AiModelRepository;
import com.talan.creditplatform.repository.EvaluationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
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
        // num_ctx must fit: prompt + full <think> reasoning + full answer.
        // deepseek-r1 variants typically need 8k–16k tokens even for short inputs.
        int workerCtx = "FULL".equalsIgnoreCase(mode) ? 16384 : 8192;
        String workerKeepAlive = "FULL".equalsIgnoreCase(mode) ? "300s" : "0s";
        int supervisorCtx = "FULL".equalsIgnoreCase(mode) ? 32768 : 16384;
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

        logger.debug("Launching parallel agent tasks for dossier {}", dossier.getId());

        // helper to run a stage and return outcome
        class StageOutcome {
            final String stageName;
            final String output;
            final long durationMs;

            StageOutcome(String stageName, String output, long durationMs) {
                this.stageName = stageName;
                this.output = output;
                this.durationMs = durationMs;
            }
        }

        Supplier<StageOutcome> solvSupplier = () -> {
            long s = System.currentTimeMillis();
            String r = solvabiliteAgent.run(dossier, workerCtx, workerKeepAlive);
            long d = System.currentTimeMillis() - s;
            return new StageOutcome("solvency", r, d);
        };

        Supplier<StageOutcome> histSupplier = () -> {
            long s = System.currentTimeMillis();
            String r = historiqueAgent.run(dossier, workerCtx, workerKeepAlive);
            long d = System.currentTimeMillis() - s;
            return new StageOutcome("history", r, d);
        };

        Supplier<StageOutcome> guarSupplier = () -> {
            long s = System.currentTimeMillis();
            String r = garantiesAgent.run(dossier, workerCtx, workerKeepAlive);
            long d = System.currentTimeMillis() - s;
            return new StageOutcome("guarantees", r, d);
        };

        Supplier<StageOutcome> confSupplier = () -> {
            long s = System.currentTimeMillis();
            String r = conformiteAgent.run(dossier, workerCtx, workerKeepAlive);
            long d = System.currentTimeMillis() - s;
            return new StageOutcome("compliance", r, d);
        };

        CompletableFuture<StageOutcome> f1 = CompletableFuture.supplyAsync(solvSupplier);
        CompletableFuture<StageOutcome> f2 = CompletableFuture.supplyAsync(histSupplier);
        CompletableFuture<StageOutcome> f3 = CompletableFuture.supplyAsync(guarSupplier);
        CompletableFuture<StageOutcome> f4 = CompletableFuture.supplyAsync(confSupplier);

        CompletableFuture<Void> all = CompletableFuture.allOf(f1, f2, f3, f4);
        try {
            all.join();
        } catch (Exception e) {
            logger.error("Error while running parallel stages", e);
        }

        // Collect stage outputs individually. Each stage is wrapped in its own
        // try-catch so a single failure doesn't prevent the others from being
        // persisted. Non-null fallback strings ensure SupervisorAgent.replace()
        // never receives a null replacement argument.
        try {
            StageOutcome so1 = f1.get();
            eval.setSolvencyStageOutput(so1.output);
            eval.setSolvencyDurationMs(toIntDuration(so1.durationMs));
        } catch (Exception e) {
            logger.error("Stage 'solvency' failed to collect: {}", e.getMessage(), e);
            eval.setSolvencyStageOutput("[STAGE FAILED: solvency — " + sanitize(e) + "]");
        }
        try {
            StageOutcome so2 = f2.get();
            eval.setHistoryStageOutput(so2.output);
            eval.setHistoryDurationMs(toIntDuration(so2.durationMs));
        } catch (Exception e) {
            logger.error("Stage 'history' failed to collect: {}", e.getMessage(), e);
            eval.setHistoryStageOutput("[STAGE FAILED: history — " + sanitize(e) + "]");
        }
        try {
            StageOutcome so3 = f3.get();
            eval.setGuaranteesStageOutput(so3.output);
            eval.setGuaranteesDurationMs(toIntDuration(so3.durationMs));
        } catch (Exception e) {
            logger.error("Stage 'guarantees' failed to collect: {}", e.getMessage(), e);
            eval.setGuaranteesStageOutput("[STAGE FAILED: guarantees — " + sanitize(e) + "]");
        }
        try {
            StageOutcome so4 = f4.get();
            eval.setComplianceStageOutput(so4.output);
            eval.setComplianceDurationMs(toIntDuration(so4.durationMs));
        } catch (Exception e) {
            logger.error("Stage 'compliance' failed to collect: {}", e.getMessage(), e);
            eval.setComplianceStageOutput("[STAGE FAILED: compliance — " + sanitize(e) + "]");
        }
        try {
            evaluationRepository.save(eval);
        } catch (Exception e) {
            logger.error("Failed to persist stage outcomes", e);
        }

        logger.info("Starting supervisor stage for dossier {}", dossier.getId());
        long start = System.currentTimeMillis();
        String finalReport = supervisorAgent.runSuperviseur(dossier, eval.getSolvencyStageOutput(), eval.getHistoryStageOutput(), eval.getGuaranteesStageOutput(), eval.getComplianceStageOutput(), supervisorCtx, supervisorKeepAlive);
        long duration = System.currentTimeMillis() - start;

        eval.setFinalReport(finalReport);
        eval.setSupervisorDurationMs(toIntDuration(duration));
        evaluationRepository.save(eval);
        
        logger.info("Pipeline completed for dossier {} in {} ms", dossier.getId(), duration);
        return eval;
    }

    @Transactional
    public Evaluation evaluateWithProgress(Dossier dossier, String mode, java.util.function.Consumer<com.talan.creditplatform.model.dto.PipelineStageEvent> listener) {
        // num_ctx must fit: prompt + full <think> reasoning + full answer.
        int workerCtx = "FULL".equalsIgnoreCase(mode) ? 16384 : 8192;
        String workerKeepAlive = "FULL".equalsIgnoreCase(mode) ? "300s" : "0s";
        int supervisorCtx = "FULL".equalsIgnoreCase(mode) ? 32768 : 16384;
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

        // Run specialist stages in parallel and stream progress via listener
        class StageOutcome {
            final String stageName;
            final String output;
            final long durationMs;

            StageOutcome(String stageName, String output, long durationMs) {
                this.stageName = stageName;
                this.output = output;
                this.durationMs = durationMs;
            }
        }

        Supplier<StageOutcome> solvSupplier = () -> {
            if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("solvency","STARTED",15,null,null));
            long s = System.currentTimeMillis();
            String r = solvabiliteAgent.run(dossier, workerCtx, workerKeepAlive);
            long d = System.currentTimeMillis() - s;
            String summary = r != null ? com.talan.creditplatform.model.entity.StageResult.extractSummary(r) : "No response";
            if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("solvency","COMPLETED",30,r,summary,toIntDuration(d)));
            return new StageOutcome("solvency", r, d);
        };

        Supplier<StageOutcome> histSupplier = () -> {
            if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("history","STARTED",35,null,null));
            long s = System.currentTimeMillis();
            String r = historiqueAgent.run(dossier, workerCtx, workerKeepAlive);
            long d = System.currentTimeMillis() - s;
            String summary = r != null ? com.talan.creditplatform.model.entity.StageResult.extractSummary(r) : "No response";
            if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("history","COMPLETED",50,r,summary,toIntDuration(d)));
            return new StageOutcome("history", r, d);
        };

        Supplier<StageOutcome> guarSupplier = () -> {
            if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("guarantees","STARTED",55,null,null));
            long s = System.currentTimeMillis();
            String r = garantiesAgent.run(dossier, workerCtx, workerKeepAlive);
            long d = System.currentTimeMillis() - s;
            String summary = r != null ? com.talan.creditplatform.model.entity.StageResult.extractSummary(r) : "No response";
            if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("guarantees","COMPLETED",70,r,summary,toIntDuration(d)));
            return new StageOutcome("guarantees", r, d);
        };

        Supplier<StageOutcome> confSupplier = () -> {
            if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("compliance","STARTED",75,null,null));
            long s = System.currentTimeMillis();
            String r = conformiteAgent.run(dossier, workerCtx, workerKeepAlive);
            long d = System.currentTimeMillis() - s;
            String summary = r != null ? com.talan.creditplatform.model.entity.StageResult.extractSummary(r) : "No response";
            if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("compliance","COMPLETED",85,r,summary,toIntDuration(d)));
            return new StageOutcome("compliance", r, d);
        };

        CompletableFuture<StageOutcome> f1 = CompletableFuture.supplyAsync(solvSupplier);
        CompletableFuture<StageOutcome> f2 = CompletableFuture.supplyAsync(histSupplier);
        CompletableFuture<StageOutcome> f3 = CompletableFuture.supplyAsync(guarSupplier);
        CompletableFuture<StageOutcome> f4 = CompletableFuture.supplyAsync(confSupplier);

        CompletableFuture<Void> all = CompletableFuture.allOf(f1, f2, f3, f4);
        try {
            all.join();
        } catch (Exception e) {
            logger.error("Error while running parallel stages (with progress)", e);
        }

        // Collect and persist outputs individually. Each stage is wrapped in its
        // own try-catch so a single failure doesn't prevent the others from being
        // persisted. Non-null fallback strings ensure SupervisorAgent.replace()
        // never receives a null replacement argument.
        try {
            StageOutcome so1 = f1.get();
            eval.setSolvencyStageOutput(so1.output);
            eval.setSolvencyDurationMs(toIntDuration(so1.durationMs));
        } catch (Exception e) {
            logger.error("Stage 'solvency' failed to collect (with progress): {}", e.getMessage(), e);
            eval.setSolvencyStageOutput("[STAGE FAILED: solvency — " + sanitize(e) + "]");
        }
        try {
            StageOutcome so2 = f2.get();
            eval.setHistoryStageOutput(so2.output);
            eval.setHistoryDurationMs(toIntDuration(so2.durationMs));
        } catch (Exception e) {
            logger.error("Stage 'history' failed to collect (with progress): {}", e.getMessage(), e);
            eval.setHistoryStageOutput("[STAGE FAILED: history — " + sanitize(e) + "]");
        }
        try {
            StageOutcome so3 = f3.get();
            eval.setGuaranteesStageOutput(so3.output);
            eval.setGuaranteesDurationMs(toIntDuration(so3.durationMs));
        } catch (Exception e) {
            logger.error("Stage 'guarantees' failed to collect (with progress): {}", e.getMessage(), e);
            eval.setGuaranteesStageOutput("[STAGE FAILED: guarantees — " + sanitize(e) + "]");
        }
        try {
            StageOutcome so4 = f4.get();
            eval.setComplianceStageOutput(so4.output);
            eval.setComplianceDurationMs(toIntDuration(so4.durationMs));
        } catch (Exception e) {
            logger.error("Stage 'compliance' failed to collect (with progress): {}", e.getMessage(), e);
            eval.setComplianceStageOutput("[STAGE FAILED: compliance — " + sanitize(e) + "]");
        }
        try {
            evaluationRepository.save(eval);
        } catch (Exception e) {
            logger.error("Failed to persist stage outcomes (with progress)", e);
        }

        logger.info("Starting supervisor stage for dossier {}", dossier.getId());
        long start = System.currentTimeMillis();
        if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("supervisor", "STARTED", 85, null, null));
        String finalReport = supervisorAgent.runSuperviseur(dossier, eval.getSolvencyStageOutput(), eval.getHistoryStageOutput(), eval.getGuaranteesStageOutput(), eval.getComplianceStageOutput(), supervisorCtx, supervisorKeepAlive);
        long duration = System.currentTimeMillis() - start;

        eval.setFinalReport(finalReport);
        eval.setSupervisorDurationMs(toIntDuration(duration));
        evaluationRepository.save(eval);
        
        String supervisorSummary = finalReport != null ? com.talan.creditplatform.model.entity.StageResult.extractSummary(finalReport) : "No response";
        if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("supervisor", "COMPLETED", 100, finalReport, supervisorSummary, toIntDuration(duration)));

        logger.info("Pipeline completed for dossier {} in {} ms", dossier.getId(), duration);
        return eval;
    }

    private String runStage(String stageName, Evaluation eval, int progressStarted, java.util.function.Consumer<com.talan.creditplatform.model.dto.PipelineStageEvent> listener, StageRunner runner) {
        logger.info("Running stage: {}", stageName);
        if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent(stageName, "STARTED", progressStarted, null, null));
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

        String stageSummary = com.talan.creditplatform.model.entity.StageResult.extractSummary(result);
        if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent(stageName, "COMPLETED", progressStarted + 15, result, stageSummary, toIntDuration(duration)));

        return result;
    }

    private Integer toIntDuration(long duration) {
        return duration > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) duration;
    }

    /**
     * Extracts a concise, safe error message from an exception for use in
     * stage fallback output strings. Unwraps ExecutionException to get the
     * actual cause.
     */
    private String sanitize(Exception e) {
        Throwable cause = (e instanceof java.util.concurrent.ExecutionException && e.getCause() != null)
                ? e.getCause() : e;
        String msg = cause.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = cause.getClass().getSimpleName();
        }
        // Truncate to avoid overly long strings in DB/logs.
        return msg.length() > 200 ? msg.substring(0, 200) + "..." : msg;
    }

    @FunctionalInterface
    private interface StageRunner {
        String run();
    }
}
