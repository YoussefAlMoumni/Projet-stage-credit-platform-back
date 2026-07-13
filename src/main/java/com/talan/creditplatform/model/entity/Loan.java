package com.talan.creditplatform.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loan")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "dossier_id", nullable = false)
    @JsonIgnore
    private Dossier dossier;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "interest_rate", nullable = false)
    private BigDecimal interestRate;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Column(name = "payment_frequency", nullable = false)
    private String paymentFrequency;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(nullable = false)
    private String status;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Collateral> collaterals = new ArrayList<>();

    @PrePersist
    @PreUpdate
    protected void applyDefaults() {
        if (interestRate == null) {
            interestRate = BigDecimal.ZERO;
        }
        if (termMonths == null) {
            termMonths = 0;
        }
        if (paymentFrequency == null || paymentFrequency.isBlank()) {
            paymentFrequency = "monthly";
        }
        if (status == null || status.isBlank()) {
            status = "active";
        }
        collaterals.forEach(collateral -> collateral.setLoan(this));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Dossier getDossier() { return dossier; }
    public void setDossier(Dossier dossier) { this.dossier = dossier; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public Integer getTermMonths() { return termMonths; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }
    public String getPaymentFrequency() { return paymentFrequency; }
    public void setPaymentFrequency(String paymentFrequency) { this.paymentFrequency = paymentFrequency; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<Collateral> getCollaterals() { return collaterals; }
    public void setCollaterals(List<Collateral> collaterals) {
        this.collaterals = collaterals == null ? new ArrayList<>() : collaterals;
        this.collaterals.forEach(collateral -> collateral.setLoan(this));
    }
}
