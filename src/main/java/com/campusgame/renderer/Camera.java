package com.campusgame.renderer;

import com.campusgame.entities.Player;
import com.campusgame.map.CampusMap;

/**
 * CAMERA (renderer/Camera.java)
 * Phase 5: added setWorldBounds() and setOffset() for interior scenes + editor pan.
 */
public class Camera {

    private float offsetX, offsetY;
    private final int screenWidth, screenHeight;
    private static final float LERP = 0.12f;

    private float worldWidth  = CampusMap.WORLD_WIDTH;
    private float worldHeight = CampusMap.WORLD_HEIGHT;

    public Camera(int screenWidth, int screenHeight) {
        this.screenWidth  = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void follow(Player player) {
        float targetX = player.getCenterX() - screenWidth  / 2f;
        float targetY = player.getCenterY() - screenHeight / 2f;
        offsetX += (targetX - offsetX) * LERP;
        offsetY += (targetY - offsetY) * LERP;
        clamp();
    }

    public void snapTo(Player player) {
        offsetX = player.getCenterX() - screenWidth  / 2f;
        offsetY = player.getCenterY() - screenHeight / 2f;
        clamp();
    }

    public int   worldToScreenX(float worldX) { return (int)(worldX - offsetX); }
    public int   worldToScreenY(float worldY) { return (int)(worldY - offsetY); }
    public float screenToWorldX(int screenX)  { return screenX + offsetX; }
    public float screenToWorldY(int screenY)  { return screenY + offsetY; }

    /** For EditorCamera middle-mouse pan. */
    public void setOffset(float ox, float oy) { offsetX = ox; offsetY = oy; clamp(); }

    /** Phase 5: re-clamp camera when entering/leaving an interior scene. */
    public void setWorldBounds(float w, float h) { worldWidth = w; worldHeight = h; clamp(); }

    public int   getOffsetX()     { return (int) offsetX; }
    public int   getOffsetY()     { return (int) offsetY; }
    public float getOffsetXf()    { return offsetX; }
    public float getOffsetYf()    { return offsetY; }
    public int   getScreenWidth() { return screenWidth; }
    public int   getScreenHeight(){ return screenHeight; }

    private void clamp() {
        float maxX = Math.max(0f, worldWidth  - screenWidth);
        float maxY = Math.max(0f, worldHeight - screenHeight);
        offsetX = Math.max(0f, Math.min(offsetX, maxX));
        offsetY = Math.max(0f, Math.min(offsetY, maxY));
    }
}