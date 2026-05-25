package com.campusgame.world.interior;

import com.campusgame.map.data.EntranceData;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A self-contained interior coordinate space representing one building interior.
 *
 * Coordinate system: (0, 0) = top-left of the interior.
 * When active, the Camera is re-clamped to (0..width, 0..height).
 *
 * Scene contents:
 *   rooms  — renderable floor/wall areas (classrooms, offices, hallways)
 *   exits  — EntranceData entries whose worldX/Z are in interior space
 *             and whose exteriorSpawnX/Z are irrelevant (InteriorManager
 *             restores the player to where they entered from)
 *
 * Future-ready for:
 *   npcSchedules   — NPC home positions per time-of-day
 *   questMarkers   — floating ! / ? icons
 *   roomFlags      — locked rooms, tutorial rooms
 *   currentFloor   — multi-floor navigation
 *   ambientMusic   — scene-level audio track id
 */
public class InteriorScene {

    public final String id;
    public final String displayName;

    /** Interior scene dimensions in interior coordinate space. */
    public final float width;
    public final float height;

    /** Background / corridor fill color. */
    public final Color backgroundColor;

    /** Default spawn position (used when no entrance-specific spawn is given). */
    public float defaultSpawnX;
    public float defaultSpawnZ;

    public final List<InteriorRoom>  rooms = new ArrayList<>();
    public final List<EntranceData>  exits = new ArrayList<>();

    // Future:
    // public final List<NpcPlacement>   npcPlacements = new ArrayList<>();
    // public final List<QuestMarker>    questMarkers  = new ArrayList<>();
    // public int currentFloor = 1;

    public InteriorScene(String id, String displayName,
                         float width, float height,
                         Color backgroundColor) {
        this.id              = id;
        this.displayName     = displayName;
        this.width           = width;
        this.height          = height;
        this.backgroundColor = backgroundColor;
        this.defaultSpawnX   = width  / 2f;
        this.defaultSpawnZ   = height * 0.85f;  // near the bottom (entrance side)
    }

    public void addRoom(InteriorRoom room) { rooms.add(room); }
    public void addExit(EntranceData exit) { exits.add(exit); }
}