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

/** HTTP клієнт для взаємодії з REST API сервером гри (port 8080). */
public class ApiClient {
    private static final String BASE_URL = "http://localhost:8080/api";
    private final HttpClient client;
    private String jwtToken;

    public ApiClient() {
        this.client =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .version(HttpClient.Version.HTTP_1_1)
                        .build();
    }

    public void setToken(String token) {
        this.jwtToken = token;
    }

    public String getToken() {
        return jwtToken;
    }

    private HttpRequest.Builder createRequestBuilder(String endpoint) {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(BASE_URL + endpoint));
        if (jwtToken != null && !jwtToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + jwtToken);
        }
        return builder;
    }

    /** Логін (отримує JWT токен) */
    public Map<String, String> login(String username, String password) {
        try {
            String jsonBody =
                    String.format(
                            "{\"username\":\"%s\", \"password\":\"%s\"}",
                            escapeJson(username), escapeJson(password));
            HttpRequest request =
                    createRequestBuilder("/players/login")
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseSimpleJsonMap(response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Реєстрація (без JWT) */
    public Map<String, String> register(String username, String password, String email) {
        try {
            String mailStr = email == null ? "" : email;
            String jsonBody =
                    String.format(
                            "{\"username\":\"%s\", \"password\":\"%s\", \"email\":\"%s\"}",
                            escapeJson(username), escapeJson(password), escapeJson(mailStr));
            HttpRequest request =
                    createRequestBuilder("/players/register")
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseSimpleJsonMap(response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Отримати лідерборд (без JWT) */
    public List<Map<String, String>> getLeaderboard() {
        try {
            HttpRequest request = createRequestBuilder("/leaderboard").GET().build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseSimpleJsonList(response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /** Відправити результат у лідерборд (з JWT) */
    public boolean submitScore(
            long playerId, int score, int levelCompleted, int completionTimeSec) {
        try {
            String jsonBody =
                    String.format(
                            "{\"playerId\":%d, \"score\":%d, \"levelCompleted\":%d, \"completionTimeSec\":%d}",
                            playerId, score, levelCompleted, completionTimeSec);
            HttpRequest request =
                    createRequestBuilder("/leaderboard")
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Зберегти гру онлайн (з JWT) */
    public boolean saveGame(
            long playerId,
            int level,
            int health,
            int maxHealth,
            int sanity,
            double posX,
            double posY,
            String saveName) {
        try {
            String jsonBody =
                    String.format(
                            "{\"playerId\":%d, \"currentLevel\":%d, \"health\":%d, \"maxHealth\":%d, \"sanity\":%d, \"positionX\":%s, \"positionY\":%s, \"saveName\":\"%s\"}",
                            playerId,
                            level,
                            health,
                            maxHealth,
                            sanity,
                            String.valueOf(posX),
                            String.valueOf(posY),
                            escapeJson(saveName));
            HttpRequest request =
                    createRequestBuilder("/saves")
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Завантажити сейви (з JWT) */
    public List<Map<String, String>> getSaves(long playerId) {
        try {
            HttpRequest request = createRequestBuilder("/saves/" + playerId).GET().build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseSimpleJsonList(response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /** Оновити статистику гравця (з JWT) */
    public boolean updatePlayerStats(long playerId, int totalScore, int maxLevel) {
        try {
            String jsonBody =
                    String.format(
                            "{\"totalScore\":%d, \"maxLevelReached\":%d}", totalScore, maxLevel);
            HttpRequest request =
                    createRequestBuilder("/players/" + playerId)
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- Прості утиліти для парсингу JSON без додаткових бібліотек ---
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\n", "\\n");
    }

    public static Map<String, String> parseSimpleJsonMap(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length >= 2) {
                String k = kv[0].replace("\"", "").trim();
                String v = kv[1].replace("\"", "").trim();
                map.put(k, v);
            }
        }
        return map;
    }

    public static List<Map<String, String>> parseSimpleJsonList(String json) {
        List<Map<String, String>> list = new ArrayList<>();
        json = json.trim();
        if (json.startsWith("[")) json = json.substring(1);
        if (json.endsWith("]")) json = json.substring(0, json.length() - 1);

        // Дуже простий парсер, підходить для простого списку об'єктів
        String[] objects = json.split("},\\{");
        for (String obj : objects) {
            if (!obj.startsWith("{")) obj = "{" + obj;
            if (!obj.endsWith("}")) obj = obj + "}";
            list.add(parseSimpleJsonMap(obj));
        }
        return list;
    }
}
