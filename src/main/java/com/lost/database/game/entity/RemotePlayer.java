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
            idleRight = new Image(getClass().getResourceAsStream("/assets/sprites/idle outline.gif"));
            runRight = new Image(getClass().getResourceAsStream("/assets/sprites/run outline.gif"));
        } catch (Exception e) {
            System.err.println("Failed to load remote player sprites.");
        }
    }

    public void setSprites(Image idle, Image run) {
        if (idle != null) this.idleRight = idle;
        if (run != null) this.runRight = run;
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
        
        // Render Sprite (handle both "RUN" and legacy "RUNNING" states)
        boolean isMoving = "RUN".equals(animationState) || "RUNNING".equals(animationState);
        Image sprite = isMoving ? runRight : idleRight;
        
        if (sprite != null && !sprite.isError()) {
            double scale = 2.0;
            double dw = sprite.getWidth() * scale;
            double dh = sprite.getHeight() * scale;
            
            if (dw <= 0 || dh <= 0) {
                dw = 40; // SPRITE_RENDER_W
                dh = 40; // SPRITE_RENDER_H
            }
            
            // Adjust to center on player's position
            // Local player hitbox: PLAYER_W = 60, PLAYER_H = 76
            double playerW = 60;
            double playerH = 76;
            double adjustedDrawX = drawX - (dw - playerW) / 2.0;
            double adjustedDrawY = drawY - (dh - playerH); // align bottom feet
            
            if (direction >= 0) {
                gc.drawImage(sprite, adjustedDrawX, adjustedDrawY, dw, dh);
            } else {
                // Flip horizontally by using negative width
                gc.drawImage(sprite, adjustedDrawX + dw, adjustedDrawY, -dw, dh);
            }
        } else {
            // Fallback square (semi-transparent green to look high-tech and intentional, but visual skin is primary)
            gc.setFill(Color.rgb(50, 200, 100, 0.8));
            gc.fillRoundRect(drawX, drawY + 12, 60, 64, 8, 8);
            gc.setStroke(Color.rgb(100, 255, 150));
            gc.setLineWidth(2);
            gc.strokeRoundRect(drawX, drawY + 12, 60, 64, 8, 8);
        }
        
        // Draw username above head
        if (username != null) {
            gc.save();
            gc.setFill(Color.rgb(220, 220, 220, 0.9));
            gc.setFont(Font.font("System", FontWeight.BOLD, 12));
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.fillText(username, drawX + 30, drawY - 10);
            gc.restore();
        }
    }
    
    public long getPlayerId() {
        return playerId;
    }
}
