package com.campusgame.physics;

import com.campusgame.entities.Player;
import com.campusgame.map.Building;
import com.campusgame.map.CampusMap;

import java.awt.*;

/**
 * COLLISION MANAGER (physics/CollisionManager.java)
 * ---------------------------------------------------
 * Detects and resolves player-vs-building collisions.
 *
 * Algorithm: AABB (Axis-Aligned Bounding Box) overlap + minimum pushout.
 *
 * Each frame (after player.update):
 *  1. Check player AABB against every building AABB
 *  2. If overlapping, find the smallest axis to push the player out
 *  3. Adjust player position accordingly
 *  4. Zero out velocity component in direction of collision
 *
 * Also:
 *  - Clamps player to world bounds (can't walk off the map)
 *
 * Future expansion:
 *  - Broad-phase (spatial grid) for large building counts
 *  - Sliding collision (move along walls)
 *  - Trigger zones (enter building detection)
 */
public class CollisionManager {

    private final CampusMap campusMap;

    // Padding adds a tiny gap so the player doesn't clip into walls
    private static final int WALL_PADDING = 1;

    public CollisionManager(CampusMap campusMap) {
        this.campusMap = campusMap;
    }

    /**
     * Main collision resolution. Call every frame after player.update().
     * Modifies player position and velocity to prevent building penetration.
     */
    public void resolve(Player player) {
        // 1. Clamp to world bounds first
        clampToWorld(player);

        // 2. Resolve each building
        for (Building building : campusMap.getBuildings()) {
            resolveBuilding(player, building);
        }
    }

    /**
     * AABB pushout for one building.
     */
    private void resolveBuilding(Player player, Building building) {
        Rectangle pRect = player.getBounds();
        Rectangle bRect = building.getBounds();

        if (!pRect.intersects(bRect)) return;

        float overlapLeft   = (bRect.x + bRect.width)  - pRect.x;
        float overlapRight  = (pRect.x + pRect.width)  - bRect.x;
        float overlapTop    = (bRect.y + bRect.height) - pRect.y;
        float overlapBottom = (pRect.y + pRect.height) - bRect.y;

        float minX = Math.min(overlapLeft, overlapRight);
        float minY = Math.min(overlapTop,  overlapBottom);

        if (minX < minY) {
            if (overlapLeft < overlapRight) {
                player.x += overlapLeft + WALL_PADDING;
            } else {
                player.x -= overlapRight + WALL_PADDING;
            }
            player.vx = 0;
        } else {
            if (overlapTop < overlapBottom) {
                player.z += overlapTop + WALL_PADDING;   // PHASE 2: z not y
            } else {
                player.z -= overlapBottom + WALL_PADDING;
            }
            player.vy = 0;
        }
    }

    private void clampToWorld(Player player) {
        float hw = Player.WIDTH  / 2f;
        float hh = Player.HEIGHT / 2f;
        int   ww = CampusMap.WORLD_WIDTH;
        int   wh = CampusMap.WORLD_HEIGHT;

        if (player.x - hw < 0)      { player.x = hw;      player.vx = 0; }
        if (player.x + hw > ww)     { player.x = ww - hw; player.vx = 0; }
        if (player.z - hh < 0)      { player.z = hh;      player.vy = 0; }  // PHASE 2: z
        if (player.z + hh > wh)     { player.z = wh - hh; player.vy = 0; }  // PHASE 2: z
    }

    /**
     * Returns true if the given rectangle overlaps any building.
     * Useful for future: NPC pathfinding, item placement, spawn point validation.
     */
    public boolean overlapsBuilding(Rectangle rect) {
        for (Building b : campusMap.getBuildings()) {
            if (rect.intersects(b.getBounds())) return true;
        }
        return false;
    }
}