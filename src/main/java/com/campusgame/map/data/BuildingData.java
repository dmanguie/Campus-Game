package com.campusgame.map.data;

/**
 * BUILDING DATA (map/data/BuildingData.java)
 * -------------------------------------------
 * Pure data record for one campus building.
 * Contains ZERO rendering or AWT code.
 *
 * This is the single source of truth for all building properties.
 * Both the 2D Swing renderer and the future 3D LWJGL renderer
 * will read from this class — they never define their own data.
 *
 * 3D coordinate system (right-hand, Y-up):
 *   x = east-west  position (world units)
 *   y = elevation  (ground level = 0)
 *   z = north-south position (world units)
 *
 * For 2D top-down rendering:
 *   x → screen X,  z → screen Y,  height is ignored visually
 *
 * For future 3D rendering:
 *   x, y, z → full 3D position,  height = floors * METERS_PER_FLOOR
 *
 * World unit scale: 1 unit = 1 pixel in 2D view (adjust when adding 3D camera)
 */
public class BuildingData {

    // ---------------------------------------------------------------
    // CONSTANTS
    // ---------------------------------------------------------------

    /** Meters (world units) per floor. Used for 3D building height. */
    public static final float METERS_PER_FLOOR = 30f;

    // ---------------------------------------------------------------
    // FIELDS — all final (immutable data record)
    // ---------------------------------------------------------------

    /** Display name shown as label */
    public final String name;

    /** World X position (top-left corner, east-west axis) */
    public final float x;

    /**
     * World Y position.
     * In 2D top-down: always 0 (ground).
     * In 3D: elevation offset (0 = ground floor).
     */
    public final float y;

    /** World Z position (top-left corner, north-south axis) */
    public final float z;

    /** East-west footprint size in world units */
    public final float width;

    /** Vertical height in world units. Derived: floors * METERS_PER_FLOOR */
    public final float height;

    /** North-south footprint size in world units */
    public final float depth;

    /** Number of floors (determines height) */
    public final int floors;

    /** Packed ARGB color integer (use ColorUtils to convert from java.awt.Color or hex) */
    public final int colorARGB;

    /** If false, physics ignores this building (e.g. P.E. open area markers) */
    public final boolean collisionEnabled;

    /** Optional tag for gameplay: "academic", "gym", "pe", "admin" etc. */
    public final String tag;

    // ---------------------------------------------------------------
    // CONSTRUCTOR
    // ---------------------------------------------------------------

    /**
     * Full constructor.
     *
     * @param name             Display label
     * @param x                World X (top-left)
     * @param z                World Z (top-left) — maps to screen Y in 2D
     * @param width            East-west size
     * @param depth            North-south size
     * @param floors           Number of floors (height = floors * METERS_PER_FLOOR)
     * @param colorARGB        Packed ARGB color (0xFFRRGGBB)
     * @param collisionEnabled Whether players can walk through this building
     * @param tag              Category tag for gameplay logic
     */
    public BuildingData(String name,
                        float x, float z,
                        float width, float depth,
                        int floors,
                        int colorARGB,
                        boolean collisionEnabled,
                        String tag) {
        this.name             = name;
        this.x                = x;
        this.y                = 0f;          // ground level
        this.z                = z;
        this.width            = width;
        this.depth            = depth;
        this.floors           = floors;
        this.height           = floors * METERS_PER_FLOOR;
        this.colorARGB        = colorARGB;
        this.collisionEnabled = collisionEnabled;
        this.tag              = tag;
    }

    /** Convenience constructor without tag (defaults to "building") */
    public BuildingData(String name,
                        float x, float z,
                        float width, float depth,
                        int floors,
                        int colorARGB,
                        boolean collisionEnabled) {
        this(name, x, z, width, depth, floors, colorARGB, collisionEnabled, "building");
    }

    // ---------------------------------------------------------------
    // DERIVED GEOMETRY HELPERS
    // ---------------------------------------------------------------

    /** Center X of footprint */
    public float centerX() { return x + width / 2f; }

    /** Center Z of footprint (maps to screen center Y in 2D) */
    public float centerZ() { return z + depth / 2f; }

    /** Right edge X */
    public float maxX() { return x + width; }

    /** Far edge Z */
    public float maxZ() { return z + depth; }

    /** Top of building in 3D world units */
    public float topY() { return y + height; }

    // ---------------------------------------------------------------
    // COLOR HELPERS
    // ---------------------------------------------------------------

    /** Extract red component (0–255) */
    public int red()   { return (colorARGB >> 16) & 0xFF; }

    /** Extract green component (0–255) */
    public int green() { return (colorARGB >>  8) & 0xFF; }

    /** Extract blue component (0–255) */
    public int blue()  { return  colorARGB        & 0xFF; }

    /** Extract alpha component (0–255) */
    public int alpha() { return (colorARGB >> 24) & 0xFF; }

    // ---------------------------------------------------------------
    // DEBUG
    // ---------------------------------------------------------------

    @Override
    public String toString() {
        return String.format("BuildingData{name='%s', x=%.0f, z=%.0f, w=%.0f, d=%.0f, floors=%d, h=%.0f, collision=%b, tag='%s'}",
                name, x, z, width, depth, floors, height, collisionEnabled, tag);
    }
}