package com.campusgame.editor;

import java.awt.*;

/**
 * CONTROLS OVERLAY (editor/ControlsOverlay.java)
 * ------------------------------------------------
 * Semi-transparent help panel drawn on top of the game/editor.
 * Toggled independently by F2 (gameplay) and F3 (editor).
 *
 * Drawn last by Renderer.paintComponent() so it always sits on top.
 * No game state is modified here — pure display.
 */
public class ControlsOverlay {

    public enum Mode { NONE, GAMEPLAY, EDITOR }

    private Mode current = Mode.NONE;

    // ── Toggle ────────────────────────────────────────────────────────

    public void toggleGameplay() {
        current = (current == Mode.GAMEPLAY) ? Mode.NONE : Mode.GAMEPLAY;
    }

    public void toggleEditor() {
        current = (current == Mode.EDITOR) ? Mode.NONE : Mode.EDITOR;
    }

    public void hide() { current = Mode.NONE; }

    public boolean isVisible() { return current != Mode.NONE; }
    public Mode    getMode()   { return current; }

    // ── Colours ───────────────────────────────────────────────────────

    private static final Color BG        = new Color( 8,  10,  22, 220);
    private static final Color BORDER    = new Color(255, 220,  50, 160);
    private static final Color HEADER    = new Color(255, 220,  50);
    private static final Color SECTION   = new Color(100, 200, 255);
    private static final Color KEY_COL   = new Color(255, 220,  50);
    private static final Color DESC_COL  = new Color(210, 210, 210);
    private static final Color DIM_COL   = new Color(130, 130, 130);
    private static final Color CLOSE_TIP = new Color(160, 160, 160);

    // ── Draw ──────────────────────────────────────────────────────────

    public void draw(Graphics2D g, int screenW, int screenH) {
        if (current == Mode.NONE) return;
        if (current == Mode.GAMEPLAY) drawGameplay(g, screenW, screenH);
        else                          drawEditor(g, screenW, screenH);
    }

    // ─────────────────────────────────────────────────────────────────
    // GAMEPLAY OVERLAY  (F2)
    // ─────────────────────────────────────────────────────────────────

    private void drawGameplay(Graphics2D g, int screenW, int screenH) {
        String[][] sections = {
                { "MOVEMENT" },
                { "W / A / S / D",  "Move" },
                { "Arrow Keys",     "Move (alternate)" },
                { "Shift",          "Sprint" },
                { "" },
                { "INTERACTION" },
                { "E",              "Interact / Enter door" },
                { "" },
                { "EDITOR" },
                { "F1",             "Toggle editor mode" },
                { "" },
                { "HELP" },
                { "F2",             "Toggle this panel" },
                { "F3",             "Editor controls reference" },
        };
        drawPanel(g, screenW, screenH, "GAMEPLAY CONTROLS", "F2", sections);
    }

    // ─────────────────────────────────────────────────────────────────
    // EDITOR OVERLAY  (F3)
    // ─────────────────────────────────────────────────────────────────

    private void drawEditor(Graphics2D g, int screenW, int screenH) {
        String[][] sections = {
                { "TOOLS  (exterior editor)" },
                { "Q",   "Select tool" },
                { "W",   "Move tool  (drag selected building)" },
                { "E",   "Resize tool  (corner handles)" },
                { "R",   "Rotate tool  (Q / E keys to rotate)" },
                { "T",   "Path tool" },
                { "P",   "Place new building" },
                { "X",   "Delete tool" },
                { "V",   "Shape / polygon edit" },
                { "" },
                { "SELECTION" },
                { "Click",          "Select object" },
                { "Shift+Click",    "Add to multi-select" },
                { "Drag (empty)",   "Box select" },
                { "Delete",         "Delete selected" },
                { "" },
                { "NAMING" },
                { "N",   "Rename selected object" },
                { "N  (no selection)", "Open entrance tool" },
                { "" },
                { "ENTRANCE TOOL  (N → select entrance)" },
                { "C",   "Assign interior scene" },
                { "N",   "Rename entrance / prompt text" },
                { "Del", "Delete selected entrance" },
                { "Esc", "Deselect" },
                { "" },
                { "PATH TOOL  (T)" },
                { "Click",          "Add waypoint" },
                { "Enter",          "Finish / save path" },
                { "Mid-click",      "Insert point on segment" },
                { "Right-click",    "Delete hovered point" },
                { "N",              "Rename path" },
                { "Del",            "Delete path" },
                { "" },
                { "SHAPE EDIT  (V)" },
                { "Drag vertex",    "Move vertex" },
                { "1-5",            "Apply shape preset" },
                { "Mid-click",      "Insert vertex on edge" },
                { "Right-click",    "Delete vertex" },
                { "Enter",          "Commit shape" },
                { "Esc",            "Cancel" },
                { "" },
                { "GRID  &  FLOORS" },
                { "G",              "Cycle grid snap" },
                { "[ / ]",          "Resize width" },
                { "- / =",          "Resize depth" },
                { "Page Up/Down",   "Next / prev floor" },
                { "" },
                { "GLOBAL" },
                { "Ctrl+S",  "Save map" },
                { "Ctrl+Z",  "Undo" },
                { "F1",      "Exit editor" },
                { "F3",      "Toggle this panel" },
                { "" },
                { "INTERIOR EDITOR  (F1 while indoors)" },
                { "P",  "Place room" },
                { "S",  "Select / move room" },
                { "X",  "Delete room" },
                { "N",  "Rename room" },
                { "G",  "Cycle grid snap" },
                { "F1", "Exit interior editor" },
        };
        drawPanel(g, screenW, screenH, "EDITOR CONTROLS", "F3", sections);
    }

