package com.talan.creditplatform.model.dto;

import com.talan.creditplatform.model.entity.Evaluation;
import com.talan.creditplatform.model.entity.StageResult;

import java.util.List;

public class EvaluationResultDto {
    private Evaluation evaluation;
    private List<StageResult> stageResults;

    public EvaluationResultDto(Evaluation evaluation, List<StageResult> stageResults) {
        this.evaluation = evaluation;
        this.stageResults = stageResults;
    }

    public Evaluation getEvaluation() { return evaluation; }
    public void setEvaluation(Evaluation evaluation) { this.evaluation = evaluation; }
    public List<StageResult> getStageResults() { return stageResults; }
    public void setStageResults(List<StageResult> stageResults) { this.stageResults = stageResults; }
}
