package com.campusgame.renderer;

import com.campusgame.editor.EditorOverlayRenderer;
import com.campusgame.engine.GameLoop;
import com.campusgame.entities.Player;
import com.campusgame.interaction.InteractionPrompt;
import com.campusgame.interaction.InteractionSystem;
import com.campusgame.map.Building;
import com.campusgame.map.CampusMap;
import com.campusgame.map.data.PathData;
import com.campusgame.world.interior.InteriorManager;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * RENDERER (renderer/Renderer.java)
 *
 * Phase 5 additions:
 *   • interiorManager   — determines exterior vs interior draw path
 *   • interactionSystem — provides nearest in-range trigger for prompt
 *   • interactionPrompt — draws the pulsing "[E] to enter …" UI
 *
 * Minimap now draws paths in addition to buildings.
 *
 * Updated draw order:
 *   1. Interior OR Exterior scene (mutually exclusive)
 *   2. Player
 *   3. HUD  (minimap hidden when indoors)
 *   4. Interaction prompt
 *   5. Editor overlay  (Phase 3 — unchanged)
 *   6. Scene transition fade  (Phase 5 — topmost)
 */
public class Renderer extends JPanel {

    private final CampusMap campusMap;
    private final Player    player;
    private final Camera    camera;

    // Phase 3: optional editor overlay
    private EditorOverlayRenderer editorOverlay = null;

    // Phase 5: interaction + interior systems
    private InteriorManager   interiorManager   = null;
    private InteractionSystem interactionSystem = null;
    private InteractionPrompt interactionPrompt = null;

    // Back buffer
    private BufferedImage backBuffer;
    private Graphics2D    bg;

    // FPS counter
    private long lastFpsTime = System.nanoTime();
    private int  frameCount  = 0;
    private int  displayFps  = 0;

    // Minimap constants
    private static final int   MINIMAP_W      = 200;
    private static final int   MINIMAP_H      = 160;
    private static final int   MINIMAP_MARGIN = 10;
    private static final float MINIMAP_SCALE  = (float) MINIMAP_W / CampusMap.WORLD_WIDTH;

    // ── Constructor ───────────────────────────────────────────────────

    public Renderer(CampusMap campusMap, Player player, Camera camera) {
        this.campusMap = campusMap;
        this.player    = player;
        this.camera    = camera;
        setPreferredSize(new Dimension(GameLoop.WINDOW_WIDTH, GameLoop.WINDOW_HEIGHT));
        setBackground(Color.BLACK);
    }

    // ── Setters ───────────────────────────────────────────────────────

    public void setEditorOverlay(EditorOverlayRenderer overlay) {
        this.editorOverlay = overlay;
    }

    public void setInteriorManager(InteriorManager m) {
        this.interiorManager = m;
    }

    public void setInteractionSystem(InteractionSystem s) {
        this.interactionSystem = s;
    }

    public void setInteractionPrompt(InteractionPrompt p) {
        this.interactionPrompt = p;
    }

    // ─────────────────────────────────────────────────────────────────
    // PAINT
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (backBuffer == null || backBuffer.getWidth() != getWidth()
                || backBuffer.getHeight() != getHeight()) {
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

        // ── Layer 1 & 2: Scene ────────────────────────────────────────
        boolean indoors = (interiorManager != null)
                && interiorManager.drawIfIndoors(bg, camera, getWidth(), getHeight());

        if (!indoors) {
            campusMap.drawGround(bg, ox, oy, getWidth(), getHeight());
            for (Building b : campusMap.getBuildings()) b.draw(bg, ox, oy);
        }

        // ── Layer 3: Player ───────────────────────────────────────────
        int psx = camera.worldToScreenX(player.getCenterX());
        int psy = camera.worldToScreenY(player.getCenterY());
        player.draw(bg, psx, psy);

        // ── Layer 4: HUD ──────────────────────────────────────────────
        drawHUD(bg, indoors);

        // ── Layer 5: Interaction prompt ───────────────────────────────
        if (interactionSystem != null && interactionPrompt != null
                && interactionSystem.hasNearest()) {
            interactionPrompt.draw(bg, interactionSystem.getNearest(),
                    camera, getWidth(), getHeight());
        }

        // ── Layer 6: Editor overlay ───────────────────────────────────
        if (editorOverlay != null) {
            editorOverlay.draw(bg, getWidth(), getHeight());
        }

        // ── Layer 7: Scene transition fade ────────────────────────────
        if (interiorManager != null) {
            interiorManager.drawTransition(bg, getWidth(), getHeight());
        }

        g.drawImage(backBuffer, 0, 0, null);
        updateFps();
    }

    // ─────────────────────────────────────────────────────────────────
    // HUD
    // ─────────────────────────────────────────────────────────────────

    private void drawHUD(Graphics2D g, boolean indoors) {
        if (!indoors) {
            drawMiniMap(g);
        }
        drawDebugPanel(g, indoors);
        drawControls(g, indoors);
    }

