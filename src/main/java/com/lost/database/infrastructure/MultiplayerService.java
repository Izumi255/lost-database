package com.lost.database.infrastructure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Сервіс для мультиплеєрної взаємодії (Long Polling) */
public class MultiplayerService {
    private static MultiplayerService instance;
    private final HttpClient client;
    private static final String BASE_URL = "http://localhost:8080/api/sessions";

    private Long currentSessionId;
    private String currentSessionCode;
    private boolean isHost;

    private MultiplayerService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public static synchronized MultiplayerService getInstance() {
        if (instance == null) {
            instance = new MultiplayerService();
        }
        return instance;
    }

    public void reset() {
        currentSessionId = null;
        currentSessionCode = null;
        isHost = false;
    }

    public Long getCurrentSessionId() { return currentSessionId; }
    public String getCurrentSessionCode() { return currentSessionCode; }
    public boolean isHost() { return isHost; }

    private HttpRequest.Builder createRequestBuilder(String endpoint) {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(BASE_URL + endpoint));
        String token = OnlineService.getInstance().isOnline() ? OnlineService.getInstance().getToken() : null;
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    public Map<String, Object> createSession(int maxPlayers) {
        try {
            HttpRequest request = createRequestBuilder("?maxPlayers=" + maxPlayers)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Map<String, String> map = ApiClient.parseSimpleJsonMap(response.body());
                if (map != null && map.containsKey("sessionId")) {
                    currentSessionId = Long.parseLong(map.get("sessionId"));
                    currentSessionCode = map.get("sessionCode");
                    isHost = true;
                    Map<String, Object> res = new HashMap<>();
                    res.put("sessionId", currentSessionId);
                    res.put("sessionCode", currentSessionCode);
                    return res;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, Object> joinSession(String code) {
        try {
            HttpRequest request = createRequestBuilder("/join?code=" + code)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Map<String, String> map = ApiClient.parseSimpleJsonMap(response.body());
                if (map != null && map.containsKey("sessionId")) {
                    currentSessionId = Long.parseLong(map.get("sessionId"));
                    currentSessionCode = code;
                    isHost = false;
                    Map<String, Object> res = new HashMap<>();
                    res.put("sessionId", currentSessionId);
                    return res;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void syncPosition(double x, double y, int health, int direction, String animationState) {
        if (currentSessionId == null) return;
        try {
            String json = String.format("{\"x\":%f,\"y\":%f,\"health\":%d,\"direction\":%d,\"animationState\":\"%s\"}",
                    x, y, health, direction, animationState);
            HttpRequest request = createRequestBuilder("/" + currentSessionId + "/sync")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            // Ignore async errors
        }
    }

    public List<Map<String, String>> getSessionState() {
        if (currentSessionId == null) return new ArrayList<>();
        try {
            HttpRequest request = createRequestBuilder("/" + currentSessionId + "/state")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return ApiClient.parseSimpleJsonList(response.body());
            }
        } catch (Exception e) {
            // Ignore
        }
        return new ArrayList<>();
    }
}
