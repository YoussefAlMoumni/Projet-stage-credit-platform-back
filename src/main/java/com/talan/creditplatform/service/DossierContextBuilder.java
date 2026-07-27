package com.talan.creditplatform.service;

import com.talan.creditplatform.model.entity.Collateral;
import com.talan.creditplatform.model.entity.CreditHistory;
import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.entity.DossierCorporate;
import com.talan.creditplatform.model.entity.DossierIndividual;
import com.talan.creditplatform.model.entity.Loan;

import java.math.BigDecimal;
import java.util.List;

/**
 * Builds a human-readable context block from a Dossier's structured DB fields,
 * to be injected verbatim into AI agent prompts so the model reasons over real data
 * instead of hallucinating values.
 */
public class DossierContextBuilder {

    private DossierContextBuilder() {}

    /**
     * Returns a plain-text summary of all known facts about the dossier.
     */
    public static String build(Dossier d) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DONNÉES DOSSIER (base de données) ===\n");
        sb.append("SIREN          : ").append(nvl(d.getSiren())).append("\n");
        sb.append("Type client    : ").append(nvl(d.getClientType())).append("\n");
        sb.append("Statut dossier : ").append(nvl(d.getStatus())).append("\n");

        // --- Individual client ---
        DossierIndividual ind = d.getIndividual();
        if (ind != null) {
            sb.append("\n-- Personne physique --\n");
            sb.append("Nom            : ").append(nvl(ind.getFirstName())).append(" ").append(nvl(ind.getLastName())).append("\n");
            sb.append("Date naissance : ").append(ind.getDateOfBirth() != null ? ind.getDateOfBirth() : "N/A").append("\n");
            sb.append("Profession     : ").append(nvl(ind.getProfession())).append("\n");
            sb.append("Revenu mensuel : ").append(formatAmount(ind.getMonthlyIncome())).append("\n");
        }

        // --- Corporate client ---
        DossierCorporate corp = d.getCorporate();
        if (corp != null) {
            sb.append("\n-- Personne morale --\n");
            sb.append("Raison sociale : ").append(nvl(corp.getCompanyName())).append("\n");
            sb.append("Forme juridique: ").append(nvl(corp.getLegalForm())).append("\n");
            sb.append("N° fiscal      : ").append(nvl(corp.getTaxRegistrationNumber())).append("\n");
            sb.append("Adresse siège  : ").append(nvl(corp.getRegisteredOfficeAddress())).append("\n");
            sb.append("Exercice fiscal: ").append(corp.getFiscalYear() != null ? corp.getFiscalYear() : "N/A").append("\n");
            sb.append("Chiffre affaires: ").append(formatAmount(corp.getTurnover())).append("\n");
            sb.append("Résultat net   : ").append(formatAmount(corp.getNetIncome())).append("\n");
            sb.append("Dettes totales : ").append(formatAmount(corp.getTotalDebt())).append("\n");
            sb.append("Capitaux propres: ").append(formatAmount(corp.getEquity())).append("\n");
            sb.append("Ratio liquidité: ").append(corp.getLiquidityRatio() != null ? corp.getLiquidityRatio() : "N/A").append("\n");
        }

        // --- Loans ---
        List<Loan> loans = d.getLoans();
        if (!loans.isEmpty()) {
            sb.append("\n-- Crédits demandés --\n");
            for (int i = 0; i < loans.size(); i++) {
                Loan l = loans.get(i);
                sb.append("Crédit ").append(i + 1).append(":\n");
                sb.append("  Montant       : ").append(formatAmount(l.getAmount())).append("\n");
                sb.append("  Taux intérêt  : ").append(l.getInterestRate() != null ? l.getInterestRate() + "%" : "N/A").append("\n");
                sb.append("  Durée         : ").append(l.getTermMonths() != null ? l.getTermMonths() + " mois" : "N/A").append("\n");
                sb.append("  Fréquence paie: ").append(nvl(l.getPaymentFrequency())).append("\n");
                sb.append("  Statut        : ").append(nvl(l.getStatus())).append("\n");

                // --- Collaterals ---
                List<Collateral> collaterals = l.getCollaterals();
                if (!collaterals.isEmpty()) {
                    sb.append("  Garanties     :\n");
                    for (Collateral c : collaterals) {
                        sb.append("    - Type: ").append(nvl(c.getType()))
                          .append(", Valeur estimée: ").append(formatAmount(c.getEstimatedValue()))
                          .append(", Statut: ").append(nvl(c.getStatus()))
                          .append(", Desc: ").append(nvl(c.getDescription()))
                          .append("\n");
                    }
                } else {
                    sb.append("  Garanties     : Aucune\n");
                }
            }
        }

        // --- Credit history ---
        List<CreditHistory> histories = d.getCreditHistories();
        if (!histories.isEmpty()) {
            sb.append("\n-- Historique de crédit (").append(histories.size()).append(" incident(s)) --\n");
            for (CreditHistory h : histories) {
                sb.append("  - [").append(h.getReportedDate()).append("] ")
                  .append(nvl(h.getInstitutionName())).append(" | ")
                  .append(nvl(h.getIncidentType())).append(" | Montant: ")
                  .append(formatAmount(h.getAmountInDelinquency())).append(" | Résolu: ")
                  .append(h.getResolutionStatus() != null ? (h.getResolutionStatus() ? "Oui" : "Non") : "N/A")
                  .append("\n");
            }
        } else {
            sb.append("\n-- Historique de crédit : Aucun incident enregistré --\n");
        }

        sb.append("==========================================\n");
        return sb.toString();
    }

    private static String nvl(String s) {
        return (s != null && !s.isBlank()) ? s : "N/A";
    }

    private static String formatAmount(BigDecimal amount) {
        return amount != null ? amount.toPlainString() + " MAD" : "N/A";
    }
}
