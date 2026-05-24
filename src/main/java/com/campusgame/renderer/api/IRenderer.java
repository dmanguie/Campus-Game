package com.campusgame.renderer.api;

import com.campusgame.core.GameWorld;
import com.campusgame.entities.Player;
import com.campusgame.map.data.MapData;
import com.campusgame.renderer.Camera;

/**
 * IRENDERER (renderer/api/IRenderer.java)
 * -----------------------------------------
 * The renderer contract. Every rendering backend must implement this.
 *
 * Current implementations:
 *   SwingRenderer2D  → Java Swing/AWT top-down (Phase 1, working now)
 *
 * Future implementations:
 *   JavaFXRenderer   → JavaFX Canvas isometric / 2.5D  (Phase 2)
 *   LwjglRenderer3D  → LWJGL OpenGL full 3D Roblox-style (Phase 3)
 *
 * GameLoop holds an IRenderer reference. To switch rendering backends:
 *   gameLoop.setRenderer(new LwjglRenderer3D(...));
 * No other code changes required.
 *
 * RENDER LAYERS (all renderers must draw in this order):
 *   1. Sky / background
 *   2. Ground plane (grass, paths)
 *   3. Buildings (opaque, back-to-front)
 *   4. Entities (player, NPCs)
 *   5. Particles / effects
 *   6. HUD overlay (minimap, health, labels)
 */
public interface IRenderer {

    /**
     * Called once during game init.
     * Load shaders, create render targets, build scene graph here.
     */
    void init();

    /**
     * Called every frame by GameLoop after all updates.
     * Draws the full scene for this frame.
     *
     * @param world  All active GameObjects (entities to render)
     * @param player The player (often needs special handling)
     * @param camera Current camera state
     */
    void render(GameWorld world, Player player, Camera camera);

    /**
     * Called when the window is resized.
     * Update viewport, projection matrix, etc.
     */
    void onResize(int width, int height);

    /**
     * Called when the game shuts down.
     * Release GPU resources, close windows, etc.
     */
    void dispose();

    /**
     * Human-readable name of this renderer backend.
     * Shown in window title / debug HUD.
     */
    String getBackendName();
}