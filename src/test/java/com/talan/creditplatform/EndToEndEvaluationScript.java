package com.talan.creditplatform;

import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.entity.Evaluation;
import com.talan.creditplatform.repository.DossierRepository;
import com.talan.creditplatform.repository.UserRepository;
import com.talan.creditplatform.service.PipelineOrchestrator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Scripted end-to-end dossier evaluation.
 * Run this test manually in Eclipse to verify the full pipeline against local Ollama.
 * Remove @Disabled to execute. Ensure Ollama is running and models are pulled.
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled("Manual E2E Test")
public class EndToEndEvaluationScript {

    @Autowired
    private PipelineOrchestrator pipelineOrchestrator;

    @Autowired
    private DossierRepository dossierRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void runFullPipeline() {
        System.out.println("Starting End-to-End Evaluation Script...");
        
        // 1. Create a dummy dossier
        Dossier dossier = new Dossier();
        dossier.setSiren("123456789");
        dossier.setName("Tech Corp");
        dossier.setTypeClient("Personne Morale");
        dossier.setMontantDemande("500000.0");
        dossier.setRawData("{\"revenue\": 1000000, \"debt\": 200000, \"collateral\": \"Building\"}");
        userRepository.findByUsername("analyst").ifPresent(dossier::setAssignedAnalyst);
        
        dossier = dossierRepository.save(dossier);
        System.out.println("Saved dummy dossier with SIREN: " + dossier.getSiren());

        // 2. Run the pipeline in FAST mode
        System.out.println("Executing PipelineOrchestrator...");
        Evaluation evaluation = pipelineOrchestrator.evaluate(dossier, "FAST");

        // 3. Print the final report
        System.out.println("\n================ FINAL REPORT ================\n");
        System.out.println(evaluation.getFinalReport());
        System.out.println("\n==============================================\n");
    }
}
