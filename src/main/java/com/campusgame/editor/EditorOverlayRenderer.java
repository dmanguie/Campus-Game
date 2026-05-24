package com.campusgame.editor;

import com.campusgame.map.Building;
import com.campusgame.map.CampusMap;
import com.campusgame.map.data.BuildingData;
import com.campusgame.renderer.Camera;

import java.awt.*;

/**
 * EDITOR OVERLAY RENDERER (editor/EditorOverlayRenderer.java)
 * -------------------------------------------------------------
 * Draws the editor HUD on top of the normal game scene.
 * Called from Renderer.paintComponent() only when editor is active.
 *
 * Draws:
 *  - Semi-transparent editor toolbar banner at the top
 *  - Ghost building preview (gray, semi-transparent) at cursor
 *  - Selection highlight (yellow border) around selected building
 *  - Grid snap dots
 *  - Status message
 *  - Tool indicator
 */
public class EditorOverlayRenderer {

    private final EditorState state;
    private final CampusMap   campusMap;
    private final Camera      camera;

    private static final Color GHOST_FILL    = new Color(150, 150, 150, 100);
    private static final Color GHOST_BORDER  = new Color(255, 255, 255, 180);
    private static final Color SELECT_COLOR  = new Color(255, 230,   0, 200);
    private static final Color BANNER_BG     = new Color(  0,   0,   0, 170);
    private static final Color STATUS_BG     = new Color( 20,  20,  20, 200);

    public EditorOverlayRenderer(EditorState state, CampusMap campusMap, Camera camera) {
        this.state     = state;
        this.campusMap = campusMap;
        this.camera    = camera;
    }

    /**
     * Call from Renderer.paintComponent() after all normal layers.
     * Only draws when editor is active.
     */
    public void draw(Graphics2D g, int screenW, int screenH) {
        if (!state.isActive()) return;

        drawBanner(g, screenW);
        drawGhost(g);
        drawSelection(g);
        drawStatus(g, screenW, screenH);
    }

    // ── Top banner ────────────────────────────────────────────────────
    private void drawBanner(Graphics2D g, int screenW) {
        g.setColor(BANNER_BG);
        g.fillRect(0, 0, screenW, 28);

        g.setFont(new Font("Consolas", Font.BOLD, 13));

        // Left: mode
        g.setColor(new Color(255, 220, 50));
        g.drawString("✏  EDITOR MODE", 10, 19);

        // Center: current tool
        String toolLabel = switch (state.getCurrentTool()) {
            case PLACE  -> "[ P ] PLACE";
            case SELECT -> "[ S ] SELECT";
            case DELETE -> "[ X ] DELETE";
        };
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(toolLabel, screenW / 2 - fm.stringWidth(toolLabel) / 2, 19);

        // Right: hint
        g.setColor(new Color(180, 180, 180));
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        String hint = "F1=Exit  P=Place  X=Delete  Ctrl+S=Save  Ctrl+Z=Undo";
        g.drawString(hint, screenW - g.getFontMetrics().stringWidth(hint) - 10, 19);
    }

    // ── Ghost building preview ────────────────────────────────────────
    private void drawGhost(Graphics2D g) {
        BuildingData ghost = state.getGhostBuilding();
        if (ghost == null || state.getCurrentTool() != EditorState.Tool.PLACE) return;

        int sx = camera.worldToScreenX(ghost.x);
        int sy = camera.worldToScreenY(ghost.z);
        int sw = (int) ghost.width;
        int sh = (int) ghost.depth;

        g.setColor(GHOST_FILL);
        g.fillRect(sx, sy, sw, sh);

        g.setColor(GHOST_BORDER);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[]{6f, 4f}, 0f));
        g.drawRect(sx, sy, sw, sh);
        g.setStroke(new BasicStroke(1f));

        // Label
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        g.drawString(ghost.name, sx + 4, sy + 12);
    }

    // ── Selection highlight ───────────────────────────────────────────
    private void drawSelection(Graphics2D g) {
        BuildingData sel = state.getSelectedBuilding();
        if (sel == null) return;

        // Find the Building wrapper to get its screen rect
        for (Building b : campusMap.getBuildings()) {
            if (b.getData() == sel) {
                int sx = camera.worldToScreenX(b.getX());
                int sy = camera.worldToScreenY(b.getY());
                int sw = b.getWidth();
                int sh = b.getDepth();

                g.setColor(SELECT_COLOR);
                g.setStroke(new BasicStroke(3f));
                g.drawRect(sx - 2, sy - 2, sw + 4, sh + 4);
                g.setStroke(new BasicStroke(1f));

                // Name tag above
                g.setFont(new Font("SansSerif", Font.BOLD, 10));
                g.setColor(new Color(0, 0, 0, 160));
                g.fillRoundRect(sx, sy - 18, g.getFontMetrics().stringWidth(sel.name) + 8, 16, 4, 4);
                g.setColor(SELECT_COLOR);
                g.drawString(sel.name, sx + 4, sy - 6);
                break;
            }
        }
    }

    // ── Status message (bottom centre) ───────────────────────────────
    private void drawStatus(Graphics2D g, int screenW, int screenH) {
        String msg = state.getStatusMessage();
        if (msg.isEmpty()) return;

        g.setFont(new Font("Consolas", Font.BOLD, 13));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(msg);
        int tx = screenW / 2 - tw / 2;
        int ty = screenH - 20;

        g.setColor(STATUS_BG);
        g.fillRoundRect(tx - 8, ty - 16, tw + 16, 22, 6, 6);
        g.setColor(new Color(100, 255, 140));
        g.drawString(msg, tx, ty);
    }
}