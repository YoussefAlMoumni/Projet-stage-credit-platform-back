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
            "Consolider les rapports des 4 spécialistes pour formuler la décision d'octroi finale.\n" +
            "CONSIGNE STRICTE: Réponse structurée, claire et synthétique en Markdown:\n" +
            "### Décision: [FAVORABLE / DEFAVORABLE / AVEC RESERVE]\n" +
            "- **Solvabilité**: {solvabilite}\n" +
            "- **Historique**: {historique}\n" +
            "- **Garanties**: {garanties}\n" +
            "- **Conformité**: {conformite}\n\n" +
            "### Synthèse Executive\n" +
            "[Maximum 3 phrases synthétiques résumant le motif de la décision].";

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

        prompt += "\n\nCONSIGNE STRICTE (Priorité absolue) : Votre réponse DOIT être structurée, claire et synthétique en Markdown.\n" +
                  "Commencez obligatoirement par '### Décision: [FAVORABLE / DEFAVORABLE / AVEC RESERVE]'.";

        OllamaOptions options = new OllamaOptions(numCtx, NUM_PREDICT, temperature);
        OllamaRequest request = new OllamaRequest(modelName, prompt, options, keepAlive);
        return ollamaClient.generate(request);
    }
}