    private void drawMiniMap(Graphics2D g) {
        int mx = getWidth() - MINIMAP_W - MINIMAP_MARGIN;
        int my = MINIMAP_MARGIN;

        // Background
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(mx - 4, my - 4, MINIMAP_W + 8, MINIMAP_H + 8, 8, 8);

        // Grass fill
        g.setColor(new Color(107, 161, 83, 200));
        g.fillRect(mx, my, MINIMAP_W, MINIMAP_H);

        // ── Paths ─────────────────────────────────────────────────────
        // Draw paths before buildings so buildings render on top.
        List<PathData> paths = campusMap.getPaths();
        for (PathData p : paths) {
            if (p.points == null || p.points.size() < 2) continue;

            // Parse the path colour; fall back to sandy beige if null/invalid.
            Color pathColor = parseMinimapPathColor(p.colorHex);
            // Scale the stroke width down to minimap space; minimum 1px.
            float minimapStroke = Math.max(1f, p.width * MINIMAP_SCALE);

            g.setColor(pathColor);
            g.setStroke(new BasicStroke(minimapStroke,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            for (int i = 0; i < p.points.size() - 1; i++) {
                int x1 = mx + (int)(p.points.get(i  )[0] * MINIMAP_SCALE);
                int y1 = my + (int)(p.points.get(i  )[1] * MINIMAP_SCALE);
                int x2 = mx + (int)(p.points.get(i+1)[0] * MINIMAP_SCALE);
                int y2 = my + (int)(p.points.get(i+1)[1] * MINIMAP_SCALE);
                g.drawLine(x1, y1, x2, y2);
            }
        }
        g.setStroke(new BasicStroke(1f));

        // ── Buildings ─────────────────────────────────────────────────
        for (Building b : campusMap.getBuildings()) {
            int bx = mx + (int)(b.getX() * MINIMAP_SCALE);
            int by = my + (int)(b.getY() * MINIMAP_SCALE);
            int bw = Math.max(2, (int)(b.getWidth()  * MINIMAP_SCALE));
            int bd = Math.max(2, (int)(b.getDepth()  * MINIMAP_SCALE));
            g.setColor(b.getRoofColor());
            g.fillRect(bx, by, bw, bd);
        }

        // ── Player dot ────────────────────────────────────────────────
        int px = mx + (int)(player.getCenterX() * MINIMAP_SCALE);
        int py = my + (int)(player.getCenterY() * MINIMAP_SCALE);
        g.setColor(Color.WHITE); g.fillOval(px - 3, py - 3, 6, 6);
        g.setColor(Color.CYAN);  g.fillOval(px - 2, py - 2, 4, 4);

        // ── Viewport rect ─────────────────────────────────────────────
        g.setColor(new Color(255, 255, 255, 80));
        g.drawRect(
                mx + (int)(camera.getOffsetX() * MINIMAP_SCALE),
                my + (int)(camera.getOffsetY() * MINIMAP_SCALE),
                (int)(getWidth()  * MINIMAP_SCALE),
                (int)(getHeight() * MINIMAP_SCALE));

        // ── Border + label ────────────────────────────────────────────
        g.setColor(new Color(255, 255, 255, 120));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(mx - 4, my - 4, MINIMAP_W + 8, MINIMAP_H + 8, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        g.setColor(Color.WHITE);
        g.drawString("CAMPUS MAP", mx, my - 6);
    }

    /**
     * Parses a path colorHex string (e.g. "#FFDCD7C8" ARGB or "#DCD7C8" RGB)
     * into a Color for minimap rendering.
     * Falls back to sandy beige if the string is null or unparseable.
     */
    private Color parseMinimapPathColor(String hex) {
        if (hex == null || hex.isEmpty()) return new Color(220, 215, 200, 200);
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            long v = Long.parseLong(clean, 16);
            if (clean.length() == 8) {
                int a = (int)((v >> 24) & 0xFF);
                int r = (int)((v >> 16) & 0xFF);
                int gr= (int)((v >>  8) & 0xFF);
                int b = (int)( v        & 0xFF);
                return new Color(r, gr, b, Math.min(a, 200));
            } else {
                int r = (int)((v >> 16) & 0xFF);
                int gr= (int)((v >>  8) & 0xFF);
                int b = (int)( v        & 0xFF);
                return new Color(r, gr, b, 200);
            }
        } catch (NumberFormatException e) {
            return new Color(220, 215, 200, 200);
        }
    }

    private void drawDebugPanel(Graphics2D g, boolean indoors) {
        int panelY = indoors ? 46 : 10;

        g.setColor(new Color(0, 0, 0, 170));
        g.fillRoundRect(10, panelY, 190, 75, 12, 12);

        g.setFont(new Font("Consolas", Font.BOLD, 14));
        g.setColor(new Color(120, 255, 120));
        g.drawString("FPS: " + displayFps, 20, panelY + 20);

        g.setColor(Color.WHITE);
        g.drawString("X: " + (int) player.x,    20,  panelY + 42);
        g.drawString("Z: " + (int) player.z,    100, panelY + 42);

        if (indoors && interiorManager != null
                && interiorManager.getCurrentScene() != null) {
            g.setFont(new Font("Consolas", Font.PLAIN, 10));
            g.setColor(new Color(140, 215, 140));
            String sceneName = interiorManager.getCurrentScene().displayName;
            if (sceneName.length() > 20) sceneName = sceneName.substring(0, 18) + "…";
            g.drawString(sceneName, 20, panelY + 60);
        }
    }

    private void drawControls(Graphics2D g, boolean indoors) {
        int y = getHeight() - 36;
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(5, y, 260, 14);
        g.setColor(new Color(220, 220, 220));

        String hint = indoors
                ? "WASD=Move  [E]=Exit door  F1=Editor"
                : "WASD=Move  Shift=Sprint  [E]=Enter  F1=Editor";
        g.drawString(hint, 8, getHeight() - 25);
    }

    // ─────────────────────────────────────────────────────────────────
    // FPS
    // ─────────────────────────────────────────────────────────────────

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