package com.lost.database.game.world;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/** Рендер тайлів з тайлсету. */
public class TileMapRenderer {

    public static final int TILE_SIZE = 64;
    public static final int TILESET_SRC_SIZE = 32;
    public static final int TILESET_COLS = 16;

    public void render(
            GraphicsContext gc,
            int[][] layer,
            Image tileset,
            double camX,
            double camY,
            double screenW,
            double screenH,
            int srcTileSize,
            int srcColumns) {
        if (layer == null || tileset == null) return;

        int mapRows = layer.length;
        if (mapRows == 0) return;
        int mapCols = layer[0].length;

        int r0 = Math.max(0, (int) (camY / TILE_SIZE));
        int c0 = Math.max(0, (int) (camX / TILE_SIZE));
        int r1 = Math.min(r0 + (int) (screenH / TILE_SIZE) + 2, mapRows);
        int c1 = Math.min(c0 + (int) (screenW / TILE_SIZE) + 2, mapCols);

        for (int r = r0; r < r1; r++) {
            for (int c = c0; c < c1; c++) {
                int id = layer[r][c];
                if (id == 0) continue;

                double dstX = c * TILE_SIZE - camX;
                double dstY = r * TILE_SIZE - camY;

                int idx = id - 1;
                int srcCol = idx % srcColumns;
                int srcRow = idx / srcColumns;

                gc.drawImage(
                        tileset,
                        srcCol * srcTileSize,
                        srcRow * srcTileSize,
                        srcTileSize,
                        srcTileSize,
                        dstX,
                        dstY,
                        TILE_SIZE,
                        TILE_SIZE);
            }
        }
    }
}
