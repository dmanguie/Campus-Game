package com.campusgame.map;

import com.campusgame.map.data.MapData;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CAMPUS MAP (map/CampusMap.java)
 * --------------------------------
 * The 2D Swing world container.
 *
 * PHASE 2 CHANGE:
 *   Building data is now loaded from MapData (map/data/MapData.java).
 *   The old BUILDING_DATA array has been removed.
 *   To add/edit buildings: edit MapData.BUILDINGS — this class auto-updates.
 *
 * Everything else is UNCHANGED:
 *   - World dimensions same
 *   - drawGround() same
 *   - getBuildings() same
 *   - CollisionManager, Renderer still work identically
 *
 * CampusMap now wraps MapData's BuildingData list into Building objects
 * for backward compatibility with the existing Swing Renderer.
 */
public class CampusMap {

    // ---------------------------------------------------------------
    // WORLD SIZE — mirrors MapData constants
    // ---------------------------------------------------------------
    public static final int WORLD_WIDTH  = MapData.WORLD_WIDTH;
    public static final int WORLD_HEIGHT = MapData.WORLD_HEIGHT;

    // ---------------------------------------------------------------
    // FIELDS
    // ---------------------------------------------------------------
    private final List<Building> buildings;

    // Ground / path colors
    public static final Color COLOR_GROUND    = new Color(200, 195, 180);
    public static final Color COLOR_GRASS     = new Color(107, 161,  83);
    public static final Color COLOR_PATH      = new Color(220, 215, 200);
    public static final Color COLOR_BORDER    = new Color( 60,  80,  40);

    // ---------------------------------------------------------------
    // CONSTRUCTOR
    // ---------------------------------------------------------------
    public CampusMap() {
        buildings = new ArrayList<>();
        loadBuildings();
    }

    /**
     * PHASE 2: Loads from MapData.BUILDINGS (BuildingData records).
     * Building wraps each BuildingData for 2D Swing rendering.
     * To add a building: add a row to MapData.BUILDINGS.
     */
    private void loadBuildings() {
        for (com.campusgame.map.data.BuildingData bd : MapData.BUILDINGS) {
            buildings.add(new Building(bd));
        }
    }

    // ---------------------------------------------------------------
    // DRAWING
    // ---------------------------------------------------------------

    /**
     * Draws the ground layer of the campus.
     * Buildings are drawn separately by Renderer after this.
     *
     * @param g  Graphics2D
     * @param ox camera offset X
     * @param oy camera offset Y
     * @param sw screen width
     * @param sh screen height
     */
    public void drawGround(Graphics2D g, int ox, int oy, int sw, int sh) {
        // Fill entire world with grass
        g.setColor(COLOR_GRASS);
        g.fillRect(-ox, -oy, WORLD_WIDTH, WORLD_HEIGHT);

        // Draw a main path grid (simple placeholder paths)
        g.setColor(COLOR_PATH);
        // Horizontal main path
        g.fillRect(-ox, 260 - oy, WORLD_WIDTH, 50);
        // Vertical main path
        g.fillRect(300 - ox, -oy, 50, WORLD_HEIGHT);
        // Second horizontal path
        g.fillRect(-ox, 520 - oy, WORLD_WIDTH, 40);
        // Second vertical path
        g.fillRect(650 - ox, -oy, 50, WORLD_HEIGHT);

        // Campus border fence
        g.setColor(COLOR_BORDER);
        g.setStroke(new BasicStroke(6f));
        g.drawRect(10 - ox, 10 - oy, WORLD_WIDTH - 20, WORLD_HEIGHT - 20);
        g.setStroke(new BasicStroke(1f));

        // Grid reference (subtle, helps during development)
        drawDebugGrid(g, ox, oy);
    }

    /** Faint grid lines for visual reference during development. */
    private void drawDebugGrid(Graphics2D g, int ox, int oy) {
        g.setColor(new Color(0, 0, 0, 15));
        g.setStroke(new BasicStroke(1f));
        int step = 100;
        for (int gx = 0; gx < WORLD_WIDTH; gx += step) {
            g.drawLine(gx - ox, -oy, gx - ox, WORLD_HEIGHT - oy);
        }
        for (int gy = 0; gy < WORLD_HEIGHT; gy += step) {
            g.drawLine(-ox, gy - oy, WORLD_WIDTH - ox, gy - oy);
        }
    }

    // ---------------------------------------------------------------
    // GETTERS
    // ---------------------------------------------------------------

    /** Unmodifiable view of all buildings. Used by CollisionManager & Renderer. */
    public List<Building> getBuildings() {
        return Collections.unmodifiableList(buildings);
    }

    public int getWorldWidth()  { return WORLD_WIDTH;  }
    public int getWorldHeight() { return WORLD_HEIGHT; }
}