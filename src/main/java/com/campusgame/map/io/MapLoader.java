package com.campusgame.map.io;

import com.campusgame.map.data.BuildingData;
import com.campusgame.map.data.EntranceData;
import com.campusgame.map.data.PathData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MapLoader {

    public static final String DEFAULT_PATH = "assets/campus.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public LoadResult load()             { return load(DEFAULT_PATH); }

    public LoadResult load(String path) {
        File f = new File(path);
        if (f.exists()) {
            try (Reader r = new FileReader(f)) { return parse(r, path); }
            catch (Exception e) { System.err.println("[MapLoader] Error: " + e.getMessage()); }
        }
        InputStream is = getClass().getClassLoader().getResourceAsStream("campus.json");
        if (is != null) {
            try (Reader r = new InputStreamReader(is)) { return parse(r, "classpath:campus.json"); }
            catch (Exception e) { System.err.println("[MapLoader] Classpath error: " + e.getMessage()); }
        }
        System.out.println("[MapLoader] campus.json not found — using MapData defaults.");
        return LoadResult.fallback();
    }

    private LoadResult parse(Reader reader, String src) {
        MapJson mj = gson.fromJson(reader, MapJson.class);
        if (mj == null || mj.buildings == null) return LoadResult.fallback();

        List<BuildingData> buildings = new ArrayList<>();
        for (MapJson.BuildingJson bj : mj.buildings) buildings.add(convert(bj));

        List<PathData> paths = new ArrayList<>();
        if (mj.paths != null)
            for (MapJson.PathJson pj : mj.paths) paths.add(pj.toPathData());

        List<EntranceData> entrances = new ArrayList<>();
        if (mj.entrances != null)
            for (MapJson.EntranceJson ej : mj.entrances) entrances.add(ej.toEntranceData());

        MapJson.MapMeta meta = mj.mapMeta != null
                ? mj.mapMeta
                : new MapJson.MapMeta("Campus", 3000, 2400, "Unknown", "");

        System.out.printf("[MapLoader] Loaded %d buildings, %d paths, %d entrances from %s%n",
                buildings.size(), paths.size(), entrances.size(), src);
        return new LoadResult(buildings, paths, entrances, meta, src);
    }

    private BuildingData convert(MapJson.BuildingJson bj) {
        int    color = parseColor(bj.colorARGB);
        String tag   = bj.tag != null ? bj.tag : "building";
        if (bj.polygonX != null && bj.polygonZ != null) {
            return new BuildingData(bj.name, bj.polygonX, bj.polygonZ,
                    bj.floors, color, bj.rotationDegrees, bj.collisionEnabled, tag);
        }
        return new BuildingData(bj.name, bj.x, bj.z, bj.width, bj.depth,
                bj.floors, color, bj.rotationDegrees, bj.collisionEnabled, tag);
    }

    public static int parseColor(String hex) {
        if (hex == null || hex.isEmpty()) return 0xFF888888;
        try {
            String c = hex.startsWith("#") ? hex.substring(1) : hex;
            if (c.length() == 6) c = "FF" + c;
            return (int) Long.parseLong(c, 16);
        } catch (NumberFormatException e) { return 0xFF888888; }
    }

    public static class LoadResult {
        public final List<BuildingData> buildings;
        public final List<PathData>     paths;
        public final List<EntranceData> entrances;
        public final MapJson.MapMeta    meta;
        public final String             source;

        public LoadResult(List<BuildingData> buildings, List<PathData> paths,
                          List<EntranceData> entrances,
                          MapJson.MapMeta meta, String source) {
            this.buildings = buildings;
            this.paths     = paths;
            this.entrances = entrances;
            this.meta      = meta;
            this.source    = source;
        }

        // Backward-compat constructor for fallback (no entrances)
        public LoadResult(List<BuildingData> buildings, List<PathData> paths,
                          MapJson.MapMeta meta, String source) {
            this(buildings, paths, new ArrayList<>(), meta, source);
        }

        public static LoadResult fallback() {
            return new LoadResult(
                    new ArrayList<>(com.campusgame.map.data.MapData.BUILDINGS),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new MapJson.MapMeta("Campus (Default)", 3000, 2400, "System", "Fallback"),
                    "fallback");
        }
    }
}