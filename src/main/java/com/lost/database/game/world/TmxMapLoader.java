package com.lost.database.game.world;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Завантажувач TMX (Tiled) карт. */
public class TmxMapLoader {
    public static class TmxObject {
        public int gid;
        public double x, y, width, height;

        public TmxObject(int gid, double x, double y, double w, double h) {
            this.gid = gid;
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }
    }

    public static class TmxData {
        public List<int[][]> layers = new ArrayList<>();
        public List<TmxObject> objects = new ArrayList<>();
    }

    public static TmxData loadMap(String path) {
        TmxData result = new TmxData();
        try {
            InputStream is = TmxMapLoader.class.getResourceAsStream(path);
            if (is == null) {
                System.err.println("Map not found: " + path);
                return null;
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(is);

            Element mapElement = (Element) doc.getElementsByTagName("map").item(0);
            int width = Integer.parseInt(mapElement.getAttribute("width"));
            int height = Integer.parseInt(mapElement.getAttribute("height"));

            NodeList dataList = doc.getElementsByTagName("data");
            for (int i = 0; i < dataList.getLength(); i++) {
                int[][] grid = new int[height][width];
                Element dataElement = (Element) dataList.item(i);
                String csv = dataElement.getTextContent().trim();
                String[] rows = csv.split("\n");

                int y = 0;
                for (String row : rows) {
                    row = row.trim();
                    if (row.endsWith(",")) row = row.substring(0, row.length() - 1);
                    if (row.isEmpty()) continue;

                    String[] tiles = row.split(",");
                    for (int x = 0; x < Math.min(width, tiles.length); x++) {
                        grid[y][x] = Integer.parseInt(tiles[x].trim());
                    }
                    y++;
                    if (y >= height) break;
                }
                result.layers.add(grid);
            }

            NodeList objList = doc.getElementsByTagName("object");
            for (int i = 0; i < objList.getLength(); i++) {
                Element obj = (Element) objList.item(i);
                if (obj.hasAttribute("gid")) {
                    int gid = Integer.parseInt(obj.getAttribute("gid"));
                    double x = Double.parseDouble(obj.getAttribute("x"));
                    double y = Double.parseDouble(obj.getAttribute("y"));
                    double w =
                            obj.hasAttribute("width")
                                    ? Double.parseDouble(obj.getAttribute("width"))
                                    : 32;
                    double h =
                            obj.hasAttribute("height")
                                    ? Double.parseDouble(obj.getAttribute("height"))
                                    : 32;
                    result.objects.add(new TmxObject(gid, x, y, w, h));
                }
            }

            return result;
        } catch (Exception e) {
            System.err.println("Error loading TMX map: " + e.getMessage());
            return null;
        }
    }
}
