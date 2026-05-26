package com.campusgame.map;

import com.campusgame.map.data.BuildingData;
import com.campusgame.map.data.EntranceData;
import com.campusgame.map.data.PathData;
import com.campusgame.map.io.MapLoader;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CAMPUS MAP (map/CampusMap.java)
 * ---------------------------------
 * Central data store for the exterior campus world.
 *
 * Phase 5: added EntranceData list + add/remove/get API.
 * PathData field fix: uses path.width and path.colorHex (not strokeWidth/colorARGB).
 * Loads paths + entrances from campus.json via MapLoader.
 */
public class CampusMap {

    public static final int WORLD_WIDTH  = 3000;
    public static final int WORLD_HEIGHT = 2400;

    // ── Data ──────────────────────────────────────────────────────────
    private final List<Building>     buildings        = new ArrayList<>();
    private final List<BuildingData> mutableBuildings = new ArrayList<>();
    private final List<PathData>     paths            = new ArrayList<>();
    private final List<EntranceData> entrances        = new ArrayList<>();  // Phase 5

    private MapLoader.LoadResult lastLoadResult;

    // ── Ground colors (fallback drawing) ─────────────────────────────
    public static final Color COLOR_GRASS  = new Color(107, 161,  83);
    public static final Color COLOR_PATH   = new Color(220, 215, 200);
    public static final Color COLOR_BORDER = new Color( 60,  80,  40);

    // ── Constructor ───────────────────────────────────────────────────
    public CampusMap() {
        MapLoader loader = new MapLoader();
        lastLoadResult   = loader.load();

        mutableBuildings.addAll(lastLoadResult.buildings);
        rebuildBuildings();

        if (lastLoadResult.paths != null && !lastLoadResult.paths.isEmpty()) {
            paths.addAll(lastLoadResult.paths);
        } else {
            seedDefaultPaths();
        }

        // Phase 5: load entrances (empty list if older JSON without them)
        if (lastLoadResult.entrances != null) {
            entrances.addAll(lastLoadResult.entrances);
        }
    }

    // ── Building editor API ───────────────────────────────────────────
    public List<BuildingData> getMutableBuildings() { return mutableBuildings; }

    public void addBuilding(BuildingData data) {
        mutableBuildings.add(data); rebuildBuildings();
    }
    public void removeBuilding(BuildingData data) {
        mutableBuildings.remove(data); rebuildBuildings();
    }
    public void replaceBuilding(BuildingData old, BuildingData next) {
        int i = mutableBuildings.indexOf(old);
        if (i >= 0) { mutableBuildings.set(i, next); rebuildBuildings(); }
    }
    public void replaceAllBuildings(List<BuildingData> newList) {
        mutableBuildings.clear(); mutableBuildings.addAll(newList); rebuildBuildings();
    }

    private void rebuildBuildings() {
        buildings.clear();
        for (BuildingData bd : mutableBuildings) buildings.add(new Building(bd));
    }

    // ── Path editor API ───────────────────────────────────────────────
    public List<PathData> getPaths()   { return paths; }
    public void addPath(PathData p)    { paths.add(p); }
    public void removePath(PathData p) { paths.remove(p); }

    // ── Entrance API (Phase 5) ────────────────────────────────────────
    public List<EntranceData> getEntrances()       { return Collections.unmodifiableList(entrances); }
    public void addEntrance(EntranceData e)        { entrances.add(e); }
    public void removeEntrance(EntranceData e)     { entrances.remove(e); }

    // ── Default path seeding (when no campus.json exists yet) ─────────
    private void seedDefaultPaths() {
        PathData mainLoop = new PathData("Main Loop", 55f, "#FFDCD7C8");
        mainLoop.addPoint(800, 680);  mainLoop.addPoint(1400, 740);
        mainLoop.addPoint(1520, 820); mainLoop.addPoint(1480, 1470);
        mainLoop.addPoint(1450, 1570);mainLoop.addPoint(820,  1570);
        mainLoop.addPoint(780,  1110);mainLoop.addPoint(800,  680);
        paths.add(mainLoop);

        PathData lrac = new PathData("LRAC Connector", 55f, "#FFDCD7C8");
        lrac.addPoint(820, 1160); lrac.addPoint(1450, 1160);
        paths.add(lrac);

        PathData gate = new PathData("Front Gate", 55f, "#FFDCD7C8");
        gate.addPoint(1450, 1570); gate.addPoint(1500, 1850);
        paths.add(gate);

        PathData acad = new PathData("ACAD Connector", 55f, "#FFDCD7C8");
        acad.addPoint(1510, 910); acad.addPoint(1680, 910);
        paths.add(acad);

        PathData gle = new PathData("GLE Connector", 55f, "#FFDCD7C8");
        gle.addPoint(1500, 1230); gle.addPoint(1600, 1230);
        paths.add(gle);
    }

    // ── Read-only views ───────────────────────────────────────────────
    public List<Building>        getBuildings()       { return Collections.unmodifiableList(buildings); }
    public MapLoader.LoadResult  getLastLoadResult()  { return lastLoadResult; }
    public int                   getWorldWidth()      { return WORLD_WIDTH; }
    public int                   getWorldHeight()     { return WORLD_HEIGHT; }

    // ── Ground drawing ────────────────────────────────────────────────
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
        for (PathData path : paths) {
            if (!path.isValid()) continue;
            // FIX: use path.colorHex (not path.colorARGB)
            Color c = parseHexColor(path.colorHex, COLOR_PATH);
            g.setColor(c);
            // FIX: use path.width (not path.strokeWidth)
            g.setStroke(new BasicStroke(path.width,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < path.points.size() - 1; i++) {
                int x1 = (int) path.points.get(i  )[0] - ox;
                int y1 = (int) path.points.get(i  )[1] - oy;
                int x2 = (int) path.points.get(i+1)[0] - ox;
                int y2 = (int) path.points.get(i+1)[1] - oy;
                g.drawLine(x1, y1, x2, y2);
            }
        }
        g.setStroke(old);
    }

    /**
     * Parses "#FFRRGGBB" (8-char ARGB) or "#RRGGBB" (6-char RGB) hex string.
     * Returns fallback color if parsing fails.
     */
    private Color parseHexColor(String hex, Color fallback) {
        if (hex == null || hex.isEmpty()) return fallback;
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            long v = Long.parseLong(clean, 16);
            if (clean.length() == 8) {
                int a = (int)((v >> 24) & 0xFF);
                int r = (int)((v >> 16) & 0xFF);
                int gr= (int)((v >>  8) & 0xFF);
                int b = (int)( v        & 0xFF);
                return new Color(r, gr, b, a);
            } else {
                return new Color(
                        (int)((v >> 16) & 0xFF),
                        (int)((v >>  8) & 0xFF),
                        (int)( v        & 0xFF));
            }
        } catch (Exception e) { return fallback; }
    }

    private void drawDebugGrid(Graphics2D g, int ox, int oy) {
        g.setColor(new Color(0, 0, 0, 15));
        g.setStroke(new BasicStroke(1f));
        for (int gx = 0; gx < WORLD_WIDTH;  gx += 100)
            g.drawLine(gx - ox, -oy, gx - ox, WORLD_HEIGHT - oy);
        for (int gy = 0; gy < WORLD_HEIGHT; gy += 100)
            g.drawLine(-ox, gy - oy, WORLD_WIDTH - ox, gy - oy);
    }
}