package com.talan.creditplatform.service;

import com.talan.creditplatform.model.dto.PipelineStageEvent;
import com.talan.creditplatform.model.entity.AiModel;
import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.entity.Evaluation;
import com.talan.creditplatform.model.entity.StageResult;
import com.talan.creditplatform.repository.AiModelRepository;
import com.talan.creditplatform.repository.EvaluationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Orchestrates the multi-agent AI pipeline for a given Dossier.
 *
 * <p>The pipeline runs 4 specialist agents (solvency, history, guarantees, compliance) in parallel,
 * then feeds their outputs to a supervisor agent for a final consolidated decision.
 *
 * <p>Cancellation: calling {@link #cancelEvaluation(Long)} interrupts all active virtual threads
 * for that dossier immediately, including any in-progress HTTP connection to Ollama.
 */
@Service
public class PipelineOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(PipelineOrchestrator.class);

    // Context window sizes (in tokens)
    private static final int WORKER_CTX      = 8192;
    private static final int SUPERVISOR_CTX  = 16384;
    private static final String KEEPALIVE    = "0s";

    // We use a VirtualThreadPerTaskExecutor to scale efficiently without blocking platform threads.
    // Future<?> objects returned by executor.submit() support true Thread.interrupt() via cancel(true).
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    // Tracks active futures for each dossier so they can be cancelled.
    private final ConcurrentHashMap<Long, List<Future<?>>> activeTasks = new ConcurrentHashMap<>();

    private final SolvabiliteAgent solvabiliteAgent;
    private final HistoriqueAgent  historiqueAgent;
    private final GarantiesAgent   garantiesAgent;
    private final ConformiteAgent  conformiteAgent;
    private final SupervisorAgent  supervisorAgent;
    private final EvaluationRepository evaluationRepository;
    private final AiModelRepository    aiModelRepository;

    public PipelineOrchestrator(SolvabiliteAgent solvabiliteAgent, HistoriqueAgent historiqueAgent,
                                GarantiesAgent garantiesAgent, ConformiteAgent conformiteAgent,
                                SupervisorAgent supervisorAgent,
                                EvaluationRepository evaluationRepository, AiModelRepository aiModelRepository) {
        this.solvabiliteAgent    = solvabiliteAgent;
        this.historiqueAgent     = historiqueAgent;
        this.garantiesAgent      = garantiesAgent;
        this.conformiteAgent     = conformiteAgent;
        this.supervisorAgent     = supervisorAgent;
        this.evaluationRepository = evaluationRepository;
        this.aiModelRepository   = aiModelRepository;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Runs the full AI pipeline synchronously and returns the completed {@link Evaluation}.
     * Delegates to {@link #runPipeline(Dossier, String, Consumer)} with no progress listener.
     */
    @Transactional
    public Evaluation evaluate(Dossier dossier, String mode) {
        return runPipeline(dossier, mode, null);
    }

    /**
     * Runs the full AI pipeline and emits {@link PipelineStageEvent} updates to the given listener
     * as each stage starts and completes. Used for SSE-backed real-time progress streaming.
     */
    @Transactional
    public Evaluation evaluateWithProgress(Dossier dossier, String mode, Consumer<PipelineStageEvent> listener) {
        return runPipeline(dossier, mode, listener);
    }

    /**
     * Cancels the active pipeline for the given dossier ID by interrupting all tracked futures.
     * The underlying virtual threads will receive an interrupt, causing the HTTP connection to Ollama
     * to be aborted immediately.
     */
    public void cancelEvaluation(Long dossierId) {
        List<Future<?>> futures = activeTasks.remove(dossierId);
        if (futures != null) {
            logger.info("Cancelling pipeline for dossier {}", dossierId);
            futures.forEach(f -> f.cancel(true));
        }
    }

    // -------------------------------------------------------------------------
    // Core orchestration — single private method used by both public methods
    // -------------------------------------------------------------------------

    /**
     * Core pipeline implementation. The {@code listener} parameter is nullable; when non-null
     * it receives STARTED/COMPLETED events for each stage.
     *
     * @param dossier  the dossier to evaluate
     * @param mode     execution mode hint (currently unused; always runs as FAST)
     * @param listener optional SSE progress callback; may be null
     * @return the persisted, completed {@link Evaluation}
     * @throws RuntimeException wrapping {@link CancellationException} or {@link ExecutionException}
     */
    private Evaluation runPipeline(Dossier dossier, String mode, Consumer<PipelineStageEvent> listener) {
        final String effectiveMode = "FAST";

        // Initialize Evaluation record
        Evaluation eval = new Evaluation();
        eval.setDossier(dossier);
        eval.setMode(effectiveMode);
        eval.setAiModel(aiModelRepository.findFirstByStageNameAndActiveTrue("supervisor")
                .orElseGet(() -> aiModelRepository.save(
                        new AiModel("supervisor", "deepseek-r1:14b", SUPERVISOR_CTX, 0.4, KEEPALIVE, true)
                )));
        evaluationRepository.save(eval);

        logger.info("Starting pipeline for dossier {} with mode {}", dossier.getId(), effectiveMode);

        final String dossierContext = DossierContextBuilder.build(dossier);

        // -- Submit 4 parallel specialist agents --
        Future<StageOutcome> f1 = executor.submit(() -> runStage("solvency",    10, 30, listener,
                () -> solvabiliteAgent.run(dossier, dossierContext, WORKER_CTX, KEEPALIVE)));
        Future<StageOutcome> f2 = executor.submit(() -> runStage("history",     35, 50, listener,
                () -> historiqueAgent.run(dossier, dossierContext, WORKER_CTX, KEEPALIVE)));
        Future<StageOutcome> f3 = executor.submit(() -> runStage("guarantees",  55, 70, listener,
                () -> garantiesAgent.run(dossier, dossierContext, WORKER_CTX, KEEPALIVE)));
        Future<StageOutcome> f4 = executor.submit(() -> runStage("compliance",  75, 85, listener,
                () -> conformiteAgent.run(dossier, dossierContext, WORKER_CTX, KEEPALIVE)));

        List<Future<?>> tracked = Collections.synchronizedList(new ArrayList<>(Arrays.asList(f1, f2, f3, f4)));
        activeTasks.put(dossier.getId(), tracked);

        try {
            // Await each parallel stage — fail fast on cancellation or error
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

            // -- Supervisor stage --
            logger.info("Starting supervisor stage for dossier {}", dossier.getId());

            Future<StageOutcome> fSuper = executor.submit(() -> runStage("supervisor", 85, 100, listener,
                    () -> supervisorAgent.runSuperviseur(dossier, dossierContext,
                            eval.getSolvencyStageOutput(), eval.getHistoryStageOutput(),
                            eval.getGuaranteesStageOutput(), eval.getComplianceStageOutput(),
                            SUPERVISOR_CTX, KEEPALIVE)));
            tracked.add(fSuper);

            StageOutcome supervisorOutcome = fSuper.get();
            eval.setFinalReport(supervisorOutcome.output);
            eval.setSupervisorDurationMs(toIntDuration(supervisorOutcome.durationMs));
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

    /**
     * Executes a single agent stage, emitting STARTED and COMPLETED SSE events when a listener
     * is provided.
     *
     * @param stageName    unique stage identifier used in SSE events
     * @param startPct     progress percentage to emit on STARTED
     * @param completedPct progress percentage to emit on COMPLETED
     * @param listener     nullable SSE callback
     * @param work         the callable that invokes the agent
     * @return a {@link StageOutcome} with the agent output and elapsed time
     */
    private StageOutcome runStage(String stageName, int startPct, int completedPct,
                                  Consumer<PipelineStageEvent> listener,
                                  java.util.concurrent.Callable<String> work) throws Exception {
        emit(listener, new PipelineStageEvent(stageName, "STARTED", startPct, null, null));
        long start = System.currentTimeMillis();
        String output = work.call();
        long durationMs = System.currentTimeMillis() - start;
        String summary = output != null ? StageResult.extractSummary(output) : "No response";
        emit(listener, new PipelineStageEvent(stageName, "COMPLETED", completedPct, output, summary, toIntDuration(durationMs)));
        return new StageOutcome(stageName, output, durationMs);
    }

    private void emit(Consumer<PipelineStageEvent> listener, PipelineStageEvent event) {
        if (listener != null) {
            listener.accept(event);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Integer toIntDuration(long duration) {
        return duration > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) duration;
    }

    /**
     * Immutable value object holding the output and timing of a completed pipeline stage.
     */
    private record StageOutcome(String stageName, String output, long durationMs) {}
}
