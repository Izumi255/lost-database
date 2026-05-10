package com.lost.database.game.world;

import java.io.Serializable;
import java.util.List;

/** Карта джунглів (TMX або процедурна генерація). */
public class JungleMap implements Serializable {
    private static final long serialVersionUID = 5L;

    private int width;
    private int height;
    private TileType[][] grid;
    private List<int[][]> tmxLayers;
    private List<TmxMapLoader.TmxObject> tmxObjects;
    private int spawnX, spawnY;
    private int cockpitX, cockpitY;

    public JungleMap() {
        this(100, 20);
    }

    public JungleMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = JungleLevelBuilder.generateLevel(width, height);

        int floorY = Math.max(0, height - 2);
        this.spawnX = 2;
        this.spawnY = Math.max(0, floorY - 1);
        this.cockpitX = Math.max(0, width - 4);
        this.cockpitY = Math.max(0, floorY - 1);
        if (cockpitX >= 0 && cockpitY >= 0 && cockpitX < width && cockpitY < height) {
            this.grid[cockpitX][cockpitY] = TileType.COCKPIT_WRECKAGE;
        }
    }

    public JungleMap(String tmxPath) {
        List<int[][]> layers = TileMapLoader.loadTMX(tmxPath);
        if (layers != null && !layers.isEmpty()) {
            this.tmxLayers = layers;
            this.tmxObjects = new java.util.ArrayList<>();

            int[][] baseGrid = tmxLayers.get(0);
            this.height = baseGrid.length;
            this.width = baseGrid[0].length;
            this.grid = new TileType[width][height];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    boolean isSolid = false;
                    for (int[][] layer : tmxLayers) {
                        int id = layer[y][x];
                        if (id > 0) {
                            isSolid = true;
                            break;
                        }
                    }
                    if (isSolid) {
                        grid[x][y] = TileType.GROUND;
                    }
                }
            }

            this.spawnX = 2;
            this.spawnY = height - 5;
            for (int sy = 0; sy < height; sy++) {
                if (isSolid(2, sy)) {
                    this.spawnY = Math.max(0, sy - 1);
                    break;
                }
            }
            this.cockpitX = width - 4;
            this.cockpitY = height - 5;
            for (int sy = 0; sy < height; sy++) {
                if (isSolid(width - 4, sy)) {
                    this.cockpitY = Math.max(0, sy - 1);
                    break;
                }
            }
        } else {
            this.width = 30;
            this.height = 20;
            this.grid = new TileType[width][height];
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getSpawnX() {
        return spawnX;
    }

    public int getSpawnY() {
        return spawnY;
    }

    public int getCockpitX() {
        return cockpitX;
    }

    public int getCockpitY() {
        return cockpitY;
    }

    public TileType getTile(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return TileType.JUNGLE_TREE;
        return grid[x][y];
    }

    public void setTile(int x, int y, TileType t) {
        if (x < 0 || x >= width || y < 0 || y >= height) return;
        grid[x][y] = t;
    }

    public boolean isSolid(int x, int y) {
        TileType t = getTile(x, y);
        return t == TileType.GROUND
                || t == TileType.FLOATING_PLATFORM
                || t == TileType.COCKPIT_WRECKAGE;
    }

    public boolean isHazard(int x, int y) {
        return getTile(x, y) == TileType.SPIKES;
    }

    public boolean isTmx() {
        return tmxLayers != null && !tmxLayers.isEmpty();
    }

    public List<int[][]> getTmxLayers() {
        return tmxLayers;
    }

    public List<TmxMapLoader.TmxObject> getTmxObjects() {
        return tmxObjects;
    }
}
