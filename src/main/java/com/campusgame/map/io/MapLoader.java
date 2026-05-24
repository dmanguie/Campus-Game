package com.campusgame.map.io;

import com.campusgame.map.data.BuildingData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MAP LOADER (map/io/MapLoader.java)
 * Reads campus.json → List<BuildingData>.
 * Falls back to MapData hardcoded defaults if file is missing or corrupt.
 *
 * Load priority:
 *   1. assets/campus.json  (file system — editable by admin)
 *   2. campus.json on classpath (bundled in JAR)
 *   3. MapData.BUILDINGS   (hardcoded fallback)
 */
public class MapLoader {

    public static final String DEFAULT_PATH = "assets/campus.json";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // ── Public API ────────────────────────────────────────────────────
    public LoadResult load()              { return load(DEFAULT_PATH); }

    public LoadResult load(String path) {
        // 1. File system
        File f = new File(path);
        if (f.exists()) {
            try (Reader r = new FileReader(f)) {
                return parse(r, path);
            } catch (Exception e) {
                System.err.println("[MapLoader] Error reading " + path + ": " + e.getMessage());
            }
        }

        // 2. Classpath
        InputStream is = getClass().getClassLoader().getResourceAsStream("campus.json");
        if (is != null) {
            try (Reader r = new InputStreamReader(is)) {
                return parse(r, "classpath:campus.json");
            } catch (Exception e) {
                System.err.println("[MapLoader] Error reading classpath: " + e.getMessage());
            }
        }

        // 3. Fallback
        System.out.println("[MapLoader] campus.json not found — using MapData defaults.");
        return LoadResult.fallback();
    }

    // ── Parsing ───────────────────────────────────────────────────────
    private LoadResult parse(Reader reader, String src) {
        MapJson mj = gson.fromJson(reader, MapJson.class);
        if (mj == null || mj.buildings == null) return LoadResult.fallback();

        List<BuildingData> buildings = new ArrayList<>();
        for (MapJson.BuildingJson bj : mj.buildings) buildings.add(convert(bj));

        List<MapJson.PathJson> paths = mj.paths != null ? mj.paths : new ArrayList<>();
        MapJson.MapMeta meta = mj.mapMeta != null
                ? mj.mapMeta
                : new MapJson.MapMeta("Campus", 3000, 2400, "Unknown", "");

        System.out.printf("[MapLoader] Loaded %d buildings from %s%n", buildings.size(), src);
        return new LoadResult(buildings, paths, meta, src);
    }

    private BuildingData convert(MapJson.BuildingJson bj) {
        int color = parseColor(bj.colorARGB);
        String tag = bj.tag != null ? bj.tag : "building";

        // Polygon building
        if (bj.polygonX != null && bj.polygonZ != null) {
            return new BuildingData(bj.name, bj.polygonX, bj.polygonZ,
                    bj.floors, color, bj.rotationDegrees, bj.collisionEnabled, tag);
        }

        // Rectangular building
        return new BuildingData(bj.name, bj.x, bj.z, bj.width, bj.depth,
                bj.floors, color, bj.rotationDegrees, bj.collisionEnabled, tag);
    }

    /** Parses "#FFRRGGBB" or "#RRGGBB" → packed int. Gray on error. */
    public static int parseColor(String hex) {
        if (hex == null || hex.isEmpty()) return 0xFF888888;
        try {
            String c = hex.startsWith("#") ? hex.substring(1) : hex;
            if (c.length() == 6) c = "FF" + c;
            return (int) Long.parseLong(c, 16);
        } catch (NumberFormatException e) {
            return 0xFF888888;
        }
    }

    // ── Result ────────────────────────────────────────────────────────
    public static class LoadResult {
        public final List<BuildingData>     buildings;
        public final List<MapJson.PathJson> paths;
        public final MapJson.MapMeta        meta;
        public final String                 source;

        public LoadResult(List<BuildingData> buildings,
                          List<MapJson.PathJson> paths,
                          MapJson.MapMeta meta, String source) {
            this.buildings = buildings;
            this.paths     = paths;
            this.meta      = meta;
            this.source    = source;
        }

        public static LoadResult fallback() {
            return new LoadResult(
                    new ArrayList<>(com.campusgame.map.data.MapData.BUILDINGS),
                    new ArrayList<>(),
                    new MapJson.MapMeta("Campus (Default)", 3000, 2400, "System", "Fallback"),
                    "fallback"
            );
        }
    }
}