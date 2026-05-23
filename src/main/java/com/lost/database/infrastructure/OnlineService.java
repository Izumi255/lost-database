package com.lost.database.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Сервіс для взаємодії з онлайн API. Реалізований як Singleton. */
public class OnlineService {
    private static OnlineService instance;
    private final ApiClient apiClient;
    private long currentOnlinePlayerId = -1;
    private String currentUsername;

    private OnlineService() {
        this.apiClient = new ApiClient();
    }

    public static synchronized OnlineService getInstance() {
        if (instance == null) {
            instance = new OnlineService();
        }
        return instance;
    }

    public boolean isOnline() {
        return apiClient.getToken() != null && !apiClient.getToken().isEmpty();
    }

    public String getToken() {
        return apiClient.getToken();
    }

    public long getOnlinePlayerId() {
        return currentOnlinePlayerId;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public Optional<Map<String, String>> loginOnline(String username, String password) {
        Map<String, String> response = apiClient.login(username, password);
        if (response != null && response.containsKey("token")) {
            apiClient.setToken(response.get("token"));
            try {
                this.currentOnlinePlayerId = Long.parseLong(response.get("playerId"));
                this.currentUsername = response.get("username");
            } catch (Exception e) {
                // Ignore parse error
            }
            return Optional.of(response);
        }
        return Optional.empty();
    }

    public Optional<Map<String, String>> registerOnline(
            String username, String password, String email) {
        Map<String, String> response = apiClient.register(username, password, email);
        if (response != null && response.containsKey("message")) {
            return Optional.of(response);
        }
        return Optional.empty();
    }

    public void logout() {
        apiClient.setToken(null);
        currentOnlinePlayerId = -1;
        currentUsername = null;
    }

    public List<Map<String, String>> getLeaderboard() {
        return apiClient.getLeaderboard();
    }

    public boolean syncScore(int score, int levelCompleted, int timeSec) {
        if (!isOnline() || currentOnlinePlayerId == -1) return false;

        // Відправляємо в лідерборд
        boolean leaderboardSuccess =
                apiClient.submitScore(currentOnlinePlayerId, score, levelCompleted, timeSec);

        // Оновлюємо загальний прогрес
        boolean statsSuccess =
                apiClient.updatePlayerStats(currentOnlinePlayerId, score, levelCompleted);

        return leaderboardSuccess && statsSuccess;
    }

    public boolean saveGameOnline(
            int level, int health, int maxHealth, int sanity, double x, double y, String saveName) {
        if (!isOnline() || currentOnlinePlayerId == -1) return false;
        return apiClient.saveGame(
                currentOnlinePlayerId, level, health, maxHealth, sanity, x, y, saveName);
    }

    public List<Map<String, String>> loadSavesOnline() {
        if (!isOnline() || currentOnlinePlayerId == -1) return List.of();
        return apiClient.getSaves(currentOnlinePlayerId);
    }
}
