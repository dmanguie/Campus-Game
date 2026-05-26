package com.campusgame.interaction;

import com.campusgame.entities.Player;
import com.campusgame.renderer.Camera;
import com.campusgame.world.interior.InteriorManager;

/**
 * INTERACTION CONTEXT (interaction/InteractionContext.java)
 * Thin bag of references passed into Interactable.onInteract().
 * Avoids coupling trigger implementations directly to GameLoop.
 */
public final class InteractionContext {
    public final Player          player;
    public final Camera          camera;
    public final InteriorManager interiorManager;

    public InteractionContext(Player player, Camera camera, InteriorManager interiorManager) {
        this.player          = player;
        this.camera          = camera;
        this.interiorManager = interiorManager;
    }
}