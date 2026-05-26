package com.campusgame.world.interior;

import com.campusgame.entities.ExitTrigger;
import com.campusgame.entities.Player;
import com.campusgame.interaction.Interactable;
import com.campusgame.interaction.InteractionSystem;
import com.campusgame.map.data.EntranceData;
import com.campusgame.renderer.Camera;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * INTERIOR MANAGER (world/interior/InteriorManager.java)
 * -------------------------------------------------------
 * Owns the exterior ↔ interior transition lifecycle.
 *
 * ENTERING a building:
 *   DoorTrigger.onInteract() → enterScene()
 *   → SceneTransition FADE_OUT
 *   → SWITCH: player teleported, scene activated, camera re-clamped,
 *             InteractionSystem swapped to exit triggers
 *   → FADE_IN
 *
 * EXITING a building:
 *   ExitTrigger.onInteract() → exitToExterior()
 *   → same fade cycle in reverse
 *   → player returned to saved exterior position
 *   → InteractionSystem swapped back to door triggers
 */
public class InteriorManager {

    private InteriorScene currentScene    = null;
    private EntranceData  pendingEntrance = null;
    private boolean       pendingIsExit   = false;

    private float savedExteriorX = 0f;
    private float savedExteriorZ = 0f;

    private final InteriorSceneRegistry registry;
    private final InteriorRenderer      renderer;
    private final SceneTransition       transition;
    private final InteractionSystem     interactionSystem;

    private final List<Interactable> exteriorTriggers;
    private final List<Interactable> exitTriggers = new ArrayList<>();

    public InteriorManager(InteriorSceneRegistry registry,
                           InteractionSystem     interactionSystem,
                           List<Interactable>    exteriorTriggers) {
        this.registry          = registry;
        this.interactionSystem = interactionSystem;
        this.exteriorTriggers  = exteriorTriggers;
        this.renderer          = new InteriorRenderer();
        this.transition        = new SceneTransition();
    }

    // ── Public API ────────────────────────────────────────────────────

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

        float sx = pendingEntrance.interiorSpawnX > 0
                ? pendingEntrance.interiorSpawnX : currentScene.defaultSpawnX;
        float sz = pendingEntrance.interiorSpawnZ > 0
                ? pendingEntrance.interiorSpawnZ : currentScene.defaultSpawnZ;
        player.x = sx;
        player.z = sz;

        camera.setWorldBounds(currentScene.width, currentScene.height);
        camera.snapTo(player);

        exitTriggers.clear();
        for (EntranceData exit : currentScene.exits)
            exitTriggers.add(new ExitTrigger(exit));
        interactionSystem.setInteractables(exitTriggers);

        pendingEntrance = null;
    }

    private void performExit(Player player, Camera camera) {
        currentScene = null;

        player.x = savedExteriorX;
        player.z = savedExteriorZ;

        camera.setWorldBounds(
                com.campusgame.map.CampusMap.WORLD_WIDTH,
                com.campusgame.map.CampusMap.WORLD_HEIGHT);
        camera.snapTo(player);

        interactionSystem.setInteractables(exteriorTriggers);
    }

    // ── Draw API ──────────────────────────────────────────────────────

    public boolean drawIfIndoors(Graphics2D g, Camera camera,
                                 int screenW, int screenH) {
        if (currentScene == null) return false;
        renderer.draw(g, currentScene, camera, screenW, screenH);
        return true;
    }

    public void drawTransition(Graphics2D g, int screenW, int screenH) {
        transition.draw(g, screenW, screenH);
    }

    // ── Queries ───────────────────────────────────────────────────────

    public boolean       isIndoors()       { return currentScene != null; }
    public boolean       isTransitioning() { return transition.isActive(); }
    public InteriorScene getCurrentScene() { return currentScene; }
}