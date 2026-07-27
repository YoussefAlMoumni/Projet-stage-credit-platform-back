package com.talan.creditplatform.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "dossier")
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String siren;

    @Column(name = "client_type", nullable = false)
    private String clientType = "corporate";

    @Column(nullable = false)
    private String status = "in_progress";

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assigned_analyst_id", nullable = false)
    @JsonIgnoreProperties({"password"})
    private User assignedAnalyst;

    @ManyToOne
    @JoinColumn(name = "approved_by_id")
    @JsonIgnoreProperties({"password"})
    private User approvedBy;

    @OneToOne(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    private DossierIndividual individual;

    @OneToOne(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    private DossierCorporate corporate;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CreditHistory> creditHistories = new ArrayList<>();

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Loan> loans = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "in_progress";
        }
        if (clientType == null || clientType.isBlank()) {
            clientType = siren == null || siren.isBlank() ? "individual" : "corporate";
        }
        if (corporate != null && ("PENDING".equals(corporate.getTaxRegistrationNumber()) || corporate.getTaxRegistrationNumber() == null)) {
            corporate.setTaxRegistrationNumber(siren == null || siren.isBlank() ? "TAX-" + System.nanoTime() : siren);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSiren() {
        return siren;
    }

    public void setSiren(String siren) {
        this.siren = siren;
        if (siren != null && !siren.isBlank() && (clientType == null || "individual".equals(clientType))) {
            this.clientType = "corporate";
        }
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = normalizeClientType(clientType);
    }

    public String getTypeClient() {
        return clientType;
    }

    public void setTypeClient(String typeClient) {
        setClientType(typeClient);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public User getAssignedAnalyst() {
        return assignedAnalyst;
    }

    public void setAssignedAnalyst(User assignedAnalyst) {
        this.assignedAnalyst = assignedAnalyst;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }

    public DossierIndividual getIndividual() {
        return individual;
    }

    public void setIndividual(DossierIndividual individual) {
        this.individual = individual;
        if (individual != null) {
            individual.setDossier(this);
            this.clientType = "individual";
        }
    }

    public DossierCorporate getCorporate() {
        return corporate;
    }

    public void setCorporate(DossierCorporate corporate) {
        this.corporate = corporate;
        if (corporate != null) {
            corporate.setDossier(this);
            this.clientType = "corporate";
        }
    }

    public List<CreditHistory> getCreditHistories() {
        return creditHistories;
    }

    public void setCreditHistories(List<CreditHistory> creditHistories) {
        this.creditHistories = creditHistories == null ? new ArrayList<>() : creditHistories;
        this.creditHistories.forEach(history -> history.setDossier(this));
    }

    public List<Loan> getLoans() {
        return loans;
    }

    public void setLoans(List<Loan> loans) {
        this.loans = loans == null ? new ArrayList<>() : loans;
        this.loans.forEach(loan -> loan.setDossier(this));
    }

    public String getName() {
        if (corporate != null) {
            return corporate.getCompanyName();
        }
        if (individual != null) {
            return individual.getFirstName() + " " + individual.getLastName();
        }
        return null;
    }

    public void setName(String name) {
        if (corporate == null) {
            setCorporate(new DossierCorporate());
        }
        corporate.setCompanyName(name);
    }

    public String getMontantDemande() {
        if (loans.isEmpty() || loans.get(0).getAmount() == null) {
            return null;
        }
        return loans.get(0).getAmount().toPlainString();
    }

    public void setMontantDemande(String montantDemande) {
        if (montantDemande == null || montantDemande.isBlank()) {
            return;
        }
        Loan loan = loans.isEmpty() ? new Loan() : loans.get(0);
        loan.setAmount(new BigDecimal(montantDemande));
        loan.setDossier(this);
        if (loan.getInterestRate() == null) {
            loan.setInterestRate(BigDecimal.ZERO);
        }
        if (loan.getTermMonths() == null) {
            loan.setTermMonths(0);
        }
        if (loan.getPaymentFrequency() == null) {
            loan.setPaymentFrequency("monthly");
        }
        if (loan.getStatus() == null) {
            loan.setStatus("active");
        }
        if (loans.isEmpty()) {
            loans.add(loan);
        }
    }

    public String getRawData() {
        return null;
    }

    public void setRawData(String rawData) {
        // The normalized schema stores dossier facts in typed tables.
    }

    private String normalizeClientType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.contains("phys") || normalized.contains("individual")) {
            return "individual";
        }
        return "corporate";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dossier dossier = (Dossier) o;
        return Objects.equals(id, dossier.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
