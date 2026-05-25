package com.campusgame.interaction;

import com.campusgame.entities.Player;
import com.campusgame.renderer.Camera;
import com.campusgame.world.interior.InteriorManager;

/**
 * Thin bag of game-system references passed into Interactable.onInteract().
 *
 * Avoids coupling trigger implementations to GameLoop.
 *
 * Future: add QuestSystem, DialogueSystem, InventorySystem, NPCScheduler.
 */
public final class InteractionContext {

    public final Player          player;
    public final Camera          camera;
    public final InteriorManager interiorManager;

    // Future:
    // public final QuestSystem   questSystem;
    // public final DialogueSystem dialogue;
    // public final Inventory     inventory;

    public InteractionContext(Player player,
                              Camera camera,
                              InteriorManager interiorManager) {
        this.player          = player;
        this.camera          = camera;
        this.interiorManager = interiorManager;
    }
}