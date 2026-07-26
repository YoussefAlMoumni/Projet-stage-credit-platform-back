package com.talan.creditplatform.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Options sent to Ollama's /api/generate endpoint.
 *
 * num_predict is intentionally omitted (null) whenever unlimited generation is
 * desired. Ollama treats the absence of the key as "no limit", which is
 * equivalent to passing -1 explicitly. We use @JsonInclude(NON_NULL) so that
 * Jackson never serialises a null field into the JSON payload.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OllamaOptions {

    /** Context-window size in tokens. Should be large enough to hold the full
     *  prompt + full <think> reasoning block + full answer for the model in use. */
    @JsonProperty("num_ctx")
    private Integer numCtx;

    /**
     * Maximum tokens to predict. Set to -1 (Ollama "no limit") when you want
     * unlimited output. Leave null to omit the key entirely (same effect).
     * NEVER set a small positive cap here — that is the primary cause of
     * truncated responses from reasoning models.
     */
    @JsonProperty("num_predict")
    private Integer numPredict;

    @JsonProperty("temperature")
    private Double temperature;

    public OllamaOptions() {}

    /**
     * Unlimited generation: numPredict is set to -1 (Ollama "no limit").
     * numCtx should be sized generously to fit prompt + thinking + answer.
     */
    public OllamaOptions(int numCtx, Double temperature) {
        this.numCtx = numCtx;
        this.numPredict = -1; // Unlimited prediction
        this.temperature = temperature;
    }

    /**
     * Full-control constructor. Pass numPredict = -1 for unlimited generation.
     */
    public OllamaOptions(int numCtx, int numPredict, Double temperature) {
        this.numCtx = numCtx;
        this.numPredict = numPredict;
        this.temperature = temperature;
    }

    public Integer getNumCtx() {
        return numCtx;
    }

    public void setNumCtx(Integer numCtx) {
        this.numCtx = numCtx;
    }

    public Integer getNumPredict() {
        return numPredict;
    }

    public void setNumPredict(Integer numPredict) {
        this.numPredict = numPredict;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
}
