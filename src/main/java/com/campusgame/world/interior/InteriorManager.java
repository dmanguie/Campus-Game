package com.campusgame.world.interior;

import com.campusgame.entities.ExitTrigger;
import com.campusgame.entities.Player;
import com.campusgame.interaction.Interactable;
import com.campusgame.interaction.InteractionSystem;
import com.campusgame.map.CampusMap;
import com.campusgame.map.data.EntranceData;
import com.campusgame.renderer.Camera;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * INTERIOR MANAGER  —  the heart of Phase 5.
 *
 * Owns the exterior ↔ interior transition lifecycle:
 *
 *   ENTERING a building:
 *     DoorTrigger.onInteract() → enterScene()
 *     → SceneTransition FADE_OUT
 *     → SWITCH frame: player teleported, scene activated, camera re-clamped,
 *                     InteractionSystem swapped to exit triggers
 *     → FADE_IN
 *
 *   EXITING a building:
 *     ExitTrigger.onInteract() → exitToExterior()
 *     → same fade cycle in reverse
 *     → player teleported back to saved exterior position
 *     → InteractionSystem swapped back to door triggers
 *
 * Camera world-bounds:
 *   When entering, Camera is re-clamped to (scene.width × scene.height).
 *   When exiting, Camera is restored to (CampusMap.WORLD_WIDTH × WORLD_HEIGHT).
 *   This keeps the camera from showing empty space outside the interior.
 */
public class InteriorManager {

    // ── Scene state ───────────────────────────────────────────────────
    private InteriorScene currentScene    = null;     // null  = exterior
    private EntranceData  pendingEntrance = null;
    private boolean       pendingIsExit   = false;

    // Where the player was before entering (restore on exit)
    private float savedExteriorX = 0f;
    private float savedExteriorZ = 0f;

    // ── Systems ───────────────────────────────────────────────────────
    private final InteriorSceneRegistry registry;
    private final InteriorRenderer      renderer;
    private final SceneTransition       transition;
    private final InteractionSystem     interactionSystem;

    // ── Trigger lists ─────────────────────────────────────────────────
    /** Door triggers for the exterior campus. Set once; kept alive across scenes. */
    private final List<Interactable> exteriorTriggers;
    /** Exit triggers built fresh each time a scene is entered. */
    private final List<Interactable> exitTriggers = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────────────

    public InteriorManager(InteriorSceneRegistry    registry,
                           InteractionSystem        interactionSystem,
                           List<Interactable>       exteriorTriggers) {
        this.registry             = registry;
        this.interactionSystem    = interactionSystem;
        this.exteriorTriggers     = exteriorTriggers;
        this.renderer             = new InteriorRenderer();
        this.transition           = new SceneTransition();
    }

    // ── Public API ────────────────────────────────────────────────────

    /**
     * Called by DoorTrigger.onInteract() — starts entry transition.
     */
    public void enterScene(EntranceData entrance, Player player, Camera camera) {
        if (transition.isActive()) return;
        if (!registry.has(entrance.interiorSceneId)) {
            System.err.println("[InteriorManager] Unknown scene: " + entrance.interiorSceneId);
            return;
        }
        savedExteriorX  = player.x;
        savedExteriorZ  = player.z;
        pendingEntrance = entrance;
        pendingIsExit   = false;
        transition.start();
    }

    /**
     * Called by ExitTrigger.onInteract() — starts exit transition.
     */
    public void exitToExterior(Player player, Camera camera) {
        if (transition.isActive()) return;
        pendingIsExit   = true;
        pendingEntrance = null;
        transition.start();
    }

    // ── Update ────────────────────────────────────────────────────────

    public void update(float delta, Player player, Camera camera) {
        transition.update(delta);

        if (transition.isReadyToSwitch()) {
            if (pendingIsExit) performExit(player, camera);
            else               performEntry(player, camera);
            transition.notifySwitched();
        }
    }

    // ── Internal switch logic ─────────────────────────────────────────

    private void performEntry(Player player, Camera camera) {
        currentScene = registry.get(pendingEntrance.interiorSceneId);

        // Teleport player to interior spawn
        float sx = pendingEntrance.interiorSpawnX > 0
                ? pendingEntrance.interiorSpawnX : currentScene.defaultSpawnX;
        float sz = pendingEntrance.interiorSpawnZ > 0
                ? pendingEntrance.interiorSpawnZ : currentScene.defaultSpawnZ;
        player.x = sx;
        player.z = sz;

        // Re-clamp camera to interior bounds
        camera.setWorldBounds(currentScene.width, currentScene.height);
        camera.snapTo(player);

        // Swap interaction triggers to the scene's exits
        exitTriggers.clear();
        for (EntranceData exit : currentScene.exits) {
            exitTriggers.add(new ExitTrigger(exit));
        }
        interactionSystem.setInteractables(exitTriggers);

        pendingEntrance = null;
    }

    private void performExit(Player player, Camera camera) {
        currentScene = null;

        // Return player to saved exterior position
        player.x = savedExteriorX;
        player.z = savedExteriorZ;

        // Restore exterior camera bounds
        camera.setWorldBounds(
                com.campusgame.map.CampusMap.WORLD_WIDTH,
                com.campusgame.map.CampusMap.WORLD_HEIGHT);
        camera.snapTo(player);

        // Swap interaction triggers back to campus doors
        interactionSystem.setInteractables(exteriorTriggers);
    }

    // ── Draw API ──────────────────────────────────────────────────────

    /**
     * Draws the interior if active.
     * @return true if interior was drawn (Renderer should skip normal exterior draw).
     */
    public boolean drawIfIndoors(Graphics2D g, Camera camera,
                                 int screenW, int screenH) {
        if (currentScene == null) return false;
        renderer.draw(g, currentScene, camera, screenW, screenH);
        return true;
    }

    /**
     * Always call after all other drawing — overlays the transition fade.
     */
    public void drawTransition(Graphics2D g, int screenW, int screenH) {
        transition.draw(g, screenW, screenH);
    }

    // ── Queries ───────────────────────────────────────────────────────

    public boolean       isIndoors()       { return currentScene != null; }
    public boolean       isTransitioning() { return transition.isActive(); }
    public InteriorScene getCurrentScene() { return currentScene; }
}