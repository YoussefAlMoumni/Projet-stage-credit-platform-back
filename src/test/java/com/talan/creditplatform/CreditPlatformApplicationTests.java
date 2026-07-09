package com.talan.creditplatform;

import com.talan.creditplatform.model.entity.Dossier;
import com.talan.creditplatform.model.entity.Evaluation;
import com.talan.creditplatform.model.repository.EvaluationRepository;
import com.talan.creditplatform.model.repository.StageResultRepository;
import com.talan.creditplatform.model.service.AnalystAgent;
import com.talan.creditplatform.model.service.OllamaClient;
import com.talan.creditplatform.model.service.PipelineOrchestrator;
import com.talan.creditplatform.model.service.SupervisorAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditPlatformApplicationTests {

    @Mock
    private OllamaClient ollamaClient;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private StageResultRepository stageResultRepository;

    private AnalystAgent analystAgent;
    private SupervisorAgent supervisorAgent;
    private PipelineOrchestrator pipelineOrchestrator;

    @BeforeEach
    void setUp() {
        analystAgent = new AnalystAgent(ollamaClient);
        supervisorAgent = new SupervisorAgent(ollamaClient);
        pipelineOrchestrator = new PipelineOrchestrator(analystAgent, supervisorAgent, evaluationRepository, stageResultRepository);
    }

    @Test
    void testAgentPromptBuilding() {
        Dossier dossier = new Dossier();
        dossier.setSiren("12345");
        dossier.setTypeClient("Personne Physique");

        when(ollamaClient.generate(any())).thenReturn("MOCK_RESPONSE");

        String response = analystAgent.runSolvabilite(dossier, 2048, "0s");
        
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
