package com.campusgame.map.io;

import com.campusgame.map.data.EntranceData;
import com.campusgame.map.data.PathData;
import java.util.ArrayList;
import java.util.List;

public class MapJson {

    public MapMeta            mapMeta;
    public List<PathJson>     paths;
    public List<BuildingJson> buildings;
    public List<EntranceJson> entrances;   // Phase 5

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
        public float       width;
        public String      colorHex;
        public List<Float> pointsX;
        public List<Float> pointsZ;

        public PathJson() {}

        public static PathJson from(PathData pd) {
            PathJson j  = new PathJson();
            j.name      = pd.name;
            j.width     = pd.width;
            j.colorHex  = pd.colorHex;
            j.pointsX   = new ArrayList<>();
            j.pointsZ   = new ArrayList<>();
            for (float[] p : pd.points) { j.pointsX.add(p[0]); j.pointsZ.add(p[1]); }
            return j;
        }

        public PathData toPathData() {
            PathData pd = new PathData(name, width, colorHex);
            if (pointsX != null && pointsZ != null) {
                int n = Math.min(pointsX.size(), pointsZ.size());
                for (int i = 0; i < n; i++) pd.addPoint(pointsX.get(i), pointsZ.get(i));
            }
            return pd;
        }
    }

    // ── Entrance JSON record ──────────────────────────────────────────
    public static class EntranceJson {
        public String id;
        public String buildingName;
        public String label;
        public float  worldX;
        public float  worldZ;
        public float  triggerRadius;
        public String interiorSceneId;
        public float  interiorSpawnX;
        public float  interiorSpawnZ;
        public float  exteriorSpawnX;
        public float  exteriorSpawnZ;

        public EntranceJson() {}

        public static EntranceJson from(EntranceData e) {
            EntranceJson j    = new EntranceJson();
            j.id              = e.id;
            j.buildingName    = e.buildingName;
            j.label           = e.label;
            j.worldX          = e.worldX;
            j.worldZ          = e.worldZ;
            j.triggerRadius   = e.triggerRadius;
            j.interiorSceneId = e.interiorSceneId;
            j.interiorSpawnX  = e.interiorSpawnX;
            j.interiorSpawnZ  = e.interiorSpawnZ;
            j.exteriorSpawnX  = e.exteriorSpawnX;
            j.exteriorSpawnZ  = e.exteriorSpawnZ;
            return j;
        }

        public EntranceData toEntranceData() {
            EntranceData e    = new EntranceData(id, buildingName, label,
                    worldX, worldZ, interiorSceneId,
                    interiorSpawnX, interiorSpawnZ,
                    exteriorSpawnX, exteriorSpawnZ);
            e.triggerRadius   = triggerRadius;
            return e;
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