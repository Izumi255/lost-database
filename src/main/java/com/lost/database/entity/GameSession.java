package com.lost.database.entity;

import java.time.LocalDateTime;

/** Сутність ігрової сесії (мультиплеєр). */
public class GameSession {

    private Long id;
    private String sessionCode;
    private Long hostPlayerId;
    private int maxPlayers = 4;
    private String status = "WAITING";
    private int currentLevel = 1;
    private LocalDateTime createdAt = LocalDateTime.now();

    public GameSession() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public Long getHostPlayerId() {
        return hostPlayerId;
    }

    public void setHostPlayerId(Long hostPlayerId) {
        this.hostPlayerId = hostPlayerId;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "GameSession{id=" + id + ", code='" + sessionCode + "', status='" + status + "'}";
    }
}
