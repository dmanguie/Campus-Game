package com.campusgame.map.io;

import com.campusgame.map.data.PathData;
import java.util.ArrayList;
import java.util.List;

public class MapJson {

    public MapMeta            mapMeta;
    public List<PathJson>     paths;
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
            this.name         = name;
            this.worldWidth   = w;
            this.worldHeight  = h;
            this.author       = author;
            this.description  = desc;
            this.lastModified = java.time.Instant.now().toString();
        }
    }

    public static class PathJson {
        public String      name;
        public float       strokeWidth;
        public String      colorARGB;
        public List<Float> pointsX;
        public List<Float> pointsZ;

        public PathJson() {}

        public static PathJson from(PathData pd) {
            PathJson j    = new PathJson();
            j.name        = pd.name;
            j.strokeWidth = pd.strokeWidth;
            j.colorARGB   = pd.colorARGB;
            j.pointsX     = new ArrayList<>();
            j.pointsZ     = new ArrayList<>();
            for (float[] p : pd.points) { j.pointsX.add(p[0]); j.pointsZ.add(p[1]); }
            return j;
        }

        public PathData toPathData() {
            PathData pd = new PathData(name, strokeWidth, colorARGB);
            if (pointsX != null && pointsZ != null) {
                int n = Math.min(pointsX.size(), pointsZ.size());
                for (int i = 0; i < n; i++) pd.addPoint(pointsX.get(i), pointsZ.get(i));
            }
            return pd;
        }
    }

    public static class BuildingJson {
        public String  name;
        public float   x, z, width, depth;
        public int     floors;
        public String  colorARGB;
        public float   rotationDegrees;
        public boolean collisionEnabled;
        public String  tag;
        public int[]   polygonX;
        public int[]   polygonZ;

        public BuildingJson() {}
    }
}