package com.talan.creditplatform;

import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.entity.Evaluation;
import com.talan.creditplatform.repository.EvaluationRepository;
import com.talan.creditplatform.repository.StageResultRepository;
import com.talan.creditplatform.service.SolvabiliteAgent;
import com.talan.creditplatform.service.HistoriqueAgent;
import com.talan.creditplatform.service.GarantiesAgent;
import com.talan.creditplatform.service.ConformiteAgent;
import com.talan.creditplatform.service.OllamaClient;
import com.talan.creditplatform.service.PipelineOrchestrator;
import com.talan.creditplatform.service.SupervisorAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditPlatformApplicationTests {

    @Mock
    private OllamaClient ollamaClient;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private StageResultRepository stageResultRepository;

    private SolvabiliteAgent solvabiliteAgent;
    private HistoriqueAgent historiqueAgent;
    private GarantiesAgent garantiesAgent;
    private ConformiteAgent conformiteAgent;
    private SupervisorAgent supervisorAgent;
    private PipelineOrchestrator pipelineOrchestrator;

    @BeforeEach
    void setUp() {
        solvabiliteAgent = new SolvabiliteAgent(ollamaClient);
        historiqueAgent = new HistoriqueAgent(ollamaClient);
        garantiesAgent = new GarantiesAgent(ollamaClient);
        conformiteAgent = new ConformiteAgent(ollamaClient);
        supervisorAgent = new SupervisorAgent(ollamaClient);
        
        pipelineOrchestrator = new PipelineOrchestrator(
            solvabiliteAgent, historiqueAgent, garantiesAgent, conformiteAgent, 
            supervisorAgent, evaluationRepository, stageResultRepository
        );
    }

    @Test
    void testAgentPromptBuilding() {
        Dossier dossier = new Dossier();
        dossier.setSiren("12345");
        dossier.setTypeClient("Personne Physique");

        when(ollamaClient.generate(any())).thenReturn("MOCK_RESPONSE");

        String response = solvabiliteAgent.run(dossier, 2048, "0s");
        
        assertEquals("MOCK_RESPONSE", response);
        verify(ollamaClient).generate(argThat(req -> 
            req.getPrompt().contains("12345") && req.getPrompt().contains("Personne Physique")
        ));
    }

    @Test
    void testOrchestratorSequentialExecution() {
        Dossier dossier = new Dossier();
        dossier.setSiren("12345");

        when(ollamaClient.generate(any())).thenReturn("MOCK_OUT");
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(i -> {
            Evaluation e = i.getArgument(0);
            e.setId(1L);
            return e;
        });

        Evaluation result = pipelineOrchestrator.evaluate(dossier, "FAST");

        assertEquals("MOCK_OUT", result.getFinalReport());
        // Verify 4 analyst calls + 1 supervisor call = 5 total calls
        verify(ollamaClient, times(5)).generate(any());
        // Verify stage results were saved 4 times
        verify(stageResultRepository, times(4)).save(any());
    }
}
