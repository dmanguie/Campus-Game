package com.campusgame.editor;

import com.campusgame.renderer.Camera;
import com.campusgame.world.interior.InteriorRoom;
import com.campusgame.world.interior.InteriorScene;

import java.awt.*;

/**
 * INTERIOR EDITOR OVERLAY RENDERER  (editor/InteriorEditorOverlayRenderer.java)
 *
 * Draws all editor chrome on top of the interior scene:
 *   1. Top editor banner (tool name + hotkey hint)
 *   2. Ghost room (placement preview)
 *   3. Room selection highlight + resize handles
 *   4. Bottom status bar
 *
 * Called from Renderer.paintComponent() after InteriorRenderer.draw().
 * Pattern mirrors EditorOverlayRenderer so it is easy to extend.
 */
public class InteriorEditorOverlayRenderer {

    // ── Colours ───────────────────────────────────────────────────────
    private static final Color BANNER_BG     = new Color(  0,  0,  0, 175);
    private static final Color GHOST_FILL    = new Color(150,200,150,  80);
    private static final Color GHOST_BORDER  = new Color(100,220,120, 200);
    private static final Color SEL_COLOR     = new Color(255,230,  0, 220);
    private static final Color HANDLE_FILL   = new Color(255,230,  0, 240);
    private static final Color HANDLE_BORDER = new Color( 80, 60,  0, 200);
    private static final Color STATUS_BG     = new Color( 20, 20, 20, 210);
    private static final Color LEGEND_BG     = new Color(  0,  0,  0, 160);
    private static final Color ROOM_LABEL    = new Color( 60,200,100, 220);

    private static final int HANDLE_R = 6;   // handle square half-size (pixels)

    private final InteriorEditorMode editor;
    private final Camera             camera;

    public InteriorEditorOverlayRenderer(InteriorEditorMode editor, Camera camera) {
        this.editor = editor;
        this.camera = camera;
    }

    // ── Main entry point ──────────────────────────────────────────────

    public void draw(Graphics2D g, int screenW, int screenH) {
        if (!editor.isActive()) return;
        drawBanner(g, screenW);
        drawGhost(g);
        drawRoomLabels(g);
        drawSelection(g);
        drawLegend(g, screenW);
        drawStatus(g, screenW, screenH);
    }

    // ── Banner ────────────────────────────────────────────────────────

    private void drawBanner(Graphics2D g, int screenW) {
        g.setColor(BANNER_BG);
        g.fillRect(0, 38, screenW, 26);   // sits below the InteriorRenderer HUD strip (38px)

        g.setFont(new Font("Consolas", Font.BOLD, 12));
        g.setColor(new Color(100, 255, 140));
        g.drawString("✏  INTERIOR EDITOR", 10, 57);

        String toolLabel = switch (editor.getCurrentTool()) {
            case PLACE  -> "[ P ] PLACE ROOM";
            case SELECT -> "[ S ] SELECT / MOVE";
            case DELETE -> "[ X ] DELETE ROOM";
        };
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(toolLabel, screenW / 2 - fm.stringWidth(toolLabel) / 2, 57);

        g.setColor(new Color(180, 180, 180));
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        String hint = "P=Place  S=Select  X=Delete  N=Rename  G=GridSnap  Esc=Deselect  F1=Exit  F3=Controls";
        g.drawString(hint, screenW - g.getFontMetrics().stringWidth(hint) - 8, 57);
    }

    // ── Ghost room (placement preview) ───────────────────────────────

