package com.talan.creditplatform.service;

import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.entity.Evaluation;
import com.talan.creditplatform.repository.AiModelRepository;
import com.talan.creditplatform.repository.EvaluationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PipelineOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(PipelineOrchestrator.class);

    // We use a VirtualThreadPerTaskExecutor to scale efficiently without blocking platform threads.
    // Future<?> objects returned by executor.submit() support true Thread.interrupt() via cancel(true).
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    
    // Tracks active futures for each dossier so they can be cancelled.
    private final ConcurrentHashMap<Long, java.util.List<Future<?>>> activeTasks = new ConcurrentHashMap<>();

    public void cancelEvaluation(Long dossierId) {
        java.util.List<Future<?>> futures = activeTasks.remove(dossierId);
        if (futures != null) {
            logger.info("Cancelling pipeline for dossier {}", dossierId);
            for (Future<?> f : futures) {
                // cancel(true) interrupts the underlying virtual thread executing the task.
                f.cancel(true);
            }
        }
    }

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
        String effectiveMode = "FAST";
        int workerCtx = 8192;
        String workerKeepAlive = "0s";
        int supervisorCtx = 16384;
        String supervisorKeepAlive = "0s";

        Evaluation eval = new Evaluation();
        eval.setDossier(dossier);
        eval.setMode(effectiveMode);
        eval.setAiModel(aiModelRepository.findFirstByStageNameAndActiveTrue("supervisor")
                .orElseGet(() -> aiModelRepository.save(
                        new com.talan.creditplatform.model.entity.AiModel("supervisor", "deepseek-r1:14b", supervisorCtx, 0.4, supervisorKeepAlive, true)
                )));
        evaluationRepository.save(eval);

        logger.info("Starting pipeline for dossier {} with mode {}", dossier.getId(), effectiveMode);

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

        // Build dossier context BEFORE spawning async threads.
        String dossierContext = DossierContextBuilder.build(dossier);

        Callable<StageOutcome> solvCallable = () -> {
            long s = System.currentTimeMillis();
            String r = solvabiliteAgent.run(dossier, dossierContext, workerCtx, workerKeepAlive);
            return new StageOutcome("solvency", r, System.currentTimeMillis() - s);
        };

        Callable<StageOutcome> histCallable = () -> {
            long s = System.currentTimeMillis();
            String r = historiqueAgent.run(dossier, dossierContext, workerCtx, workerKeepAlive);
            return new StageOutcome("history", r, System.currentTimeMillis() - s);
        };

        Callable<StageOutcome> guarCallable = () -> {
            long s = System.currentTimeMillis();
            String r = garantiesAgent.run(dossier, dossierContext, workerCtx, workerKeepAlive);
            return new StageOutcome("guarantees", r, System.currentTimeMillis() - s);
        };

        Callable<StageOutcome> confCallable = () -> {
            long s = System.currentTimeMillis();
            String r = conformiteAgent.run(dossier, dossierContext, workerCtx, workerKeepAlive);
            return new StageOutcome("compliance", r, System.currentTimeMillis() - s);
        };

        Future<StageOutcome> f1 = executor.submit(solvCallable);
        Future<StageOutcome> f2 = executor.submit(histCallable);
        Future<StageOutcome> f3 = executor.submit(guarCallable);
        Future<StageOutcome> f4 = executor.submit(confCallable);

        // Track the parallel stages for cancellation
        activeTasks.put(dossier.getId(), java.util.Collections.synchronizedList(new java.util.ArrayList<>(java.util.Arrays.asList(f1, f2, f3, f4))));

        try {
            // Await each stage, but fail fast if the thread was interrupted or cancelled
            StageOutcome so1 = f1.get();
            eval.setSolvencyStageOutput(so1.output);
            eval.setSolvencyDurationMs(toIntDuration(so1.durationMs));

            StageOutcome so2 = f2.get();
            eval.setHistoryStageOutput(so2.output);
            eval.setHistoryDurationMs(toIntDuration(so2.durationMs));

            StageOutcome so3 = f3.get();
            eval.setGuaranteesStageOutput(so3.output);
            eval.setGuaranteesDurationMs(toIntDuration(so3.durationMs));

            StageOutcome so4 = f4.get();
            eval.setComplianceStageOutput(so4.output);
            eval.setComplianceDurationMs(toIntDuration(so4.durationMs));

            evaluationRepository.save(eval);

            logger.info("Starting supervisor stage for dossier {}", dossier.getId());
            
            Callable<String> supervisorCallable = () -> {
                long start = System.currentTimeMillis();
                String report = supervisorAgent.runSuperviseur(dossier, dossierContext, 
                        eval.getSolvencyStageOutput(), eval.getHistoryStageOutput(), 
                        eval.getGuaranteesStageOutput(), eval.getComplianceStageOutput(), 
                        supervisorCtx, supervisorKeepAlive);
                eval.setSupervisorDurationMs(toIntDuration(System.currentTimeMillis() - start));
                return report;
            };

            // Track the supervisor stage for cancellation as well
            Future<String> fSuper = executor.submit(supervisorCallable);
            java.util.List<Future<?>> tasks = activeTasks.get(dossier.getId());
            if (tasks != null) {
                tasks.add(fSuper);
            }

            String finalReport = fSuper.get();
            eval.setFinalReport(finalReport);
            evaluationRepository.save(eval);

            logger.info("Pipeline completed for dossier {}", dossier.getId());
            return eval;

        } catch (InterruptedException | CancellationException e) {
            logger.warn("Pipeline cancelled for dossier {}", dossier.getId());
            throw new RuntimeException("Pipeline Cancelled", e);
        } catch (ExecutionException e) {
            logger.error("Error while running pipeline stages", e);
            throw new RuntimeException("Pipeline Failed", e.getCause());
        } finally {
            activeTasks.remove(dossier.getId());
        }
    }

    @Transactional
    public Evaluation evaluateWithProgress(Dossier dossier, String mode, java.util.function.Consumer<com.talan.creditplatform.model.dto.PipelineStageEvent> listener) {
        String effectiveMode = "FAST";
        int workerCtx = 8192;
        String workerKeepAlive = "0s";
        int supervisorCtx = 16384;
        String supervisorKeepAlive = "0s";

        Evaluation eval = new Evaluation();
        eval.setDossier(dossier);
        eval.setMode(effectiveMode);
        eval.setAiModel(aiModelRepository.findFirstByStageNameAndActiveTrue("supervisor")
                .orElseGet(() -> aiModelRepository.save(
                        new com.talan.creditplatform.model.entity.AiModel("supervisor", "deepseek-r1:14b", supervisorCtx, 0.4, supervisorKeepAlive, true)
                )));
        evaluationRepository.save(eval);

        logger.info("Starting pipeline for dossier {} with mode {}", dossier.getId(), effectiveMode);

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

        // Build dossier context BEFORE spawning async threads.
        String dossierContext = DossierContextBuilder.build(dossier);

        Callable<StageOutcome> solvCallable = () -> {
            if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("solvency","STARTED",15,null,null));
            long s = System.currentTimeMillis();
            String r = solvabiliteAgent.run(dossier, dossierContext, workerCtx, workerKeepAlive);
            long d = System.currentTimeMillis() - s;
            String summary = r != null ? com.talan.creditplatform.model.entity.StageResult.extractSummary(r) : "No response";
            if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("solvency","COMPLETED",30,r,summary,toIntDuration(d)));
            return new StageOutcome("solvency", r, d);
        };

        Callable<StageOutcome> histCallable = () -> {
            if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("history","STARTED",35,null,null));
            long s = System.currentTimeMillis();
            String r = historiqueAgent.run(dossier, dossierContext, workerCtx, workerKeepAlive);
            long d = System.currentTimeMillis() - s;
            String summary = r != null ? com.talan.creditplatform.model.entity.StageResult.extractSummary(r) : "No response";
            if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("history","COMPLETED",50,r,summary,toIntDuration(d)));
            return new StageOutcome("history", r, d);
        };

        Callable<StageOutcome> guarCallable = () -> {
            if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("guarantees","STARTED",55,null,null));
            long s = System.currentTimeMillis();
            String r = garantiesAgent.run(dossier, dossierContext, workerCtx, workerKeepAlive);
            long d = System.currentTimeMillis() - s;
            String summary = r != null ? com.talan.creditplatform.model.entity.StageResult.extractSummary(r) : "No response";
            if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("guarantees","COMPLETED",70,r,summary,toIntDuration(d)));
            return new StageOutcome("guarantees", r, d);
        };

        Callable<StageOutcome> confCallable = () -> {
            if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("compliance","STARTED",75,null,null));
            long s = System.currentTimeMillis();
            String r = conformiteAgent.run(dossier, dossierContext, workerCtx, workerKeepAlive);
            long d = System.currentTimeMillis() - s;
            String summary = r != null ? com.talan.creditplatform.model.entity.StageResult.extractSummary(r) : "No response";
            if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("compliance","COMPLETED",85,r,summary,toIntDuration(d)));
            return new StageOutcome("compliance", r, d);
        };

        Future<StageOutcome> f1 = executor.submit(solvCallable);
        Future<StageOutcome> f2 = executor.submit(histCallable);
        Future<StageOutcome> f3 = executor.submit(guarCallable);
        Future<StageOutcome> f4 = executor.submit(confCallable);

        activeTasks.put(dossier.getId(), java.util.Collections.synchronizedList(new java.util.ArrayList<>(java.util.Arrays.asList(f1, f2, f3, f4))));

        try {
            // Await each stage. If ANY stage is cancelled or throws an exception,
            // we catch it below and ABORT the pipeline, skipping the supervisor stage.
            StageOutcome so1 = f1.get();
            eval.setSolvencyStageOutput(so1.output);
            eval.setSolvencyDurationMs(toIntDuration(so1.durationMs));

            StageOutcome so2 = f2.get();
            eval.setHistoryStageOutput(so2.output);
            eval.setHistoryDurationMs(toIntDuration(so2.durationMs));

            StageOutcome so3 = f3.get();
            eval.setGuaranteesStageOutput(so3.output);
            eval.setGuaranteesDurationMs(toIntDuration(so3.durationMs));

            StageOutcome so4 = f4.get();
            eval.setComplianceStageOutput(so4.output);
            eval.setComplianceDurationMs(toIntDuration(so4.durationMs));

            evaluationRepository.save(eval);

            logger.info("Starting supervisor stage for dossier {}", dossier.getId());
            
            Callable<String> supervisorCallable = () -> {
                long start = System.currentTimeMillis();
                if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("supervisor", "STARTED", 85, null, null));
                
                String report = supervisorAgent.runSuperviseur(dossier, dossierContext, 
                        eval.getSolvencyStageOutput(), eval.getHistoryStageOutput(), 
                        eval.getGuaranteesStageOutput(), eval.getComplianceStageOutput(), 
                        supervisorCtx, supervisorKeepAlive);
                        
                long duration = System.currentTimeMillis() - start;
                eval.setSupervisorDurationMs(toIntDuration(duration));
                
                String supervisorSummary = report != null ? com.talan.creditplatform.model.entity.StageResult.extractSummary(report) : "No response";
                if (listener != null) listener.accept(new com.talan.creditplatform.model.dto.PipelineStageEvent("supervisor", "COMPLETED", 100, report, supervisorSummary, toIntDuration(duration)));
                
                return report;
            };

            // Track supervisor future so it can also be cancelled
            Future<String> fSuper = executor.submit(supervisorCallable);
            java.util.List<Future<?>> tasks = activeTasks.get(dossier.getId());
            if (tasks != null) {
                tasks.add(fSuper);
            }

            String finalReport = fSuper.get();
            eval.setFinalReport(finalReport);
            evaluationRepository.save(eval);

            logger.info("Pipeline completed for dossier {}", dossier.getId());
            return eval;

        } catch (InterruptedException | CancellationException e) {
            logger.warn("Pipeline cancelled for dossier {}", dossier.getId());
            // Fast fail. This stops the orchestrator from progressing to the next stages.
            throw new RuntimeException("Pipeline Cancelled", e);
        } catch (ExecutionException e) {
            logger.error("Error while running pipeline stages", e);
            throw new RuntimeException("Pipeline Failed", e.getCause());
        } finally {
            activeTasks.remove(dossier.getId());
        }
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
        Throwable cause = (e instanceof ExecutionException && e.getCause() != null)
                ? e.getCause() : e;
        String msg = cause.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = cause.getClass().getSimpleName();
        }
        // Truncate to avoid overly long strings in DB/logs.
        return msg.length() > 200 ? msg.substring(0, 200) + "..." : msg;
    }
}
