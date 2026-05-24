package com.campusgame.map;

import com.campusgame.map.data.BuildingData;
import com.campusgame.map.data.MapData;
import com.campusgame.map.io.MapLoader;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CAMPUS MAP (map/CampusMap.java)
 * Phase 3: loads from campus.json via MapLoader; falls back to MapData if missing.
 * Exposes mutable building list for the editor (addBuilding, removeBuilding, etc.).
 * Read-only view still returned by getBuildings() so Renderer/CollisionManager are unchanged.
 */
public class CampusMap {

    public static final int WORLD_WIDTH  = MapData.WORLD_WIDTH;
    public static final int WORLD_HEIGHT = MapData.WORLD_HEIGHT;

    // Mutable list — editor can add/remove entries
    private final List<Building>     buildings     = new ArrayList<>();
    private final List<BuildingData> buildingDatas = new ArrayList<>(); // mirrors buildings

    // Cached loader result (paths + meta kept for MapSaver)
    private MapLoader.LoadResult lastLoadResult;

    public static final Color COLOR_GRASS  = new Color(107, 161,  83);
    public static final Color COLOR_PATH   = new Color(220, 215, 200);
    public static final Color COLOR_BORDER = new Color( 60,  80,  40);

    // ── Construction ─────────────────────────────────────────────────
    public CampusMap() {
        MapLoader loader = new MapLoader();
        lastLoadResult   = loader.load();           // tries assets/campus.json first
        applyLoadResult(lastLoadResult);
    }

    private void applyLoadResult(MapLoader.LoadResult result) {
        buildings.clear();
        buildingDatas.clear();
        for (BuildingData bd : result.buildings) {
            buildings.add(new Building(bd));
            buildingDatas.add(bd);
        }
        System.out.printf("[CampusMap] Loaded %d buildings from: %s%n",
                buildings.size(), result.source);
    }

    // ── Editor API (Phase 3) ─────────────────────────────────────────

    /** Add a brand-new building (called by EditorMode on click-to-place). */
    public void addBuilding(BuildingData bd) {
        buildingDatas.add(bd);
        buildings.add(new Building(bd));
    }

    /** Remove a building by reference equality on its BuildingData. */
    public void removeBuilding(BuildingData bd) {
        buildingDatas.removeIf(b -> b == bd);
        buildings.removeIf(b -> b.getData() == bd);
    }

    /**
     * Replace entire building list (used by undo).
     * Rebuilds Building wrappers from the snapshot.
     */
    public void replaceAllBuildings(List<BuildingData> snapshot) {
        buildings.clear();
        buildingDatas.clear();
        for (BuildingData bd : snapshot) {
            buildings.add(new Building(bd));
            buildingDatas.add(bd);
        }
    }

    /**
     * Mutable copy of the BuildingData list.
     * Used by EditorMode (undo snapshots) and MapSaver.
     */
    public List<BuildingData> getMutableBuildings() {
        return new ArrayList<>(buildingDatas);
    }

    /** Cached loader result — provides paths + meta for MapSaver. */
    public MapLoader.LoadResult getLastLoadResult() { return lastLoadResult; }

    // ── Read-only view (unchanged — Renderer + CollisionManager use this) ──
    public List<Building> getBuildings() {
        return Collections.unmodifiableList(buildings);
    }

    // ── Ground drawing (unchanged from your version) ─────────────────
    public void drawGround(Graphics2D g, int ox, int oy, int sw, int sh) {
        g.setColor(COLOR_GRASS);
        g.fillRect(-ox, -oy, WORLD_WIDTH, WORLD_HEIGHT);

        drawCampusPaths(g, ox, oy);

        g.setColor(COLOR_BORDER);
        g.setStroke(new BasicStroke(6f));
        g.drawRect(10 - ox, 10 - oy, WORLD_WIDTH - 20, WORLD_HEIGHT - 20);
        g.setStroke(new BasicStroke(1f));

        drawDebugGrid(g, ox, oy);
    }

    private void drawCampusPaths(Graphics2D g, int ox, int oy) {
        Stroke old = g.getStroke();
        g.setColor(COLOR_PATH);
        g.setStroke(new BasicStroke(55f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int[] lx = {800-ox,1400-ox,1520-ox,1480-ox,1450-ox, 820-ox, 780-ox, 800-ox};
        int[] ly = {680-oy, 740-oy, 820-oy,1470-oy,1570-oy,1570-oy,1110-oy, 680-oy};
        for (int i = 0; i < lx.length - 1; i++) g.drawLine(lx[i],ly[i],lx[i+1],ly[i+1]);

        g.drawLine( 820-ox, 1160-oy, 1450-ox, 1160-oy);  // LRAC connector
        g.drawLine(1450-ox, 1570-oy, 1500-ox, 1850-oy);  // Front gate
        g.drawLine(1510-ox,  910-oy, 1680-ox,  910-oy);  // ACAD connector
        g.drawLine(1500-ox, 1230-oy, 1600-ox, 1230-oy);  // GLE connector

        g.setStroke(old);
    }

    private void drawDebugGrid(Graphics2D g, int ox, int oy) {
        g.setColor(new Color(0, 0, 0, 15));
        g.setStroke(new BasicStroke(1f));
        for (int gx = 0; gx < WORLD_WIDTH;  gx += 100)
            g.drawLine(gx-ox, -oy, gx-ox, WORLD_HEIGHT-oy);
        for (int gy = 0; gy < WORLD_HEIGHT; gy += 100)
            g.drawLine(-ox, gy-oy, WORLD_WIDTH-ox, gy-oy);
    }

    public int getWorldWidth()  { return WORLD_WIDTH;  }
    public int getWorldHeight() { return WORLD_HEIGHT; }
}