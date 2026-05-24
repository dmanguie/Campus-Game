package com.campusgame.renderer;

import com.campusgame.entities.Player;
import com.campusgame.engine.GameLoop;
import com.campusgame.map.Building;
import com.campusgame.map.CampusMap;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * RENDERER (renderer/Renderer.java)
 * -----------------------------------
 * The main drawing class. Extends JPanel so it integrates with Swing.
 *
 * Responsibilities:
 *  - Double-buffered rendering (uses BufferedImage backbuffer)
 *  - Draws in layer order: ground → buildings → player → HUD
 *  - Receives Camera offsets and applies them to all draw calls
 *  - Houses the mini-map HUD overlay
 *
 * Rendering layers (bottom to top):
 *  1. Ground / grass / paths (CampusMap.drawGround)
 *  2. Building footprints (Building.draw for each)
 *  3. Player character (Player.draw)
 *  4. HUD: mini-map, building names, FPS counter
 *
 * Future expansion:
 *  - Sprite/texture loading
 *  - Lighting/shadows
 *  - Interior room rendering
 *  - Isometric projection layer
 */
public class Renderer extends JPanel {

    private final CampusMap campusMap;
    private final Player    player;
    private final Camera    camera;

    // Double buffer
    private BufferedImage backBuffer;
    private Graphics2D    bg;

    // FPS counter
    private long lastFpsTime = System.nanoTime();
    private int  frameCount  = 0;
    private int  displayFps  = 0;

    // Mini-map settings
    private static final int MINIMAP_W      = 200;
    private static final int MINIMAP_H      = 160;
    private static final int MINIMAP_MARGIN = 10;
    private static final float MINIMAP_SCALE =
            (float) MINIMAP_W / CampusMap.WORLD_WIDTH;

    public Renderer(CampusMap campusMap, Player player, Camera camera) {
        this.campusMap = campusMap;
        this.player    = player;
        this.camera    = camera;

        setPreferredSize(new Dimension(GameLoop.WINDOW_WIDTH, GameLoop.WINDOW_HEIGHT));
        setBackground(Color.BLACK);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Lazy init back buffer
        if (backBuffer == null || backBuffer.getWidth() != getWidth()) {
            backBuffer = new BufferedImage(getWidth(), getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            bg = backBuffer.createGraphics();
            bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            bg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        int ox = camera.getOffsetX();
        int oy = camera.getOffsetY();

        // === LAYER 1: Ground ===
        campusMap.drawGround(bg, ox, oy, getWidth(), getHeight());

        // === LAYER 2: Buildings ===
        for (Building b : campusMap.getBuildings()) {
            b.draw(bg, ox, oy);
        }

        // === LAYER 3: Player ===
        int playerScreenX = camera.worldToScreenX(player.getCenterX());
        int playerScreenY = camera.worldToScreenY(player.getCenterY());
        player.draw(bg, playerScreenX, playerScreenY);

        // === LAYER 4: HUD ===
        drawHUD(bg);

        // Flip back buffer to screen
        g.drawImage(backBuffer, 0, 0, null);

        // FPS tracking
        updateFps();
    }

    // ---------------------------------------------------------------
    // HUD
    // ---------------------------------------------------------------

    private void drawHUD(Graphics2D g) {
        drawMiniMap(g);
        drawFps(g);
        drawControls(g);
    }

    /** Mini-map in the top-right corner */
    private void drawMiniMap(Graphics2D g) {
        int mx = getWidth()  - MINIMAP_W - MINIMAP_MARGIN;
        int my = MINIMAP_MARGIN;

        // Background
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(mx - 4, my - 4, MINIMAP_W + 8, MINIMAP_H + 8, 8, 8);

        // Ground color
        g.setColor(new Color(107, 161, 83, 200));
        g.fillRect(mx, my, MINIMAP_W, MINIMAP_H);

        // Buildings on mini-map
        for (Building b : campusMap.getBuildings()) {
            int bx = mx + (int)(b.getX() * MINIMAP_SCALE);
            int by = my + (int)(b.getY() * MINIMAP_SCALE);
            int bw = Math.max(2, (int)(b.getWidth() * MINIMAP_SCALE));
            int bd = Math.max(2, (int)(b.getDepth() * MINIMAP_SCALE));
            g.setColor(b.getRoofColor());
            g.fillRect(bx, by, bw, bd);
        }

        // Player dot on mini-map
        int px = mx + (int)(player.getCenterX() * MINIMAP_SCALE);
        int py = my + (int)(player.getCenterY() * MINIMAP_SCALE);
        g.setColor(Color.WHITE);
        g.fillOval(px - 3, py - 3, 6, 6);
        g.setColor(Color.CYAN);
        g.fillOval(px - 2, py - 2, 4, 4);

        // Camera viewport rect on mini-map
        g.setColor(new Color(255, 255, 255, 80));
        g.drawRect(
                mx + (int)(camera.getOffsetX() * MINIMAP_SCALE),
                my + (int)(camera.getOffsetY() * MINIMAP_SCALE),
                (int)(getWidth()  * MINIMAP_SCALE),
                (int)(getHeight() * MINIMAP_SCALE)
        );

        // Border
        g.setColor(new Color(255, 255, 255, 120));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(mx - 4, my - 4, MINIMAP_W + 8, MINIMAP_H + 8, 8, 8);

        // Title
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        g.setColor(Color.WHITE);
        g.drawString("CAMPUS MAP", mx, my - 6);
    }

    /** FPS counter bottom-left */
    private void drawFps(Graphics2D g) {
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(5, getHeight() - 18, 65, 14);
        g.setColor(new Color(180, 255, 180));
        g.drawString("FPS: " + displayFps, 8, getHeight() - 7);
    }

    /** Control hints bottom-left above FPS */
    private void drawControls(Graphics2D g) {
        String[] hints = { "W/A/S/D — Move" };
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(5, getHeight() - 36, 120, 14);
        g.setColor(new Color(220, 220, 220));
        g.drawString(hints[0], 8, getHeight() - 25);
    }

    private void updateFps() {
        frameCount++;
        long now = System.nanoTime();
        if (now - lastFpsTime >= 1_000_000_000L) {
            displayFps  = frameCount;
            frameCount  = 0;
            lastFpsTime = now;
        }
    }
}