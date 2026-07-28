package com.talan.creditplatform.service;

import com.talan.creditplatform.model.entity.AiModel;
import com.talan.creditplatform.model.entity.AiPrompt;
import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.repository.AiModelRepository;
import com.talan.creditplatform.repository.AiPromptRepository;

import org.springframework.stereotype.Service;

@Service
public class HistoriqueAgent {

    private final OllamaClient ollamaClient;
    private final AiModelRepository aiModelRepository;
    private final AiPromptRepository aiPromptRepository;
    private static final String STAGE_NAME = "history";
    private static final String FALLBACK_MODEL = "deepseek-r1:8b";

    private static final String DEFAULT_PROMPT = "Analyste Historique:\n" +
            "Vérifier les incidents de paiement et antécédents bancaires pour le dossier {siren}.\n" +
            "Nombre d'incidents: {incidentCount}\n\n" +
            "CONSIGNE STRICTE DE FORMAT OBLIGATOIRE:\n" +
            "DECISION: FAVORABLE / INFAVORABLE / INDETERMINEE\n\n" +
            "RAISONNEMENT:\n" +
            "- Analyse des antécédents bancaires et des incidents récents.\n" +
            "- Explication synthétique et justification du niveau de risque historique.";

    public HistoriqueAgent(OllamaClient ollamaClient, AiModelRepository aiModelRepository,
                           AiPromptRepository aiPromptRepository) {
        this.ollamaClient = ollamaClient;
        this.aiModelRepository = aiModelRepository;
        this.aiPromptRepository = aiPromptRepository;
    }

    /**
     * @param dossierContext Pre-built context string from {@link DossierContextBuilder}.
     *                       Must be built inside a Hibernate session (i.e. before async dispatch)
     *                       to avoid LazyInitializationException / JdbcValuesSourceProcessingState errors.
     */
    public String run(Dossier dossier, String dossierContext, int numCtx, String keepAlive) {
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

        String prompt = dossierContext + "\n" + promptTemplate
                .replace("{siren}", dossier.getSiren() != null ? dossier.getSiren() : "N/A")
                .replace("{incidentCount}", String.valueOf(dossier.getCreditHistories().size()));

        prompt += "\n\nCONSIGNE STRICTE (Priorité absolue) : Votre réponse DOIT obligatoirement commencer par 'DECISION: [FAVORABLE / INFAVORABLE / INDETERMINEE]', suivi de 'RAISONNEMENT:' contenant l'analyse des données pertinentes et l'explication du raisonnement.";

        return execute(prompt, modelName, numCtx, keepAlive, temperature);
    }

    private String execute(String prompt, String modelName, int numCtx, String keepAlive, Double temperature) {
        OllamaOptions options = new OllamaOptions(numCtx, temperature);
        OllamaRequest request = new OllamaRequest(modelName, prompt, options, keepAlive);
        return ollamaClient.generate(request);
    }
}
