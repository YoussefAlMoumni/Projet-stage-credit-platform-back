package com.talan.creditplatform.desktop.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class BackendClient {
    private static final String BASE_URL = "http://localhost:8081/api";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    private static String jwtToken = null;
    public static String currentRole = null;

    public static boolean login(String username, String password) {
        try {
            String jsonPayload = mapper.writeValueAsString(Map.of("username", username, "password", password));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                var resMap = mapper.readValue(response.body(), Map.class);
                jwtToken = (String) resMap.get("token");
                currentRole = (String) resMap.get("role");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static HttpRequest.Builder authenticatedRequest(String endpoint) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Authorization", "Bearer " + jwtToken)
                .header("Content-Type", "application/json");
    }

    public static HttpClient getClient() {
        return client;
    }
}
