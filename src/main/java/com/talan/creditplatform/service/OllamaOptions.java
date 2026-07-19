package com.talan.creditplatform.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OllamaOptions {

    @JsonProperty("num_ctx")
    private int numCtx;

    @JsonProperty("num_predict")
    private int numPredict;

    @JsonProperty("temperature")
    private Double temperature;

    public OllamaOptions() {}

    public OllamaOptions(int numCtx, int numPredict) {
        this.numCtx = numCtx;
        this.numPredict = numPredict;
    }

    public OllamaOptions(int numCtx, int numPredict, Double temperature) {
        this.numCtx = numCtx;
        this.numPredict = numPredict;
        this.temperature = temperature;
    }

    public int getNumCtx() {
        return numCtx;
    }

    public void setNumCtx(int numCtx) {
        this.numCtx = numCtx;
    }

    public int getNumPredict() {
        return numPredict;
    }

    public void setNumPredict(int numPredict) {
        this.numPredict = numPredict;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
}
