package com.campusgame.map.data;

import java.util.ArrayList;
import java.util.List;

/**
 * PATH DATA (map/data/PathData.java)
 * ------------------------------------
 * A campus pathway: ordered waypoints rendered as a thick stroked polyline.
 * Saved/loaded via MapJson.PathJson.
 *
 * Field names match the Phase 5 MapJson / CampusMap:
 *   width    — stroke width in world units (was strokeWidth)
 *   colorHex — "#FFRRGGBB" or "#RRGGBB" colour string (was colorARGB)
 *
 * The old field names caused a mismatch between PathData, MapJson.PathJson,
 * CampusMap.drawCampusPaths(), MapSaver, and MapLoader. This version aligns all five.
 */
public class PathData {

    public String        name;
    public float         width;       // stroke width in world units
    public String        colorHex;    // "#FFDCD7C8" etc.
    public List<float[]> points;      // each float[]{worldX, worldZ}

    public PathData(String name, float width, String colorHex) {
        this.name     = name;
        this.width    = width;
        this.colorHex = colorHex;
        this.points   = new ArrayList<>();
    }

    public void addPoint(float x, float z) { points.add(new float[]{x, z}); }

    public void movePoint(int idx, float x, float z) {
        if (idx >= 0 && idx < points.size()) {
            points.get(idx)[0] = x;
            points.get(idx)[1] = z;
        }
    }

    public void removePoint(int idx) {
        if (points.size() > 2) points.remove(idx);
    }

    public boolean isValid() { return points != null && points.size() >= 2; }
}