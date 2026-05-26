package com.campusgame.world.interior;

import com.campusgame.map.data.EntranceData;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * INTERIOR SCENE (world/interior/InteriorScene.java)
 * A self-contained interior coordinate space representing one building interior.
 *
 * Coordinate system: (0,0) = top-left of the interior.
 * When active, the Camera is re-clamped to (0..width, 0..height).
 */
public class InteriorScene {

    public final String id;
    public final String displayName;

    public final float width;
    public final float height;

    public final Color backgroundColor;

    public float defaultSpawnX;
    public float defaultSpawnZ;

    public final List<InteriorRoom>  rooms = new ArrayList<>();
    public final List<EntranceData>  exits = new ArrayList<>();

    public InteriorScene(String id, String displayName,
                         float width, float height,
                         Color backgroundColor) {
        this.id              = id;
        this.displayName     = displayName;
        this.width           = width;
        this.height          = height;
        this.backgroundColor = backgroundColor;
        this.defaultSpawnX   = width  / 2f;
        this.defaultSpawnZ   = height * 0.85f;
    }

    public void addRoom(InteriorRoom room) { rooms.add(room); }
    public void addExit(EntranceData exit) { exits.add(exit); }
}