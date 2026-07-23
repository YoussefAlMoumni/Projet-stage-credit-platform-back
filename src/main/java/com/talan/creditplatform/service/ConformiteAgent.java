package com.talan.creditplatform.service;

import com.talan.creditplatform.model.entity.AiModel;
import com.talan.creditplatform.model.entity.AiPrompt;
import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.repository.AiModelRepository;
import com.talan.creditplatform.repository.AiPromptRepository;

import org.springframework.stereotype.Service;

@Service
public class ConformiteAgent {

    private final OllamaClient ollamaClient;
    private final AiModelRepository aiModelRepository;
    private final AiPromptRepository aiPromptRepository;
    private static final String STAGE_NAME = "compliance";
    private static final String FALLBACK_MODEL = "deepseek-r1:8b";
    private static final int NUM_PREDICT = 256;

    private static final String DEFAULT_PROMPT = "Officier de Conformité:\n" +
            "Vérifications réglementaires (KYC/AML) pour le dossier {siren}.\n" +
            "CONSIGNE STRICTE: Votre réponse DOIT commencer par '- Décision: [FAVORABLE / DEFAVORABLE]' suivi de 2 puces synthétiques de justification.";

    public ConformiteAgent(OllamaClient ollamaClient, AiModelRepository aiModelRepository,
                           AiPromptRepository aiPromptRepository) {
        this.ollamaClient = ollamaClient;
        this.aiModelRepository = aiModelRepository;
        this.aiPromptRepository = aiPromptRepository;
    }

    public String run(Dossier dossier, int numCtx, String keepAlive) {
        AiModel model = aiModelRepository.findFirstByStageNameAndActiveTrue(STAGE_NAME).orElse(null);

        String modelName = FALLBACK_MODEL;
        Double temperature = null;
        String promptTemplate = DEFAULT_PROMPT;

        if (model != null) {
            modelName = model.getModelName();
            temperature = model.getTemperature();
            promptTemplate = aiPromptRepository.findFirstByAiModelIdOrderByUpdatedAtDesc(model.getId())
                    .map(AiPrompt::getPromptText)
                    .orElse(DEFAULT_PROMPT);
        }

        String prompt = promptTemplate
                .replace("{siren}", dossier.getSiren() != null ? dossier.getSiren() : "N/A");

        prompt += "\n\nCONSIGNE STRICTE (Priorité absolue) : Votre réponse DOIT être brève (3 à 4 puces maximum) et inclure '- Décision: [FAVORABLE / DEFAVORABLE]'.";

        return execute(prompt, modelName, numCtx, keepAlive, temperature);
    }

    private String execute(String prompt, String modelName, int numCtx, String keepAlive, Double temperature) {
        OllamaOptions options = new OllamaOptions(numCtx, NUM_PREDICT, temperature);
        OllamaRequest request = new OllamaRequest(modelName, prompt, options, keepAlive);
        return ollamaClient.generate(request);
    }
}
