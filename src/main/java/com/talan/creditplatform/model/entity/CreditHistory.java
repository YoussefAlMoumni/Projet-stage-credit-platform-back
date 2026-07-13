package com.talan.creditplatform.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "credit_history")
public class CreditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "dossier_id", nullable = false)
    @JsonIgnore
    private Dossier dossier;

    @Column(name = "institution_name", nullable = false)
    private String institutionName;

    @Column(name = "incident_type", nullable = false)
    private String incidentType;

    @Column(name = "amount_in_delinquency")
    private BigDecimal amountInDelinquency;

    @Column(name = "resolution_status", nullable = false)
    private Boolean resolutionStatus;

    @Column(name = "reported_date", nullable = false)
    private LocalDate reportedDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Dossier getDossier() { return dossier; }
    public void setDossier(Dossier dossier) { this.dossier = dossier; }
    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }
    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String incidentType) { this.incidentType = incidentType; }
    public BigDecimal getAmountInDelinquency() { return amountInDelinquency; }
    public void setAmountInDelinquency(BigDecimal amountInDelinquency) { this.amountInDelinquency = amountInDelinquency; }
    public Boolean getResolutionStatus() { return resolutionStatus; }
    public void setResolutionStatus(Boolean resolutionStatus) { this.resolutionStatus = resolutionStatus; }
    public LocalDate getReportedDate() { return reportedDate; }
    public void setReportedDate(LocalDate reportedDate) { this.reportedDate = reportedDate; }
}
