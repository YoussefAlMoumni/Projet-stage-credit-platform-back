package com.talan.creditplatform.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaResponse {

    private String model;
    private String response;
    private boolean done;
    private final Map<String, Object> other = new HashMap<>();

    public OllamaResponse() {}

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    @JsonAnySetter
    public void set(String key, Object value) {
        other.put(key, value);
    }

    public Map<String, Object> getOther() {
        return other;
    }

    @Override
    public String toString() {
        return "OllamaResponse{" +
                "model='" + model + '\'' +
                ", response='" + response + '\'' +
                ", done=" + done +
                ", other=" + other +
                '}';
    }
}
