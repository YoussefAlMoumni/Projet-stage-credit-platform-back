package com.talan.creditplatform.service;

import com.talan.creditplatform.model.entity.Dossier;

import org.springframework.stereotype.Service;

@Service
public class SolvabiliteAgent {

    private final OllamaClient ollamaClient;
    private static final String MODEL = "deepseek-r1:8b";
    private static final int NUM_PREDICT = 256;

    public SolvabiliteAgent(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    public String run(Dossier dossier, int numCtx, String keepAlive) {
        String prompt = String.format("Analyste Solvabilité:\n" +
                "Évaluer la capacité brute de remboursement pour le dossier %s " +
                "concernant un client de type '%s'. Extraire et analyser le ratio d'endettement.\n" +
                "Générer une recommandation structurée.", dossier.getSiren(), dossier.getTypeClient());
        
        return execute(prompt, numCtx, keepAlive);
    }

    private String execute(String prompt, int numCtx, String keepAlive) {
        OllamaOptions options = new OllamaOptions(numCtx, NUM_PREDICT);
        OllamaRequest request = new OllamaRequest(MODEL, prompt, options, keepAlive);
        return ollamaClient.generate(request);
    }
}
