package com.campusgame.interaction;

import com.campusgame.renderer.Camera;
import java.awt.*;

/**
 * INTERACTION PROMPT (interaction/InteractionPrompt.java)
 * Renders the pulsing "[E] to enter Library" prompt.
 * "[E]" is coloured yellow, the rest grey.
 */
public class InteractionPrompt {

    private static final Color BOX_BG   = new Color(8,   8,  18, 210);
    private static final Color BOX_EDGE = new Color(255, 220,  50, 130);
    private static final Color KEY_COL  = new Color(255, 220,  50);
    private static final Color BODY_COL = new Color(215, 215, 215);
    private static final Color DOT_COL  = new Color(255, 220,  50);

    private float pulse = 0f;

    public void update(float delta) {
        pulse = (float)((pulse + delta * 3.2) % (Math.PI * 2));
    }

    public void draw(Graphics2D g, Interactable nearest,
                     Camera camera, int screenW, int screenH) {
        if (nearest == null) return;
        float breathe = 0.72f + 0.28f * (float) Math.sin(pulse);
        drawWorldDot(g, nearest, camera, breathe);
        drawPromptBox(g, nearest.getPromptText(), screenW, screenH, breathe);
    }

    private void drawWorldDot(Graphics2D g, Interactable i,
                              Camera camera, float breathe) {
        int sx = camera.worldToScreenX(i.getWorldX());
        int sy = camera.worldToScreenY(i.getWorldZ());
        int a  = (int)(breathe * 240);

        g.setColor(new Color(DOT_COL.getRed(), DOT_COL.getGreen(), DOT_COL.getBlue(), a));
        g.fillOval(sx - 7, sy - 7, 14, 14);
        g.setColor(new Color(255, 255, 255, a));
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(sx - 7, sy - 7, 14, 14);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(255, 220, 50, a / 3));
        g.drawLine(sx, sy + 7, sx, sy + 26);
    }

    private void drawPromptBox(Graphics2D g, String prompt,
                               int screenW, int screenH, float breathe) {
        g.setFont(new Font("Consolas", Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        int boxW = fm.stringWidth(prompt) + 28;
        int boxH = 30;
        int bx   = screenW / 2 - boxW / 2;
        int by   = screenH - 86;
        int bgA  = (int)(breathe * 220);

        g.setColor(new Color(8, 8, 18, bgA));
        g.fillRoundRect(bx, by, boxW, boxH, 8, 8);
        g.setColor(new Color(255, 220, 50, (int)(breathe * 140)));
        g.setStroke(new BasicStroke(1.3f));
        g.drawRoundRect(bx, by, boxW, boxH, 8, 8);
        g.setStroke(new BasicStroke(1f));

        int eIdx = prompt.indexOf("[E]");
        int tx = bx + 14, ty = by + 20;

        if (eIdx >= 0) {
            String before = prompt.substring(0, eIdx);
            String rest   = prompt.substring(eIdx + 3);
            int bodyA = (int)(breathe * 220), keyA = (int)(breathe * 255);

            if (!before.isEmpty()) {
                g.setColor(new Color(BODY_COL.getRed(), BODY_COL.getGreen(),
                        BODY_COL.getBlue(), bodyA));
                g.drawString(before, tx, ty);
                tx += fm.stringWidth(before);
            }
            g.setColor(new Color(KEY_COL.getRed(), KEY_COL.getGreen(),
                    KEY_COL.getBlue(), keyA));
            g.drawString("[E]", tx, ty);
            tx += fm.stringWidth("[E]");
            g.setColor(new Color(BODY_COL.getRed(), BODY_COL.getGreen(),
                    BODY_COL.getBlue(), bodyA));
            g.drawString(rest, tx, ty);
        } else {
            g.setColor(new Color(BODY_COL.getRed(), BODY_COL.getGreen(),
                    BODY_COL.getBlue(), (int)(breathe * 220)));
            g.drawString(prompt, tx, ty);
        }
    }
}