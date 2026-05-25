package com.campusgame.interaction;

import com.campusgame.entities.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Scans all active interactables every frame to find the one nearest to
 * the player that is within range.
 *
 * Scene changes (exterior ↔ interior) call setInteractables() to swap
 * the entire trigger set in one operation.
 */
public class InteractionSystem {

    private final List<Interactable> interactables = new ArrayList<>();
    private Interactable nearest = null;

    // ── Trigger management ────────────────────────────────────────────

    /** Full replacement — use when changing scene. */
    public void setInteractables(List<? extends Interactable> list) {
        interactables.clear();
        interactables.addAll(list);
        nearest = null;
    }

    public void addInteractable(Interactable i)    { interactables.add(i); }
    public void removeInteractable(Interactable i) { interactables.remove(i); }
    public void clearInteractables()               { interactables.clear(); nearest = null; }

    // ── Per-frame update ──────────────────────────────────────────────

    /**
     * Call once per frame after player.update().
     * Finds the nearest in-range interactable (or null).
     */
    public void update(Player player) {
        float px = player.getCenterX();
        float pz = player.getCenterY();   // screen-Y == world-Z in this engine

        nearest = null;
        float bestDist2 = Float.MAX_VALUE;

        for (Interactable i : interactables) {
            if (!i.isInRange(px, pz)) continue;

            float dx = i.getWorldX() - px;
            float dz = i.getWorldZ() - pz;
            float d2 = dx * dx + dz * dz;

            if (d2 < bestDist2) {
                bestDist2 = d2;
                nearest   = i;
            }
        }
    }

    // ── Interaction ───────────────────────────────────────────────────

    /** Call when player presses [E]. No-op if nothing is in range. */
    public void tryInteract(InteractionContext ctx) {
        if (nearest != null) nearest.onInteract(ctx);
    }

    // ── Accessors ─────────────────────────────────────────────────────

    public Interactable getNearest() { return nearest; }
    public boolean      hasNearest() { return nearest != null; }
}