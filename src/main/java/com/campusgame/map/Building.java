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
 * Phase zoom: draw() and drawPseudo3D() now accept zoom from Camera
 * so buildings scale correctly with mouse wheel zoom.
 */
public class Building {

    private final BuildingData data;
    private final Color roofColor, wallColor, labelColor;
    private final String name;

    private final int x, y, width, depth, floors;

    public static final int PIXELS_PER_FLOOR = 20;

    // ── Phase-3 constructor: from BuildingData ────────────────────────
    public Building(BuildingData data) {
        this.data      = data;
        this.name      = data.name;
        this.x         = (int) data.x;
        this.y         = (int) data.z;
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

    // ── Draw (2D top-down) ────────────────────────────────────────────

    /** Called by Renderer — legacy signature kept for compatibility. */
    public void draw(Graphics2D g, int ox, int oy) {
        draw(g, ox, oy, 1f);
    }

    /** Zoom-aware 2D draw. ox/oy are raw world offsets from Camera. */
    public void draw(Graphics2D g, int ox, int oy, float zoom) {
        AffineTransform saved = g.getTransform();

        if (data != null && data.isPolygon()) {
            drawPolygon(g, ox, oy, zoom);
            g.setTransform(saved);
            return;
        }

        int sx = (int)((x - ox) * zoom);
        int sy = (int)((y - oy) * zoom);
        int sw = (int)(width  * zoom);
        int sh = (int)(depth  * zoom);

        if (data != null && data.rotationDegrees != 0f) {
            float cx = sx + sw / 2f;
            float cy = sy + sh / 2f;
            g.rotate(Math.toRadians(data.rotationDegrees), cx, cy);
        }

        // Shadow
        g.setColor(new Color(0, 0, 0, 50));
        g.fillRect(sx + 4, sy + 4, sw, sh);

        // Roof
        g.setColor(roofColor);
        g.fillRect(sx, sy, sw, sh);

        // Wall accents
        g.setColor(wallColor);
        g.fillRect(sx, sy + sh - 4, sw, 4);
        g.fillRect(sx + sw - 4, sy, 4, sh);

        // Border
        g.setColor(wallColor.darker());
        g.setStroke(new BasicStroke(Math.max(0.5f, 1.5f * zoom)));
        g.drawRect(sx, sy, sw, sh);
        g.setStroke(new BasicStroke(1f));

        drawLabel(g, sx, sy, sw, sh);
        g.setTransform(saved);
    }

    // ── Polygon draw ──────────────────────────────────────────────────

    private void drawPolygon(Graphics2D g, int ox, int oy, float zoom) {
        Polygon poly = buildScreenPolygon(ox, oy, zoom);

        g.setColor(roofColor);
        g.fillPolygon(poly);

        g.setColor(wallColor.darker());
        g.setStroke(new BasicStroke(Math.max(0.5f, 1.5f * zoom)));
        g.drawPolygon(poly);
        g.setStroke(new BasicStroke(1f));

        Rectangle b = poly.getBounds();
        drawLabel(g, b.x, b.y, b.width, b.height);
    }

    // ── Pseudo-3D draw ────────────────────────────────────────────────

    public void drawPseudo3D(Graphics2D g, Camera cam, ProjectionMode mode) {
        float zoom = cam.getZoom();
        int sx  = mode.screenX(data.x, data.z, cam);
        int sy  = mode.screenY(data.x, data.z, cam);
        int sw  = (int)(data.width  * zoom);
        int sh  = (int)(data.depth  * zoom);
        int wh  = (int)(mode.wallHeight(data.floors) * zoom);

        // Shadow
        g.setColor(new Color(0, 0, 0, 40));
        g.fillRect(sx + 5, sy + 5, sw, sh);

        // South wall (front face)
        if (wh > 0) {
            int[] wx = { sx,      sx + sw, sx + sw,      sx           };
            int[] wy = { sy + sh, sy + sh, sy + sh + wh, sy + sh + wh };
            g.setColor(wallColor);
            g.fillPolygon(wx, wy, 4);

            // East wall
            int[] ex = { sx + sw, sx + sw, sx + sw,      sx + sw };
            int[] ey = { sy,      sy + sh, sy + sh + wh, sy + wh };
            g.setColor(wallColor.darker());
            g.fillPolygon(ex, ey, 4);
        }

        // Roof
        g.setColor(roofColor);
        g.fillRect(sx, sy - wh, sw, sh);

        // Roof border
        g.setColor(wallColor.darker());
        g.setStroke(new BasicStroke(Math.max(0.5f, 1.5f * zoom)));
        g.drawRect(sx, sy - wh, sw, sh);
        g.setStroke(new BasicStroke(1f));

        drawLabel(g, sx, sy - wh, sw, sh);
    }

    // ── Label ─────────────────────────────────────────────────────────

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

    /** Called by Renderer instead of draw() when editor is active — shows floor count. */
    public void drawEditor(Graphics2D g, int ox, int oy, float zoom) {
        AffineTransform saved = g.getTransform();

        if (data != null && data.isPolygon()) {
            drawPolygonEditor(g, ox, oy, zoom);
            g.setTransform(saved);
            return;
        }

        int sx = (int)((x - ox) * zoom);
        int sy = (int)((y - oy) * zoom);
        int sw = (int)(width  * zoom);
        int sh = (int)(depth  * zoom);

        if (data != null && data.rotationDegrees != 0f) {
            float cx = sx + sw / 2f;
            float cy = sy + sh / 2f;
            g.rotate(Math.toRadians(data.rotationDegrees), cx, cy);
        }

        // Shadow
        g.setColor(new Color(0, 0, 0, 50));
        g.fillRect(sx + 4, sy + 4, sw, sh);

        // Roof
        g.setColor(roofColor);
        g.fillRect(sx, sy, sw, sh);

        // Wall accents
        g.setColor(wallColor);
        g.fillRect(sx, sy + sh - 4, sw, 4);
        g.fillRect(sx + sw - 4, sy, 4, sh);

        // Border
        g.setColor(wallColor.darker());
        g.setStroke(new BasicStroke(Math.max(0.5f, 1.5f * zoom)));
        g.drawRect(sx, sy, sw, sh);
        g.setStroke(new BasicStroke(1f));

        drawLabelWithFloors(g, sx, sy, sw, sh);
        g.setTransform(saved);
    }

    private void drawPolygonEditor(Graphics2D g, int ox, int oy, float zoom) {
        Polygon poly = buildScreenPolygon(ox, oy, zoom);

        g.setColor(roofColor);
        g.fillPolygon(poly);

        g.setColor(wallColor.darker());
        g.setStroke(new BasicStroke(Math.max(0.5f, 1.5f * zoom)));
        g.drawPolygon(poly);
        g.setStroke(new BasicStroke(1f));

        Rectangle b = poly.getBounds();
        drawLabelWithFloors(g, b.x, b.y, b.width, b.height);
    }

    private void drawLabelWithFloors(Graphics2D g, int bx, int by, int bw, int bh) {
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        FontMetrics fm = g.getFontMetrics();

        // Name + floor count on separate lines
        String floorStr = data != null ? data.floors + "F" : "";
        String[] lines = floorStr.isEmpty()
                ? name.split(" ")
                : concat(name.split(" "), floorStr);

        int lineH  = fm.getHeight();
        int totalH = lineH * lines.length;
        int startY = by + bh / 2 - totalH / 2 + fm.getAscent();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // Floor count line gets a different color
            boolean isFloorLine = (i == lines.length - 1) && !floorStr.isEmpty();
            int tw = fm.stringWidth(line);
            int tx = bx + bw / 2 - tw / 2;
            g.setColor(new Color(0, 0, 0, 120));
            g.drawString(line, tx + 1, startY + 1);
            g.setColor(isFloorLine ? new Color(255, 220, 80) : labelColor);
            g.drawString(line, tx, startY);
            startY += lineH;
        }
    }

