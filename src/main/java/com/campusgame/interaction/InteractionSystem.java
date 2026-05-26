package com.campusgame.interaction;

import com.campusgame.entities.Player;
import java.util.ArrayList;
import java.util.List;

/**
 * INTERACTION SYSTEM (interaction/InteractionSystem.java)
 * Scans all active interactables every frame to find the nearest
 * one in range. Scene changes swap the full trigger list via setInteractables().
 */
public class InteractionSystem {

    private final List<Interactable> interactables = new ArrayList<>();
    private Interactable nearest = null;

    public void setInteractables(List<? extends Interactable> list) {
        interactables.clear();
        interactables.addAll(list);
        nearest = null;
    }

    public void addInteractable(Interactable i)    { interactables.add(i); }
    public void removeInteractable(Interactable i) { interactables.remove(i); }
    public void clearInteractables()               { interactables.clear(); nearest = null; }

    public void update(Player player) {
        float px = player.getCenterX();
        float pz = player.getCenterY();
        nearest = null;
        float bestDist2 = Float.MAX_VALUE;
        for (Interactable i : interactables) {
            if (!i.isInRange(px, pz)) continue;
            float dx = i.getWorldX() - px;
            float dz = i.getWorldZ() - pz;
            float d2 = dx * dx + dz * dz;
            if (d2 < bestDist2) { bestDist2 = d2; nearest = i; }
        }
    }

    public void tryInteract(InteractionContext ctx) {
        if (nearest != null) nearest.onInteract(ctx);
    }

    public Interactable getNearest() { return nearest; }
    public boolean      hasNearest() { return nearest != null; }
}