package com.campusgame.map.io;

import com.campusgame.map.data.BuildingData;
import com.campusgame.map.data.PathData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MapSaver {

    private final String savePath;
    private final Gson   gson;

    public MapSaver()                { this(MapLoader.DEFAULT_PATH); }
    public MapSaver(String savePath) {
        this.savePath = savePath;
        this.gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    }

    public boolean save(List<BuildingData> buildings, List<PathData> paths, MapJson.MapMeta meta) {
        try {
            meta.lastModified = Instant.now().toString();
            MapJson mj   = new MapJson();
            mj.mapMeta   = meta;
            mj.paths     = new ArrayList<>();
            mj.buildings = new ArrayList<>();

            if (paths != null)
                for (PathData pd : paths) mj.paths.add(MapJson.PathJson.from(pd));

            for (BuildingData b : buildings) mj.buildings.add(toJson(b));

            File saveFile = new File(savePath);
            saveFile.getParentFile().mkdirs();
            backup(saveFile);

            File tmp = new File(savePath + ".tmp");
            try (Writer w = new FileWriter(tmp)) { gson.toJson(mj, w); }
            Files.move(tmp.toPath(), saveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            System.out.printf("[MapSaver] Saved %d buildings, %d paths → %s%n",
                    buildings.size(), mj.paths.size(), savePath);
            return true;
        } catch (Exception e) {
            System.err.println("[MapSaver] FAILED: " + e.getMessage());
            return false;
        }
    }

    public boolean save(List<BuildingData> buildings) {
        return save(buildings, new ArrayList<>(),
                new MapJson.MapMeta("Main Campus", 3000, 2400, "Admin", "Saved by editor"));
    }

    private MapJson.BuildingJson toJson(BuildingData b) {
        MapJson.BuildingJson bj = new MapJson.BuildingJson();
        bj.name             = b.name;
        bj.colorARGB        = String.format("#%08X", b.colorARGB);
        bj.floors           = b.floors;
        bj.rotationDegrees  = b.rotationDegrees;
        bj.collisionEnabled = b.collisionEnabled;
        bj.tag              = b.tag;
        if (b.isPolygon()) { bj.polygonX = b.polygonX; bj.polygonZ = b.polygonZ; }
        else { bj.x = b.x; bj.z = b.z; bj.width = b.width; bj.depth = b.depth; }
        return bj;
    }

    private void backup(File f) {
        if (!f.exists()) return;
        try { Files.copy(f.toPath(), new File(savePath + ".bak").toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {}
    }

    public String getSavePath() { return savePath; }
}