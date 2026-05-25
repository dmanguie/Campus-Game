package com.campusgame.interaction;

/**
 * Contract for anything a player can trigger with [E].
 *
 * Implementors:
 *   DoorTrigger   — enter a building interior
 *   ExitTrigger   — leave a building back to campus
 *
 * Future implementors:
 *   NpcTrigger    — start dialogue
 *   QuestMarker   — pick up / turn in quest
 *   ItemPickup    — grab an item
 *   TerminalKiosk — open a UI panel
 */
public interface Interactable {

    /** True when the player is within activation distance. */
    boolean isInRange(float playerX, float playerZ);

    /** Text rendered in the interaction prompt. Include "[E]" for highlight. */
    String getPromptText();

    /** Fires when player presses E while this is the nearest in-range trigger. */
    void onInteract(InteractionContext ctx);

    /** World position of this trigger — used for distance sorting and dot indicator. */
    float getWorldX();
    float getWorldZ();
}