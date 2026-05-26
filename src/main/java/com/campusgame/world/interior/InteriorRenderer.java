package com.campusgame.world.interior;

import com.campusgame.map.data.EntranceData;
import com.campusgame.renderer.Camera;
import java.awt.*;

/**
 * INTERIOR RENDERER (world/interior/InteriorRenderer.java)
 * Draws an active InteriorScene onto the back buffer.
 * Called by Renderer instead of the normal exterior pipeline
 * when InteriorManager.isIndoors() == true.
 *
 * Draw order:
 *   1. Background fill
 *   2. Rooms (floor + walls + label)
 *   3. Exit door markers
 *   4. Top HUD strip (scene name breadcrumb)
 */
public class InteriorRenderer {

    private static final int   WALL_STROKE = 6;
    private static final Color EXIT_GREEN  = new Color(70, 200, 110, 230);
    private static final Color LABEL_BG    = new Color(0, 0, 0, 90);
    private static final Color HUD_BG      = new Color(0, 0, 0, 185);

    public void draw(Graphics2D g, InteriorScene scene, Camera camera,
                     int screenW, int screenH) {
        g.setColor(scene.backgroundColor);
        g.fillRect(0, 0, screenW, screenH);

        for (InteriorRoom room : scene.rooms) drawRoom(g, room, camera);
        for (EntranceData exit : scene.exits)  drawExit(g, exit, camera);

        drawHudStrip(g, scene, screenW);
    }

    private void drawRoom(Graphics2D g, InteriorRoom room, Camera camera) {
        int rx = camera.worldToScreenX(room.x);
        int rz = camera.worldToScreenY(room.z);
        int rw = (int) room.width;
        int rd = (int) room.depth;

        g.setColor(room.floorColor);
        g.fillRect(rx, rz, rw, rd);

        g.setColor(room.wallColor);
        g.setStroke(new BasicStroke(WALL_STROKE,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawRect(rx, rz, rw, rd);
        g.setStroke(new BasicStroke(1f));

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();
        int lw = fm.stringWidth(room.displayName);
        int lx = rx + rw/2 - lw/2;
        int ly = rz + 18;

        g.setColor(LABEL_BG);
        g.fillRoundRect(lx-5, ly-13, lw+10, 17, 4, 4);
        g.setColor(new Color(50, 40, 30, 200));
        g.drawString(room.displayName, lx, ly);
    }

    private void drawExit(Graphics2D g, EntranceData exit, Camera camera) {
        int sx = camera.worldToScreenX(exit.worldX);
        int sz = camera.worldToScreenY(exit.worldZ);

        g.setColor(EXIT_GREEN);
        g.setStroke(new BasicStroke(3f));
        g.drawRect(sx-18, sz-6, 36, 24);
        g.setStroke(new BasicStroke(1f));

        g.setColor(new Color(70, 200, 110, 80));
        g.fillRect(sx-18, sz-6, 36, 24);

        g.setColor(EXIT_GREEN);
        g.fillOval(sx+9, sz+4, 5, 5);

        g.setStroke(new BasicStroke(2.5f));
        g.drawArc(sx-18, sz-24, 36, 36, 0, 180);
        g.setStroke(new BasicStroke(1f));

        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(40, 160, 80));
        g.drawString("EXIT", sx - fm.stringWidth("EXIT")/2, sz-28);
    }

    private void drawHudStrip(Graphics2D g, InteriorScene scene, int screenW) {
        g.setColor(HUD_BG);
        g.fillRect(0, 0, screenW, 38);

        g.setFont(new Font("Consolas", Font.BOLD, 13));
        g.setColor(new Color(255, 220, 60));
        g.drawString("[ " + scene.displayName + " ]", 12, 20);

        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(new Color(140, 215, 140));
        g.drawString("INTERIOR  •  Walk to EXIT door and press [E] to leave", 12, 33);
    }
}