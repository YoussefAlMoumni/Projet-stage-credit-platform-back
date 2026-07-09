package com.talan.creditplatform.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OllamaOptions {

    @JsonProperty("num_ctx")
    private int numCtx;

    @JsonProperty("num_predict")
    private int numPredict;

    public OllamaOptions() {}

    public OllamaOptions(int numCtx, int numPredict) {
        this.numCtx = numCtx;
        this.numPredict = numPredict;
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
}
