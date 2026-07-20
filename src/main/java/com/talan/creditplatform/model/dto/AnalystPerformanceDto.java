package com.talan.creditplatform.model.dto;

import com.talan.creditplatform.model.entity.User;

public class AnalystPerformanceDto {
    private User analyst;
    private long totalDossiers;
    private long completedDossiers;
    private double performanceScore;

    public AnalystPerformanceDto(User analyst, long totalDossiers, long completedDossiers) {
        this.analyst = analyst;
        this.totalDossiers = totalDossiers;
        this.completedDossiers = completedDossiers;
        this.performanceScore = totalDossiers > 0 ? (completedDossiers * 100.0 / totalDossiers) : 0.0;
    }

    public User getAnalyst() { return analyst; }
    public void setAnalyst(User analyst) { this.analyst = analyst; }
    public long getTotalDossiers() { return totalDossiers; }
    public void setTotalDossiers(long totalDossiers) { this.totalDossiers = totalDossiers; }
    public long getCompletedDossiers() { return completedDossiers; }
    public void setCompletedDossiers(long completedDossiers) { this.completedDossiers = completedDossiers; }
    public double getPerformanceScore() { return performanceScore; }
    public void setPerformanceScore(double performanceScore) { this.performanceScore = performanceScore; }
}
