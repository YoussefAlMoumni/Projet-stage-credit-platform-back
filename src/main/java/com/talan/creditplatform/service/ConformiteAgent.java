package com.talan.creditplatform.service;

import com.talan.creditplatform.model.entity.Dossier;

import org.springframework.stereotype.Service;

@Service
public class ConformiteAgent {

    private final OllamaClient ollamaClient;
    private static final String MODEL = "deepseek-r1:8b";
    private static final int NUM_PREDICT = 256;

    public ConformiteAgent(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    public String run(Dossier dossier, int numCtx, String keepAlive) {
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