    private String[] concat(String[] arr, String extra) {
        String[] result = new String[arr.length + 1];
        System.arraycopy(arr, 0, result, 0, arr.length);
        result[arr.length] = extra;
        return result;
    }

    /** Pseudo-3D draw with exaggerated wall height for editor mode. */
    public void drawPseudo3DEditor(Graphics2D g, Camera cam, ProjectionMode mode) {
        float zoom = cam.getZoom();
        int sx  = mode.screenX(data.x, data.z, cam);
        int sy  = mode.screenY(data.x, data.z, cam);
        int sw  = (int)(data.width * zoom);
        int sh  = (int)(data.depth * zoom);

        // Exaggerated wall height so floor differences are clearly visible
        int wh  = (int)(data.floors * 22 * zoom);

        // Shadow
        g.setColor(new Color(0, 0, 0, 40));
        g.fillRect(sx + 5, sy + 5, sw, sh);

        // South wall
        if (wh > 0) {
            int[] wx = { sx,      sx + sw, sx + sw,      sx           };
            int[] wy = { sy + sh, sy + sh, sy + sh + wh, sy + sh + wh };
            g.setColor(wallColor);
            g.fillPolygon(wx, wy, 4);

            // East wall — slightly darker
            int[] ex = { sx + sw, sx + sw, sx + sw,      sx + sw };
            int[] ey = { sy,      sy + sh, sy + sh + wh, sy + wh };
            g.setColor(wallColor.darker());
            g.fillPolygon(ex, ey, 4);

            // Floor count indicator on the south wall face
            g.setFont(new Font("Consolas", Font.BOLD, (int)(10 * zoom)));
            FontMetrics fm = g.getFontMetrics();
            String floorLabel = data.floors + "F";
            int lw = fm.stringWidth(floorLabel);
            int lx = sx + sw / 2 - lw / 2;
            int ly = sy + sh + wh / 2 + fm.getAscent() / 2;
            g.setColor(new Color(0, 0, 0, 100));
            g.drawString(floorLabel, lx + 1, ly + 1);
            g.setColor(new Color(255, 220, 80));
            g.drawString(floorLabel, lx, ly);
        }

        // Roof
        g.setColor(roofColor);
        g.fillRect(sx, sy - wh, sw, sh);

        // Roof border
        g.setColor(wallColor.darker());
        g.setStroke(new BasicStroke(Math.max(0.5f, 1.5f * zoom)));
        g.drawRect(sx, sy - wh, sw, sh);
        g.setStroke(new BasicStroke(1f));

        drawLabelWithFloors(g, sx, sy - wh, sw, sh);
    }

