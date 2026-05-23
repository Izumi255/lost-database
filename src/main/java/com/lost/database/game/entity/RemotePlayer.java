package com.lost.database.game.entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class RemotePlayer {
    private long playerId;
    private String username;
    
    // Server positions
    private double serverX;
    private double serverY;
    
    // Interpolated rendering positions
    private double renderX;
    private double renderY;
    
    private int health;
    private boolean isAlive;
    private int direction = 1;
    private String animationState = "IDLE";
    
    // Sprites
    private Image idleRight;
    private Image idleLeft;
    private Image runRight;
    private Image runLeft;
    
    private double animationTimer = 0;
    
    public RemotePlayer(long playerId) {
        this.playerId = playerId;
        try {
            idleRight = new Image(getClass().getResourceAsStream("/assets/sprites/player_idle_right.png"));
            idleLeft = new Image(getClass().getResourceAsStream("/assets/sprites/player_idle_left.png"));
            runRight = new Image(getClass().getResourceAsStream("/assets/sprites/player_run_right.gif"));
            runLeft = new Image(getClass().getResourceAsStream("/assets/sprites/player_run_left.gif"));
        } catch (Exception e) {
            System.err.println("Failed to load remote player sprites.");
        }
    }
    
    public void updateState(double x, double y, int health, boolean isAlive, int direction, String animationState) {
        // If distance is too large, teleport directly instead of interpolating
        if (Math.abs(this.serverX - x) > 100 || Math.abs(this.serverY - y) > 100) {
            this.renderX = x;
            this.renderY = y;
        }
        
        this.serverX = x;
        this.serverY = y;
        this.health = health;
        this.isAlive = isAlive;
        this.direction = direction;
        this.animationState = animationState;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public void update(double dt) {
        // Simple linear interpolation towards server position
        double lerpSpeed = 10.0;
        renderX += (serverX - renderX) * lerpSpeed * dt;
        renderY += (serverY - renderY) * lerpSpeed * dt;
        
        animationTimer += dt;
    }
    
    public void render(GraphicsContext gc, double cameraX, double cameraY) {
        if (!isAlive) return;
        
        double drawX = renderX - cameraX;
        double drawY = renderY - cameraY;
        
        // Render Sprite
        Image sprite = idleRight;
        if (direction > 0) {
            sprite = "RUNNING".equals(animationState) ? runRight : idleRight;
        } else {
            sprite = "RUNNING".equals(animationState) ? runLeft : idleLeft;
        }
        
        if (sprite != null) {
            // Draw slightly larger or exact size of player (typically 64x64 or whatever player uses)
            gc.drawImage(sprite, drawX - 16, drawY - 16, 64, 64);
        } else {
            // Fallback square
            gc.setFill(Color.BLUE);
            gc.fillRect(drawX, drawY, 32, 32);
        }
        
        // Draw username above head
        if (username != null) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            gc.fillText(username, drawX - 10, drawY - 20);
        }
    }
    
    public long getPlayerId() {
        return playerId;
    }
}
