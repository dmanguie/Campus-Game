package com.campusgame.renderer.projection;

import com.campusgame.renderer.Camera;

// ProjectionMode.java  (new file — ~60 lines total)
public enum ProjectionMode {
    TOP_DOWN {
        @Override public int screenX(float wx, float wz, Camera c) {
            return c.worldToScreenX(wx);
        }
        @Override public int screenY(float wx, float wz, Camera c) {
            return c.worldToScreenY(wz);
        }
        @Override public int wallHeight(int floors) { return 0; } // flat
    },

    PSEUDO_3D {
        // Top-down + extruded walls drawn upward on screen
        // Wall height shrinks with distance (no perspective — isometric style)
        private static final float FLOOR_PX = 14f; // pixels per floor at base zoom

        @Override public int screenX(float wx, float wz, Camera c) {
            return c.worldToScreenX(wx);
        }
        @Override public int screenY(float wx, float wz, Camera c) {
            return c.worldToScreenY(wz);
        }
        @Override public int wallHeight(int floors) {
            return (int)(floors * FLOOR_PX);
        }
    },

    ISOMETRIC {
        // Classic 2:1 isometric tile projection
        // worldX, worldZ → screen using standard iso matrix
        @Override public int screenX(float wx, float wz, Camera c) {
            // iso: screenX = (wx - wz) * cos(30°)  ≈  (wx - wz) * 0.866
            float iso = (wx - wz) * 0.5f;
            return c.worldToScreenX(iso + c.getScreenWidth() / 2f);
        }
        @Override public int screenY(float wx, float wz, Camera c) {
            // iso: screenY = (wx + wz) * sin(30°)  ≈  (wx + wz) * 0.5
            float iso = (wx + wz) * 0.25f;
            return c.worldToScreenY(iso);
        }
        @Override public int wallHeight(int floors) {
            return floors * 10;
        }
    };

    public abstract int screenX(float wx, float wz, Camera c);
    public abstract int screenY(float wx, float wz, Camera c);
    public abstract int wallHeight(int floors);
}