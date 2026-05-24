package com.campusgame.editor;

import com.campusgame.map.CampusMap;
import com.campusgame.map.data.BuildingData;
import com.campusgame.map.io.MapJson;
import com.campusgame.map.io.MapLoader;
import com.campusgame.map.io.MapSaver;
import com.campusgame.renderer.Camera;

import java.util.ArrayList;
import java.util.List;

/**
 * EDITOR MODE (editor/EditorMode.java)
 * Logic layer for the admin map editor.
 *
 * Key bindings (wired in InputHandler):
 *   F1       — toggle editor on/off
 *   P        — switch to PLACE tool
 *   X        — switch to DELETE tool
 *   Ctrl+S   — save campus.json
 *   Ctrl+Z   — undo
 *
 * Mouse (wired in EditorInputAdapter, forwarded by GameLoop):
 *   Move     — update ghost building preview
 *   L-click  — place / select / delete depending on tool
 */
public class EditorMode {

    private final CampusMap   campusMap;
    private final Camera      camera;
    private final EditorState state    = new EditorState();
    private final MapSaver    saver    = new MapSaver();

    // Snapping grid (world units)
    public static final float GRID_SNAP = 20f;

    // Preserved from load so save round-trips correctly
    private List<MapJson.PathJson> loadedPaths = new ArrayList<>();
    private MapJson.MapMeta        loadedMeta  = null;

    private int nameCounter = 1;

    public EditorMode(CampusMap campusMap, Camera camera) {
        this.campusMap = campusMap;
        this.camera    = camera;
        // Cache paths + meta from whatever CampusMap loaded
        MapLoader.LoadResult lr = campusMap.getLastLoadResult();
        if (lr != null) {
            loadedPaths = lr.paths != null ? lr.paths : new ArrayList<>();
            loadedMeta  = lr.meta;
        }
    }

    // ── Toggle & state ────────────────────────────────────────────────
    public void    toggleEditor() { state.toggle(); }
    public boolean isActive()     { return state.isActive(); }
    public EditorState getState() { return state; }

    // ── Tool switching (called by InputHandler) ───────────────────────
    public void switchToPlace()  { state.setCurrentTool(EditorState.Tool.PLACE);  }
    public void switchToDelete() { state.setCurrentTool(EditorState.Tool.DELETE); }
    public void switchToSelect() { state.setCurrentTool(EditorState.Tool.SELECT); }

    // ── Mouse move: update ghost building position ────────────────────
    public void onMouseMoved(int screenX, int screenY) {
        if (!state.isActive()) return;

        float wx = snap(camera.screenToWorldX(screenX));
        float wz = snap(camera.screenToWorldY(screenY));
        state.setCursorWorld(wx, wz);

        if (state.getCurrentTool() == EditorState.Tool.PLACE) {
            state.setGhostBuilding(new BuildingData(
                    "NEW BLDG " + nameCounter,
                    wx, wz,
                    EditorState.DEFAULT_WIDTH, EditorState.DEFAULT_DEPTH,
                    EditorState.DEFAULT_FLOORS, EditorState.DEFAULT_COLOR,
                    true, EditorState.DEFAULT_TAG
            ));
        } else {
            state.setGhostBuilding(null);
        }
    }

    // ── Mouse click: place / select / delete ─────────────────────────
    public void onLeftClick(int screenX, int screenY) {
        if (!state.isActive()) return;
        float wx = snap(camera.screenToWorldX(screenX));
        float wz = snap(camera.screenToWorldY(screenY));
        switch (state.getCurrentTool()) {
            case PLACE  -> place(wx, wz);
            case SELECT -> select(wx, wz);
            case DELETE -> delete(wx, wz);
        }
    }

    private void place(float wx, float wz) {
        state.pushUndo(campusMap.getMutableBuildings());
        String n = "NEW BLDG " + nameCounter++;
        BuildingData bd = new BuildingData(n, wx, wz,
                EditorState.DEFAULT_WIDTH, EditorState.DEFAULT_DEPTH,
                EditorState.DEFAULT_FLOORS, EditorState.DEFAULT_COLOR, true, EditorState.DEFAULT_TAG);
        campusMap.addBuilding(bd);
        state.showStatus("Placed: " + n);
        autoSave();
    }

    private void select(float wx, float wz) {
        BuildingData hit = hitTest(wx, wz);
        state.setSelectedBuilding(hit);
        state.showStatus(hit != null ? "Selected: " + hit.name : "Nothing selected");
    }

    private void delete(float wx, float wz) {
        BuildingData hit = hitTest(wx, wz);
        if (hit == null) { state.showStatus("Nothing to delete"); return; }
        state.pushUndo(campusMap.getMutableBuildings());
        campusMap.removeBuilding(hit);
        if (state.getSelectedBuilding() == hit) state.setSelectedBuilding(null);
        state.showStatus("Deleted: " + hit.name);
        autoSave();
    }

    // ── Save (Ctrl+S) ─────────────────────────────────────────────────
    public void saveMap() {
        MapJson.MapMeta meta = loadedMeta != null ? loadedMeta
                : new MapJson.MapMeta("Main Campus", 3000, 2400, "Admin", "Editor save");
        boolean ok = saver.save(campusMap.getMutableBuildings(), loadedPaths, meta);
        state.showStatus(ok ? "✓ Saved to campus.json" : "✗ Save FAILED!");
    }

    private void autoSave() {
        MapJson.MapMeta meta = loadedMeta != null ? loadedMeta
                : new MapJson.MapMeta("Main Campus", 3000, 2400, "Admin", "Auto-save");
        saver.save(campusMap.getMutableBuildings(), loadedPaths, meta);
    }

    // ── Undo (Ctrl+Z) ─────────────────────────────────────────────────
    public void undo() {
        List<BuildingData> snap = state.popUndo();
        if (snap == null) { state.showStatus("Nothing to undo"); return; }
        campusMap.replaceAllBuildings(snap);
        state.showStatus("Undone");
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private BuildingData hitTest(float wx, float wz) {
        // Iterate in reverse so top-most (last placed) is hit first
        List<BuildingData> all = campusMap.getMutableBuildings();
        for (int i = all.size() - 1; i >= 0; i--) {
            BuildingData b = all.get(i);
            if (!b.isPolygon()
                    && wx >= b.x && wx <= b.maxX()
                    && wz >= b.z && wz <= b.maxZ()) return b;
        }
        return null;
    }

    private float snap(float v) { return Math.round(v / GRID_SNAP) * GRID_SNAP; }
}