package com.talan.creditplatform.service;

import com.talan.creditplatform.model.entity.AiModel;
import com.talan.creditplatform.model.entity.AiPrompt;
import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.repository.AiModelRepository;
import com.talan.creditplatform.repository.AiPromptRepository;

import org.springframework.stereotype.Service;

@Service
public class SupervisorAgent {

    private final OllamaClient ollamaClient;
    private final AiModelRepository aiModelRepository;
    private final AiPromptRepository aiPromptRepository;
    private static final String STAGE_NAME = "supervisor";
    private static final String FALLBACK_MODEL = "deepseek-r1:14b";
    private static final int NUM_PREDICT = 768;

    private static final String DEFAULT_PROMPT = "Directeur d'Engagement (Superviseur):\n" +
            "Consolider les rapports des 4 spécialistes ci-dessous pour formuler la décision d'octroi finale.\n\n" +
            "### Données de base:\n Dossier: {siren} | Montant: {montantDemande}\n\n" +
            "### Analyse Solvabilité:\n {solvabilite}\n\n" +
            "### Analyse Historique:\n {historique}\n\n" +
            "### Analyse Garanties:\n {garanties}\n\n" +
            "### Contrôle Conformité:\n {conformite}\n\n" +
            "Rédiger un rapport de décision complet, clair et formalisé au format Markdown.";

    public SupervisorAgent(OllamaClient ollamaClient, AiModelRepository aiModelRepository,
                           AiPromptRepository aiPromptRepository) {
        this.ollamaClient = ollamaClient;
        this.aiModelRepository = aiModelRepository;
        this.aiPromptRepository = aiPromptRepository;
    }

    public String runSuperviseur(Dossier dossier, String solvabiliteOut, String historiqueOut,
                                 String garantiesOut, String conformiteOut, int numCtx, String keepAlive) {

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
                .replace("{siren}", dossier.getSiren() != null ? dossier.getSiren() : "N/A")
                .replace("{montantDemande}", dossier.getMontantDemande() != null ? dossier.getMontantDemande() : "N/A")
                .replace("{solvabilite}", solvabiliteOut)
                .replace("{historique}", historiqueOut)
                .replace("{garanties}", garantiesOut)
                .replace("{conformite}", conformiteOut);

        OllamaOptions options = new OllamaOptions(numCtx, NUM_PREDICT, temperature);
        OllamaRequest request = new OllamaRequest(modelName, prompt, options, keepAlive);
        return ollamaClient.generate(request);
    }
}
