package com.talan.creditplatform.model.dto;

public class PipelineStageEvent {
    private String stageName;
    private String status; // STARTED, COMPLETED, FAILED
    private int progress; // 0..100
    private String output;
    private String summary;
    private Integer durationMs;

    public PipelineStageEvent() {
    }

    public PipelineStageEvent(String stageName, String status, int progress, String output, Integer durationMs) {
        this(stageName, status, progress, output, null, durationMs);
    }

    public PipelineStageEvent(String stageName, String status, int progress, String output, String summary, Integer durationMs) {
        this.stageName = stageName;
        this.status = status;
        this.progress = progress;
        this.output = output;
        this.summary = summary;
        this.durationMs = durationMs;
    }

    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }
}
