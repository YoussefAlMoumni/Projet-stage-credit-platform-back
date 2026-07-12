package com.talan.creditplatform.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OllamaClient {

    private final RestClient restClient;

    public OllamaClient(@Value("${ollama.api.url}") String ollamaApiUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(ollamaApiUrl)
                .build();
    }

    public String generate(OllamaRequest request) {
        OllamaResponse response = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OllamaResponse.class);

        if (response != null && response.getResponse() != null) {
            return response.getResponse();
        }
        
        throw new RuntimeException("Empty or malformed response from Ollama API");
    }
}
