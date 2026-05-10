package com.lost.database.game.world;

/** Генератор рівнів (fallback якщо TMX не завантажився). */
public class JungleLevelBuilder {

    public static TileType[][] generateLevel(int width, int height) {
        TileType[][] grid = new TileType[width][height];
        java.util.Random rnd = new java.util.Random(42);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = null;
            }
        }

        int baseSurfaceLevel = height - 6;
        int[] surfaceHeights = new int[width];
        double noiseScale = 0.08;

        for (int x = 0; x < width; x++) {
            double noise = 0;
            noise += Math.sin(x * noiseScale) * 2.0;
            noise += Math.sin(x * noiseScale * 2.3 + 1.7) * 1.0;
            noise += Math.sin(x * noiseScale * 4.1 + 3.2) * 0.5;
            if (x > 30 && x < 50) noise -= 2;
            if (x > 60 && x < 75) noise -= 3;
            surfaceHeights[x] =
                    Math.max(height - 10, Math.min(height - 3, baseSurfaceLevel + (int) noise));
        }

        for (int x = 0; x < width; x++) {
            int surface = surfaceHeights[x];
            for (int y = surface; y < height; y++) {
                grid[x][y] = TileType.GROUND;
            }
        }

        for (int i = 0; i < 4; i++) {
            int caveX = 15 + rnd.nextInt(width - 30);
            int caveSurface = surfaceHeights[Math.min(caveX, width - 1)];
            int caveY = caveSurface + 2 + rnd.nextInt(2);
            int caveW = 3 + rnd.nextInt(4);
            int caveH = 2 + rnd.nextInt(2);
            for (int cx = caveX; cx < Math.min(caveX + caveW, width); cx++) {
                for (int cy = caveY; cy < Math.min(caveY + caveH, height - 1); cy++) {
                    grid[cx][cy] = null;
                }
            }
        }

        int[][] platforms = {
            {8, baseSurfaceLevel - 4, 4},
            {22, baseSurfaceLevel - 5, 5},
            {38, baseSurfaceLevel - 6, 4},
            {52, baseSurfaceLevel - 4, 6},
            {68, baseSurfaceLevel - 7, 5},
            {80, baseSurfaceLevel - 5, 4},
            {90, baseSurfaceLevel - 4, 3}
        };
        for (int[] plat : platforms) {
            int px = plat[0], py = plat[1], pw = plat[2];
            for (int x = px; x < Math.min(px + pw, width); x++) {
                if (x >= 0 && x < width && py >= 0 && py < height) {
                    grid[x][py] = TileType.FLOATING_PLATFORM;
                }
            }
        }

        for (int i = 0; i < 3; i++) {
            int spikeX = 20 + i * 25 + rnd.nextInt(5);
            if (spikeX < width) {
                int surface = surfaceHeights[spikeX];
                if (surface - 1 >= 0) {
                    grid[spikeX][surface - 1] = TileType.SPIKES;
                    if (spikeX + 1 < width) grid[spikeX + 1][surface - 1] = TileType.SPIKES;
                }
            }
        }

        for (int x = 3; x < width - 3; x++) {
            int surface = surfaceHeights[x];
            if (surface - 1 >= 0 && grid[x][surface] == TileType.GROUND) {
                double chance = rnd.nextDouble();
                if (chance < 0.05) {
                    for (int dy = 1; dy <= 3 && surface - dy >= 0; dy++) {
                        if (grid[x][surface - dy] == null)
                            grid[x][surface - dy] = TileType.DECORATION;
                    }
                } else if (chance < 0.12) {
                    if (grid[x][surface - 1] == null) grid[x][surface - 1] = TileType.DECORATION;
                }
            }
        }

        int[] healthPackPositions = {15, 35, 55, 75, 92};
        for (int hpX : healthPackPositions) {
            if (hpX < width) {
                int surface = surfaceHeights[hpX];
                if (surface - 1 >= 0 && grid[hpX][surface - 1] == null)
                    grid[hpX][surface - 1] = TileType.HEALTH_PACK;
            }
        }

        for (int i = 0; i < 8; i++) {
            int foodX = 5 + rnd.nextInt(width - 10);
            if (foodX < width) {
                int surface = surfaceHeights[foodX];
                if (surface - 1 >= 0 && grid[foodX][surface - 1] == null)
                    grid[foodX][surface - 1] = TileType.FOOD_ITEM;
            }
        }

        int[] enemyPositions = {18, 40, 58, 72, 88};
        for (int eX : enemyPositions) {
            if (eX < width) {
                int surface = surfaceHeights[eX];
                if (surface - 1 >= 0 && grid[eX][surface - 1] == null)
                    grid[eX][surface - 1] = TileType.ENEMY_PATROL;
            }
        }

        return grid;
    }
}
