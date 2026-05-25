package com.campusgame.editor.gizmo;

/**
 * RESIZE HANDLE (editor/gizmo/ResizeHandle.java)
 * -------------------------------------------------
 * Represents one of the 8 resize handles around a selected rectangle building.
 *
 * Handles:
 *   NW ── N ── NE
 *   W         E
 *   SW ── S ── SE
 *
 * Each handle knows:
 *   - Its world-space position (computed each frame from the building bounds)
 *   - Which edges it controls (affects x, z, width, depth on drag)
 *   - Its screen-space hit rectangle for mouse testing
 */
public class ResizeHandle {

    public enum Position { NW, N, NE, E, SE, S, SW, W }

    public static final int HANDLE_SIZE = 10; // screen pixels

    public final Position pos;

    // Whether this handle moves the origin (x/z) or just the size
    public final boolean affectsLeft;
    public final boolean affectsTop;
    public final boolean affectsRight;
    public final boolean affectsBottom;

    // Current world-space center of this handle (updated each frame)
    public float worldX, worldZ;

    public ResizeHandle(Position pos) {
        this.pos           = pos;
        this.affectsLeft   = (pos == Position.NW || pos == Position.W || pos == Position.SW);
        this.affectsTop    = (pos == Position.NW || pos == Position.N || pos == Position.NE);
        this.affectsRight  = (pos == Position.NE || pos == Position.E || pos == Position.SE);
        this.affectsBottom = (pos == Position.SW || pos == Position.S || pos == Position.SE);
    }

    /**
     * Update this handle's world position from the current building AABB.
     * Call every frame after the building is known.
     */
    public void updatePosition(float bx, float bz, float bw, float bd) {
        worldX = switch (pos) {
            case NW, W, SW -> bx;
            case N,  S     -> bx + bw / 2f;
            case NE, E, SE -> bx + bw;
        };
        worldZ = switch (pos) {
            case NW, N, NE -> bz;
            case W,     E  -> bz + bd / 2f;
            case SW, S, SE -> bz + bd;
        };
    }

    /**
     * Apply a drag delta to produce updated building bounds.
     * Returns float[]{newX, newZ, newW, newD}.
     */
    public float[] applyDrag(float origX, float origZ, float origW, float origD,
                             float deltaX, float deltaZ) {
        float nx = origX, nz = origZ, nw = origW, nd = origD;

        if (affectsLeft)   { nx += deltaX; nw = Math.max(20, nw - deltaX); }
        if (affectsRight)  {               nw = Math.max(20, nw + deltaX); }
        if (affectsTop)    { nz += deltaZ; nd = Math.max(20, nd - deltaZ); }
        if (affectsBottom) {               nd = Math.max(20, nd + deltaZ); }

        return new float[]{nx, nz, nw, nd};
    }

    /** True if the screen point (sx, sy) hits this handle. */
    public boolean hitTest(int sx, int sy, int screenHandleX, int screenHandleY) {
        int half = HANDLE_SIZE / 2;
        return sx >= screenHandleX - half && sx <= screenHandleX + half
                && sy >= screenHandleY - half && sy <= screenHandleY + half;
    }

    /** Factory: create all 8 handles. */
    public static ResizeHandle[] all() {
        ResizeHandle[] h = new ResizeHandle[8];
        Position[] positions = Position.values();
        for (int i = 0; i < 8; i++) h[i] = new ResizeHandle(positions[i]);
        return h;
    }
}