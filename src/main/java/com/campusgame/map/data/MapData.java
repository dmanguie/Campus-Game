package com.campusgame.map.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapData {

    // ---------------------------------------------------------------
    // WORLD SIZE
    // ---------------------------------------------------------------
    public static final int WORLD_WIDTH  = 3000;
    public static final int WORLD_HEIGHT = 2400;

    public static final List<BuildingData> BUILDINGS;

    static {
        List<BuildingData> list = new ArrayList<>();
// WEST SIDE
        list.add(new BuildingData(
                "GYM",
                620, 470,
                260, 180,
                1,
                0xFF8E44AD,
                true,
                "gym"
        ));

        list.add(new BuildingData(
                "SAL BLDG",
                620, 710,
                120, 300,
                3,
                0xFF2980B9,
                true,
                "academic"
        ));

        list.add(new BuildingData(
                "ALLIED BLDG",
                540, 1110,
                180, 520,
                3,
                0xFFC0392B,
                true,
                "academic"
        ));

// NORTH
        list.add(new BuildingData(
                "ELEM BLDG",
                1040, 430,
                420, 120,
                2,
                0xFF27AE60,
                -6f,
                true,
                "academic"
        ));

// CENTER
        list.add(new BuildingData(
                "P.E AREA",
                1020, 670,
                260, 180,
                1,
                0xFF27AE60,
                false,
                "pe"
        ));

        list.add(new BuildingData(
                "LRAC",
                980, 970,
                320, 140,
                2,
                0xFFE74C3C,
                true,
                "resource"
        ));


        list.add(new BuildingData(
                "RTL BLDG",
                930, 1190,
                420, 55,
                2,
                0xFFE67E22,
                true,
                "academic"
        ));

        // RTL BLDG - thick rectangle doughnut
// Much thicker school hallways

        list.add(new BuildingData(
                "RTL BLDG 1",
                930, 1190,
                420, 80,
                2,
                0xFFE67E22,
                true,
                "academic"
        ));

        list.add(new BuildingData(
                "RTL BLDG 2",
                930, 1370,
                420, 80,
                2,
                0xFFE67E22,
                true,
                "academic"
        ));

        list.add(new BuildingData(
                "RTL BLDG 3",
                930, 1270,
                80, 100,
                2,
                0xFFE67E22,
                true,
                "academic"
        ));

        list.add(new BuildingData(
                "RTL BLDG 4",
                1270, 1270,
                80, 100,
                2,
                0xFFE67E22,
                true,
                "academic"
        ));

// EAST SIDE
        // ACAD / Engineering Building - long upper building
        list.add(new BuildingData(
                "ACAD BLDG",

                new int[]{ // X
                        1580, // A
                        1590, // B
                        1600,
                        1610,
                        1680,
                        1660,
                        1905,
                        1900
                },

                new int[]{ // Y
                        850, // A
                        780,
                        780,
                        670,
                        675,
                        780,
                        810,
                        880
                },
                4,
                0xFF3498DB,
                4f,
                true,
                "academic"
        ));

        list.add(new BuildingData(
                "GLE BLDG",

                new int[] { // X
                        1630, // C
                        1900, // D
                        1890, // E
                        1800, // F
                        1680, // B
                        1640  // A
                },

                new int[] { // Y
                        880, // C
                        890, // D
                        1080, // E
                        1100, // F
                        1310, // B
                        1150  // A
                },

                2,
                0xFF16A085,
                2f,
                true,
                "academic"
        ));

        list.add(new BuildingData(
                "GLE Firewall",

                new int[] { // X
                        1915, // B
                        1975, // C
                        1960, // D
                        1900  // A
                },

                new int[] { // Y
                        805, // B
                        815, // C
                        1050, // D
                        1050 // A
                },

                2,
                0xFF16A085,
                2f,
                true,
                "wall"
        ));

        // NGE BLDG - smaller but still thick rectangle doughnut

        list.add(new BuildingData(
                "NGE BLDG 1",
                1100, 1530,
                230, 70,
                4,
                0xFFF39C12,
                true,
                "academic"
        ));

        list.add(new BuildingData(
                "NGE BLDG 2",
                1100, 1705,
                230, 70,
                4,
                0xFFF39C12,
                true,
                "academic"
        ));

        list.add(new BuildingData(
                "NGE BLDG 3",
                1100, 1600,
                90, 110,
                4,
                0xFFF39C12,
                true,
                "academic"
        ));

        list.add(new BuildingData(
                "NGE BLDG 4",
                1280, 1600,
                90, 110,
                4,
                0xFFF39C12,
                true,
                "academic"
        ));


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