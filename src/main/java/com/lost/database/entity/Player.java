package com.lost.database.entity;

import java.time.LocalDateTime;

/** Сутність гравця. */
public class Player {

    private Long id;
    private String username;
    private String passwordHash;
    private String email;
    private String role = "ROLE_USER";
    private int totalScore;
    private int maxLevelReached = 1;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastLogin;

    public Player() {}

    public Player(String username, String passwordHash, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getMaxLevelReached() {
        return maxLevelReached;
    }

    public void setMaxLevelReached(int maxLevelReached) {
        this.maxLevelReached = maxLevelReached;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Override
    public String toString() {
        return "Player{id=" + id + ", username='" + username + "', role='" + role + "'}";
    }
}
