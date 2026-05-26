package com.campusgame.map;

import com.campusgame.map.data.BuildingData;
import java.awt.*;
import java.awt.geom.AffineTransform;
import com.campusgame.renderer.Camera;
import com.campusgame.renderer.projection.ProjectionMode;

/**
 * BUILDING (map/Building.java)
 * 2D Swing renderable wrapper around BuildingData.
 * Supports rectangles, rotated rectangles, and arbitrary polygons.
 *
 * BUG FIXES vs previous version:
 *  - drawLabel() for polygon used wrong center (sx/sy instead of polygon centroid)
 *  - getBounds() now returns polygon.getBounds() correctly for collision
 *  - getX/Y/Width/Depth delegate to getBounds() so minimap is correct for polygons
 */
public class Building {

    private final BuildingData data;
    private final Color roofColor, wallColor, labelColor;
    private final String name;

    // For rectangular buildings only (polygon buildings use data.polygonX/Z directly)
    private final int x, y, width, depth, floors;

    public static final int PIXELS_PER_FLOOR = 20;

    // ── Phase-3 constructor: from BuildingData ────────────────────────
    public Building(BuildingData data) {
        this.data      = data;
        this.name      = data.name;
        this.x         = (int) data.x;
        this.y         = (int) data.z;   // z → screen Y
        this.width     = (int) data.width;
        this.depth     = (int) data.depth;
        this.floors    = data.floors;
        this.roofColor  = new Color(data.red(), data.green(), data.blue());
        this.wallColor  = roofColor.darker();
        this.labelColor = Color.WHITE;
    }

    // ── Legacy constructor ────────────────────────────────────────────
    public Building(String name, int x, int y, int width, int depth,
                    int floors, Color roofColor) {
        this.data       = null;
        this.name       = name;
        this.x          = x;
        this.y          = y;
        this.width      = width;
        this.depth      = depth;
        this.floors     = floors;
        this.roofColor  = roofColor;
        this.wallColor  = roofColor.darker();
        this.labelColor = Color.WHITE;
    }

    // ── Draw ──────────────────────────────────────────────────────────
    public void draw(Graphics2D g, int ox, int oy) {
        AffineTransform saved = g.getTransform();

        if (data != null && data.isPolygon()) {
            drawPolygon(g, ox, oy);
            g.setTransform(saved);
            return;
        }

        int sx = x - ox;
        int sy = y - oy;

        if (data != null && data.rotationDegrees != 0f) {
            float cx = sx + width / 2f;
            float cy = sy + depth / 2f;
            g.rotate(Math.toRadians(data.rotationDegrees), cx, cy);
        }

        // Shadow
        g.setColor(new Color(0, 0, 0, 50));
        g.fillRect(sx + 4, sy + 4, width, depth);

        // Roof
        g.setColor(roofColor);
        g.fillRect(sx, sy, width, depth);

        // Wall accents
        g.setColor(wallColor);
        g.fillRect(sx, sy + depth - 4, width, 4);
        g.fillRect(sx + width - 4, sy, 4, depth);

        // Border
        g.setColor(wallColor.darker());
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(sx, sy, width, depth);
        g.setStroke(new BasicStroke(1f));

        drawLabel(g, sx, sy, width, depth);
        g.setTransform(saved);
    }

    private void drawPolygon(Graphics2D g, int ox, int oy) {
        Polygon poly = buildScreenPolygon(ox, oy);

        g.setColor(roofColor);
        g.fillPolygon(poly);

        g.setColor(wallColor.darker());
        g.setStroke(new BasicStroke(1.5f));
        g.drawPolygon(poly);
        g.setStroke(new BasicStroke(1f));

        // FIX: label at polygon centroid, not at (sx,sy)
        Rectangle b = poly.getBounds();
        drawLabel(g, b.x, b.y, b.width, b.height);
    }

