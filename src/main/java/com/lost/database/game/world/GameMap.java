package com.lost.database.game.world;

import java.io.Serializable;
import java.util.Random;

/** Базова карта гри (острів). */
public class GameMap implements Serializable {
    private static final long serialVersionUID = 1L;
    private int width;
    private int height;
    private TileType[][] grid;

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new TileType[width][height];
        generateIsland();
    }

    private void generateIsland() {
        Random random = new Random();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = TileType.WATER;
            }
        }

        int centerX = width / 2;
        int centerY = height / 2;
        int islandRadius = Math.min(width, height) / 2 - 10;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int dx = x - centerX;
                int dy = y - centerY;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance < islandRadius) {
                    if (distance > islandRadius - 5) {
                        grid[x][y] = TileType.SAND;
                    } else {
                        grid[x][y] = TileType.GRASS;
                    }
                }
            }
        }

        addForestZone(centerX - 20, centerY - 20, 12, 12);
        addForestZone(centerX + 8, centerY - 20, 12, 12);
        addForestZone(centerX - 20, centerY + 8, 12, 12);
        addForestZone(centerX + 8, centerY + 8, 12, 12);
        addMountainZone(centerX - 8, centerY - 25, 16, 10);
        createBunkerComplex(centerX - 10, centerY - 5);
        addPath(centerX, centerY - 25, centerX, centerY + 25);
        addPath(centerX - 25, centerY, centerX + 25, centerY);
    }

    private void addForestZone(int startX, int startY, int w, int h) {
        for (int x = startX; x < startX + w; x++) {
            for (int y = startY; y < startY + h; y++) {
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    if (grid[x][y] == TileType.GRASS) grid[x][y] = TileType.FOREST;
                }
            }
        }
    }

    private void addMountainZone(int startX, int startY, int w, int h) {
        for (int x = startX; x < startX + w; x++) {
            for (int y = startY; y < startY + h; y++) {
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    if (grid[x][y] == TileType.GRASS) grid[x][y] = TileType.MOUNTAIN;
                }
            }
        }
    }

    private void createBunkerComplex(int startX, int startY) {
        int bunkerWidth = 20;
        int bunkerHeight = 10;
        for (int x = startX; x < startX + bunkerWidth; x++) {
            for (int y = startY; y < startY + bunkerHeight; y++) {
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    if (x == startX
                            || x == startX + bunkerWidth - 1
                            || y == startY
                            || y == startY + bunkerHeight - 1) {
                        grid[x][y] = TileType.BUNKER_WALL;
                    } else {
                        grid[x][y] = TileType.BUNKER_FLOOR;
                    }
                }
            }
        }
        int doorY = startY + bunkerHeight - 1;
        int doorX1 = startX + bunkerWidth / 2 - 1;
        int doorX2 = startX + bunkerWidth / 2;
        if (doorX1 >= 0 && doorX1 < width && doorY >= 0 && doorY < height)
            grid[doorX1][doorY] = TileType.BUNKER_DOOR;
        if (doorX2 >= 0 && doorX2 < width && doorY >= 0 && doorY < height)
            grid[doorX2][doorY] = TileType.BUNKER_DOOR;
    }

    private void addPath(int x1, int y1, int x2, int y2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            double t = steps > 0 ? (double) i / steps : 0;
            int x = (int) (x1 + t * (x2 - x1));
            int y = (int) (y1 + t * (y2 - y1));
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int px = x + dx;
                    int py = y + dy;
                    if (px >= 0 && px < width && py >= 0 && py < height) {
                        if (grid[px][py] == TileType.GRASS) grid[px][py] = TileType.SAND;
                    }
                }
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public TileType getTile(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return TileType.WATER;
        return grid[x][y];
    }
}
