package com.campusgame.renderer.lwjgl3d;

import com.campusgame.map.CampusMap;
import com.campusgame.map.data.BuildingData;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Prototype3DLauncher
 * ──────────────────────────────────────────────────────────────────────────
 * Standalone main() for the 3-D prototype.
 *
 * Loads campus.json via the existing CampusMap/MapLoader pipeline,
 * filters out polygon buildings (not supported in first prototype),
 * then opens a LWJGL window.
 *
 * The Swing game is NOT started.  GameLoop is NOT touched.
 *
 * Run with:
 *   mvn exec:java -Dexec.mainClass="com.campusgame.renderer.lwjgl3d.Prototype3DLauncher"
 *
 * Or via your IDE by pointing the run configuration at this class.
 */
public class Prototype3DLauncher {

    public static void main(String[] args) {
        System.out.println("=== Campus Quest — 3D Prototype ===");
        System.out.println("Loading campus data…");

        // Reuse existing map loader — no game systems started
        CampusMap map = new CampusMap();

        List<BuildingData> buildings = map.getMutableBuildings()
                .stream()
                .filter(b -> !b.isPolygon())   // rectangular only
                .collect(Collectors.toList());

        System.out.printf("Loaded %d rectangular buildings (polygons skipped).%n",
                buildings.size());

        System.out.println("Controls: W/A/S/D = move, Mouse drag = look, Scroll = zoom, ESC = quit");

        new CampusRenderer3D(buildings).run();

        System.out.println("3D prototype closed.");
    }
}