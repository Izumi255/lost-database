package com.lost.database.entity;

import java.time.LocalDateTime;

/** Сутність зв'язку гравця та сесії (M:N). */
public class SessionPlayer {

    private Long id;
    private Long sessionId;
    private Long playerId;
    private double positionX;
    private double positionY;
    private int health = 100;
    private boolean alive = true;
    private LocalDateTime joinedAt = LocalDateTime.now();

    public SessionPlayer() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
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

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    @Override
    public String toString() {
        return "SessionPlayer{id="
                + id
                + ", sessionId="
                + sessionId
                + ", playerId="
                + playerId
                + "}";
    }
}