    // ── Screen polygon helpers ────────────────────────────────────────

    private Polygon buildScreenPolygon(int ox, int oy, float zoom) {
        Polygon p = new Polygon();
        for (int i = 0; i < data.polygonX.length; i++) {
            p.addPoint(
                    (int)((data.polygonX[i] - ox) * zoom),
                    (int)((data.polygonZ[i] - oy) * zoom)
            );
        }
        return p;
    }

    /** No-zoom version used for collision only. */
    private Polygon buildScreenPolygon(int ox, int oy) {
        return buildScreenPolygon(ox, oy, 1f);
    }

    // ── Collision ─────────────────────────────────────────────────────

    public Rectangle getBounds() {
        if (data != null && data.isPolygon()) {
            return buildWorldPolygon().getBounds();
        }
        return new Rectangle(x, y, width, depth);
    }

    private Polygon buildWorldPolygon() {
        Polygon p = new Polygon();
        for (int i = 0; i < data.polygonX.length; i++)
            p.addPoint(data.polygonX[i], data.polygonZ[i]);
        return p;
    }

    // ── Getters ───────────────────────────────────────────────────────

    public String getName()       { return name; }
    public int    getFloors()     { return floors; }
    public Color  getRoofColor()  { return roofColor; }
    public BuildingData getData() { return data; }

    public int getX() {
        if (data != null && data.isPolygon()) return min(data.polygonX);
        return (int) data.x;
    }

    public int getY() {
        if (data != null && data.isPolygon()) return min(data.polygonZ);
        return (int) data.z;
    }

    public int getWidth() {
        if (data != null && data.isPolygon()) return max(data.polygonX) - min(data.polygonX);
        return (int) data.width;
    }

    public int getDepth() {
        if (data != null && data.isPolygon()) return max(data.polygonZ) - min(data.polygonZ);
        return (int) data.depth;
    }

    public float getCenterX() { return getX() + getWidth()  / 2f; }
    public float getCenterY() { return getY() + getDepth() / 2f; }

    // ── Helpers ───────────────────────────────────────────────────────

    private int min(int[] arr) { int m = arr[0]; for (int v : arr) if (v < m) m = v; return m; }
    private int max(int[] arr) { int m = arr[0]; for (int v : arr) if (v > m) m = v; return m; }
}