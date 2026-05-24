package com.campusgame.map.data;

/**
 * BUILDING DATA (map/data/BuildingData.java)
 * Pure data record — zero rendering, zero AWT.
 * Supports both rectangular and polygon-shaped buildings.
 * Phase 3: added rotationDegrees, polygonX/Z, mutable copy helper.
 */
public class BuildingData {

    public static final float METERS_PER_FLOOR = 30f;

    public final String  name;
    public final float   x, y, z;
    public final float   width, height, depth;
    public final int     floors;
    public final int     colorARGB;
    public final float   rotationDegrees;
    public final boolean collisionEnabled;
    public final String  tag;
    public final int[]   polygonX;
    public final int[]   polygonZ;

    // ── Rectangular constructor (with rotation) ──────────────────────
    public BuildingData(String name,
                        float x, float z,
                        float width, float depth,
                        int floors,
                        int colorARGB,
                        float rotationDegrees,
                        boolean collisionEnabled,
                        String tag) {
        this.name             = name;
        this.x                = x;
        this.y                = 0f;
        this.z                = z;
        this.width            = width;
        this.depth            = depth;
        this.floors           = floors;
        this.height           = floors * METERS_PER_FLOOR;
        this.colorARGB        = colorARGB;
        this.rotationDegrees  = rotationDegrees;
        this.collisionEnabled = collisionEnabled;
        this.tag              = tag;
        this.polygonX         = null;
        this.polygonZ         = null;
    }

    // ── Rectangular convenience (no rotation) ────────────────────────
    public BuildingData(String name,
                        float x, float z,
                        float width, float depth,
                        int floors,
                        int colorARGB,
                        boolean collisionEnabled,
                        String tag) {
        this(name, x, z, width, depth, floors, colorARGB, 0f, collisionEnabled, tag);
    }

    // ── Polygon constructor ───────────────────────────────────────────
    public BuildingData(String name,
                        int[] polygonX,
                        int[] polygonZ,
                        int floors,
                        int colorARGB,
                        float rotationDegrees,
                        boolean collisionEnabled,
                        String tag) {
        this.name             = name;
        this.x                = polygonX[0];
        this.y                = 0f;
        this.z                = polygonZ[0];
        this.width            = 0;
        this.depth            = 0;
        this.floors           = floors;
        this.height           = floors * METERS_PER_FLOOR;
        this.colorARGB        = colorARGB;
        this.rotationDegrees  = rotationDegrees;
        this.collisionEnabled = collisionEnabled;
        this.tag              = tag;
        this.polygonX         = polygonX;
        this.polygonZ         = polygonZ;
    }

    // ── Geometry helpers ─────────────────────────────────────────────
    public boolean isPolygon() { return polygonX != null && polygonZ != null; }

    public float centerX() { return x + width / 2f; }
    public float centerZ() { return z + depth / 2f; }
    public float maxX()    { return x + width; }
    public float maxZ()    { return z + depth; }
    public float topY()    { return y + height; }

    // ── Color helpers ────────────────────────────────────────────────
    public int red()   { return (colorARGB >> 16) & 0xFF; }
    public int green() { return (colorARGB >>  8) & 0xFF; }
    public int blue()  { return  colorARGB        & 0xFF; }
    public int alpha() { return (colorARGB >> 24) & 0xFF; }

    /**
     * Returns a copy of this BuildingData with a new position.
     * Used by editor undo/redo and placement preview.
     */
    public BuildingData withPosition(float newX, float newZ) {
        if (isPolygon()) return this; // polygon move not supported yet
        return new BuildingData(name, newX, newZ, width, depth,
                floors, colorARGB, rotationDegrees,
                collisionEnabled, tag);
    }

    /** Returns a copy with a new name. Used by editor when naming placed buildings. */
    public BuildingData withName(String newName) {
        if (isPolygon()) {
            return new BuildingData(newName, polygonX, polygonZ,
                    floors, colorARGB, rotationDegrees,
                    collisionEnabled, tag);
        }
        return new BuildingData(newName, x, z, width, depth,
                floors, colorARGB, rotationDegrees,
                collisionEnabled, tag);
    }

    @Override
    public String toString() {
        return String.format(
                "BuildingData{name='%s', x=%.0f, z=%.0f, w=%.0f, d=%.0f, " +
                        "floors=%d, h=%.0f, rot=%.1f, collision=%b, tag='%s', polygon=%b}",
                name, x, z, width, depth, floors, height,
                rotationDegrees, collisionEnabled, tag, isPolygon());
    }
}