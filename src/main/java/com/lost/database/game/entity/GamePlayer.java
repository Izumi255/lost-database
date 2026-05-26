package com.lost.database.game.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Ігровий персонаж: позиція, HP, velocity, інвентар. */
public class GamePlayer implements Serializable {
    private static final long serialVersionUID = 2L;
    private int id;
    private double x;
    private double y;
    private double vx;
    private double vy;
    private boolean isGrounded = false;
    private List<String> inventory = new ArrayList<>();
    private double spawnX, spawnY;

    private int health = 100;
    private int maxHealth = 100;
    private transient double damageCooldown = 0;
    private static final double DAMAGE_COOLDOWN_TIME = 1.0;

    public GamePlayer(int id, double x, double y) {
        this.id = id;
        this.spawnX = 150.0;
        this.spawnY = 300.0;
        if (x <= 0 && y <= 0) {
            this.x = this.spawnX;
            this.y = this.spawnY;
        } else {
            this.x = x;
            this.y = y;
        }
    }

    public int getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getVx() {
        return vx;
    }

    public void setVx(double vx) {
        this.vx = vx;
    }

    public double getVy() {
        return vy;
    }

    public void setVy(double vy) {
        this.vy = vy;
    }

    public boolean isGrounded() {
        return isGrounded;
    }

    public void setGrounded(boolean grounded) {
        isGrounded = grounded;
        if (grounded) {
            this.vy = 0;
        }
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

    public boolean isAlive() {
        return health > 0;
    }

    public boolean isInvincible() {
        return damageCooldown > 0;
    }

    public void updateCooldown(double dt) {
        if (damageCooldown > 0) {
            damageCooldown -= dt;
        }
    }

    private boolean godMode = false;

    public boolean isGodMode() {
        return godMode;
    }

    public void setGodMode(boolean godMode) {
        this.godMode = godMode;
    }

    public boolean takeDamage(int amount) {
        if (godMode) return false;
        if (damageCooldown > 0 || health <= 0) return false;
        health = Math.max(0, health - amount);
        damageCooldown = DAMAGE_COOLDOWN_TIME;
        return true;
    }

    public void heal(int amount) {
        health = Math.min(maxHealth, health + amount);
    }

    public void resetToSpawn() {
        this.x = spawnX;
        this.y = spawnY;
        this.vx = 0;
        this.vy = 0;
        this.isGrounded = false;
    }

    public void fullReset() {
        resetToSpawn();
        this.health = maxHealth;
        this.damageCooldown = 0;
    }

    public void setSpawnPosition(double x, double y) {
        this.spawnX = x;
        this.spawnY = y;
    }

    public void addItem(String item) {
        inventory.add(item);
    }

    public boolean hasItem(String item) {
        return inventory.contains(item);
    }

    public List<String> getInventory() {
        return inventory;
    }
}