    // ─────────────────────────────────────────────────────────────────
    // SHARED PANEL RENDERER
    // ─────────────────────────────────────────────────────────────────

    /**
     * Renders a scrollable two-column help panel centred on screen.
     *
     * @param sections  Each entry is either:
     *                    { "SECTION HEADER" }  — one element, drawn as a section title
     *                    { "" }                — blank spacer row
     *                    { "Key", "Desc" }     — normal key → description row
     */
    private void drawPanel(Graphics2D g, int screenW, int screenH,
                           String title, String closeKey,
                           String[][] sections) {

        final int COL_W   = 420;   // panel width
        final int LINE_H  = 17;
        final int PAD     = 14;
        final int KEY_W   = 160;   // left column width

        // Measure content height
        int contentLines = 0;
        for (String[] row : sections) {
            contentLines += (row.length == 1 && row[0].isEmpty()) ? 1 : 1;
        }
        int maxH     = screenH - 80;
        int panelH   = Math.min(contentLines * LINE_H + PAD * 2 + 38, maxH);
        int panelX   = screenW / 2 - COL_W / 2;
        int panelY   = screenH / 2 - panelH / 2;

        // Background + border
        g.setColor(BG);
        g.fillRoundRect(panelX, panelY, COL_W, panelH, 12, 12);
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(panelX, panelY, COL_W, panelH, 12, 12);
        g.setStroke(new BasicStroke(1f));

        // Title bar
        g.setFont(new Font("Consolas", Font.BOLD, 14));
        FontMetrics fmT = g.getFontMetrics();
        g.setColor(HEADER);
        g.drawString(title, panelX + PAD, panelY + PAD + fmT.getAscent());

        // Close tip (top-right)
        g.setFont(new Font("Consolas", Font.PLAIN, 10));
        FontMetrics fmC = g.getFontMetrics();
        String tip = closeKey + " = close";
        g.setColor(CLOSE_TIP);
        g.drawString(tip, panelX + COL_W - fmC.stringWidth(tip) - PAD,
                panelY + PAD + fmT.getAscent());

        // Separator under title
        int sepY = panelY + PAD + fmT.getAscent() + 4;
        g.setColor(new Color(255, 220, 50, 60));
        g.drawLine(panelX + PAD, sepY, panelX + COL_W - PAD, sepY);

        // Content rows — clipped to panel
        Shape clip = g.getClip();
        g.setClip(panelX + 1, sepY + 2, COL_W - 2, panelY + panelH - sepY - PAD - 2);

        g.setFont(new Font("Consolas", Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();
        int ty = sepY + LINE_H;

        for (String[] row : sections) {
            if (ty > panelY + panelH) break;   // overflow guard

            if (row.length == 1 && row[0].isEmpty()) {
                // Blank spacer
                ty += LINE_H / 2;
                continue;
            }

            if (row.length == 1) {
                // Section header
                g.setFont(new Font("Consolas", Font.BOLD, 11));
                g.setColor(SECTION);
                g.drawString(row[0], panelX + PAD, ty);
                g.setFont(new Font("Consolas", Font.PLAIN, 11));
                // underline
                g.setColor(new Color(100, 200, 255, 50));
                g.drawLine(panelX + PAD, ty + 2, panelX + COL_W - PAD, ty + 2);
                ty += LINE_H;
                continue;
            }

            // Key → description
            g.setColor(KEY_COL);
            g.setFont(new Font("Consolas", Font.BOLD, 11));
            g.drawString(row[0], panelX + PAD, ty);

            if (row.length > 1) {
                g.setColor(DESC_COL);
                g.setFont(new Font("Consolas", Font.PLAIN, 11));
                g.drawString(row[1], panelX + PAD + KEY_W, ty);
            }
            ty += LINE_H;
        }

        g.setClip(clip);   // restore clip
    }
}