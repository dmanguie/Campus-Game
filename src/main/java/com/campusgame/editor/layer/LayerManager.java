package com.campusgame.editor.layer;

import java.util.EnumMap;
import java.util.Map;

/**
 * LAYER MANAGER (editor/layer/LayerManager.java)
 * -------------------------------------------------
 * Tracks visibility and lock state for each EditorLayer.
 *
 * Consumed by:
 *   EditorOverlayRenderer — skips hidden layers
 *   EditorMode.hitTest()  — skips locked layers
 *   LayerPanelRenderer    — draws the layer panel UI
 *
 * Future: serialize layer states into campus.json under "editorSettings".
 */
public class LayerManager {

    private final Map<EditorLayer, Boolean> visible = new EnumMap<>(EditorLayer.class);
    private final Map<EditorLayer, Boolean> locked  = new EnumMap<>(EditorLayer.class);

    /** Active layer — newly placed buildings go onto this layer. */
    private EditorLayer activeLayer = EditorLayer.BUILDINGS;

    public LayerManager() {
        for (EditorLayer l : EditorLayer.values()) {
            visible.put(l, true);
            locked.put(l, false);
        }
    }

    // ── Visibility ────────────────────────────────────────────────────
    public boolean isVisible(EditorLayer l)      { return visible.getOrDefault(l, true); }
    public void    setVisible(EditorLayer l, boolean v) { visible.put(l, v); }
    public void    toggleVisible(EditorLayer l)  { visible.put(l, !isVisible(l)); }

    // ── Lock ──────────────────────────────────────────────────────────
    public boolean isLocked(EditorLayer l)       { return locked.getOrDefault(l, false); }
    public void    setLocked(EditorLayer l, boolean v)  { locked.put(l, v); }
    public void    toggleLocked(EditorLayer l)   { locked.put(l, !isLocked(l)); }

    // ── Active layer ──────────────────────────────────────────────────
    public EditorLayer getActiveLayer()          { return activeLayer; }
    public void        setActiveLayer(EditorLayer l) { activeLayer = l; }

    /** Returns true if a tag can be selected (layer visible and not locked). */
    public boolean isSelectable(String tag) {
        EditorLayer l = EditorLayer.fromTag(tag);
        return isVisible(l) && !isLocked(l) && l.selectable;
    }
}
