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
 *
 * Central data store for the exterior campus world.
 *
 * Holds:
 *   buildings  — all placed Building objects (wrapping BuildingData)
 *   paths      — walkway / road splines
 *   entrances  — Phase 5 door trigger data (links world pos → interior scene)
 *
 * Phase 5 additions (marked ── Phase 5):
 *   • entrances list + add/remove/get API
 */
public class CampusMap {

    // ── World dimensions ──────────────────────────────────────────────
    public static final int WORLD_WIDTH  = 3000;
    public static final int WORLD_HEIGHT = 2400;

    // ── Data ──────────────────────────────────────────────────────────
    private final List<Building>     buildings = new ArrayList<>();
    private final List<PathData>     paths     = new ArrayList<>();
    private final List<EntranceData> entrances = new ArrayList<>();   // Phase 5

    private MapLoader.LoadResult lastLoadResult = null;

    // ── Ground tile colours ───────────────────────────────────────────
    private static final Color GRASS_A = new Color(88,  144, 68);
    private static final Color GRASS_B = new Color(96,  154, 76);
    private static final int   TILE    = 120;

    // ── Constructor ───────────────────────────────────────────────────

    public CampusMap() {
        MapLoader loader = new MapLoader();
        lastLoadResult   = loader.load();

        if (lastLoadResult != null && lastLoadResult.buildings != null) {
            for (BuildingData bd : lastLoadResult.buildings)
                buildings.add(new Building(bd));

            if (lastLoadResult.paths != null)
                paths.addAll(lastLoadResult.paths);
        } else {
            buildDefaults();
        }
    }

    /** Fallback buildings when no campus.json exists yet. */
    private void buildDefaults() {
        addBuilding(new BuildingData("Main Hall",    400,  300, 280, 200, 4, 0xFF8899AA, 0, true, "academic"));
        addBuilding(new BuildingData("Library",      820,  260, 220, 180, 3, 0xFF99AA88, 0, true, "academic"));
        addBuilding(new BuildingData("Gymnasium",    410,  710, 300, 250, 2, 0xFF778899, 0, true, "gym"));
        addBuilding(new BuildingData("Science Bldg", 1120, 400, 240, 190, 5, 0xFF8899CC, 0, true, "academic"));
        addBuilding(new BuildingData("Cafeteria",    820,  700, 260, 200, 2, 0xFFAA9977, 0, true, "resource"));
        addBuilding(new BuildingData("Admin",        150,  560, 200, 160, 3, 0xFF99AABB, 0, true, "academic"));
    }

    // ─────────────────────────────────────────────────────────────────
    // GROUND + PATH RENDERING
    // ─────────────────────────────────────────────────────────────────

    /**
     * Draws the ground layer: tiled grass + subtle grid + campus paths.
     * Called by Renderer as Layer 1 (exterior only).
     */
    public void drawGround(Graphics2D g, int ox, int oy, int screenW, int screenH) {
        drawGrassTiles(g, ox, oy, screenW, screenH);
        drawPaths(g, ox, oy);
    }

    private void drawGrassTiles(Graphics2D g, int ox, int oy, int screenW, int screenH) {
        int startX = (ox / TILE) * TILE;
        int startY = (oy / TILE) * TILE;

        for (int wx = startX; wx < ox + screenW + TILE; wx += TILE) {
            for (int wy = startY; wy < oy + screenH + TILE; wy += TILE) {
                boolean alt = ((wx / TILE) + (wy / TILE)) % 2 == 0;
                g.setColor(alt ? GRASS_A : GRASS_B);
                g.fillRect(wx - ox, wy - oy, TILE, TILE);
            }
        }

        // Subtle grid lines
        g.setColor(new Color(0, 0, 0, 14));
        g.setStroke(new BasicStroke(0.5f));
        int startX2 = (ox / TILE) * TILE;
        int startY2 = (oy / TILE) * TILE;
        for (int wx = startX2; wx < ox + screenW + TILE; wx += TILE)
            g.drawLine(wx - ox, 0, wx - ox, screenH);
        for (int wy = startY2; wy < oy + screenH + TILE; wy += TILE)
            g.drawLine(0, wy - oy, screenW, wy - oy);
        g.setStroke(new BasicStroke(1f));
    }

