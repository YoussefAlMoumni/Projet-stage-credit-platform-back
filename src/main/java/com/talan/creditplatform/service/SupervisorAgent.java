package com.talan.creditplatform.service;

import com.talan.creditplatform.model.entity.Dossier;

import org.springframework.stereotype.Service;

@Service
public class SupervisorAgent {

    private final OllamaClient ollamaClient;
    private static final String MODEL = "deepseek-r1:14b";
    private static final int NUM_PREDICT = 768;

    public SupervisorAgent(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    public String runSuperviseur(Dossier dossier, String solvabiliteOut, String historiqueOut,
                                 String garantiesOut, String conformiteOut, int numCtx, String keepAlive) {
        
        String prompt = String.format(
                "Directeur d'Engagement (Superviseur):\n" +
                "Consolider les rapports des 4 spécialistes ci-dessous pour formuler la décision d'octroi finale.\n\n" +
                "### Données de base:\n Dossier: %s | Montant: %s\n\n" +
                "### Analyse Solvabilité:\n %s\n\n" +
                "### Analyse Historique:\n %s\n\n" +
                "### Analyse Garanties:\n %s\n\n" +
                "### Contrôle Conformité:\n %s\n\n" +
                "Rédiger un rapport de décision complet, clair et formalisé au format Markdown.",
                dossier.getSiren(), dossier.getMontantDemande(),
                solvabiliteOut, historiqueOut, garantiesOut, conformiteOut
        );

        OllamaOptions options = new OllamaOptions(numCtx, NUM_PREDICT);
        OllamaRequest request = new OllamaRequest(MODEL, prompt, options, keepAlive);
        return ollamaClient.generate(request);
    }
}
