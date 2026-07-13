package com.talan.creditplatform.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "dossier_corporate")
public class DossierCorporate {

    @Id
    @Column(name = "dossier_id")
    private Long dossierId;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "dossier_id")
    @JsonIgnore
    private Dossier dossier;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "tax_registration_number", unique = true, nullable = false)
    private String taxRegistrationNumber = "PENDING";

    @Column(name = "legal_form", nullable = false)
    private String legalForm = "SARL";

    @Column(name = "registered_office_address", columnDefinition = "TEXT")
    private String registeredOfficeAddress;

    private Integer fiscalYear;
    private BigDecimal turnover;
    private BigDecimal netIncome;
    private BigDecimal totalDebt;
    private BigDecimal equity;
    private Double liquidityRatio;

    public Long getDossierId() { return dossierId; }
    public void setDossierId(Long dossierId) { this.dossierId = dossierId; }
    public Dossier getDossier() { return dossier; }
    public void setDossier(Dossier dossier) { this.dossier = dossier; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getTaxRegistrationNumber() { return taxRegistrationNumber; }
    public void setTaxRegistrationNumber(String taxRegistrationNumber) { this.taxRegistrationNumber = taxRegistrationNumber; }
    public String getLegalForm() { return legalForm; }
    public void setLegalForm(String legalForm) { this.legalForm = legalForm; }
    public String getRegisteredOfficeAddress() { return registeredOfficeAddress; }
    public void setRegisteredOfficeAddress(String registeredOfficeAddress) { this.registeredOfficeAddress = registeredOfficeAddress; }
    public Integer getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(Integer fiscalYear) { this.fiscalYear = fiscalYear; }
    public BigDecimal getTurnover() { return turnover; }
    public void setTurnover(BigDecimal turnover) { this.turnover = turnover; }
    public BigDecimal getNetIncome() { return netIncome; }
    public void setNetIncome(BigDecimal netIncome) { this.netIncome = netIncome; }
    public BigDecimal getTotalDebt() { return totalDebt; }
    public void setTotalDebt(BigDecimal totalDebt) { this.totalDebt = totalDebt; }
    public BigDecimal getEquity() { return equity; }
    public void setEquity(BigDecimal equity) { this.equity = equity; }
    public Double getLiquidityRatio() { return liquidityRatio; }
    public void setLiquidityRatio(Double liquidityRatio) { this.liquidityRatio = liquidityRatio; }
}
