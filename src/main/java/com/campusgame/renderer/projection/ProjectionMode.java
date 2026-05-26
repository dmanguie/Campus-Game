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

        @Override public int screenX(float wx, float wz, Camera cam) {
            return (int)((wx - cam.getOffsetXf()) * cam.getZoom());
        }
        @Override public int screenY(float wx, float wz, Camera cam) {
            return (int)((wz - cam.getOffsetYf()) * cam.getZoom());
        }
        @Override public int wallHeight(int floors) {
            return (int)(floors * FLOOR_PX);
        }
    },

    ISOMETRIC {
        @Override public int screenX(float wx, float wz, Camera c) {
            // Standard iso: screenX = (wx - wz) * 0.5, centered on screen
            float iso = (wx - wz) * 0.5f;
            // Treat iso as a world coordinate, apply offset + zoom manually
            return (int)((iso - c.getOffsetXf()) * c.getZoom()) + c.getScreenWidth() / 2;
        }
        @Override public int screenY(float wx, float wz, Camera c) {
            float iso = (wx + wz) * 0.25f;
            return (int)((iso - c.getOffsetYf()) * c.getZoom());
        }
        @Override public int wallHeight(int floors) {
            return floors * 10;
        }
    };

    public abstract int screenX(float wx, float wz, Camera c);
    public abstract int screenY(float wx, float wz, Camera c);
    public abstract int wallHeight(int floors);
}