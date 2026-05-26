package com.campusgame.renderer;

import com.campusgame.entities.Player;
import com.campusgame.map.CampusMap;
import com.campusgame.renderer.projection.ProjectionMode;

public class Camera {

    private float offsetX, offsetY;
    private final int screenWidth, screenHeight;
    private static final float LERP = 0.12f;

    private float worldWidth  = CampusMap.WORLD_WIDTH;
    private float worldHeight = CampusMap.WORLD_HEIGHT;

    // ── Zoom ──────────────────────────────────────────────────────────
    private float zoom    = 1.0f;
    private static final float ZOOM_MIN  = 0.25f;
    private static final float ZOOM_MAX  = 3.0f;
    private static final float ZOOM_STEP = 0.1f;

    public Camera(int screenWidth, int screenHeight) {
        this.screenWidth  = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void follow(Player player) {
        float targetX = player.getCenterX() - (screenWidth  / 2f) / zoom;
        float targetY = player.getCenterY() - (screenHeight / 2f) / zoom;
        offsetX += (targetX - offsetX) * LERP;
        offsetY += (targetY - offsetY) * LERP;
        clamp();
    }

    public void snapTo(Player player) {
        offsetX = player.getCenterX() - (screenWidth  / 2f) / zoom;
        offsetY = player.getCenterY() - (screenHeight / 2f) / zoom;
        clamp();
    }

    // ── Coordinate transforms (zoom-aware) ───────────────────────────
    public int   worldToScreenX(float worldX) { return (int)((worldX - offsetX) * zoom); }
    public int   worldToScreenY(float worldY) { return (int)((worldY - offsetY) * zoom); }
    public float screenToWorldX(int screenX)  { return screenX / zoom + offsetX; }
    public float screenToWorldY(int screenY)  { return screenY / zoom + offsetY; }

    /**
     * Zoom toward/away from a screen-space anchor point (e.g. mouse cursor).
     * Adjusts offset so the world point under the cursor stays fixed.
     */
    public void zoomAt(int screenAnchorX, int screenAnchorY, int wheelRotation) {
        // World point currently under the cursor
        float worldAnchorX = screenToWorldX(screenAnchorX);
        float worldAnchorY = screenToWorldY(screenAnchorY);

        float newZoom = zoom + (-wheelRotation * ZOOM_STEP);
        zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, newZoom));

        // Reposition offset so the same world point stays under the cursor
        offsetX = worldAnchorX - screenAnchorX / zoom;
        offsetY = worldAnchorY - screenAnchorY / zoom;
        clamp();
    }

    public float getZoom() { return zoom; }
    public void  setZoom(float z) { zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, z)); clamp(); }

    public void setOffset(float ox, float oy) { offsetX = ox; offsetY = oy; clamp(); }
    public void setWorldBounds(float w, float h) { worldWidth = w; worldHeight = h; clamp(); }

    public int   getOffsetX()      { return (int) offsetX; }
    public int   getOffsetY()      { return (int) offsetY; }
    public float getOffsetXf()     { return offsetX; }
    public float getOffsetYf()     { return offsetY; }
    public int   getScreenWidth()  { return screenWidth; }
    public int   getScreenHeight() { return screenHeight; }

    private void clamp() {
        float maxX = Math.max(0f, worldWidth  - screenWidth  / zoom);
        float maxY = Math.max(0f, worldHeight - screenHeight / zoom);
        offsetX = Math.max(0f, Math.min(offsetX, maxX));
        offsetY = Math.max(0f, Math.min(offsetY, maxY));
    }

    private ProjectionMode projectionMode = ProjectionMode.TOP_DOWN;
    public void setProjectionMode(ProjectionMode m) { this.projectionMode = m; }
    public ProjectionMode getProjectionMode()        { return projectionMode; }

    public int[] project(float wx, float wz) {
        return new int[]{
                projectionMode.screenX(wx, wz, this),
                projectionMode.screenY(wx, wz, this)
        };
    }
}