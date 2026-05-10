package com.lost.database.game.world;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/** Завантажувач TMX карт (CSV формат). */
public class TileMapLoader {

    public static List<int[][]> loadTMX(String resourcePath) {
        List<String> csvBlocks = new ArrayList<>();
        StringBuilder cur = null;
        boolean inData = false;

        try {
            InputStream is = TileMapLoader.class.getResourceAsStream(resourcePath);
            if (is == null) {
                System.err.println("Map not found: " + resourcePath);
                return new ArrayList<>();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (t.startsWith("<data encoding=\"csv\">")) {
                    inData = true;
                    cur = new StringBuilder();
                } else if (t.equals("</data>") && inData) {
                    inData = false;
                    csvBlocks.add(cur.toString());
                    cur = null;
                } else if (inData && cur != null) {
                    cur.append(t).append("\n");
                }
            }
            br.close();
        } catch (Exception e) {
            System.err.println("Error loading TMX from " + resourcePath + ": " + e.getMessage());
            return new ArrayList<>();
        }

        List<int[][]> layers = new ArrayList<>();
        for (String csv : csvBlocks) {
            layers.add(parseCSV(csv));
        }
        return layers;
    }

    public static int[][] parseCSV(String csv) {
        String[] rows = csv.trim().split("\n");
        int rCount = rows.length;
        if (rCount == 0) return new int[0][0];

        int cCount = rows[0].trim().replaceAll(",$", "").split(",").length;
        int[][] map = new int[rCount][cCount];

        for (int r = 0; r < rCount; r++) {
            String[] cells = rows[r].trim().replaceAll(",$", "").split(",");
            for (int c = 0; c < Math.min(cells.length, cCount); c++) {
                try {
                    map[r][c] = Integer.parseInt(cells[c].trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return map;
    }
}
