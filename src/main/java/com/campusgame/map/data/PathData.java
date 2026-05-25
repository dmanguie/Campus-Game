package com.campusgame.map.data;

import java.util.ArrayList;
import java.util.List;

/**
 * A campus pathway: an ordered list of waypoints rendered as a
 * thick stroked polyline. Saved/loaded via MapJson.PathJson.
 */
public class PathData {

    public String        name;
    public float         strokeWidth;
    public String        colorARGB;
    public List<float[]> points;

    public PathData(String name, float strokeWidth, String colorARGB) {
        this.name        = name;
        this.strokeWidth = strokeWidth;
        this.colorARGB   = colorARGB;
        this.points      = new ArrayList<>();
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

    public boolean isValid() { return points.size() >= 2; }
}