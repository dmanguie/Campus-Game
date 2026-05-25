package com.campusgame.editor.layer;

/**
 * EDITOR LAYER (editor/layer/EditorLayer.java)
 * -----------------------------------------------
 * Defines all map layers. Each BuildingData tag maps to a layer.
 * Layers control render order and editor selectability.
 *
 * Layers (bottom to top):
 *   GROUND → PATHS → PE → BUILDINGS → WALLS → DECORATIONS → SPAWNS
 *
 * Future: per-layer visibility/lock toggles in the layer panel.
 */
public enum EditorLayer {
    GROUND      (0, "Ground",      false, 0xFF4CAF50),
    PATHS       (1, "Paths",       true,  0xFFDCD7C8),
    PE          (2, "P.E Areas",   true,  0xFF27AE60),
    BUILDINGS   (3, "Buildings",   true,  0xFF3498DB),
    WALLS       (4, "Walls",       true,  0xFF95A5A6),
    DECORATIONS (5, "Decorations", true,  0xFFF39C12),
    SPAWNS      (6, "Spawns",      true,  0xFFE74C3C);

    public final int     zOrder;
    public final String  displayName;
    public final boolean selectable;
    public final int     colorARGB;

    EditorLayer(int zOrder, String displayName, boolean selectable, int colorARGB) {
        this.zOrder      = zOrder;
        this.displayName = displayName;
        this.selectable  = selectable;
        this.colorARGB   = colorARGB;
    }

    public static EditorLayer fromTag(String tag) {
        if (tag == null) return BUILDINGS;
        return switch (tag.toLowerCase()) {
            case "wall"       -> WALLS;
            case "pe"         -> PE;
            case "decoration" -> DECORATIONS;
            case "spawn"      -> SPAWNS;
            default           -> BUILDINGS;
        };
    }
}