    /**
     * Shared label renderer. cx/cy are top-left of the bounding area,
     * cw/ch are width and height — label is centred inside.
     */
    private void drawLabel(Graphics2D g, int bx, int by, int bw, int bh) {
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        FontMetrics fm = g.getFontMetrics();
        String[] words = name.split(" ");
        int lineH  = fm.getHeight();
        int totalH = lineH * words.length;
        int startY = by + bh / 2 - totalH / 2 + fm.getAscent();

        for (String word : words) {
            int tw = fm.stringWidth(word);
            int tx = bx + bw / 2 - tw / 2;
            g.setColor(new Color(0, 0, 0, 120));
            g.drawString(word, tx + 1, startY + 1);
            g.setColor(labelColor);
            g.drawString(word, tx, startY);
            startY += lineH;
        }
    }

    // ── Collision ─────────────────────────────────────────────────────
    public Rectangle getBounds() {
        if (data != null && data.isPolygon()) {
            return buildWorldPolygon().getBounds();
        }
        return new Rectangle(x, y, width, depth);
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private Polygon buildWorldPolygon() {
        Polygon p = new Polygon();
        for (int i = 0; i < data.polygonX.length; i++)
            p.addPoint(data.polygonX[i], data.polygonZ[i]);
        return p;
    }

    private Polygon buildScreenPolygon(int ox, int oy) {
        Polygon p = new Polygon();
        for (int i = 0; i < data.polygonX.length; i++)
            p.addPoint(data.polygonX[i] - ox, data.polygonZ[i] - oy);
        return p;
    }

    // ── Getters — all delegate to getBounds() so polygons are correct ─
    public String getName()      { return name; }
    public int    getFloors()    { return floors; }
    public Color  getRoofColor() { return roofColor; }
    public BuildingData getData(){ return data; }

    // In Building.java — replace or supplement getX(), getY(), getWidth(), getDepth()

    public int getX() {
        if (data.isPolygon()) return min(data.polygonX);
        return (int) data.x;
    }

    public int getY() {  // this is Z in world space
        if (data.isPolygon()) return min(data.polygonZ);
        return (int) data.z;
    }

    public int getWidth() {
        if (data.isPolygon()) return max(data.polygonX) - min(data.polygonX);
        return (int) data.width;
    }

    public int getDepth() {
        if (data.isPolygon()) return max(data.polygonZ) - min(data.polygonZ);
        return (int) data.depth;
    }

    public void drawPseudo3D(Graphics2D g, Camera cam, ProjectionMode mode) {
        int sx  = mode.screenX(data.x, data.z, cam);
        int sy  = mode.screenY(data.x, data.z, cam);
        int sw  = (int) data.width;
        int sh  = (int) data.depth;
        int wh  = mode.wallHeight(data.floors);   // wall extrusion upward

        // --- Shadow (offset rectangle, drawn first)
        g.setColor(new Color(0, 0, 0, 40));
        g.fillRect(sx + 5, sy + 5, sw, sh);

        // --- South wall (front face — highest Z, visible to camera)
        //     Drawn as a parallelogram below the roof rect
        if (wh > 0) {
            int[] wx = { sx,      sx + sw,      sx + sw,      sx      };
            int[] wy = { sy + sh, sy + sh,      sy + sh + wh, sy + sh + wh };
            g.setColor(wallColor);          // roofColor.darker()
            g.fillPolygon(wx, wy, 4);
            // East wall
            int[] ex = { sx + sw, sx + sw,       sx + sw,       sx + sw };
            int[] ey = { sy,      sy + sh,        sy + sh + wh,  sy + wh };
            g.setColor(wallColor.darker());
            g.fillPolygon(ex, ey, 4);
        }

        // --- Roof
        g.setColor(roofColor);
        g.fillRect(sx, sy - wh, sw, sh);   // roof sits wh pixels above ground rect

        // --- Roof border
        g.setColor(wallColor.darker());
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(sx, sy - wh, sw, sh);
        g.setStroke(new BasicStroke(1f));

        drawLabel(g, sx, sy - wh, sw, sh);
    }

    private int min(int[] arr) { int m = arr[0]; for (int v : arr) if (v < m) m = v; return m; }
    private int max(int[] arr) { int m = arr[0]; for (int v : arr) if (v > m) m = v; return m; }
    public float getCenterX() { return getX() + getWidth()  / 2f; }
    public float getCenterY() { return getY() + getDepth() / 2f; }
}