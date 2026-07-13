package com.talan.creditplatform.model.entity;

public class StageResult {

    private Long id;
    private Evaluation evaluation;
    private String stageName;
    private String output;
    private Long durationMs;

    public StageResult() {}

    public StageResult(Evaluation evaluation, String stageName, String output, Number durationMs) {
        this.evaluation = evaluation;
        this.stageName = stageName;
        this.output = output;
        this.durationMs = durationMs == null ? null : durationMs.longValue();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Evaluation getEvaluation() { return evaluation; }
    public void setEvaluation(Evaluation evaluation) { this.evaluation = evaluation; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}
