package com.campusgame.renderer;

import com.campusgame.editor.EditorOverlayRenderer;
import com.campusgame.engine.GameLoop;
import com.campusgame.entities.Player;
import com.campusgame.interaction.InteractionPrompt;
import com.campusgame.interaction.InteractionSystem;
import com.campusgame.map.Building;
import com.campusgame.map.CampusMap;
import com.campusgame.world.interior.InteriorManager;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * RENDERER (renderer/Renderer.java)
 * Phase 5: added interiorManager, interactionSystem, interactionPrompt.
 * Draw order:
 *   1. Interior OR Exterior scene
 *   2. Player
 *   3. HUD (minimap hidden indoors)
 *   4. Interaction prompt
 *   5. Editor overlay
 *   6. Scene transition fade
 */
public class Renderer extends JPanel {

    private final CampusMap campusMap;
    private final Player    player;
    private final Camera    camera;

    private EditorOverlayRenderer editorOverlay     = null;
    private InteriorManager       interiorManager   = null;
    private InteractionSystem     interactionSystem = null;
    private InteractionPrompt     interactionPrompt = null;

    private BufferedImage backBuffer;
    private Graphics2D    bg;

    private long lastFpsTime = System.nanoTime();
    private int  frameCount  = 0;
    private int  displayFps  = 0;

    private static final int   MINIMAP_W     = 200;
    private static final int   MINIMAP_H     = 160;
    private static final int   MINIMAP_MARGIN= 10;
    private static final float MINIMAP_SCALE = (float) MINIMAP_W / CampusMap.WORLD_WIDTH;

    public Renderer(CampusMap campusMap, Player player, Camera camera) {
        this.campusMap = campusMap;
        this.player    = player;
        this.camera    = camera;
        setPreferredSize(new Dimension(GameLoop.WINDOW_WIDTH, GameLoop.WINDOW_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
    }

    public void setEditorOverlay(EditorOverlayRenderer o)  { this.editorOverlay     = o; }
    public void setInteriorManager(InteriorManager m)      { this.interiorManager   = m; }
    public void setInteractionSystem(InteractionSystem s)  { this.interactionSystem = s; }
    public void setInteractionPrompt(InteractionPrompt p)  { this.interactionPrompt = p; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (backBuffer == null || backBuffer.getWidth() != getWidth()
                || backBuffer.getHeight() != getHeight()) {
            backBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            bg = backBuffer.createGraphics();
            bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_ON);
            bg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        int ox = camera.getOffsetX();
        int oy = camera.getOffsetY();

        // Layer 1 & 2: Scene
        boolean indoors = interiorManager != null
                && interiorManager.drawIfIndoors(bg, camera, getWidth(), getHeight());

        if (!indoors) {
            campusMap.drawGround(bg, ox, oy, getWidth(), getHeight());
            for (Building b : campusMap.getBuildings()) b.draw(bg, ox, oy);
        }

        // Layer 3: Player
        player.draw(bg, camera.worldToScreenX(player.getCenterX()),
                camera.worldToScreenY(player.getCenterY()));

        // Layer 4: HUD
        drawHUD(bg, indoors);

        // Layer 5: Interaction prompt
        if (interactionSystem != null && interactionPrompt != null
                && interactionSystem.hasNearest()) {
            interactionPrompt.draw(bg, interactionSystem.getNearest(),
                    camera, getWidth(), getHeight());
        }

        // Layer 6: Editor overlay
        if (editorOverlay != null) editorOverlay.draw(bg, getWidth(), getHeight());

        // Layer 7: Transition fade
        if (interiorManager != null)
            interiorManager.drawTransition(bg, getWidth(), getHeight());

        g.drawImage(backBuffer, 0, 0, null);
        updateFps();
    }

    // ── HUD ───────────────────────────────────────────────────────────

    private void drawHUD(Graphics2D g, boolean indoors) {
        if (!indoors) drawMiniMap(g);
        drawDebugPanel(g, indoors);
        drawControls(g, indoors);
    }

    private void drawMiniMap(Graphics2D g) {
        int mx = getWidth() - MINIMAP_W - MINIMAP_MARGIN;
        int my = MINIMAP_MARGIN;

        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(mx-4, my-4, MINIMAP_W+8, MINIMAP_H+8, 8, 8);
        g.setColor(new Color(107, 161, 83, 200));
        g.fillRect(mx, my, MINIMAP_W, MINIMAP_H);

        for (Building b : campusMap.getBuildings()) {
            int bx = mx + (int)(b.getX() * MINIMAP_SCALE);
            int by = my + (int)(b.getY() * MINIMAP_SCALE);
            int bw = Math.max(2, (int)(b.getWidth()  * MINIMAP_SCALE));
            int bd = Math.max(2, (int)(b.getDepth()  * MINIMAP_SCALE));
            g.setColor(b.getRoofColor());
            g.fillRect(bx, by, bw, bd);
        }

        int px = mx + (int)(player.getCenterX() * MINIMAP_SCALE);
        int py = my + (int)(player.getCenterY() * MINIMAP_SCALE);
        g.setColor(Color.WHITE); g.fillOval(px-3, py-3, 6, 6);
        g.setColor(Color.CYAN);  g.fillOval(px-2, py-2, 4, 4);

        g.setColor(new Color(255,255,255,80));
        g.drawRect(mx+(int)(camera.getOffsetX()*MINIMAP_SCALE),
                my+(int)(camera.getOffsetY()*MINIMAP_SCALE),
                (int)(getWidth()*MINIMAP_SCALE), (int)(getHeight()*MINIMAP_SCALE));

        g.setColor(new Color(255,255,255,120));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(mx-4, my-4, MINIMAP_W+8, MINIMAP_H+8, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        g.setColor(Color.WHITE);
        g.drawString("CAMPUS MAP", mx, my-6);
    }

    private void drawDebugPanel(Graphics2D g, boolean indoors) {
        int panelY = indoors ? 46 : 10;
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRoundRect(10, panelY, 190, 75, 12, 12);
        g.setFont(new Font("Consolas", Font.BOLD, 14));
        g.setColor(new Color(120, 255, 120));
        g.drawString("FPS: " + displayFps, 20, panelY + 20);
        g.setColor(Color.WHITE);
        g.drawString("X: " + (int)player.x,  20,  panelY + 42);
        g.drawString("Z: " + (int)player.z,  100, panelY + 42);
        if (indoors && interiorManager != null && interiorManager.getCurrentScene() != null) {
            g.setFont(new Font("Consolas", Font.PLAIN, 10));
            g.setColor(new Color(140, 215, 140));
            String name = interiorManager.getCurrentScene().displayName;
            if (name.length() > 20) name = name.substring(0, 18) + "…";
            g.drawString(name, 20, panelY + 60);
        }
    }

    private void drawControls(Graphics2D g, boolean indoors) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(5, getHeight()-36, 280, 14);
        g.setColor(new Color(220, 220, 220));
        String hint = indoors
                ? "WASD=Move  [E]=Exit  F1=Editor"
                : "WASD=Move  Shift=Sprint  [E]=Enter  F1=Editor";
        g.drawString(hint, 8, getHeight()-25);
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