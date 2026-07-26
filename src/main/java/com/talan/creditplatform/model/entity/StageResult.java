package com.talan.creditplatform.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class StageResult {

    private Long id;
    @JsonIgnore
    private Evaluation evaluation;
    private String stageName;
    private String output;
    private String summary;
    private Long durationMs;

    public StageResult() {}

    public StageResult(com.talan.creditplatform.model.entity.Evaluation evaluation, String stageName, String output, Number durationMs) {
        this(evaluation, stageName, output, extractSummary(output), durationMs);
    }

    public StageResult(com.talan.creditplatform.model.entity.Evaluation evaluation, String stageName, String output, String summary, Number durationMs) {
        this.evaluation = evaluation;
        this.stageName = stageName;
        this.output = output;
        this.summary = summary;
        this.durationMs = durationMs == null ? null : durationMs.longValue();
    }

    public static String extractSummary(String fullDecision) {
        if (fullDecision == null || fullDecision.isBlank()) {
            return "";
        }

        String[] lines = fullDecision.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            trimmed = trimmed.replaceFirst("^#+\\s*", "");
            trimmed = trimmed.replaceFirst("^[•\\-\\*]+\\s*", "");
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() > 140) {
                return trimmed.substring(0, 140).trim() + "...";
            }
            return trimmed;
        }

        String trimmed = fullDecision.trim();
        if (trimmed.length() > 140) {
            return trimmed.substring(0, 140).trim() + "...";
        }
        return trimmed;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public com.talan.creditplatform.model.entity.Evaluation getEvaluation() { return evaluation; }
    public void setEvaluation(com.talan.creditplatform.model.entity.Evaluation evaluation) { this.evaluation = evaluation; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getFullDecision() { return output; }
    public void setFullDecision(String fullDecision) { this.output = fullDecision; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}
