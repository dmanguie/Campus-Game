package com.campusgame.editor.gizmo;

import com.campusgame.map.data.BuildingData;
import com.campusgame.renderer.Camera;

import java.awt.*;

/**
 * GIZMO RENDERER (editor/gizmo/GizmoRenderer.java)
 * ---------------------------------------------------
 * Draws all visual editor handles (gizmos) on top of the scene.
 *
 * Gizmos drawn:
 *   Selected rect building  → 8 resize handles + bounding box outline
 *   Selected polygon bldg   → vertex dots + edge midpoint hints
 *   Drag-move active        → dashed outline at drag target position
 *   Multi-select            → cyan bounding box around all selected
 *   Grid snap dots          → subtle dot grid when placing/dragging
 *
 * Called by EditorOverlayRenderer after all other overlays.
 */
public class GizmoRenderer {

    // ── Colors ────────────────────────────────────────────────────────
    private static final Color HANDLE_FILL     = new Color(255, 255, 255, 220);
    private static final Color HANDLE_BORDER   = new Color( 50, 150, 255, 255);
    private static final Color HANDLE_HOVER    = new Color(255, 220,  50, 255);
    private static final Color HANDLE_ACTIVE   = new Color(255,  80,  80, 255);
    private static final Color SELECTION_BOX   = new Color( 50, 180, 255, 200);
    private static final Color MULTI_SELECT    = new Color(  0, 220, 255, 180);
    private static final Color DRAG_GHOST      = new Color(255, 255, 255,  60);
    private static final Color DRAG_OUTLINE    = new Color(255, 255, 255, 160);
    private static final Color GRID_DOT        = new Color(255, 255, 255,  30);
    private static final Color MOVE_CROSSHAIR  = new Color(255, 220,  50, 200);

    private static final int HANDLE_SIZE = ResizeHandle.HANDLE_SIZE;

    private final Camera camera;

    public GizmoRenderer(Camera camera) {
        this.camera = camera;
    }

    // ── Main draw entry points ────────────────────────────────────────

    /**
     * Draw resize handles for a selected rectangle building.
     * @param handles   the 8 ResizeHandle objects (already updated with world pos)
     * @param hoveredIdx which handle index the mouse is over (-1 = none)
     * @param activeIdx  which handle is being dragged (-1 = none)
     */
    public void drawResizeHandles(Graphics2D g, ResizeHandle[] handles,
                                  int hoveredIdx, int activeIdx) {
        for (int i = 0; i < handles.length; i++) {
            ResizeHandle h = handles[i];
            int sx = camera.worldToScreenX(h.worldX);
            int sy = camera.worldToScreenY(h.worldZ);

            Color fill = (i == activeIdx) ? HANDLE_ACTIVE
                    : (i == hoveredIdx) ? HANDLE_HOVER
                    : HANDLE_FILL;

            g.setColor(fill);
            g.fillRect(sx - HANDLE_SIZE/2, sy - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
            g.setColor(HANDLE_BORDER);
            g.setStroke(new BasicStroke(1.5f));
            g.drawRect(sx - HANDLE_SIZE/2, sy - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
            g.setStroke(new BasicStroke(1f));
        }
    }

    /**
     * Draw selection bounding box around a rectangle building.
     */
    public void drawSelectionBox(Graphics2D g, BuildingData b) {
        if (b.isPolygon()) { drawPolygonSelectionBox(g, b); return; }

        int sx = camera.worldToScreenX(b.x);
        int sy = camera.worldToScreenY(b.z);
        int sw = (int) b.width;
        int sh = (int) b.depth;

        g.setColor(SELECTION_BOX);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(sx - 2, sy - 2, sw + 4, sh + 4);
        g.setStroke(new BasicStroke(1f));
    }

    private void drawPolygonSelectionBox(Graphics2D g, BuildingData b) {
        int n = b.polygonX.length;
        int[] sx = new int[n], sy = new int[n];
        for (int i = 0; i < n; i++) {
            sx[i] = camera.worldToScreenX(b.polygonX[i]);
            sy[i] = camera.worldToScreenY(b.polygonZ[i]);
        }
        g.setColor(SELECTION_BOX);
        g.setStroke(new BasicStroke(2f));
        g.drawPolygon(sx, sy, n);
        g.setStroke(new BasicStroke(1f));
    }

    /**
     * Draw drag-move ghost: semi-transparent copy of building at drag target.
     */
    public void drawDragGhost(Graphics2D g, BuildingData b, float targetX, float targetZ) {
        if (b.isPolygon()) return; // polygon drag ghost handled by ShapeEditState
        int sx = camera.worldToScreenX(targetX);
        int sy = camera.worldToScreenY(targetZ);
        int sw = (int) b.width;
        int sh = (int) b.depth;

        g.setColor(DRAG_GHOST);
        g.fillRect(sx, sy, sw, sh);
        g.setColor(DRAG_OUTLINE);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[]{6f, 4f}, 0f));
        g.drawRect(sx, sy, sw, sh);
        g.setStroke(new BasicStroke(1f));
    }

