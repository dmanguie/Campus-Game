package com.campusgame.editor.panel;

import com.campusgame.editor.EditorState;
import com.campusgame.editor.layer.EditorLayer;
import com.campusgame.editor.layer.LayerManager;

import java.awt.*;

/**
 * HOTBAR RENDERER (editor/panel/HotbarRenderer.java)
 * ----------------------------------------------------
 * Draws the editor toolbar along the bottom of the screen.
 *
 * Left section  — Tool buttons (P/S/X/V/R)
 * Centre section — Layer toggles (Buildings / Walls / Paths / PE / Decorations)
 * Right section  — Snap size display, floor counter
 *
 * Layout:
 *  [P][S][X][V][R]    [Bldg][Wall][Path][PE][Deco]    Grid:20  Floor:1
 *
 * Each tool button highlights when active.
 * Each layer button dims when hidden, shows lock icon when locked.
 */
public class HotbarRenderer {

    private static final int BAR_H     = 36;
    private static final int BTN_W     = 42;
    private static final int BTN_H     = 28;
    private static final int BTN_PAD   = 4;
    private static final Color BAR_BG  = new Color(10,  10,  15, 210);
    private static final Color ACTIVE  = new Color(50, 150, 255, 220);
    private static final Color IDLE    = new Color(60,  60,  70, 200);
    private static final Color HIDDEN  = new Color(30,  30,  35, 180);
    private static final Color TEXT    = Color.WHITE;
    private static final Color DIM_TEXT= new Color(120, 120, 120);

    public void draw(Graphics2D g, int screenW, int screenH,
                     EditorState state, LayerManager layers,
                     float gridSnap, int currentFloor) {

        int barY = screenH - BAR_H;

        // Background bar
        g.setColor(BAR_BG);
        g.fillRect(0, barY, screenW, BAR_H);
        g.setColor(new Color(255, 255, 255, 30));
        g.drawLine(0, barY, screenW, barY);

        int x = 8;
        int y = barY + (BAR_H - BTN_H) / 2;

        // ── Tool buttons ─────────────────────────────────────────────
        x = drawToolBtn(g, x, y, "P", "Place",  state.getCurrentTool() == EditorState.Tool.PLACE);
        x = drawToolBtn(g, x, y, "S", "Select", state.getCurrentTool() == EditorState.Tool.SELECT);
        x = drawToolBtn(g, x, y, "X", "Delete", state.getCurrentTool() == EditorState.Tool.DELETE);
        x = drawToolBtn(g, x, y, "V", "Shape",  state.getCurrentTool() == EditorState.Tool.SHAPE_EDIT);
        x = drawToolBtn(g, x, y, "R", "Path",   state.getCurrentTool() == EditorState.Tool.PATH_EDIT);

        // Divider
        x += 8;
        g.setColor(new Color(255,255,255,30));
        g.drawLine(x, barY+4, x, barY+BAR_H-4);
        x += 8;

        // ── Layer toggles ─────────────────────────────────────────────
        EditorLayer[] layerOrder = {
                EditorLayer.BUILDINGS, EditorLayer.WALLS,
                EditorLayer.PATHS, EditorLayer.PE, EditorLayer.DECORATIONS
        };
        String[] layerKeys = {"Bldg", "Wall", "Path", "PE", "Deco"};

        for (int i = 0; i < layerOrder.length; i++) {
            EditorLayer layer = layerOrder[i];
            boolean vis    = layers.isVisible(layer);
            boolean locked = layers.isLocked(layer);
            boolean active = layers.getActiveLayer() == layer;

            // Button background
            Color bg = !vis ? HIDDEN : active ? ACTIVE : IDLE;
            g.setColor(bg);
            g.fillRoundRect(x, y, BTN_W, BTN_H, 5, 5);

            // Layer color dot
            g.setColor(new Color(layer.colorARGB, true));
            g.fillOval(x + 4, y + BTN_H/2 - 4, 8, 8);

            // Label
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(vis ? TEXT : DIM_TEXT);
            g.drawString(layerKeys[i], x + 14, y + BTN_H - 8);

            // Lock indicator
            if (locked) {
                g.setColor(new Color(255, 80, 80, 200));
                g.setFont(new Font("SansSerif", Font.PLAIN, 9));
                g.drawString("🔒", x + BTN_W - 14, y + 12);
            }

            x += BTN_W + BTN_PAD;
        }

        // Divider
        x += 8;
        g.setColor(new Color(255,255,255,30));
        g.drawLine(x, barY+4, x, barY+BAR_H-4);
        x += 8;

        // ── Info section ─────────────────────────────────────────────
        g.setFont(new Font("Consolas", Font.PLAIN, 11));
        g.setColor(new Color(180, 220, 255));
        g.drawString("Grid:" + (int)gridSnap, x, barY + 22);
        x += 70;
        g.drawString("Floor:" + currentFloor, x, barY + 22);
        x += 70;
        g.setColor(new Color(120, 120, 120));
        g.drawString("F1=exit  Ctrl+S=save  Ctrl+Z=undo", x, barY + 22);
    }

    private int drawToolBtn(Graphics2D g, int x, int y,
                            String key, String label, boolean active) {
        g.setColor(active ? ACTIVE : IDLE);
        g.fillRoundRect(x, y, BTN_W, BTN_H, 6, 6);

        if (active) {
            g.setColor(new Color(100, 180, 255, 100));
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(x, y, BTN_W, BTN_H, 6, 6);
            g.setStroke(new BasicStroke(1f));
        }

        g.setFont(new Font("Consolas", Font.BOLD, 13));
        g.setColor(active ? Color.WHITE : new Color(200, 200, 200));
        g.drawString(key, x + 8, y + 16);

        g.setFont(new Font("SansSerif", Font.PLAIN, 8));
        g.setColor(active ? new Color(200, 230, 255) : new Color(130, 130, 130));
        g.drawString(label, x + 4, y + BTN_H - 3);

        return x + BTN_W + BTN_PAD;
    }
}