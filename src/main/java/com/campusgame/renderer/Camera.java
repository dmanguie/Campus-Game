package com.campusgame.renderer;

import com.campusgame.entities.Player;
import com.campusgame.map.CampusMap;

/**
 * CAMERA (renderer/Camera.java)
 * ------------------------------
 * Converts world coordinates to screen coordinates.
 *
 * Responsibilities:
 *  - Follows the player by centering on them
 *  - Clamps so the camera never shows outside the world
 *  - Provides offsetX / offsetY that all renderers subtract
 *    from world positions to get screen positions
 *
 * Usage:
 *   int screenX = (int)(worldX - camera.getOffsetX());
 *   int screenY = (int)(worldY - camera.getOffsetY());
 *
 * Future expansion:
 *  - Zoom level
 *  - Shake effect
 */
public class Camera {

    private float offsetX, offsetY;

    private final int screenWidth;
    private final int screenHeight;

    // Lerp smoothing factor (0 = no movement, 1 = instant snap)
    private static final float LERP = 0.12f;

    public Camera(int screenWidth, int screenHeight) {
        this.screenWidth  = screenWidth;
        this.screenHeight = screenHeight;
    }

    // ---------------------------------------------------------------
    // FOLLOW
    // ---------------------------------------------------------------

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

    /** Snap camera instantly to player (no lerp). Useful on spawn/teleport. */
    public void snapTo(Player player) {
        offsetX = player.getCenterX() - screenWidth  / 2f;
        offsetY = player.getCenterY() - screenHeight / 2f;
        clamp();
    }

    // ---------------------------------------------------------------
    // COORDINATE HELPERS
    // ---------------------------------------------------------------

    /** Convert world X to screen X. */
    public int worldToScreenX(float worldX) {
        return (int)(worldX - offsetX);
    }

    /** Convert world Z/Y to screen Y. */
    public int worldToScreenY(float worldY) {
        return (int)(worldY - offsetY);
    }

    /** Convert screen X to world X. */
    public float screenToWorldX(int screenX) {
        return screenX + offsetX;
    }

    /** Convert screen Y to world Z/Y. */
    public float screenToWorldY(int screenY) {
        return screenY + offsetY;
    }

    // ---------------------------------------------------------------
    // OFFSET  (used by EditorCamera for pan / middle-mouse drag)
    // ---------------------------------------------------------------

    /**
     * Directly set the viewport offset.
     * EditorCamera calls this for keyboard pan and middle-mouse drag.
     * Automatically clamped to world bounds.
     */
    public void setOffset(float ox, float oy) {
        offsetX = ox;
        offsetY = oy;
        clamp();
    }

    // ---------------------------------------------------------------
    // GETTERS
    // ---------------------------------------------------------------

    /** Integer offset — used by legacy renderers that subtract directly. */
    public int getOffsetX() { return (int) offsetX; }
    public int getOffsetY() { return (int) offsetY; }

    /** Float offset — used by EditorCamera for smooth pan math. */
    public float getOffsetXf() { return offsetX; }
    public float getOffsetYf() { return offsetY; }

    public int getScreenWidth()  { return screenWidth;  }
    public int getScreenHeight() { return screenHeight; }

    // ---------------------------------------------------------------
    // INTERNAL
    // ---------------------------------------------------------------

    private void clamp() {
        offsetX = Math.max(0, Math.min(offsetX, CampusMap.WORLD_WIDTH  - screenWidth));
        offsetY = Math.max(0, Math.min(offsetY, CampusMap.WORLD_HEIGHT - screenHeight));
    }
}