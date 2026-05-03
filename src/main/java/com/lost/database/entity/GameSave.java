package com.lost.database.entity;

import java.time.LocalDateTime;

/** Сутність збереження гри. */
public class GameSave {

    private Long id;
    private Long playerId;
    private int currentLevel;
    private int health;
    private int maxHealth = 100;
    private double sanity = 100.0;
    private double positionX;
    private double positionY;
    private String saveName;
    private LocalDateTime savedAt = LocalDateTime.now();

    public GameSave() {}

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

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public double getSanity() {
        return sanity;
    }

    public void setSanity(double sanity) {
        this.sanity = sanity;
    }

    public double getPositionX() {
        return positionX;
    }

    public void setPositionX(double positionX) {
        this.positionX = positionX;
    }

    public double getPositionY() {
        return positionY;
    }

    public void setPositionY(double positionY) {
        this.positionY = positionY;
    }

    public String getSaveName() {
        return saveName;
    }

    public void setSaveName(String saveName) {
        this.saveName = saveName;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    @Override
    public String toString() {
        return "GameSave{id=" + id + ", playerId=" + playerId + ", saveName='" + saveName + "'}";
    }
}
