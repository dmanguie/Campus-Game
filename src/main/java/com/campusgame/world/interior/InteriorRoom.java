package com.campusgame.world.interior;

import java.awt.Color;

/**
 * A named rectangular area within an InteriorScene.
 * Could be a classroom, office, hallway, library section, etc.
 *
 * Coordinates are in interior scene space (origin = top-left of scene).
 *
 * Future:
 *   - List<NpcPlacement>  npcs
 *   - List<ItemDrop>      items
 *   - QuestMarker         questMarker
 *   - int                 floor  (for multi-floor buildings)
 *   - boolean             locked
 *   - String              ambientSound
 */
public class InteriorRoom {

    public final String id;
    public final String displayName;

    // Interior coordinate bounds
    public final float x, z, width, depth;

    // Visual
    public final Color floorColor;
    public final Color wallColor;

    public InteriorRoom(String id, String displayName,
                        float x, float z, float width, float depth,
                        Color floorColor, Color wallColor) {
        this.id          = id;
        this.displayName = displayName;
        this.x           = x;
        this.z           = z;
        this.width       = width;
        this.depth       = depth;
        this.floorColor  = floorColor;
        this.wallColor   = wallColor;
    }
}