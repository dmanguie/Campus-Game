package com.campusgame.renderer.api;

import com.campusgame.core.GameWorld;
import com.campusgame.entities.Player;
import com.campusgame.map.data.BuildingData;
import com.campusgame.map.data.MapData;
import com.campusgame.renderer.Camera;

/**
 * JAVAFX ISOMETRIC RENDERER — STUB (renderer/api/JavaFXIsometricRenderer.java)
 * -------------------------------------------------------------------------------
 * Phase 2 renderer stub. Implement this when you're ready for isometric/2.5D view.
 *
 * HOW ISOMETRIC PROJECTION WORKS:
 *   World (x, z) → Screen (sx, sy):
 *     sx = (x - z) * TILE_W / 2
 *     sy = (x + z) * TILE_H / 4
 *
 *   Buildings are drawn as 3D boxes using three faces:
 *     - Top face  (roof)
 *     - Left face (west wall)
 *     - Right face (east wall)
 *
 *   Floors multiply the box height for multi-story buildings.
 *
 * SETUP STEPS (when ready):
 *   1. Add JavaFX to pom.xml (org.openjfx:javafx-graphics:21)
 *   2. Replace TODO stubs below with real JavaFX Canvas draw calls
 *   3. In GameLoop, change:
 *        IRenderer activeRenderer = new SwingRenderer2D(...)
 *      to:
 *        IRenderer activeRenderer = new JavaFXIsometricRenderer(...)
 *
 * Everything else (GameWorld, Player, Camera, MapData) stays the same.
 */
public class JavaFXIsometricRenderer implements IRenderer {

    // Isometric tile dimensions
    public static final int ISO_TILE_W = 64;   // pixels wide per world unit
    public static final int ISO_TILE_H = 32;   // pixels tall per world unit

    private int screenWidth;
    private int screenHeight;

    public JavaFXIsometricRenderer(int screenWidth, int screenHeight) {
        this.screenWidth  = screenWidth;
        this.screenHeight = screenHeight;
    }

    @Override
    public void init() {
        // TODO Phase 2:
        //   JavaFX.launch(...)
        //   Create Canvas + GraphicsContext
        //   Load building textures from assets/textures/
        System.out.println("[JavaFXIsometricRenderer] STUB — not yet implemented.");
        System.out.println("  → Switch back to SwingRenderer2D for Phase 1.");
    }

    @Override
    public void render(GameWorld world, Player player, Camera camera) {
        // TODO Phase 2 render order:
        //   1. Clear canvas (sky color gradient)
        //   2. Draw ground tiles (grass + path polygons in iso projection)
        //   3. Sort buildings by (x + z) for correct painter's algorithm order
        //   4. Draw each building as 3-face iso box using BuildingData
        //   5. Draw player sprite (iso projected)
        //   6. Draw HUD overlay (minimap stays 2D, draw on top)

        // Example isometric projection (uncomment in Phase 2):
        // for (BuildingData b : MapData.BUILDINGS) {
        //     int sx = isoX(b.x, b.z);
        //     int sy = isoY(b.x, b.z);
        //     drawIsoBox(gc, sx, sy, b.width, b.depth, b.height, b.colorARGB);
        // }
    }

    @Override
    public void onResize(int width, int height) {
        this.screenWidth  = width;
        this.screenHeight = height;
        // TODO: update JavaFX canvas size and projection origin
    }

    @Override
    public void dispose() {
        // TODO: release JavaFX resources
    }

    @Override
    public String getBackendName() {
        return "JavaFX Isometric 2.5D (Phase 2 — STUB)";
    }

    // ---------------------------------------------------------------
    // ISOMETRIC MATH — ready to use in Phase 2
    // ---------------------------------------------------------------

    /**
     * Converts world X,Z to isometric screen X.
     * Origin is at screen center (shift by screenWidth/2 for centering).
     */
    public int isoX(float worldX, float worldZ) {
        return (int)((worldX - worldZ) * ISO_TILE_W / 2f) + screenWidth / 2;
    }

    /**
     * Converts world X,Z to isometric screen Y.
     */
    public int isoY(float worldX, float worldZ) {
        return (int)((worldX + worldZ) * ISO_TILE_H / 4f);
    }

    /**
     * Converts world height (Y) to isometric screen Y offset.
     * Subtract this from the building's base screenY to get the roof Y.
     */
    public int isoHeight(float worldHeight) {
        return (int)(worldHeight * ISO_TILE_H / 2f);
    }
}