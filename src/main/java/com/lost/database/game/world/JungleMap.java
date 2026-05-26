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
                    int tileId = 0;
                    for (int[][] layer : tmxLayers) {
                        int id = layer[y][x];
                        if (id > 0) {
                            tileId = id;
                            break;
                        }
                    }
                    if (tileId > 0) {
                        // Slope tiles:
                        // 121 = gentle slope up (first half), 122 = second half
                        // 90 = steep ascending slope (0 → full height)
                        // 91 = steep descending slope (full height → 0)
                        // 106, 107 = solid ground below slopes
                        if (tileId == 121) {
                            grid[x][y] = TileType.SLOPE_LEFT;
                        } else if (tileId == 122) {
                            grid[x][y] = TileType.SLOPE_LEFT_2;
                        } else if (tileId == 123) {
                            grid[x][y] = TileType.SLOPE_RIGHT_GENTLE;
                        } else if (tileId == 124) {
                            grid[x][y] = TileType.SLOPE_RIGHT_GENTLE_2;
                        } else if (tileId == 90) {
                            grid[x][y] = TileType.SLOPE_RIGHT_2; // steep ascending
                        } else if (tileId == 91) {
                            grid[x][y] = TileType.SLOPE_RIGHT; // steep descending
                        } else {
                            grid[x][y] = TileType.GROUND;
                        }
                    }
                }
            }

            this.spawnX = 2;
            this.spawnY = height - 5;
            for (int sy = height - 1; sy >= 6; sy--) {
                if (isSolid(2, sy) && !isSolid(2, sy - 1)) {
                    this.spawnY = sy - 1;
                    break;
                }
            }
            this.cockpitX = 40;
            this.cockpitY = height - 5;
            for (int sy = height - 1; sy >= 6; sy--) {
                if (isSolid(40, sy) && !isSolid(40, sy - 1)) {
                    this.cockpitY = sy - 1;
                    break;
                }
            }
            // Place cockpit tile on the grid so it renders (only on Level 1)
            if (tmxPath != null && tmxPath.contains("level1.tmx")) {
                if (cockpitX >= 0 && cockpitY >= 0 && cockpitX < width && cockpitY < height) {
                    this.grid[cockpitX][cockpitY] = TileType.COCKPIT_WRECKAGE;
                }
            }

            // Programmatically scatter spikes on each level to add challenge
            if (tmxPath != null) {
                if (tmxPath.contains("level1.tmx")) {
                    placeSpikes(15, 18);
                    placeSpikes(25, 27);
                    placeSpikes(35, 37);
                } else if (tmxPath.contains("level2.tmx")) {
                    placeSpikes(12, 14);
                    placeSpikes(22, 24);
                    placeSpikes(32, 34);
                } else if (tmxPath.contains("level3.tmx")) {
                    placeSpikes(18, 21);
                    placeSpikes(30, 33);
                    placeSpikes(42, 45);
                } else if (tmxPath.contains("level4.tmx")) {
                    placeSpikes(8, 10);
                    placeSpikes(18, 20);
                    placeSpikes(28, 30);
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

    public void setCockpitPosition(int x) {
        this.cockpitX = x;
        this.cockpitY = height - 5;
        for (int sy = 0; sy < height; sy++) {
            if (isSolid(x, sy)) {
                this.cockpitY = Math.max(0, sy - 1);
                break;
            }
        }
    }

    public void setCockpitPosition(int x, int y) {
        this.cockpitX = x;
        this.cockpitY = y;
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
                || t == TileType.COCKPIT_WRECKAGE
                || t == TileType.SLOPE_LEFT
                || t == TileType.SLOPE_LEFT_2
                || t == TileType.SLOPE_RIGHT
                || t == TileType.SLOPE_RIGHT_2
                || t == TileType.SLOPE_RIGHT_GENTLE
                || t == TileType.SLOPE_RIGHT_GENTLE_2;
    }

    public boolean isSlope(int x, int y) {
        TileType t = getTile(x, y);
        return t == TileType.SLOPE_LEFT
                || t == TileType.SLOPE_LEFT_2
                || t == TileType.SLOPE_RIGHT
                || t == TileType.SLOPE_RIGHT_2
                || t == TileType.SLOPE_RIGHT_GENTLE
                || t == TileType.SLOPE_RIGHT_GENTLE_2;
    }

    /**
     * Returns the ground height (in pixels from top of tile) at a given pixel X within a slope
     * tile. For SLOPE_LEFT (121): rises from left to right (low at left, high at right). For
     * SLOPE_RIGHT (91): falls from left to right (high at left, low at right). Returns TILE_SIZE
     * (full block) for non-slope tiles.
     */
    public double getSlopeHeight(int tileX, int tileY, double pixelX, int tileSize) {
        TileType t = getTile(tileX, tileY);
        double localX = pixelX - tileX * tileSize;
        double fraction = Math.max(0, Math.min(1, localX / tileSize));

        if (t == TileType.SLOPE_LEFT) {
            // First half of the gentle slope: 0 to 0.5 height
            return fraction * (tileSize / 2.0);
        } else if (t == TileType.SLOPE_LEFT_2) {
            // Second half of the gentle slope: 0.5 to 1.0 height
            return (tileSize / 2.0) + (fraction * (tileSize / 2.0));
        } else if (t == TileType.SLOPE_RIGHT_GENTLE) {
            // First half of gentle descending: 1.0 to 0.5 height
            return (tileSize / 2.0) + ((1.0 - fraction) * (tileSize / 2.0));
        } else if (t == TileType.SLOPE_RIGHT_GENTLE_2) {
            // Second half of gentle descending: 0.5 to 0.0 height
            return (1.0 - fraction) * (tileSize / 2.0);
        } else if (t == TileType.SLOPE_RIGHT) {
            // Steep descending: full height at left, zero at right (tile 91)
            return (1.0 - fraction) * tileSize;
        } else if (t == TileType.SLOPE_RIGHT_2) {
            // Steep ascending: zero at left, full height at right (tile 90)
            return fraction * tileSize;
        }
        return tileSize; // Full block
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

    private void placeSpikes(int startX, int endX) {
        if (startX < 0 || endX >= width) return;
        for (int x = startX; x <= endX; x++) {
            for (int y = 1; y < height; y++) {
                if (isSolid(x, y) && !isSolid(x, y - 1) && getTile(x, y) == TileType.GROUND) {
                    this.grid[x][y - 1] = TileType.SPIKES;
                    break;
                }
            }
        }
    }
}
