package com.campusgame.interaction;

/**
 * INTERACTABLE (interaction/Interactable.java)
 * Contract for anything a player can trigger with [E].
 *
 * Implementors:
 *   DoorTrigger  — enter a building interior
 *   ExitTrigger  — leave a building back to campus
 *
 * Future:
 *   NpcTrigger, QuestMarker, ItemPickup, TerminalKiosk
 */
public interface Interactable {
    boolean isInRange(float playerX, float playerZ);
    String  getPromptText();
    void    onInteract(InteractionContext ctx);
    float   getWorldX();
    float   getWorldZ();
}