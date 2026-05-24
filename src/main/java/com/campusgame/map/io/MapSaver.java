package com.campusgame.map.io;

import com.campusgame.map.data.BuildingData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * MAP SAVER (map/io/MapSaver.java)
 * Writes current BuildingData list → campus.json.
 *
 * Features:
 *  - Atomic write (.tmp → rename) so a crash never corrupts the file
 *  - One .bak backup of the previous save
 *  - Supports both rectangular and polygon buildings
 *  - colorARGB int → "#FFRRGGBB" hex string
 */
public class MapSaver {

    private final String savePath;
    private final Gson   gson;

    public MapSaver()                  { this(MapLoader.DEFAULT_PATH); }
    public MapSaver(String savePath)   {
        this.savePath = savePath;
        this.gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    }

    // ── Public API ────────────────────────────────────────────────────
    public boolean save(List<BuildingData> buildings,
                        List<MapJson.PathJson> paths,
                        MapJson.MapMeta meta) {
        try {
            meta.lastModified = Instant.now().toString();
            MapJson mj = new MapJson();
            mj.mapMeta   = meta;
            mj.paths     = paths != null ? paths : defaultPaths();
            mj.buildings = new ArrayList<>();
            for (BuildingData b : buildings) mj.buildings.add(toJson(b));

            File saveFile = new File(savePath);
            saveFile.getParentFile().mkdirs();
            backup(saveFile);

            File tmp = new File(savePath + ".tmp");
            try (Writer w = new FileWriter(tmp)) { gson.toJson(mj, w); }
            Files.move(tmp.toPath(), saveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            System.out.printf("[MapSaver] Saved %d buildings → %s%n", buildings.size(), savePath);
            return true;
        } catch (Exception e) {
            System.err.println("[MapSaver] FAILED: " + e.getMessage());
            return false;
        }
    }

    /** Convenience: save with minimal meta. */
    public boolean save(List<BuildingData> buildings) {
        return save(buildings, defaultPaths(),
                new MapJson.MapMeta("Main Campus", 3000, 2400, "Admin", "Saved by editor"));
    }

    // ── Conversion ────────────────────────────────────────────────────
    private MapJson.BuildingJson toJson(BuildingData b) {
        MapJson.BuildingJson bj = new MapJson.BuildingJson();
        bj.name             = b.name;
        bj.colorARGB        = String.format("#%08X", b.colorARGB);
        bj.floors           = b.floors;
        bj.rotationDegrees  = b.rotationDegrees;
        bj.collisionEnabled = b.collisionEnabled;
        bj.tag              = b.tag;
        if (b.isPolygon()) {
            bj.polygonX = b.polygonX;
            bj.polygonZ = b.polygonZ;
        } else {
            bj.x = b.x; bj.z = b.z;
            bj.width = b.width; bj.depth = b.depth;
        }
        return bj;
    }

    private void backup(File f) {
        if (!f.exists()) return;
        try { Files.copy(f.toPath(), new File(savePath + ".bak").toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {}
    }

    private List<MapJson.PathJson> defaultPaths() {
        List<MapJson.PathJson> p = new ArrayList<>();
        p.add(new MapJson.PathJson("loop-segment",  800,  680, 55, 0));
        p.add(new MapJson.PathJson("lrac-connector",820, 1160, 630, 55));
        return p;
    }

    public String getSavePath() { return savePath; }
}