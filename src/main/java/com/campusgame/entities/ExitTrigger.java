package com.campusgame.entities;

import com.campusgame.interaction.Interactable;
import com.campusgame.interaction.InteractionContext;
import com.campusgame.map.data.EntranceData;

/**
 * Placed near the door of an interior scene.
 * When triggered, returns the player to the exterior campus world.
 *
 * The exit's worldX/Z is in INTERIOR coordinate space.
 * The exterior return position comes from InteriorManager's saved state.
 */
public class ExitTrigger implements Interactable {

    private final EntranceData exitData;

    public ExitTrigger(EntranceData exitData) {
        this.exitData = exitData;
    }

    @Override
    public boolean isInRange(float playerX, float playerZ) {
        float dx = playerX - exitData.worldX;
        float dz = playerZ - exitData.worldZ;
        return (dx * dx + dz * dz) <= (exitData.triggerRadius * exitData.triggerRadius);
    }

    @Override
    public String getPromptText() {
        return "Press [E] to exit to campus";
    }

    @Override
    public void onInteract(InteractionContext ctx) {
        ctx.interiorManager.exitToExterior(ctx.player, ctx.camera);
    }

    @Override public float      getWorldX() { return exitData.worldX; }
    @Override public float      getWorldZ() { return exitData.worldZ; }
    public    EntranceData getExitData()    { return exitData; }
}