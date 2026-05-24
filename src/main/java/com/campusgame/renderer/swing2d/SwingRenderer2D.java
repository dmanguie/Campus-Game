package com.campusgame.renderer.swing2d;

import com.campusgame.core.GameWorld;
import com.campusgame.entities.Player;
import com.campusgame.map.CampusMap;
import com.campusgame.renderer.Camera;
import com.campusgame.renderer.Renderer;
import com.campusgame.renderer.api.IRenderer;

/**
 * SWING RENDERER 2D (renderer/swing2d/SwingRenderer2D.java)
 * -----------------------------------------------------------
 * Adapter that wraps the existing Renderer (Phase 1 JPanel) and
 * exposes it through the IRenderer interface.
 *
 * WHY AN ADAPTER?
 *  - The existing Renderer.java stays completely unchanged (Phase 1 safe)
 *  - GameLoop now calls render() through IRenderer, not directly on Renderer
 *  - When you're ready for Phase 3, swap this for LwjglRenderer3D — done
 *
 * WHAT CHANGES IN GAMELOOP:
 *  - Store `IRenderer activeRenderer` instead of `Renderer renderer`
 *  - Call `activeRenderer.render(world, player, camera)` instead of `renderer.repaint()`
 *
 * The underlying Renderer JPanel still calls repaint() internally;
 * this adapter just bridges the interface gap.
 */
public class SwingRenderer2D implements IRenderer {

    private final Renderer swingPanel;  // the existing Phase 1 JPanel renderer

    public SwingRenderer2D(Renderer swingPanel) {
        this.swingPanel = swingPanel;
    }

    @Override
    public void init() {
        // Nothing extra to initialize — Swing panel is already set up by GameLoop
        System.out.println("[SwingRenderer2D] Initialized — Phase 1 Swing backend active.");
    }

    @Override
    public void render(GameWorld world, Player player, Camera camera) {
        // Trigger Swing repaint — Renderer.paintComponent() handles actual drawing
        swingPanel.repaint();
    }

    @Override
    public void onResize(int width, int height) {
        // Swing handles resize automatically via layout manager
    }

    @Override
    public void dispose() {
        // Nothing to dispose for Swing panel
        System.out.println("[SwingRenderer2D] Disposed.");
    }

    @Override
    public String getBackendName() {
        return "Java Swing 2D (Phase 1)";
    }

    /** Direct access to the JPanel for adding to JFrame. */
    public Renderer getPanel() {
        return swingPanel;
    }
}