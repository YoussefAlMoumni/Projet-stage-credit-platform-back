package com.talan.creditplatform.service;

import com.talan.creditplatform.model.entity.Dossier;

import org.springframework.stereotype.Service;

@Service
public class HistoriqueAgent {

    private final OllamaClient ollamaClient;
    private static final String MODEL = "deepseek-r1:8b";
    private static final int NUM_PREDICT = 256;

    public HistoriqueAgent(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    public String run(Dossier dossier, int numCtx, String keepAlive) {
        String prompt = String.format("Analyste Historique:\n" +
                "Vérifier les incidents de paiement historiques et l'état des engagements en cours " +
                "pour le dossier %s.\n" +
                "Fournir un score de risque sur les antécédents.", dossier.getSiren());
        
        return execute(prompt, numCtx, keepAlive);
    }

    private String execute(String prompt, int numCtx, String keepAlive) {
        OllamaOptions options = new OllamaOptions(numCtx, NUM_PREDICT);
        OllamaRequest request = new OllamaRequest(MODEL, prompt, options, keepAlive);
        return ollamaClient.generate(request);
    }
}
