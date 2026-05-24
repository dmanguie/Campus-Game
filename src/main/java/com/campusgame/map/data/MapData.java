package com.campusgame.map.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MAP DATA (map/data/MapData.java)
 * ----------------------------------
 * Pure data registry: world size + all building definitions.
 * Contains ZERO rendering, ZERO AWT/Swing/JavaFX/LWJGL code.
 *
 * This is the canonical data source for the campus.
 * Both CampusMap (2D legacy) and any future 3D world loader
 * will pull from this single registry.
 *
 * HOW TO ADD A BUILDING:
 *   Add one entry to BUILDINGS below. That's it.
 *   No other file needs to change.
 *
 * COLOR FORMAT: 0xFFRRGGBB packed ARGB integer
 *   e.g. 0xFF8E44AD = fully opaque purple
 *        0xFF27AE60 = fully opaque green
 *
 * COORDINATE SYSTEM:
 *   x = east-west   (screen X in 2D)
 *   z = north-south (screen Y in 2D)
 *   All values in world units (1 unit = 1 pixel at 2D zoom level)
 *
 * FUTURE:
 *   Replace this static array with a JSON loader:
 *   List<BuildingData> = GsonLoader.load("assets/campus_map.json")
 */
public class MapData {

    // ---------------------------------------------------------------
    // WORLD SIZE
    // ---------------------------------------------------------------
    public static final int WORLD_WIDTH  = 2000;
    public static final int WORLD_HEIGHT = 1600;

    // ---------------------------------------------------------------
    // BUILDING REGISTRY
    //
    // new BuildingData(
    //   "NAME",
    //   x,   z,         ← top-left corner (world units)
    //   width, depth,   ← footprint size
    //   floors,         ← determines 3D height
    //   0xFFRRGGBB,     ← roof/wall color
    //   collisionEnabled,
    //   "tag"           ← optional category
    // )
    // ---------------------------------------------------------------
    public static final List<BuildingData> BUILDINGS;

    static {
        List<BuildingData> list = new ArrayList<>();

        list.add(new BuildingData("GYM",          120,  120, 160, 120, 1, 0xFF8E44AD, true,  "gym"));
        list.add(new BuildingData("ELEM BLDG",    350,  100, 180, 100, 2, 0xFF27AE60, true,  "academic"));
        list.add(new BuildingData("SAL BLDG",     600,   90, 140, 110, 3, 0xFF2980B9, true,  "academic"));
        list.add(new BuildingData("LRAC",         820,  100, 170, 130, 2, 0xFFE74C3C, true,  "resource"));
        list.add(new BuildingData("RTL BLDG",    1060,  110, 150, 100, 2, 0xFFE67E22, true,  "academic"));
        list.add(new BuildingData("GLE BLDG",     120,  340, 160, 120, 2, 0xFF16A085, true,  "academic"));
        list.add(new BuildingData("ACAD BLDG",    380,  330, 220, 150, 4, 0xFF3498DB, true,  "academic"));
        list.add(new BuildingData("NGE BLDG",     700,  320, 160, 130, 3, 0xFFF39C12, true,  "academic"));
        list.add(new BuildingData("ALLIED BLDG",  960,  310, 180, 140, 3, 0xFFC0392B, true,  "academic"));
        list.add(new BuildingData("P.E AREA",     200,  580, 300, 200, 1, 0xFF27AE60, false, "pe"));

        BUILDINGS = Collections.unmodifiableList(list);
    }

    // ---------------------------------------------------------------
    // QUERY HELPERS
    // ---------------------------------------------------------------

    /** Returns all buildings with collision enabled. Used by physics. */
    public static List<BuildingData> getCollidable() {
        List<BuildingData> result = new ArrayList<>();
        for (BuildingData b : BUILDINGS) {
            if (b.collisionEnabled) result.add(b);
        }
        return result;
    }

    /** Returns all buildings matching a tag. e.g. getByTag("academic") */
    public static List<BuildingData> getByTag(String tag) {
        List<BuildingData> result = new ArrayList<>();
        for (BuildingData b : BUILDINGS) {
            if (tag.equalsIgnoreCase(b.tag)) result.add(b);
        }
        return result;
    }

    /** Returns the BuildingData whose footprint contains world point (px, pz). */
    public static BuildingData findAt(float px, float pz) {
        for (BuildingData b : BUILDINGS) {
            if (px >= b.x && px <= b.maxX() &&
                    pz >= b.z && pz <= b.maxZ()) {
                return b;
            }
        }
        return null;
    }

    private MapData() {} // static utility class — do not instantiate
}