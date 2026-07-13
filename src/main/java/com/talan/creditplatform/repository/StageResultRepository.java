package com.talan.creditplatform.repository;

import com.talan.creditplatform.model.entity.Evaluation;
import com.talan.creditplatform.model.entity.StageResult;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StageResultRepository {

    public List<StageResult> findByEvaluationId(Long evaluationId) {
        return List.of();
    }

    public List<StageResult> fromEvaluation(Evaluation evaluation) {
        return List.of(
                new StageResult(evaluation, "solvency", evaluation.getSolvencyStageOutput(), evaluation.getSolvencyDurationMs()),
                new StageResult(evaluation, "history", evaluation.getHistoryStageOutput(), evaluation.getHistoryDurationMs()),
                new StageResult(evaluation, "guarantees", evaluation.getGuaranteesStageOutput(), evaluation.getGuaranteesDurationMs()),
                new StageResult(evaluation, "compliance", evaluation.getComplianceStageOutput(), evaluation.getComplianceDurationMs()),
                new StageResult(evaluation, "supervisor", evaluation.getSupervisorStageOutput(), evaluation.getSupervisorDurationMs())
        );
    }
}
