package com.campusgame.entities;

import com.campusgame.interaction.Interactable;
import com.campusgame.interaction.InteractionContext;
import com.campusgame.map.data.EntranceData;

/**
 * DOOR TRIGGER (entities/DoorTrigger.java)
 * Placed at a campus building entrance.
 * When player presses [E] within range, starts the interior transition.
 *
 * Phase 6: prompt text now uses EntranceData.effectivePromptName() so that
 * renamed targetDisplayName values (set in the editor, saved to JSON) are
 * reflected here without any other changes.
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

    /**
     * Returns the [E] prompt string.
     * Uses effectivePromptName() which honours targetDisplayName → displayName → label.
     */
    @Override
    public String getPromptText() {
        return "Press [E] to enter " + data.effectivePromptName();
    }

    @Override
    public void onInteract(InteractionContext ctx) {
        ctx.interiorManager.enterScene(data, ctx.player, ctx.camera);
    }

    @Override public float      getWorldX() { return data.worldX; }
    @Override public float      getWorldZ() { return data.worldZ; }
    public    EntranceData getData()        { return data; }
}