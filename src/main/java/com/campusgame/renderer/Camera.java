package com.campusgame.renderer;

import com.campusgame.entities.Player;
import com.campusgame.map.CampusMap;

/**
 * CAMERA (renderer/Camera.java)
 *
 * Phase 5 additions:
 *   • worldWidth / worldHeight fields (mutable)
 *   • setWorldBounds(w, h)  — called by InteriorManager to re-clamp
 *     camera when entering/leaving an interior scene
 *
 * Everything else UNCHANGED from Phase 4.
 */
public class Camera {

    private float offsetX, offsetY;

    private final int screenWidth;
    private final int screenHeight;

    // Lerp smoothing factor (0 = no movement, 1 = instant snap)
    private static final float LERP = 0.12f;

    // ── Phase 5: mutable world bounds ────────────────────────────────
    // Default = exterior campus size.
    // InteriorManager calls setWorldBounds() when entering/leaving a building.
    private float worldWidth  = CampusMap.WORLD_WIDTH;
    private float worldHeight = CampusMap.WORLD_HEIGHT;

    public Camera(int screenWidth, int screenHeight) {
        this.screenWidth  = screenWidth;
        this.screenHeight = screenHeight;
    }

    // ─────────────────────────────────────────────────────────────────
    // FOLLOW
    // ─────────────────────────────────────────────────────────────────

    /**
     * Smoothly moves camera toward the player center.
     * Called every frame after player.update().
     */
    public void follow(Player player) {
        float targetX = player.getCenterX() - screenWidth  / 2f;
        float targetY = player.getCenterY() - screenHeight / 2f;

        offsetX += (targetX - offsetX) * LERP;
        offsetY += (targetY - offsetY) * LERP;

        clamp();
    }

    /** Snap camera instantly to player (no lerp). Used on spawn / teleport. */
    public void snapTo(Player player) {
        offsetX = player.getCenterX() - screenWidth  / 2f;
        offsetY = player.getCenterY() - screenHeight / 2f;
        clamp();
    }

    // ─────────────────────────────────────────────────────────────────
    // COORDINATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    public int   worldToScreenX(float worldX) { return (int)(worldX - offsetX); }
    public int   worldToScreenY(float worldY) { return (int)(worldY - offsetY); }
    public float screenToWorldX(int screenX)  { return screenX + offsetX; }
    public float screenToWorldY(int screenY)  { return screenY + offsetY; }

    // ─────────────────────────────────────────────────────────────────
    // OFFSET  (EditorCamera pan / middle-mouse drag)
    // ─────────────────────────────────────────────────────────────────

    public void setOffset(float ox, float oy) {
        offsetX = ox;
        offsetY = oy;
        clamp();
    }

    // ─────────────────────────────────────────────────────────────────
    // PHASE 5 — WORLD BOUNDS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Re-clamp camera to a different world size.
     *
     * Called by InteriorManager:
     *   • enterScene()    → setWorldBounds(scene.width, scene.height)
     *   • exitToExterior()→ setWorldBounds(WORLD_WIDTH, WORLD_HEIGHT)
     *
     * When the interior is smaller than the screen the camera stays
     * at (0, 0) so the interior draws from the top-left corner with the
     * background colour filling any remaining space.
     */
    public void setWorldBounds(float w, float h) {
        worldWidth  = w;
        worldHeight = h;
        clamp();
    }

    // ─────────────────────────────────────────────────────────────────
    // GETTERS
    // ─────────────────────────────────────────────────────────────────

    public int   getOffsetX()      { return (int) offsetX; }
    public int   getOffsetY()      { return (int) offsetY; }
    public float getOffsetXf()     { return offsetX; }
    public float getOffsetYf()     { return offsetY; }
    public int   getScreenWidth()  { return screenWidth;  }
    public int   getScreenHeight() { return screenHeight; }

    // ─────────────────────────────────────────────────────────────────
    // INTERNAL
    // ─────────────────────────────────────────────────────────────────

    /**
     * Clamp to [0 .. worldSize - screenSize].
     * When worldSize < screenSize the max is negative, so Math.max(0,…)
     * keeps offset at 0 — the scene draws from the top-left.
     */
    private void clamp() {
        float maxX = Math.max(0f, worldWidth  - screenWidth);
        float maxY = Math.max(0f, worldHeight - screenHeight);
        offsetX = Math.max(0f, Math.min(offsetX, maxX));
        offsetY = Math.max(0f, Math.min(offsetY, maxY));
    }
}