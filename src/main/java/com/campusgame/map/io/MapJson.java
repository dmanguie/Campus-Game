package com.campusgame.map.io;

import com.campusgame.map.data.EntranceData;
import com.campusgame.map.data.PathData;
import java.util.ArrayList;
import java.util.List;

/**
 * MAP JSON (map/io/MapJson.java)
 * --------------------------------
 * Gson-friendly POJO that mirrors campus.json exactly.
 * No domain logic — pure serialization boundary.
 *
 * Phase 5 : added EntranceJson for door/entrance data.
 * Phase 6 : added displayName + targetDisplayName to EntranceJson for runtime rename.
 *
 * PathJson field alignment with PathData:
 *   PathJson.width    ↔ PathData.width
 *   PathJson.colorHex ↔ PathData.colorHex
 *
 * NOTE: campus.json files saved by older versions used "strokeWidth" and
 * "colorARGB". The loader handles both spellings via graceful fallback.
 */
public class MapJson {

    public MapMeta            mapMeta;
    public List<PathJson>     paths;
    public List<BuildingJson> buildings;
    public List<EntranceJson> entrances;

    // ── Map metadata ─────────────────────────────────────────────────
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

    // ── Path JSON ─────────────────────────────────────────────────────
    public static class PathJson {
        public String      name;
        public float       width;       // matches PathData.width
        public String      colorHex;    // matches PathData.colorHex

        // Legacy field names — Gson will populate these if present in old JSON
        public float       strokeWidth; // old name for width
        public String      colorARGB;   // old name for colorHex

        public List<Float> pointsX;
        public List<Float> pointsZ;

        public PathJson() {}

        /** Convert a live PathData → PathJson for saving. */
        public static PathJson from(PathData pd) {
            PathJson j   = new PathJson();
            j.name       = pd.name;
            j.width      = pd.width;
            j.colorHex   = pd.colorHex;
            j.pointsX    = new ArrayList<>();
            j.pointsZ    = new ArrayList<>();
            for (float[] p : pd.points) { j.pointsX.add(p[0]); j.pointsZ.add(p[1]); }
            return j;
        }

        /**
         * Convert a deserialized PathJson → PathData.
         * Handles both new field names (width/colorHex) and old names
         * (strokeWidth/colorARGB) so existing campus.json files still load.
         */
        public PathData toPathData() {
            float  resolvedWidth    = (width > 0) ? width : (strokeWidth > 0 ? strokeWidth : 55f);
            String resolvedColorHex = (colorHex != null && !colorHex.isEmpty())
                    ? colorHex
                    : (colorARGB != null ? colorARGB : "#FFDCD7C8");

            PathData pd = new PathData(name, resolvedWidth, resolvedColorHex);
            if (pointsX != null && pointsZ != null) {
                int n = Math.min(pointsX.size(), pointsZ.size());
                for (int i = 0; i < n; i++) pd.addPoint(pointsX.get(i), pointsZ.get(i));
            }
            return pd;
        }
    }

    // ── Entrance JSON ─────────────────────────────────────────────────
    public static class EntranceJson {
        public String id;
        public String buildingName;
        public String label;

        // Phase 6: rename fields — written on save, read on load.
        // Absent in old JSON → Gson leaves them null → fallback to label.
        public String displayName;
        public String targetDisplayName;

        public float  worldX;
        public float  worldZ;
        public float  triggerRadius;
        public String interiorSceneId;
        public float  interiorSpawnX;
        public float  interiorSpawnZ;
        public float  exteriorSpawnX;
        public float  exteriorSpawnZ;

        public EntranceJson() {}

        /** EntranceData → EntranceJson for saving. */
        public static EntranceJson from(EntranceData e) {
            EntranceJson j      = new EntranceJson();
            j.id                = e.id;
            j.buildingName      = e.buildingName;
            j.label             = e.label;
            j.displayName       = e.displayName;
            j.targetDisplayName = e.targetDisplayName;
            j.worldX            = e.worldX;
            j.worldZ            = e.worldZ;
            j.triggerRadius     = e.triggerRadius;
            j.interiorSceneId   = e.interiorSceneId;
            j.interiorSpawnX    = e.interiorSpawnX;
            j.interiorSpawnZ    = e.interiorSpawnZ;
            j.exteriorSpawnX    = e.exteriorSpawnX;
            j.exteriorSpawnZ    = e.exteriorSpawnZ;
            return j;
        }

        /** EntranceJson → EntranceData for loading. Gracefully handles old JSON without rename fields. */
        public EntranceData toEntranceData() {
            EntranceData e  = new EntranceData(
                    id, buildingName, label,
                    worldX, worldZ,
                    interiorSceneId,
                    interiorSpawnX, interiorSpawnZ,
                    exteriorSpawnX, exteriorSpawnZ);
            e.triggerRadius = triggerRadius > 0 ? triggerRadius : 55f;

            // Restore rename fields — fall back gracefully if absent (old JSON)
            e.displayName       = (displayName != null && !displayName.isBlank())
                    ? displayName : label;
            e.targetDisplayName = (targetDisplayName != null) ? targetDisplayName : "";
            return e;
        }
    }

    // ── Building JSON ─────────────────────────────────────────────────
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