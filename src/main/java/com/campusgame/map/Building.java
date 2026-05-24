package com.campusgame.map;

import com.campusgame.map.data.BuildingData;

import java.awt.*;

/**
 * BUILDING (map/Building.java)
 * -----------------------------
 * The 2D Swing renderable wrapper around a BuildingData record.
 *
 * PHASE 2 CHANGE:
 *   Now accepts a BuildingData in its constructor.
 *   All data (name, x, z, width, depth, floors, color) comes from BuildingData.
 *   The legacy constructor is kept for backward compatibility.
 *
 * Drawing logic is UNCHANGED — Phase 1 still works identically.
 *
 * Note on coordinates:
 *   BuildingData uses (x, z) for the 3D-ready horizontal plane.
 *   In 2D rendering, BuildingData.z maps to screen Y.
 *   This class reads b.x → screenX, b.z → screenY.
 */
public class Building {

    // --- Source of truth: the pure data record ---
    private final BuildingData data;

    // --- Derived AWT colors (2D rendering only) ---
    private final Color roofColor;
    private final Color wallColor;
    private final Color labelColor;

    // Convenience aliases (keep old field names working internally)
    private final int x, y, width, depth, floors;
    private final String name;

    // Derived 3D height (pixels per floor for future isometric rendering)
    public static final int PIXELS_PER_FLOOR = 20;

    // ---------------------------------------------------------------
    // PHASE 2 CONSTRUCTOR — preferred
    // ---------------------------------------------------------------
    /**
     * Create a renderable Building from a pure BuildingData record.
     * BuildingData.z is used as the top-down Y position.
     */
    public Building(BuildingData data) {
        this.data   = data;
        this.name   = data.name;
        this.x      = (int) data.x;
        this.y      = (int) data.z;   // z = north-south = screen Y in 2D
        this.width  = (int) data.width;
        this.depth  = (int) data.depth;
        this.floors = data.floors;
        this.roofColor  = new Color(data.red(), data.green(), data.blue());
        this.wallColor  = roofColor.darker();
        this.labelColor = Color.WHITE;
    }

    // ---------------------------------------------------------------
    // LEGACY CONSTRUCTOR — kept so old CampusMap code still compiles
    // ---------------------------------------------------------------
    public Building(String name, int x, int y, int width, int depth,
                    int floors, Color roofColor) {
        this.data   = null; // no BuildingData backing in legacy mode
        this.name   = name;
        this.x      = x;
        this.y      = y;
        this.width  = width;
        this.depth  = depth;
        this.floors = floors;
        this.roofColor  = roofColor;
        this.wallColor  = roofColor.darker();
        this.labelColor = Color.WHITE;
    }

    // ---------------------------------------------------------------
    // DRAWING
    // ---------------------------------------------------------------

    /**
     * Draws this building at its camera-adjusted screen position.
     * Top-down 2D view with a subtle pseudo-3D shadow offset.
     *
     * @param g  Graphics2D context
     * @param ox Camera offset X (subtract from world coords to get screen coords)
     * @param oy Camera offset Y
     */
    public void draw(Graphics2D g, int ox, int oy) {
        int sx = x - ox;
        int sy = y - oy;

        // ---- Shadow ----
        g.setColor(new Color(0, 0, 0, 50));
        g.fillRect(sx + 4, sy + 4, width, depth);

        // ---- Roof (top face) ----
        g.setColor(roofColor);
        g.fillRect(sx, sy, width, depth);

        // ---- Wall accent (bottom & right edges simulate depth) ----
        g.setColor(wallColor);
        g.fillRect(sx, sy + depth - 4, width, 4);       // bottom edge
        g.fillRect(sx + width - 4, sy, 4, depth);       // right edge

        // ---- Border ----
        g.setColor(wallColor.darker());
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(sx, sy, width, depth);
        g.setStroke(new BasicStroke(1f));

        // ---- Label ----
        drawLabel(g, sx, sy);
    }

    private void drawLabel(Graphics2D g, int sx, int sy) {
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        FontMetrics fm = g.getFontMetrics();

        // Wrap long names at spaces
        String[] words = name.split(" ");
        int lineH = fm.getHeight();
        int totalH = lineH * words.length;
        int startY = sy + depth / 2 - totalH / 2 + fm.getAscent();

        for (String word : words) {
            int tw = fm.stringWidth(word);
            int tx = sx + width / 2 - tw / 2;
            // Drop shadow
            g.setColor(new Color(0, 0, 0, 120));
            g.drawString(word, tx + 1, startY + 1);
            // Label
            g.setColor(labelColor);
            g.drawString(word, tx, startY);
            startY += lineH;
        }
    }

    // ---------------------------------------------------------------
    // COLLISION
    // ---------------------------------------------------------------

    /**
     * Returns the AABB collision rectangle (world space).
     * Used by CollisionManager.
     */
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, depth);
    }

    // ---------------------------------------------------------------
    // GETTERS
    // ---------------------------------------------------------------

    public String getName()   { return name;   }
    public int    getX()      { return x;      }
    public int    getY()      { return y;      }
    public int    getWidth()  { return width;  }
    public int    getDepth()  { return depth;  }
    public int    getFloors() { return floors; }
    public Color  getRoofColor() { return roofColor; }

    /** World-space center X */
    public float getCenterX() { return x + width / 2f; }

    /** World-space center Y */
    public float getCenterY() { return y + depth / 2f; }

    /**
     * Returns the backing BuildingData (null if created via legacy constructor).
     * Use this in Phase 2+ code to access 3D-ready fields (z, height, tag, etc.).
     */
    public BuildingData getData() { return data; }
}