package com.campusgame.editor.camera;

import com.campusgame.map.CampusMap;
import com.campusgame.renderer.Camera;

/**
 * EDITOR CAMERA (editor/camera/EditorCamera.java)
 * -------------------------------------------------
 * Wraps the base Camera and adds editor-specific controls:
 *   - Middle-mouse-drag to pan
 *   - Scroll wheel to zoom (scale factor)
 *   - WASD/arrow-key pan when editor is active
 *   - Decoupled from player (editor camera moves independently)
 *
 * Phase 4: pan + zoom. Phase 5: perspective tilt for isometric preview.
 *
 * Usage:
 *   Replace camera.follow(player) with editorCamera.update() when editor is on.
 *   All world↔screen math already in Camera; EditorCamera just adjusts offsetX/Y.
 */
public class EditorCamera {

    private final Camera base;

    // Pan velocity (world units/second) — set by keyboard input
    private float panVX = 0f, panVZ = 0f;
    public static final float PAN_SPEED = 600f;

    // Drag state
    private boolean dragging   = false;
    private int     dragStartX = 0, dragStartY = 0;
    private float   dragOffsetXAtStart = 0f, dragOffsetYAtStart = 0f;

    // Zoom — not applied yet (reserved for Phase 5)
    private float zoom = 1.0f;
    public static final float ZOOM_MIN = 0.25f;
    public static final float ZOOM_MAX = 3.0f;

    public EditorCamera(Camera base) {
        this.base = base;
    }

    // ── Update (call instead of camera.follow when editor is active) ──
    public void update(float delta) {
        // Keyboard pan
        float newOffX = base.getOffsetX() + panVX * delta;
        float newOffY = base.getOffsetY() + panVZ * delta;
        newOffX = Math.max(0, Math.min(newOffX, CampusMap.WORLD_WIDTH  - base.getScreenWidth()));
        newOffY = Math.max(0, Math.min(newOffY, CampusMap.WORLD_HEIGHT - base.getScreenHeight()));
        base.setOffset(newOffX, newOffY);
    }

    // ── Middle-mouse drag ─────────────────────────────────────────────
    public void beginDrag(int screenX, int screenY) {
        dragging            = true;
        dragStartX          = screenX;
        dragStartY          = screenY;
        dragOffsetXAtStart  = base.getOffsetX();
        dragOffsetYAtStart  = base.getOffsetY();
    }

    public void updateDrag(int screenX, int screenY) {
        if (!dragging) return;
        float dx = dragStartX - screenX;
        float dy = dragStartY - screenY;
        float newX = Math.max(0, Math.min(dragOffsetXAtStart + dx,
                CampusMap.WORLD_WIDTH  - base.getScreenWidth()));
        float newY = Math.max(0, Math.min(dragOffsetYAtStart + dy,
                CampusMap.WORLD_HEIGHT - base.getScreenHeight()));
        base.setOffset(newX, newY);
    }

    public void endDrag() { dragging = false; }
    public boolean isDragging() { return dragging; }

    // ── Scroll zoom ───────────────────────────────────────────────────
    public void zoom(int wheelRotation) {
        // Reserved — zoom implementation goes here in Phase 5
        // zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom - wheelRotation * 0.1f));
    }

    // ── Keyboard pan (set by InputHandler) ───────────────────────────
    public void setPanX(float vx) { this.panVX = vx; }
    public void setPanZ(float vz) { this.panVZ = vz; }

    public float getZoom() { return zoom; }
}