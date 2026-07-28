package com.talan.creditplatform.service;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import com.talan.creditplatform.exception.AppException;
import com.talan.creditplatform.exception.ErrorCode;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import org.springframework.http.client.JdkClientHttpRequestFactory;

@Service
public class OllamaClient {

    private static final Logger logger = LoggerFactory.getLogger(OllamaClient.class);
    private final RestClient restClient;

    public OllamaClient(@Value("${ollama.api.url}") String ollamaApiUrl) {
        // Use JdkClientHttpRequestFactory instead of the default HttpURLConnection-based
        // factory. The Java 11 HttpClient underlying JdkClientHttpRequestFactory
        // correctly propagates Thread.interrupt(), which causes the HTTP request to
        // immediately abort when CompletableFuture.cancel(true) is called. The default
        // factory ignores interrupts, causing Ollama to keep generating even after Stop.
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(java.time.Duration.ofMinutes(5));

        this.restClient = RestClient.builder()
                .baseUrl(ollamaApiUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Retry(name = "ollamaApi")
    @CircuitBreaker(name = "ollamaApi")
    public String generate(OllamaRequest request) {
        logger.debug("Sending Ollama request: model={}, prompt={}...", request.getModel(), trimPrompt(request.getPrompt(), 300));
        OllamaResponse response;
        try {
            response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(OllamaResponse.class);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            logger.warn("Ollama connection failed. Attempting to start local Ollama process...");
            startOllamaProcess();
            throw e; // Let @Retry try again after Ollama has started
        }

        logger.debug("Received Ollama payload: {}", response);
        if (response != null) {
            // -----------------------------------------------------------------------
            // SAFETY NET: detect truncated responses (done_reason = "length").
            // This can still occur if the context window fills up before the model
            // finishes its answer. We return a clearly-flagged partial result instead
            // of letting null/empty text crash the calling code.
            // -----------------------------------------------------------------------
            if (response.isDone() && response.getOther() != null) {
                Object doneReason = response.getOther().get("done_reason");
                if ("length".equals(doneReason)) {
                    logger.warn("Ollama response truncated (done_reason=length). Model: {}, prompt excerpt: {}",
                            request.getModel(), trimPrompt(request.getPrompt(), 120));

                    // Try to return whatever partial text was produced rather than failing hard.
                    String partial = extractText(response);
                    if (partial != null && !partial.isBlank()) {
                        logger.warn("Returning partial (truncated) response. Consider increasing num_ctx.");
                        return "[PARTIAL RESPONSE — TRUNCATED BY CONTEXT LIMIT]\n" + cleanOutput(partial);
                    }

                    // No usable text at all — return INCOMPLETE sentinel instead of crashing.
                    logger.error("Ollama returned done_reason=length with no usable text field. " +
                            "Returning INCOMPLETE sentinel to prevent NPE in orchestrator.");
                    return "[INCOMPLETE — Ollama response was truncated before producing any usable text. " +
                            "Increase num_ctx or shorten the prompt.]";
                }
            }

            // Primary: response.response
            String rawResponse = response.getResponse();
            if (rawResponse != null && !rawResponse.trim().isEmpty()) {
                return cleanOutput(rawResponse);
            }

            // Fallbacks: inspect other captured fields for common keys
            try {
                String fallback = extractText(response);
                if (fallback != null && !fallback.isBlank()) {
                    return cleanOutput(fallback);
                }
            } catch (Exception e) {
                logger.warn("Error while attempting to extract fallback text from Ollama payload", e);
            }

            logger.warn("Ollama payload did not contain a usable text field; raw payload: {}", response.getOther());
        }

        throw new AppException(ErrorCode.CP_ERR_5000, "Empty or malformed response from Ollama API");
    }

    /**
     * Attempts to extract a usable text string from the "other" map of an
     * OllamaResponse, covering several common alternate response structures.
     *
     * @return the extracted text, or null if nothing usable was found
     */
    private String extractText(OllamaResponse response) {
        Map<String, Object> other = response.getOther();
        if (other == null) {
            return null;
        }

        // 1) outputs: [{"content":"..."}, ...]
        Object outputs = other.get("outputs");
        if (outputs instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) outputs;
            if (!list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) first;
                    Object content = m.get("content");
                    if (content instanceof String && !((String) content).isBlank()) {
                        return (String) content;
                    }
                    Object text = m.get("text");
                    if (text instanceof String && !((String) text).isBlank()) {
                        return (String) text;
                    }
                }
            }
        }

        // 2) choices: [{"text":"..."}, ...]
        Object choices = other.get("choices");
        if (choices instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) choices;
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                Map<?, ?> m = (Map<?, ?>) list.get(0);
                Object text = m.get("text");
                if (text instanceof String && !((String) text).isBlank()) {
                    return (String) text;
                }
            }
        }

        // 3) text / output / result / content top-level keys
        for (String key : java.util.Arrays.asList("text", "output", "result", "content")) {
            Object val = other.get(key);
            if (val instanceof String && !((String) val).isBlank()) {
                return (String) val;
            }
        }

        return null;
    }

    private String trimPrompt(String prompt, int maxLength) {
        if (prompt == null) {
            return "";
        }
        String trimmed = prompt.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed.replaceAll("\\s+", " ");
        }
        return trimmed.substring(0, maxLength).replaceAll("\\s+", " ") + "...";
    }

    private String cleanOutput(String text) {
        if (text == null || text.trim().isEmpty()) return "No output generated by the AI model.";
        // Strip <think> reasoning blocks produced by models like deepseek-r1
        String cleaned = text.replaceAll("(?s)<think>.*?</think>", "").trim();
        return cleaned.isEmpty() ? text.trim() : cleaned;
    }

    private static volatile long lastOllamaStartAttempt = 0;

    private synchronized void startOllamaProcess() {
        // Prevent launching a dozen instances if parallel agents all fail at once.
        // Wait at least 15 seconds before trying to start the process again.
        if (System.currentTimeMillis() - lastOllamaStartAttempt < 15000) {
            return;
        }
        lastOllamaStartAttempt = System.currentTimeMillis();

        try {
            logger.info("Attempting to start Ollama automatically via 'ollama serve'...");
            
            // On Windows, running `ollama serve` in a background process
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "/b", "ollama", "serve");
            pb.redirectErrorStream(true);
            pb.start();
            
            // Give Ollama a few seconds to initialize its HTTP server before the next retry
            Thread.sleep(4000);
            logger.info("Ollama start command issued. Retrying connection...");
        } catch (Exception ex) {
            logger.error("Failed to start Ollama process automatically", ex);
        }
    }
}