    /**
     * Draw a move crosshair at the cursor when dragging a building.
     */
    public void drawMoveCursor(Graphics2D g, int screenX, int screenY) {
        int arm = 12;
        g.setColor(MOVE_CROSSHAIR);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(screenX - arm, screenY, screenX + arm, screenY);
        g.drawLine(screenX, screenY - arm, screenX, screenY + arm);
        g.fillOval(screenX - 3, screenY - 3, 6, 6);
        g.setStroke(new BasicStroke(1f));
    }

    /**
     * Draw multi-select cyan bounding box.
     */
    public void drawMultiSelectBox(Graphics2D g, int x1, int y1, int x2, int y2) {
        int sx = Math.min(x1, x2), sy = Math.min(y1, y2);
        int sw = Math.abs(x2 - x1), sh = Math.abs(y2 - y1);
        g.setColor(new Color(0, 220, 255, 30));
        g.fillRect(sx, sy, sw, sh);
        g.setColor(MULTI_SELECT);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[]{4f, 3f}, 0f));
        g.drawRect(sx, sy, sw, sh);
        g.setStroke(new BasicStroke(1f));
    }

    /**
     * Subtle snap grid dots — drawn when user is actively placing or dragging.
     * Only draws dots within the visible screen area for performance.
     */
    public void drawSnapGrid(Graphics2D g, int screenW, int screenH,
                             float gridSize, int camOX, int camOY) {
        g.setColor(GRID_DOT);
        int startX = (int)(camOX / gridSize) * (int)gridSize;
        int startY = (int)(camOY / gridSize) * (int)gridSize;
        for (int wx = startX; wx < camOX + screenW + gridSize; wx += gridSize) {
            for (int wz = startY; wz < camOY + screenH + gridSize; wz += gridSize) {
                int sx = camera.worldToScreenX(wx);
                int sy = camera.worldToScreenY(wz);
                if (sx >= 0 && sx <= screenW && sy >= 0 && sy <= screenH) {
                    g.fillRect(sx - 1, sy - 1, 2, 2);
                }
            }
        }
    }

    /**
     * Draw building name label above the selection box.
     */
    public void drawSelectionLabel(Graphics2D g, BuildingData b, Color labelColor) {
        int sx = b.isPolygon()
                ? camera.worldToScreenX(b.polygonX[0])
                : camera.worldToScreenX(b.x);
        int sy = b.isPolygon()
                ? camera.worldToScreenY(b.polygonZ[0])
                : camera.worldToScreenY(b.z);

        g.setFont(new Font("Consolas", Font.BOLD, 11));
        FontMetrics fm = g.getFontMetrics();
        String label = b.name + " [" + b.floors + "F]";
        int tw = fm.stringWidth(label);

        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(sx - 2, sy - 20, tw + 10, 17, 4, 4);
        g.setColor(labelColor);
        g.drawString(label, sx + 3, sy - 7);
    }
}