    private void drawPaths(Graphics2D g, int ox, int oy) {
        for (PathData p : paths) {
            if (p.points == null || p.points.size() < 2) continue;
            if (!p.isValid()) continue;

            Color base = parseHexColor(p.colorHex);
            float stroke = Math.max(8f, p.width); // never thinner than 8px in world view

            // ── Shadow pass — dark outline makes the path pop off the grass ──
            g.setColor(new Color(0, 0, 0, 60));
            g.setStroke(new BasicStroke(stroke + 6f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < p.points.size() - 1; i++) {
                int x1 = (int) p.points.get(i  )[0] - ox;
                int y1 = (int) p.points.get(i  )[1] - oy;
                int x2 = (int) p.points.get(i+1)[0] - ox;
                int y2 = (int) p.points.get(i+1)[1] - oy;
                g.drawLine(x1, y1, x2, y2);
            }

            // ── Main path fill ────────────────────────────────────────────
            g.setColor(base);
            g.setStroke(new BasicStroke(stroke,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < p.points.size() - 1; i++) {
                int x1 = (int) p.points.get(i  )[0] - ox;
                int y1 = (int) p.points.get(i  )[1] - oy;
                int x2 = (int) p.points.get(i+1)[0] - ox;
                int y2 = (int) p.points.get(i+1)[1] - oy;
                g.drawLine(x1, y1, x2, y2);
            }

            // ── Centre line — subtle darker stripe for realism ────────────
            g.setColor(new Color(0, 0, 0, 25));
            g.setStroke(new BasicStroke(Math.max(1f, stroke * 0.15f),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < p.points.size() - 1; i++) {
                int x1 = (int) p.points.get(i  )[0] - ox;
                int y1 = (int) p.points.get(i  )[1] - oy;
                int x2 = (int) p.points.get(i+1)[0] - ox;
                int y2 = (int) p.points.get(i+1)[1] - oy;
                g.drawLine(x1, y1, x2, y2);
            }

            g.setStroke(new BasicStroke(6f));
        }
    }
    /**
     * Parses "#FFDCD7C8" (ARGB) or "FFDCD7C8" into a Color.
     * Falls back to sandy cream if parsing fails.
     */
    private static Color parseHexColor(String hex) {
        if (hex == null || hex.isEmpty()) return new Color(220, 215, 200);
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
                int r = (int)((v >> 16) & 0xFF);
                int gr= (int)((v >>  8) & 0xFF);
                int b = (int)( v        & 0xFF);
                return new Color(r, gr, b);
            }
        } catch (NumberFormatException e) {
            return new Color(220, 215, 200);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // BUILDING MANAGEMENT
    // ─────────────────────────────────────────────────────────────────

    /** Unmodifiable view — used by Renderer and minimap. */
    public List<Building> getBuildings() {
        return Collections.unmodifiableList(buildings);
    }

    /** Live mutable BuildingData list — used by EditorMode and MapSaver. */
    public List<BuildingData> getMutableBuildings() {
        List<BuildingData> out = new ArrayList<>();
        for (Building b : buildings) out.add(b.getData());
        return out;
    }

    public void addBuilding(BuildingData bd) {
        buildings.add(new Building(bd));
    }

    public void removeBuilding(BuildingData bd) {
        buildings.removeIf(b -> b.getData() == bd);
    }

    public void replaceBuilding(BuildingData old, BuildingData next) {
        for (int i = 0; i < buildings.size(); i++) {
            if (buildings.get(i).getData() == old) {
                buildings.set(i, new Building(next));
                return;
            }
        }
    }

    public void replaceAllBuildings(List<BuildingData> list) {
        buildings.clear();
        for (BuildingData bd : list) buildings.add(new Building(bd));
    }

    // ─────────────────────────────────────────────────────────────────
    // PATH MANAGEMENT
    // ─────────────────────────────────────────────────────────────────

    public List<PathData> getPaths()   { return Collections.unmodifiableList(paths); }
    public void addPath(PathData p)    { paths.add(p); }
    public void removePath(PathData p) { paths.remove(p); }

    // ─────────────────────────────────────────────────────────────────
    // ENTRANCE MANAGEMENT  (Phase 5)
    // ─────────────────────────────────────────────────────────────────

    public List<EntranceData> getEntrances() {
        return Collections.unmodifiableList(entrances);
    }

    public void addEntrance(EntranceData e)    { entrances.add(e); }
    public void removeEntrance(EntranceData e) { entrances.remove(e); }

    // ─────────────────────────────────────────────────────────────────
    // META
    // ─────────────────────────────────────────────────────────────────

    public MapLoader.LoadResult getLastLoadResult() { return lastLoadResult; }
}