    private void drawGhost(Graphics2D g) {
        InteriorRoom ghost = editor.getGhost();
        if (ghost == null || editor.getCurrentTool() != InteriorEditorMode.Tool.PLACE) return;

        int rx = camera.worldToScreenX(ghost.x);
        int rz = camera.worldToScreenY(ghost.z);
        int rw = (int) ghost.width;
        int rd = (int) ghost.depth;

        g.setColor(GHOST_FILL);
        g.fillRect(rx, rz, rw, rd);

        g.setColor(GHOST_BORDER);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[]{6f, 4f}, 0f));
        g.drawRect(rx, rz, rw, rd);
        g.setStroke(new BasicStroke(1f));

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        g.drawString("New Room", rx + 5, rz + 14);
    }

    // ── Room labels (id tags while editor open) ───────────────────────

    private void drawRoomLabels(Graphics2D g) {
        InteriorScene scene = editor.getScene();
        if (scene == null) return;
        g.setFont(new Font("Consolas", Font.PLAIN, 9));
        for (InteriorRoom r : scene.rooms) {
            int rx = camera.worldToScreenX(r.x);
            int rz = camera.worldToScreenY(r.z);
            g.setColor(ROOM_LABEL);
            g.drawString("[" + r.id + "]", rx + 3, rz + 10);
        }
    }

    // ── Selection highlight + resize handles ──────────────────────────

    private void drawSelection(Graphics2D g) {
        InteriorRoom sel = editor.getSelected();
        if (sel == null) return;

        int rx = camera.worldToScreenX(sel.x);
        int rz = camera.worldToScreenY(sel.z);
        int rw = (int) sel.width;
        int rd = (int) sel.depth;

        // Selection outline
        g.setColor(SEL_COLOR);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRect(rx - 2, rz - 2, rw + 4, rd + 4);
        g.setStroke(new BasicStroke(1f));

        // Name tag above selection
        g.setFont(new Font("Consolas", Font.BOLD, 10));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(sel.displayName);
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRoundRect(rx, rz - 20, tw + 10, 16, 4, 4);
        g.setColor(SEL_COLOR);
        g.drawString(sel.displayName, rx + 5, rz - 8);

        // Resize handles (NW=0, NE=1, SW=2, SE=3)
        int[][] corners = editor.handleScreenCorners(sel);
        for (int i = 0; i < corners.length; i++) {
            int hx = corners[i][0];
            int hz = corners[i][1];
            boolean active = (i == editor.getActiveHandle());
            g.setColor(active ? Color.WHITE : HANDLE_FILL);
            g.fillRect(hx - HANDLE_R, hz - HANDLE_R, HANDLE_R * 2, HANDLE_R * 2);
            g.setColor(HANDLE_BORDER);
            g.drawRect(hx - HANDLE_R, hz - HANDLE_R, HANDLE_R * 2, HANDLE_R * 2);
        }
    }

    // ── Legend panel ─────────────────────────────────────────────────

    private void drawLegend(Graphics2D g, int screenW) {
        String[] lines = {
                "INTERIOR EDITOR",
                "P = place room",
                "S = select / move",
                "  drag = move",
                "  corner = resize",
                "X = delete room",
                "N = rename selected",
                "G = cycle grid snap",
                "Esc = deselect",
                "F1 = exit editor"
        };

        g.setFont(new Font("Consolas", Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();
        int lineH = fm.getHeight() + 1;
        int boxW  = 190;
        int boxH  = lines.length * lineH + 10;
        int boxX  = screenW - boxW - 10;
        int boxY  = 68;   // below interior HUD + editor banner

        g.setColor(LEGEND_BG);
        g.fillRoundRect(boxX, boxY, boxW, boxH, 6, 6);

        int ty = boxY + lineH;
        for (String line : lines) {
            boolean header = line.equals("INTERIOR EDITOR");
            if (header) {
                g.setFont(new Font("Consolas", Font.BOLD, 11));
                g.setColor(new Color(100, 255, 140));
            } else {
                g.setFont(new Font("Consolas", Font.PLAIN, 11));
                g.setColor(new Color(140, 220, 255));
            }
            g.drawString(line, boxX + 8, ty);
            ty += lineH;
        }
    }

    // ── Status bar ────────────────────────────────────────────────────

    private void drawStatus(Graphics2D g, int screenW, int screenH) {
        String msg = editor.getStatusMessage();
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