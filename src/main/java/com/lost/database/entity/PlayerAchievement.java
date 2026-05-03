package com.lost.database.entity;

import java.time.LocalDateTime;

/** Сутність досягнення гравця. */
public class PlayerAchievement {

    private Long id;
    private Long playerId;
    private String achievementCode;
    private String achievementName;
    private String description;
    private LocalDateTime unlockedAt = LocalDateTime.now();

    public PlayerAchievement() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getAchievementCode() {
        return achievementCode;
    }

    public void setAchievementCode(String achievementCode) {
        this.achievementCode = achievementCode;
    }

    public String getAchievementName() {
        return achievementName;
    }

    public void setAchievementName(String achievementName) {
        this.achievementName = achievementName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(LocalDateTime unlockedAt) {
        this.unlockedAt = unlockedAt;
    }

    @Override
    public String toString() {
        return "PlayerAchievement{id=" + id + ", code='" + achievementCode + "'}";
    }
}
