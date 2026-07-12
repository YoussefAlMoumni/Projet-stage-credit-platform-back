package com.talan.creditplatform.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OllamaRequest {

    private String model;
    private String prompt;
    private boolean stream = false;
    private OllamaOptions options;

    @JsonProperty("keep_alive")
    private String keepAlive;

    public OllamaRequest() {}

    public OllamaRequest(String model, String prompt, OllamaOptions options, String keepAlive) {
        this.model = model;
        this.prompt = prompt;
        this.options = options;
        this.keepAlive = keepAlive;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public OllamaOptions getOptions() {
        return options;
    }

    public void setOptions(OllamaOptions options) {
        this.options = options;
    }

    public String getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(String keepAlive) {
        this.keepAlive = keepAlive;
    }
}
