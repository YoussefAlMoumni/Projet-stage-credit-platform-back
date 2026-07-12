package com.talan.creditplatform.service;

import com.talan.creditplatform.model.entity.Dossier;

import org.springframework.stereotype.Service;

@Service
public class GarantiesAgent {

    private final OllamaClient ollamaClient;
    private static final String MODEL = "deepseek-r1:8b";
    private static final int NUM_PREDICT = 256;

    public GarantiesAgent(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    public String run(Dossier dossier, int numCtx, String keepAlive) {
        String prompt = String.format("Évaluateur de Garanties:\n" +
                "Évaluer la liquidité, la valeur estimée et le ratio de couverture du collatéral proposé " +
                "par rapport au montant demandé de %s.\n" +
                "Rédiger un avis sur la couverture du risque.", dossier.getMontantDemande());
        
        return execute(prompt, numCtx, keepAlive);
    }

    private String execute(String prompt, int numCtx, String keepAlive) {
        OllamaOptions options = new OllamaOptions(numCtx, NUM_PREDICT);
        OllamaRequest request = new OllamaRequest(MODEL, prompt, options, keepAlive);
        return ollamaClient.generate(request);
    }
}
