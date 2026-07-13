package com.talan.creditplatform.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "evaluation")
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "dossier_id", nullable = false)
    private Dossier dossier;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ai_model_id", nullable = false)
    private AiModel aiModel;

    @Column(name = "execution_mode", nullable = false)
    private String executionMode;

    @Column(name = "solvency_stage_output", columnDefinition = "TEXT")
    private String solvencyStageOutput;

    @Column(name = "solvency_duration_ms")
    private Integer solvencyDurationMs;

    @Column(name = "history_stage_output", columnDefinition = "TEXT")
    private String historyStageOutput;

    @Column(name = "history_duration_ms")
    private Integer historyDurationMs;

    @Column(name = "guarantees_stage_output", columnDefinition = "TEXT")
    private String guaranteesStageOutput;

    @Column(name = "guarantees_duration_ms")
    private Integer guaranteesDurationMs;

    @Column(name = "compliance_stage_output", columnDefinition = "TEXT")
    private String complianceStageOutput;

    @Column(name = "compliance_duration_ms")
    private Integer complianceDurationMs;

    @Column(name = "supervisor_stage_output", columnDefinition = "TEXT")
    private String supervisorStageOutput;

    @Column(name = "supervisor_duration_ms")
    private Integer supervisorDurationMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Dossier getDossier() { return dossier; }
    public void setDossier(Dossier dossier) { this.dossier = dossier; }
    public AiModel getAiModel() { return aiModel; }
    public void setAiModel(AiModel aiModel) { this.aiModel = aiModel; }
    public String getExecutionMode() { return executionMode; }
    public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }
    public String getMode() { return executionMode; }
    public void setMode(String mode) { this.executionMode = mode; }
    public String getSolvencyStageOutput() { return solvencyStageOutput; }
    public void setSolvencyStageOutput(String solvencyStageOutput) { this.solvencyStageOutput = solvencyStageOutput; }
    public Integer getSolvencyDurationMs() { return solvencyDurationMs; }
    public void setSolvencyDurationMs(Integer solvencyDurationMs) { this.solvencyDurationMs = solvencyDurationMs; }
    public String getHistoryStageOutput() { return historyStageOutput; }
    public void setHistoryStageOutput(String historyStageOutput) { this.historyStageOutput = historyStageOutput; }
    public Integer getHistoryDurationMs() { return historyDurationMs; }
    public void setHistoryDurationMs(Integer historyDurationMs) { this.historyDurationMs = historyDurationMs; }
    public String getGuaranteesStageOutput() { return guaranteesStageOutput; }
    public void setGuaranteesStageOutput(String guaranteesStageOutput) { this.guaranteesStageOutput = guaranteesStageOutput; }
    public Integer getGuaranteesDurationMs() { return guaranteesDurationMs; }
    public void setGuaranteesDurationMs(Integer guaranteesDurationMs) { this.guaranteesDurationMs = guaranteesDurationMs; }
    public String getComplianceStageOutput() { return complianceStageOutput; }
    public void setComplianceStageOutput(String complianceStageOutput) { this.complianceStageOutput = complianceStageOutput; }
    public Integer getComplianceDurationMs() { return complianceDurationMs; }
    public void setComplianceDurationMs(Integer complianceDurationMs) { this.complianceDurationMs = complianceDurationMs; }
    public String getSupervisorStageOutput() { return supervisorStageOutput; }
    public void setSupervisorStageOutput(String supervisorStageOutput) { this.supervisorStageOutput = supervisorStageOutput; }
    public Integer getSupervisorDurationMs() { return supervisorDurationMs; }
    public void setSupervisorDurationMs(Integer supervisorDurationMs) { this.supervisorDurationMs = supervisorDurationMs; }
    public String getFinalReport() { return supervisorStageOutput; }
    public void setFinalReport(String finalReport) { this.supervisorStageOutput = finalReport; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Evaluation that = (Evaluation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
