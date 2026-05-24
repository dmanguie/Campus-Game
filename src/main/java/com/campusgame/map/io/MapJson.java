package com.campusgame.map.io;

import java.util.List;

/**
 * MAP JSON (map/io/MapJson.java)
 * Gson-friendly POJO that mirrors campus.json exactly.
 * No domain logic here — pure serialization boundary.
 */
public class MapJson {

    public MapMeta         mapMeta;
    public List<PathJson>  paths;
    public List<BuildingJson> buildings;

    public static class MapMeta {
        public String name;
        public int    worldWidth;
        public int    worldHeight;
        public String author;
        public String lastModified;
        public String description;

        public MapMeta() {}
        public MapMeta(String name, int w, int h, String author, String desc) {
            this.name        = name;
            this.worldWidth  = w;
            this.worldHeight = h;
            this.author      = author;
            this.description = desc;
            this.lastModified = java.time.Instant.now().toString();
        }
    }

    public static class PathJson {
        public String type;   // "horizontal" | "vertical"
        public float  x, z, width, depth;

        public PathJson() {}
        public PathJson(String type, float x, float z, float w, float d) {
            this.type = type; this.x = x; this.z = z; this.width = w; this.depth = d;
        }
    }

    public static class BuildingJson {
        public String  name;
        public float   x, z, width, depth;
        public int     floors;
        public String  colorARGB;       // "#FFRRGGBB"
        public float   rotationDegrees;
        public boolean collisionEnabled;
        public String  tag;
        // Polygon support
        public int[]   polygonX;
        public int[]   polygonZ;

        public BuildingJson() {}
    }
}