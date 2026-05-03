package com.lost.database.entity;

import java.time.LocalDateTime;

/** Сутність запису таблиці лідерів. */
public class LeaderboardEntry {

    private Long id;
    private Long playerId;
    private int score;
    private int levelCompleted;
    private double completionTimeSec;
    private LocalDateTime achievedAt = LocalDateTime.now();

    public LeaderboardEntry() {}

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

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getLevelCompleted() {
        return levelCompleted;
    }

    public void setLevelCompleted(int levelCompleted) {
        this.levelCompleted = levelCompleted;
    }

    public double getCompletionTimeSec() {
        return completionTimeSec;
    }

    public void setCompletionTimeSec(double completionTimeSec) {
        this.completionTimeSec = completionTimeSec;
    }

    public LocalDateTime getAchievedAt() {
        return achievedAt;
    }

    public void setAchievedAt(LocalDateTime achievedAt) {
        this.achievedAt = achievedAt;
    }

    @Override
    public String toString() {
        return "LeaderboardEntry{id=" + id + ", score=" + score + ", level=" + levelCompleted + "}";
    }
}
