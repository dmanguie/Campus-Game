package com.campusgame.entities;

import com.campusgame.interaction.Interactable;
import com.campusgame.interaction.InteractionContext;
import com.campusgame.map.data.EntranceData;

/**
 * Placed at a campus building entrance.
 * When triggered, starts the transition into the linked InteriorScene.
 *
 * Future: add locked state, prerequisite quest check.
 */
public class DoorTrigger implements Interactable {

    private final EntranceData data;

    public DoorTrigger(EntranceData data) {
        this.data = data;
    }

    @Override
    public boolean isInRange(float playerX, float playerZ) {
        float dx = playerX - data.worldX;
        float dz = playerZ - data.worldZ;
        return (dx * dx + dz * dz) <= (data.triggerRadius * data.triggerRadius);
    }

    @Override
    public String getPromptText() {
        return "Press [E] to enter " + data.label;
    }

    @Override
    public void onInteract(InteractionContext ctx) {
        ctx.interiorManager.enterScene(data, ctx.player, ctx.camera);
    }

    @Override public float      getWorldX() { return data.worldX; }
    @Override public float      getWorldZ() { return data.worldZ; }
    public    EntranceData getData()        { return data; }
}