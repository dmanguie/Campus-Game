package com.campusgame.editor.panel;

import com.campusgame.map.data.BuildingData;

import java.awt.*;

/**
 * PROPERTY PANEL RENDERER (editor/panel/PropertyPanelRenderer.java)
 * -------------------------------------------------------------------
 * Draws the building property panel in the bottom-left of the editor screen.
 *
 * Panel layout:
 *  ┌────────────────────────────┐
 *  │ ✏ BUILDING PROPERTIES      │
 *  │  Name:     [ACAD BLDG    ] │
 *  │  Floors:   [4            ] │
 *  │  Tag:      [academic     ] │
 *  │  Collision:[true         ] │
 *  │  Rotation: [4.0°         ] │
 *  │  Color:    ████            │
 *  │  Tab=next  Enter=commit    │
 *  └────────────────────────────┘
 *
 * Active field has a cyan highlight.
 * Tab cycles fields. Enter commits. Esc cancels.
 */
public class PropertyPanelRenderer {

    private static final int PANEL_X     = 10;
    private static final int PANEL_W     = 260;
    private static final int ROW_H       = 22;
    private static final int PADDING     = 10;

    private static final Color BG        = new Color(15,  15,  20,  210);
    private static final Color TITLE_COL = new Color(255, 220,  50);
    private static final Color LABEL_COL = new Color(160, 200, 255);
    private static final Color VALUE_COL = new Color(240, 240, 240);
    private static final Color FOCUS_BG  = new Color( 50, 130, 255,  80);
    private static final Color BORDER    = new Color(255, 255, 255,  60);

    public void draw(Graphics2D g, PropertyPanel panel, int screenH) {
        if (!panel.isVisible()) return;

        PropertyPanel.Field[] fields = PropertyPanel.Field.values();
        int rows    = fields.length + 2; // title + fields + hint
        int panelH  = rows * ROW_H + PADDING * 2;
        int panelY  = screenH - panelH - 10;

        // Background
        g.setColor(BG);
        g.fillRoundRect(PANEL_X, panelY, PANEL_W, panelH, 8, 8);
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(PANEL_X, panelY, PANEL_W, panelH, 8, 8);

        int y = panelY + PADDING + ROW_H;

        // Title
        g.setFont(new Font("Consolas", Font.BOLD, 12));
        g.setColor(TITLE_COL);
        g.drawString("✏  BUILDING PROPERTIES", PANEL_X + PADDING, y - 4);
        y += ROW_H / 2;

        // Separator
        g.setColor(new Color(255, 255, 255, 40));
        g.drawLine(PANEL_X + 4, y - 6, PANEL_X + PANEL_W - 4, y - 6);

        // Fields
        g.setFont(new Font("Consolas", Font.PLAIN, 11));
        BuildingData src = panel.getSource();

        drawField(g, "Name",      panel.name,                              panel, PropertyPanel.Field.NAME,      PANEL_X, y); y += ROW_H;
        drawField(g, "Floors",    String.valueOf(panel.floors),             panel, PropertyPanel.Field.FLOORS,    PANEL_X, y); y += ROW_H;
        drawField(g, "Tag",       panel.tag,                               panel, PropertyPanel.Field.TAG,       PANEL_X, y); y += ROW_H;
        drawField(g, "Collision", String.valueOf(panel.collisionEnabled),   panel, PropertyPanel.Field.COLLISION, PANEL_X, y); y += ROW_H;
        drawField(g, "Rotation",  String.format("%.1f°", panel.rotationDegrees), panel, PropertyPanel.Field.ROTATION, PANEL_X, y); y += ROW_H;

        // Color swatch
        boolean colorFocused = panel.getFocusedField() == PropertyPanel.Field.COLOR;
        if (colorFocused) { g.setColor(FOCUS_BG); g.fillRoundRect(PANEL_X+4, y-14, PANEL_W-8, ROW_H, 4, 4); }
        g.setColor(LABEL_COL); g.drawString("Color:", PANEL_X + PADDING, y);
        g.setColor(new Color(panel.colorARGB, true));
        g.fillRoundRect(PANEL_X + 85, y - 12, 40, 14, 3, 3);
        g.setColor(new Color(255,255,255,80));
        g.drawRoundRect(PANEL_X + 85, y - 12, 40, 14, 3, 3);
        y += ROW_H + 4;

        // Hint
        g.setColor(new Color(150, 150, 150));
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.drawString("Tab=next field  Enter=commit  Esc=cancel", PANEL_X + PADDING, y);
    }

    private void drawField(Graphics2D g, String label, String value,
                           PropertyPanel panel, PropertyPanel.Field field,
                           int px, int y) {
        boolean focused = (panel.getFocusedField() == field);

        if (focused) {
            g.setColor(FOCUS_BG);
            g.fillRoundRect(px + 4, y - 14, PANEL_W - 8, ROW_H, 4, 4);
        }

        g.setColor(LABEL_COL);
        g.setFont(new Font("Consolas", Font.PLAIN, 11));
        g.drawString(label + ":", px + PADDING, y);

        g.setColor(focused ? Color.WHITE : VALUE_COL);
        String display = value.length() > 18 ? value.substring(0, 17) + "…" : value;
        if (focused) display += "|"; // cursor blink effect
        g.drawString(display, px + 90, y);
    }
}