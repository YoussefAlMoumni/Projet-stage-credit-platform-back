package com.talan.creditplatform.model.service;

import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.dto.OllamaOptions;
import com.talan.creditplatform.model.dto.OllamaRequest;
import org.springframework.stereotype.Service;

@Service
public class AnalystAgent {

    private final OllamaClient ollamaClient;
    private static final String MODEL = "deepseek-r1:8b";
    private static final int NUM_PREDICT = 256;

    public AnalystAgent(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    public String runSolvabilite(Dossier dossier, int numCtx, String keepAlive) {
        String prompt = String.format("Analyste Solvabilité:\n" +
                "Évaluer la capacité brute de remboursement pour le dossier %s " +
                "concernant un client de type '%s'. Extraire et analyser le ratio d'endettement.\n" +
                "Générer une recommandation structurée.", dossier.getSiren(), dossier.getTypeClient());
        
        return execute(prompt, numCtx, keepAlive);
    }

    public String runHistorique(Dossier dossier, int numCtx, String keepAlive) {
        String prompt = String.format("Analyste Historique:\n" +
                "Vérifier les incidents de paiement historiques et l'état des engagements en cours " +
                "pour le dossier %s.\n" +
                "Fournir un score de risque sur les antécédents.", dossier.getSiren());
        
        return execute(prompt, numCtx, keepAlive);
    }

    public String runGaranties(Dossier dossier, int numCtx, String keepAlive) {
        String prompt = String.format("Évaluateur de Garanties:\n" +
                "Évaluer la liquidité, la valeur estimée et le ratio de couverture du collatéral proposé " +
                "par rapport au montant demandé de %s.\n" +
                "Rédiger un avis sur la couverture du risque.", dossier.getMontantDemande());
        
        return execute(prompt, numCtx, keepAlive);
    }

    public String runConformite(Dossier dossier, int numCtx, String keepAlive) {
        String prompt = String.format("Officier de Conformité:\n" +
                "Exécuter les vérifications réglementaires d'usage (KYC/AML) sur le dossier %s.\n" +
                "CONTRAINTE DE FORMAT STRICTE: Votre réponse DOIT commencer explicitement par l'un des drapeaux suivants:\n" +
                "soit 'STATUT: APPROUVE' soit 'STATUT: REFUS'. Justifier ensuite votre choix.", dossier.getSiren());
        
        return execute(prompt, numCtx, keepAlive);
    }

    private String execute(String prompt, int numCtx, String keepAlive) {
        OllamaOptions options = new OllamaOptions(numCtx, NUM_PREDICT);
        OllamaRequest request = new OllamaRequest(MODEL, prompt, options, keepAlive);
        return ollamaClient.generate(request);
    }
}
