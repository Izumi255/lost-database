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
    private String baseUrl;

    private Long currentSessionId;
    private String currentSessionCode;
    private boolean isHost;
    private volatile List<Map<String, String>> cachedState = new ArrayList<>();

    private MultiplayerService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        
        // Load saved server IP, default to 26.4.16.99
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(MultiplayerService.class);
        String savedIp = prefs.get("server_ip", "26.4.16.99");
        setServerAddress(savedIp);
    }

    public void setServerAddress(String hostOrIp) {
        if (hostOrIp == null || hostOrIp.trim().isEmpty()) {
            hostOrIp = "26.4.16.99";
        }
        
        String ipStr = hostOrIp.trim();
        String clean = ipStr;
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            if (!clean.contains(":")) {
                clean = clean + ":8080";
            }
            clean = "http://" + clean;
        }
        if (!clean.endsWith("/api/sessions")) {
            if (clean.endsWith("/api")) {
                clean = clean + "/sessions";
            } else if (clean.endsWith("/api/")) {
                clean = clean + "sessions";
            } else {
                if (clean.endsWith("/")) {
                    clean = clean + "api/sessions";
                } else {
                    clean = clean + "/api/sessions";
                }
            }
        }
        this.baseUrl = clean;
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
        cachedState = new ArrayList<>();
    }

    public Long getCurrentSessionId() { return currentSessionId; }
    public String getCurrentSessionCode() { return currentSessionCode; }
    public boolean isHost() { return isHost; }

    private HttpRequest.Builder createRequestBuilder(String endpoint) {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(baseUrl + endpoint));
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
            String json = String.format(java.util.Locale.US, "{\"x\":%f,\"y\":%f,\"health\":%d,\"direction\":%d,\"animationState\":\"%s\"}",
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

    public void fetchSessionStateAsync() {
        if (currentSessionId == null) return;
        try {
            HttpRequest request = createRequestBuilder("/" + currentSessionId + "/state")
                    .GET()
                    .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                  .thenAccept(response -> {
                      if (response.statusCode() == 200) {
                          cachedState = ApiClient.parseSimpleJsonList(response.body());
                      }
                  });
        } catch (Exception e) {
            // Ignore
        }
    }

    public List<Map<String, String>> getLastCachedState() {
        return cachedState != null ? cachedState : new ArrayList<>();
    }
}
