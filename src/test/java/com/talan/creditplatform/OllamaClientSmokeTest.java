package com.talan.creditplatform;

import com.talan.creditplatform.model.dto.OllamaOptions;
import com.talan.creditplatform.model.dto.OllamaRequest;
import com.talan.creditplatform.service.OllamaClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Scripted manual smoke-test for OllamaClient.
 * Run this test manually in Eclipse to verify connectivity with the local Ollama instance.
 * Remove @Disabled to execute. Ensure Ollama is running and deepseek-r1:8b is pulled.
 */
@SpringBootTest
@Disabled("Manual Smoke Test")
public class OllamaClientSmokeTest {

    @Autowired
    private OllamaClient ollamaClient;

    @Test
    public void testOllamaConnection() {
        System.out.println("Starting OllamaClient Smoke Test...");
        
        OllamaOptions options = new OllamaOptions(2048, 128);
        OllamaRequest request = new OllamaRequest(
            "deepseek-r1:8b",
            "Bonjour, ceci est un test de connectivité. Réponds par un simple 'Bonjour!'.",
            options,
            "0s"
        );

        try {
            System.out.println("Sending request to Ollama...");
            String response = ollamaClient.generate(request);
            System.out.println("\n--- OLLAMA RESPONSE ---");
            System.out.println(response);
            System.out.println("-----------------------\n");
            System.out.println("Smoke test successful!");
        } catch (Exception e) {
            System.err.println("Smoke test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
