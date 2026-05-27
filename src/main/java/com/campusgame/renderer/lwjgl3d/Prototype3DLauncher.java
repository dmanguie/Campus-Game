package com.campusgame.renderer.lwjgl3d;

import com.campusgame.map.CampusMap;
import com.campusgame.map.data.BuildingData;

import java.util.List;

public class Prototype3DLauncher {

    public static void main(String[] args) {
        System.out.println("=== Campus Quest — 3D Prototype ===");
        System.out.println("Loading campus data…");

        CampusMap map = new CampusMap();
        List<BuildingData> all = map.getMutableBuildings();

        System.out.printf("Total buildings in map: %d%n", all.size());
        System.out.println("─────────────────────────────────────────────────────");

        int rectCount = 0, polyCount = 0, zeroSizeCount = 0, zeroFloorCount = 0;

        for (BuildingData b : all) {
            boolean poly     = b.isPolygon();
            boolean zeroSize = !poly && (b.width <= 0 || b.depth <= 0);
            boolean zeroFloor = b.floors <= 0;

            String status;
            if (poly)            status = "POLYGON→BBOX fallback";
            else if (zeroSize)   status = "SKIP (zero width/depth)";
            else if (zeroFloor)  status = "SKIP (zero floors)";
            else                 status = "RENDER";

            System.out.printf("  [%-22s] poly=%-5b x=%-7.0f z=%-7.0f w=%-6.0f d=%-6.0f floors=%-2d color=#%06X  → %s%n",
                    b.name.length() > 22 ? b.name.substring(0, 22) : b.name,
                    poly,
                    b.x, b.z, b.width, b.depth,
                    b.floors,
                    b.colorARGB & 0xFFFFFF,
                    status);

            if (poly)          polyCount++;
            else if (zeroSize || zeroFloor) zeroSizeCount++;
            else               rectCount++;
            if (zeroFloor)     zeroFloorCount++;
        }

        System.out.println("─────────────────────────────────────────────────────");
        System.out.printf("  Rectangular (rendered):   %d%n", rectCount);
        System.out.printf("  Polygon (bbox fallback):  %d%n", polyCount);
        System.out.printf("  Skipped (zero size/floor):%d%n", zeroSizeCount);
        System.out.printf("  Zero floors total:        %d%n", zeroFloorCount);
        System.out.println("─────────────────────────────────────────────────────");

        if (rectCount + polyCount == 0) {
            System.out.println("WARNING: No renderable buildings found. Check campus.json.");
        }

        // Pass ALL buildings — renderer now handles polygon bbox fallback
        System.out.println("Controls: W/A/S/D=move  F=fly/walk  Tab=pick mode  ESC=quit");
        new CampusRenderer3D(all, map.getPaths()).run();
        System.out.println("3D prototype closed.");
    }
}