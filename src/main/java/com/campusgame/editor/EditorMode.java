package com.campusgame.editor;

import com.campusgame.editor.gizmo.ResizeHandle;
import com.campusgame.editor.layer.EditorLayer;
import com.campusgame.editor.layer.LayerManager;
import com.campusgame.map.CampusMap;
import com.campusgame.map.data.BuildingData;
import com.campusgame.map.data.EntranceData;
import com.campusgame.map.data.PathData;
import com.campusgame.map.io.MapJson;
import com.campusgame.map.io.MapLoader;
import com.campusgame.map.io.MapSaver;
import com.campusgame.renderer.Camera;
import com.campusgame.world.interior.InteriorSceneRegistry;

import java.util.List;

public class EditorMode implements Renameable {

    private final CampusMap             campusMap;
    private final Camera                camera;
    private final EditorState           state  = new EditorState();
    private final MapSaver              saver  = new MapSaver();
    private final InteriorSceneRegistry sceneRegistry;   // NEW

    private final ResizeHandle[] resizeHandles = ResizeHandle.all();
    private float resizeDragOriginX, resizeDragOriginZ;
    private float resizeDragOrigW,   resizeDragOrigD;
    private float resizeDragStartWX, resizeDragStartWZ;

    private MapJson.MapMeta        loadedMeta  = null;
    private List<MapJson.PathJson> loadedPaths = null;

    private int nameCounter     = 1;
    private int pathNameCounter = 1;
    private int entranceCounter = 1;

    // Pass the registry in so the editor knows which scene IDs exist
    public EditorMode(CampusMap campusMap, Camera camera,
                      InteriorSceneRegistry sceneRegistry) {
        this.campusMap    = campusMap;
        this.camera       = camera;
        this.sceneRegistry = sceneRegistry;
        MapLoader.LoadResult lr = campusMap.getLastLoadResult();
        if (lr != null) { loadedMeta = lr.meta; }
        for (BuildingData b : campusMap.getMutableBuildings())
            if (b.name.startsWith("NEW BLDG")) nameCounter++;
        for (EntranceData e : campusMap.getEntrances())
            if (e.id.startsWith("entrance_")) entranceCounter++;
    }

    public void        toggleEditor() { state.toggle(); }
    public boolean     isActive()     { return state.isActive(); }
    public EditorState getState()     { return state; }

    // ── Tool switching ────────────────────────────────────────────────
    public void switchToPlace()    { state.setCurrentTool(EditorState.Tool.PLACE);  }
    public void switchToDelete()   { state.setCurrentTool(EditorState.Tool.DELETE); }
    public void switchToSelect()   { state.setCurrentTool(EditorState.Tool.SELECT); }
    public void switchToEntrance() {
        state.setCurrentTool(EditorState.Tool.ENTRANCE);
        state.showStatus("ENTRANCE TOOL | Click=place | Click marker=select | C=assign scene | Del=delete");
    }

    public void switchToMove()   {
        state.setCurrentTool(EditorState.Tool.MOVE);
        state.showStatus("MOVE TOOL — select a building, then drag to move");
    }

    public void switchToResize() {
        state.setCurrentTool(EditorState.Tool.RESIZE);
        state.showStatus("RESIZE TOOL — select a building, then drag corner handles");
    }

    public void switchToRotate() {
        state.setCurrentTool(EditorState.Tool.ROTATE);
        state.showStatus("ROTATE TOOL — select a building, then use Q/E keys to rotate ±5°");
    }

    public void cycleGridSnap() {
        state.cycleGridSnap();
        state.showStatus("Grid snap: " + (int) state.getGridSnap() + " units");
    }

    public void nextFloor() { state.nextFloor(); }
    public void prevFloor() { state.prevFloor(); }

    // ── Delete selected ───────────────────────────────────────────────
    public void deleteSelected() {
        if (state.getCurrentTool() == EditorState.Tool.ENTRANCE) {
            deleteSelectedEntrance(); return;
        }
        if (state.hasMultiSelect()) {
            state.pushUndo(campusMap.getMutableBuildings());
            for (BuildingData b : state.getMultiSelect()) campusMap.removeBuilding(b);
            state.clearMultiSelect();
            state.setSelectedBuilding(null);
            state.showStatus("Deleted selected buildings");
            autoSave();
        } else if (state.getSelectedBuilding() != null) {
            delete(state.getCursorWorldX(), state.getCursorWorldZ());
        }
    }
// ── Rename selected object (N key, non-ENTRANCE tool) ─────────────

    /**
     * Renames whichever object is currently selected.
     * Routes to building rename or path rename based on active tool.
     * Called by InputHandler via the Renameable interface.
     */
    @Override
    public void renameSelected(java.awt.Component parent) {
        EditorState.Tool tool = state.getCurrentTool();

        if (tool == EditorState.Tool.PATH_EDIT) {
            renameSelectedPath(parent);
            return;
        }

        // Default: rename building (works in PLACE, SELECT, DELETE, SHAPE_EDIT)
        BuildingData sel = state.getSelectedBuilding();
        if (sel == null) {
            state.showStatus("Select a building first (S), then press N");
            return;
        }
        String input = javax.swing.JOptionPane.showInputDialog(
                parent, "Rename building:", sel.name);
        if (input == null || input.isBlank()) return;
        String trimmed = input.trim();
        state.pushUndo(campusMap.getMutableBuildings());
        BuildingData renamed = sel.withName(trimmed);   // BuildingData.withName() already exists
        campusMap.replaceBuilding(sel, renamed);
        state.setSelectedBuilding(renamed);
        autoSave();
        state.showStatus("Renamed to: " + trimmed);
    }

    /**
     * Renames the path currently open in the path editor.
     * PathData.name is mutable so no copy is needed.
     */
    private void renameSelectedPath(java.awt.Component parent) {
        PathEditState pe = state.getPathEdit();
        if (!pe.isActive()) {
            state.showStatus("Select a path first (R tool), then press N");
            return;
        }
        PathData src = pe.getSource();
        String input = javax.swing.JOptionPane.showInputDialog(
                parent, "Rename path:", src.name);
        if (input == null || input.isBlank()) return;
        src.name = input.trim();
        autoSave();
        state.showStatus("Path renamed to: " + src.name);
    }
    // ── Escape ────────────────────────────────────────────────────────
    public void escape() {
        if (state.getCurrentTool() == EditorState.Tool.SHAPE_EDIT) { cancelShapeEdit(); return; }
        if (state.getCurrentTool() == EditorState.Tool.PATH_EDIT)  { cancelPathEdit();  return; }
        if (state.getCurrentTool() == EditorState.Tool.ENTRANCE)   { escapeEntrance();  return; }
        state.setSelectedBuilding(null);
        state.clearMultiSelect();
        state.dragState.cancel();
        state.showStatus("Deselected");
    }

    public void escapeEntrance() {
        EditorState.EntranceEditState ee = state.getEntranceEdit();
        if (ee.pickerOpen) { ee.closePicker(); state.showStatus("Picker closed"); return; }
        ee.clear();
        state.showStatus("Entrance deselected");
    }

    // ── Open scene picker for selected entrance ───────────────────────
    public void openScenePicker() {
        EditorState.EntranceEditState ee = state.getEntranceEdit();
        if (ee.selected == null) {
            state.showStatus("Click an entrance marker first, then press C");
            return;
        }
        ee.openPicker(sceneRegistry.getSceneIds());
        state.showStatus("Pick a scene — click to assign, Esc=cancel");
    }

    /** Called by EditorOverlayRenderer when user clicks a picker row. */
    public void assignSceneToSelected(String sceneId) {
        EditorState.EntranceEditState ee = state.getEntranceEdit();
        if (ee.selected == null) return;
        ee.selected.interiorSceneId = sceneId;
        ee.closePicker();
        autoSave();
        state.showStatus("Assigned scene \"" + sceneId + "\" to " + ee.selected.id);
    }

    // ─────────────────────────────────────────────────────────────────
    // MOUSE MOVE
    // ─────────────────────────────────────────────────────────────────
    public void onMouseMoved(int screenX, int screenY) {
        if (!state.isActive()) return;
        float wx = snap(camera.screenToWorldX(screenX));
        float wz = snap(camera.screenToWorldY(screenY));
        state.setCursorWorld(wx, wz);

        // If picker is open, update hover index
        EditorState.EntranceEditState ee = state.getEntranceEdit();
        if (ee.pickerOpen) {
            ee.pickerHover = pickerIndexAt(screenX, screenY, ee);
            return;
        }

        switch (state.getCurrentTool()) {
            case PLACE -> state.setGhostBuilding(makePlaceholder(wx, wz));
            case SELECT, MOVE, RESIZE, ROTATE -> {
                state.setGhostBuilding(null);
                BuildingData sel = state.getSelectedBuilding();
                if (sel != null && !sel.isPolygon()) {
                    updateResizeHandles(sel);
                    state.setHoveredHandleIdx(findHoveredHandle(screenX, screenY));
                } else state.setHoveredHandleIdx(-1);
            }
            case ENTRANCE -> {
                if (!ee.dragging) ee.ghost = makeGhostEntrance(wx, wz);
            }
            case SHAPE_EDIT -> {
                ShapeEditState se = state.getShapeEdit();
                if (!se.isActive() || se.isDragging()) return;
                int[][] sv = projectToScreen(se.getVerticesX(), se.getVerticesZ());
                se.setHoveredVertex(se.findNearestVertex(screenX, screenY, sv[0], sv[1]));
            }
            case PATH_EDIT -> {
                PathEditState pe = state.getPathEdit();
                if (!pe.isActive() || pe.isDragging()) return;
                int[][] sv = projectPathToScreen(pe);
                pe.setHoveredPoint(pe.findNearestPoint(screenX, screenY, sv[0], sv[1]));
            }
            default -> state.setGhostBuilding(null);
        }
    }
    // ─────────────────────────────────────────────────────────────────
    // MOUSE DRAG
    // ─────────────────────────────────────────────────────────────────
    public void onMouseDragged(int screenX, int screenY) {
        if (!state.isActive()) return;
        float wx = snap(camera.screenToWorldX(screenX));
        float wz = snap(camera.screenToWorldY(screenY));
        state.setCursorWorld(wx, wz);

        switch (state.getCurrentTool()) {
            case SELECT -> {
                if (state.getActiveHandleIdx() >= 0) { applyResizeDrag(wx, wz); return; }
                if (state.dragState.active)           { state.dragState.update(wx, wz); return; }
                if (state.boxSelecting) {
                    state.boxSelectCurSX = screenX; state.boxSelectCurSY = screenY;
                }
            }
            case ENTRANCE -> {
                EditorState.EntranceEditState ee = state.getEntranceEdit();
                if (ee.dragging && ee.selected != null) {
                    ee.selected.worldX = wx; ee.selected.worldZ = wz;
                }
            }
            case SHAPE_EDIT -> {
                ShapeEditState se = state.getShapeEdit();
                if (!se.isActive() || !se.isDragging()) return;
                int idx = se.getSelectedVertex();
                if (idx >= 0) se.moveVertex(idx,
                        (int) snap(camera.screenToWorldX(screenX)),
                        (int) snap(camera.screenToWorldY(screenY)));
            }
            case PATH_EDIT -> {
                PathEditState pe = state.getPathEdit();
                if (!pe.isActive() || !pe.isDragging()) return;
                int idx = pe.getSelectedPoint();
                if (idx >= 0) pe.movePoint(idx,
                        snap(camera.screenToWorldX(screenX)),
                        snap(camera.screenToWorldY(screenY)));
            }
            default -> {}
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // MOUSE PRESS (LEFT)
    // ─────────────────────────────────────────────────────────────────
    public void onLeftClick(int screenX, int screenY) {
        if (!state.isActive()) return;

        // If picker is open, a click either picks a scene or closes
        EditorState.EntranceEditState ee = state.getEntranceEdit();
        if (ee.pickerOpen) {
            int idx = pickerIndexAt(screenX, screenY, ee);
            if (idx >= 0) assignSceneToSelected(ee.sceneIds.get(idx));
            else          ee.closePicker();
            return;
        }

        float wx = snap(camera.screenToWorldX(screenX));
        float wz = snap(camera.screenToWorldY(screenY));

        switch (state.getCurrentTool()) {
            case PLACE                        -> place(wx, wz);
            case DELETE                       -> delete(wx, wz);
            case SELECT, MOVE, RESIZE, ROTATE -> selectPress(wx, wz, screenX, screenY, false);
            case SHAPE_EDIT                   -> shapeEditPress(screenX, screenY);
            case PATH_EDIT                    -> pathEditClick(wx, wz, screenX, screenY);
            case ENTRANCE                     -> entranceClick(wx, wz, screenX, screenY);
        }
    }

    public void onShiftLeftClick(int screenX, int screenY) {
        if (!state.isActive() || state.getCurrentTool() != EditorState.Tool.SELECT) return;
        float wx = snap(camera.screenToWorldX(screenX));
        float wz = snap(camera.screenToWorldY(screenY));
        selectPress(wx, wz, screenX, screenY, true);
    }

    // ─────────────────────────────────────────────────────────────────
    // MOUSE RELEASE (LEFT)
    // ─────────────────────────────────────────────────────────────────
    public void onLeftRelease(int screenX, int screenY) {
        if (!state.isActive()) return;
        switch (state.getCurrentTool()) {
            case SELECT, MOVE, RESIZE, ROTATE -> {
                if (state.getActiveHandleIdx() >= 0) { commitResizeDrag(); return; }
                if (state.dragState.active)           { commitDragMove();   return; }
                if (state.boxSelecting) {
                    commitBoxSelect(screenX, screenY);
                    state.boxSelecting = false;
                }
            }
            case ENTRANCE -> {
                EditorState.EntranceEditState ee = state.getEntranceEdit();
                if (ee.dragging) {
                    ee.dragging = false;
                    autoSave();
                    state.showStatus("Entrance moved — C=assign scene | Del=delete");
                }
            }
            case SHAPE_EDIT -> {
                ShapeEditState se = state.getShapeEdit();
                if (se.isDragging()) {
                    se.setDragging(false); se.setSelectedVertex(-1);
                    state.showStatus("Vertex moved — Enter=commit  Esc=cancel");
                }
            }
            case PATH_EDIT -> {
                PathEditState pe = state.getPathEdit();
                if (pe.isDragging()) {
                    pe.setDragging(false); pe.setSelectedPoint(-1);
                    state.showStatus("Point moved — Enter=finish  Esc=cancel");
                }
            }
            default -> {}
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ENTRANCE TOOL LOGIC
    // ─────────────────────────────────────────────────────────────────

    private void entranceClick(float wx, float wz, int screenX, int screenY) {
        EditorState.EntranceEditState ee = state.getEntranceEdit();

        EntranceData hit = hitTestEntrance(screenX, screenY);
        if (hit != null) {
            ee.selected = hit;
            ee.dragging = true;
            String sceneInfo = (hit.interiorSceneId != null && !hit.interiorSceneId.isEmpty())
                    ? hit.interiorSceneId : "(no scene — press C to assign)";
            state.showStatus("Selected: " + hit.id + " → " + sceneInfo
                    + " | C=scene | Del=delete | Esc=deselect");
            return;
        }

        // Place new entrance
        String newId = "entrance_" + entranceCounter++;
        String label = nearestBuildingName(wx, wz);
        EntranceData newEntrance = new EntranceData(
                newId, label, label,
                wx, wz,
                "",             // interiorSceneId — assign with C
                wx, wz + 50f,
                wx, wz + 70f);
        newEntrance.triggerRadius = 55f;

        campusMap.addEntrance(newEntrance);
        ee.selected = newEntrance;
        ee.dragging = false;
        autoSave();
        state.showStatus("Placed " + newId + " — press C to assign a scene");
    }

    public void deleteSelectedEntrance() {
        EditorState.EntranceEditState ee = state.getEntranceEdit();
        if (ee.selected == null) { state.showStatus("No entrance selected — click one first"); return; }
        campusMap.removeEntrance(ee.selected);
        state.showStatus("Deleted entrance: " + ee.selected.id);
        ee.clear();
        autoSave();
    }

    /** Returns the picker row index under the given screen pixel, or -1. */
    private int pickerIndexAt(int screenX, int screenY,
                              EditorState.EntranceEditState ee) {
        if (!ee.pickerOpen || ee.sceneIds.isEmpty()) return -1;
        // Picker drawn at fixed position — must match EditorOverlayRenderer.drawScenePicker()
        int px = 20, py = 60;
        int rowH = 22, boxW = 260;
        for (int i = 0; i < ee.sceneIds.size(); i++) {
            int ry = py + i * rowH;
            if (screenX >= px && screenX <= px + boxW
                    && screenY >= ry && screenY <= ry + rowH) return i;
        }
        return -1;
    }

    private EntranceData hitTestEntrance(int screenX, int screenY) {
        for (EntranceData e : campusMap.getEntrances()) {
            int ex = camera.worldToScreenX(e.worldX);
            int ez = camera.worldToScreenY(e.worldZ);
            int dx = ex - screenX, dz = ez - screenY;
            if (dx * dx + dz * dz <= 20 * 20) return e;
        }
        return null;
    }

    private EntranceData makeGhostEntrance(float wx, float wz) {
        EntranceData ghost = new EntranceData();
        ghost.worldX = wx; ghost.worldZ = wz;
        ghost.label  = nearestBuildingName(wx, wz);
        ghost.triggerRadius = 55f;
        return ghost;
    }

    private String nearestBuildingName(float wx, float wz) {
        String best = "Building"; float bestDist2 = Float.MAX_VALUE;
        for (BuildingData b : campusMap.getMutableBuildings()) {
            float cx = b.isPolygon() ? b.x : b.x + b.width / 2f;
            float cz = b.isPolygon() ? b.z : b.z + b.depth / 2f;
            float d2 = (wx - cx) * (wx - cx) + (wz - cz) * (wz - cz);
            if (d2 < bestDist2) { bestDist2 = d2; best = b.name; }
        }
        return best;
    }

    // ── All remaining methods unchanged from your current file ────────
    // (selectPress, commitDragMove, movedBuilding, updateResizeHandles,
    //  findHoveredHandle, applyResizeDrag, commitResizeDrag, commitBoxSelect,
    //  place, delete, commitPropertyPanel, switchToShapeEdit, applyShapePreset,
    //  commitShapeEdit, cancelShapeEdit, switchToPathEdit, pathEditClick,
    //  commitPathEdit, cancelPathEdit, onRightClick, onMiddleClick,
    //  resizeSelected, rotateSelected, saveMap, autoSave, undo,
    //  deleteSelectedPath, getResizeHandles, makePlaceholder, hitTest,
    //  hitTestPath, distToSeg, buildFromShape, isAxisAlignedRect,
    //  projectToScreen, projectPathToScreen, snap, min, max)
    // ── Copy them in exactly as they are in your current EditorMode.java ──

    private void selectPress(float wx, float wz, int sx, int sy, boolean addToMulti) {
        BuildingData current = state.getSelectedBuilding();
        if (current != null && !current.isPolygon()) {
            updateResizeHandles(current);
            int hi = findHoveredHandle(sx, sy);
            if (hi >= 0) {
                state.setActiveHandleIdx(hi);
                resizeDragStartWX = wx; resizeDragStartWZ = wz;
                resizeDragOriginX = current.x; resizeDragOriginZ = current.z;
                resizeDragOrigW   = current.width; resizeDragOrigD = current.depth;
                return;
            }
        }

        BuildingData hit = hitTest(wx, wz);
        if (hit != null) {
            if (addToMulti) {
                state.addToMultiSelect(hit);
                state.showStatus("Multi-select: " + state.getMultiSelect().size());
                return;
            }
            if (!state.isMultiSelected(hit)) state.clearMultiSelect();
            state.setSelectedBuilding(hit);

            // Only begin drag-move when SELECT or MOVE tool is active
            EditorState.Tool tool = state.getCurrentTool();
            if (tool == EditorState.Tool.SELECT || tool == EditorState.Tool.MOVE) {
                state.dragState.begin(wx, wz, hit.x, hit.z);
                state.showStatus("Selected: " + hit.name + "  (drag to move)");
            } else if (tool == EditorState.Tool.RESIZE) {
                state.showStatus("Selected: " + hit.name + "  — drag a corner handle to resize");
            } else if (tool == EditorState.Tool.ROTATE) {
                state.showStatus("Selected: " + hit.name + "  — Q = rotate left  E = rotate right");
            }
        } else {
            state.setSelectedBuilding(null); state.clearMultiSelect();
            state.boxSelecting = true;
            state.boxSelectStartSX = sx; state.boxSelectStartSY = sy;
            state.boxSelectCurSX   = sx; state.boxSelectCurSY   = sy;
        }
    }

    public void onMouseWheel(int screenX, int screenY, int wheelRotation) {
        camera.zoomAt(screenX, screenY, wheelRotation);
    }

    private void commitDragMove() {
        BuildingData sel = state.getSelectedBuilding();
        if (sel == null) { state.dragState.cancel(); return; }
        float nx = snap(state.dragState.currentX), nz = snap(state.dragState.currentZ);
        if (Math.abs(nx - sel.x) < 1f && Math.abs(nz - sel.z) < 1f) { state.dragState.cancel(); return; }
        state.pushUndo(campusMap.getMutableBuildings());
        BuildingData moved = movedBuilding(sel, nx, nz);
        campusMap.replaceBuilding(sel, moved);
        state.setSelectedBuilding(moved);
        state.dragState.cancel();
        autoSave(); state.showStatus("Moved: " + moved.name);
    }

    private BuildingData movedBuilding(BuildingData b, float nx, float nz) {
        if (b.isPolygon()) {
            int dx = (int)(nx - b.x), dz = (int)(nz - b.z);
            int[] px = b.polygonX.clone(), pz = b.polygonZ.clone();
            for (int i = 0; i < px.length; i++) { px[i] += dx; pz[i] += dz; }
            return new BuildingData(b.name, px, pz, b.floors, b.colorARGB, b.rotationDegrees, b.collisionEnabled, b.tag);
        }
        return new BuildingData(b.name, nx, nz, b.width, b.depth, b.floors, b.colorARGB, b.rotationDegrees, b.collisionEnabled, b.tag);
    }

    private void updateResizeHandles(BuildingData b) {
        for (ResizeHandle h : resizeHandles) h.updatePosition(b.x, b.z, b.width, b.depth);
    }

    private int findHoveredHandle(int screenX, int screenY) {
        for (int i = 0; i < resizeHandles.length; i++) {
            ResizeHandle h = resizeHandles[i];
            int hsx = camera.worldToScreenX(h.worldX), hsy = camera.worldToScreenY(h.worldZ);
            if (h.hitTest(screenX, screenY, hsx, hsy)) return i;
        }
        return -1;
    }

    private void applyResizeDrag(float wx, float wz) {
        BuildingData sel = state.getSelectedBuilding(); if (sel == null) return;
        ResizeHandle h = resizeHandles[state.getActiveHandleIdx()];
        float dWX = wx - resizeDragStartWX, dWZ = wz - resizeDragStartWZ;
        float[] bounds = h.applyDrag(resizeDragOriginX, resizeDragOriginZ, resizeDragOrigW, resizeDragOrigD, dWX, dWZ);
        BuildingData preview = new BuildingData(sel.name, bounds[0], bounds[1], bounds[2], bounds[3],
                sel.floors, sel.colorARGB, sel.rotationDegrees, sel.collisionEnabled, sel.tag);
        campusMap.replaceBuilding(sel, preview);
        state.setSelectedBuilding(preview);
        updateResizeHandles(preview);
    }

    private void commitResizeDrag() {
        state.setActiveHandleIdx(-1);
        BuildingData sel = state.getSelectedBuilding();
        if (sel != null) { autoSave(); state.showStatus("Resized: " + sel.name); }
    }

    private void commitBoxSelect(int curSX, int curSY) {
        int x1 = Math.min(state.boxSelectStartSX, curSX), y1 = Math.min(state.boxSelectStartSY, curSY);
        int x2 = Math.max(state.boxSelectStartSX, curSX), y2 = Math.max(state.boxSelectStartSY, curSY);
        if (x2 - x1 < 5 && y2 - y1 < 5) return;
        state.clearMultiSelect();
        for (BuildingData b : campusMap.getMutableBuildings()) {
            if (!state.getLayerManager().isSelectable(b.tag)) continue;
            int bsx = camera.worldToScreenX(b.x), bsy = camera.worldToScreenY(b.z);
            if (bsx >= x1 && bsx <= x2 && bsy >= y1 && bsy <= y2) state.addToMultiSelect(b);
        }
        if (state.hasMultiSelect()) state.showStatus("Box selected: " + state.getMultiSelect().size());
    }

    private void place(float wx, float wz) {
        state.pushUndo(campusMap.getMutableBuildings());
        String n   = "NEW BLDG " + nameCounter++;
        String tag = state.getLayerManager().getActiveLayer() == EditorLayer.WALLS ? "wall"
                : state.getLayerManager().getActiveLayer() == EditorLayer.PE    ? "pe" : "building";
        campusMap.addBuilding(new BuildingData(n, wx, wz,
                EditorState.DEFAULT_WIDTH, EditorState.DEFAULT_DEPTH,
                EditorState.DEFAULT_FLOORS, EditorState.DEFAULT_COLOR, true, tag));
        state.showStatus("Placed: " + n); autoSave();
    }

    private void delete(float wx, float wz) {
        BuildingData hit = hitTest(wx, wz);
        if (hit == null) { state.showStatus("Nothing to delete here"); return; }
        state.pushUndo(campusMap.getMutableBuildings());
        campusMap.removeBuilding(hit);
        if (state.getSelectedBuilding() == hit) state.setSelectedBuilding(null);
        state.showStatus("Deleted: " + hit.name); autoSave();
    }

    public void commitPropertyPanel() {
        BuildingData updated  = state.getPropertyPanel().buildUpdated(); if (updated == null) return;
        BuildingData original = state.getPropertyPanel().getSource();
        state.pushUndo(campusMap.getMutableBuildings());
        campusMap.replaceBuilding(original, updated);
        state.setSelectedBuilding(updated); autoSave();
        state.showStatus("Properties updated: " + updated.name);
    }

    private void shapeEditPress(int screenX, int screenY) {
        ShapeEditState se = state.getShapeEdit(); if (!se.isActive()) return;
        int[][] sv = projectToScreen(se.getVerticesX(), se.getVerticesZ());
        int vi = se.findNearestVertex(screenX, screenY, sv[0], sv[1]);
        if (vi >= 0) { se.setSelectedVertex(vi); se.setDragging(true); return; }
        int ei = se.findNearestEdgeMidpoint(screenX, screenY, sv[0], sv[1]);
        if (ei >= 0) { se.insertVertexAfterEdge(ei); state.showStatus("Vertex inserted (" + se.getVerticesX().length + " verts)"); }
    }

    public void switchToShapeEdit() {
        BuildingData sel = state.getSelectedBuilding();
        if (sel == null) { state.showStatus("Select a building first (S), then press V"); return; }
        state.setCurrentTool(EditorState.Tool.SHAPE_EDIT);
        state.getShapeEdit().beginEdit(sel);
        state.showStatus("SHAPE EDIT | drag verts | 1-5=presets | Enter=commit | Esc=cancel");
    }

    public void applyShapePreset(ShapeEditState.ShapePreset preset) {
        ShapeEditState se = state.getShapeEdit(); if (!se.isActive()) return;
        se.applyPreset(preset, se.getSource());
        state.showStatus("Preset: " + preset + "  Enter=commit  Esc=cancel");
    }

    public void commitShapeEdit() {
        ShapeEditState se = state.getShapeEdit();
        if (!se.isActive()) { state.showStatus("Nothing to commit"); return; }
        BuildingData original = se.getSource();
        state.pushUndo(campusMap.getMutableBuildings());
        BuildingData updated = buildFromShape(se, original);
        campusMap.replaceBuilding(original, updated);
        state.setSelectedBuilding(updated); se.endEdit();
        state.setCurrentTool(EditorState.Tool.SELECT);
        autoSave(); state.showStatus("Shape committed: " + updated.name);
    }

    public void cancelShapeEdit() {
        state.getShapeEdit().endEdit();
        state.setCurrentTool(EditorState.Tool.SELECT);
        state.showStatus("Shape edit cancelled");
    }

    public void switchToPathEdit() {
        state.setCurrentTool(EditorState.Tool.PATH_EDIT);
        state.getPathEdit().endEdit();
        state.showStatus("PATH EDIT | click=new/add | Mid=insert | R=delete | Enter=finish");
    }

    private void pathEditClick(float wx, float wz, int screenX, int screenY) {
        PathEditState pe = state.getPathEdit();
        if (pe.isActive()) {
            if (pe.isAppendMode()) {
                int[][] sv = projectPathToScreen(pe);
                int vi = pe.findNearestPoint(screenX, screenY, sv[0], sv[1]);
                if (vi >= 0) { pe.setSelectedPoint(vi); pe.setDragging(true); return; }
                int ei = pe.findNearestEdgeMidpoint(screenX, screenY, sv[0], sv[1]);
                if (ei >= 0) { pe.insertPointAfter(ei); state.showStatus("Point inserted (" + pe.getWx().length + ")"); return; }
                pe.appendPoint(wx, wz);
                state.showStatus("Waypoint added (" + pe.getWx().length + ") — Enter=finish");
                return;
            }
            int[][] sv = projectPathToScreen(pe);
            int vi = pe.findNearestPoint(screenX, screenY, sv[0], sv[1]);
            if (vi >= 0) { pe.setSelectedPoint(vi); pe.setDragging(true); return; }
            PathData hit = hitTestPath(wx, wz);
            if (hit != null && hit != pe.getSource()) { pe.endEdit(); pe.beginEdit(hit); state.showStatus("Selected: " + hit.name); }
            else if (hit == null) { pe.endEdit(); state.showStatus("Path deselected"); }
            return;
        }
        PathData hit = hitTestPath(wx, wz);
        if (hit != null) {
            pe.beginEdit(hit);
            state.showStatus("Selected: " + hit.name + " — drag points | Del=delete | A=add");
        } else {
            PathData np = new PathData("PATH " + pathNameCounter++, 55f, "#FFDCD7C8");
            np.addPoint(wx, wz); campusMap.addPath(np); pe.beginEdit(np);
            pe.setAppendMode(true);
            state.showStatus("New path — click to add waypoints, Enter to finish");
        }
    }

    public void commitPathEdit() {
        PathEditState pe = state.getPathEdit();
        if (!pe.isActive()) { state.showStatus("Nothing to commit"); return; }
        pe.applyTo(pe.getSource()); pe.setAppendMode(false); pe.endEdit();
        autoSave(); state.showStatus("Path saved");
    }

    public void cancelPathEdit() {
        PathEditState pe = state.getPathEdit(); if (!pe.isActive()) return;
        if (pe.getSource().points.size() < 2) campusMap.removePath(pe.getSource());
        pe.endEdit(); state.showStatus("Path edit cancelled");
    }

    public void onRightClick(int screenX, int screenY) {
        if (!state.isActive()) return;
        switch (state.getCurrentTool()) {
            case SHAPE_EDIT -> {
                ShapeEditState se = state.getShapeEdit(); if (!se.isActive()) return;
                int[][] sv = projectToScreen(se.getVerticesX(), se.getVerticesZ());
                int i = se.findNearestVertex(screenX, screenY, sv[0], sv[1]);
                if (i >= 0) { se.deleteVertex(i); state.showStatus("Vertex deleted (" + se.getVerticesX().length + " remain)"); }
            }
            case PATH_EDIT -> {
                PathEditState pe = state.getPathEdit(); if (!pe.isActive()) return;
                int[][] sv = projectPathToScreen(pe);
                int i = pe.findNearestPoint(screenX, screenY, sv[0], sv[1]);
                if (i >= 0) { pe.deletePoint(i); state.showStatus("Point deleted"); }
            }
            default -> {}
        }
    }

    public void onMiddleClick(int screenX, int screenY) {
        if (!state.isActive()) return;
        switch (state.getCurrentTool()) {
            case SHAPE_EDIT -> {
                ShapeEditState se = state.getShapeEdit(); if (!se.isActive()) return;
                int[][] sv = projectToScreen(se.getVerticesX(), se.getVerticesZ());
                int e = se.findNearestEdgeMidpoint(screenX, screenY, sv[0], sv[1]);
                if (e >= 0) { se.insertVertexAfterEdge(e); state.showStatus("Vertex inserted"); }
            }
            case PATH_EDIT -> {
                PathEditState pe = state.getPathEdit(); if (!pe.isActive()) return;
                int[][] sv = projectPathToScreen(pe);
                int e = pe.findNearestEdgeMidpoint(screenX, screenY, sv[0], sv[1]);
                if (e >= 0) { pe.insertPointAfter(e); state.showStatus("Point inserted"); }
            }
            default -> {}
        }
    }

    public void resizeSelected(float dw, float dd) {
        BuildingData sel = state.getSelectedBuilding();
        if (sel == null) { state.showStatus("No building selected"); return; }
        if (sel.isPolygon()) { state.showStatus("Use SHAPE EDIT (V) for polygon"); return; }
        state.pushUndo(campusMap.getMutableBuildings());
        BuildingData u = new BuildingData(sel.name, sel.x, sel.z,
                Math.max(20, sel.width + dw), Math.max(20, sel.depth + dd),
                sel.floors, sel.colorARGB, sel.rotationDegrees, sel.collisionEnabled, sel.tag);
        campusMap.replaceBuilding(sel, u); state.setSelectedBuilding(u); autoSave();
    }

    public void rotateSelected(float deg) {
        BuildingData sel = state.getSelectedBuilding();
        if (sel == null || sel.isPolygon()) return;
        state.pushUndo(campusMap.getMutableBuildings());
        BuildingData u = new BuildingData(sel.name, sel.x, sel.z, sel.width, sel.depth,
                sel.floors, sel.colorARGB, sel.rotationDegrees + deg, sel.collisionEnabled, sel.tag);
        campusMap.replaceBuilding(sel, u); state.setSelectedBuilding(u); autoSave();
    }

    public void saveMap() {
        MapJson.MapMeta meta = loadedMeta != null ? loadedMeta
                : new MapJson.MapMeta("Main Campus", 3000, 2400, "Admin", "Editor save");
        boolean ok = saver.save(campusMap.getMutableBuildings(),
                campusMap.getPaths(), campusMap.getEntrances(), meta);
        state.showStatus(ok ? "✓ Saved to campus.json" : "✗ Save FAILED!");
    }

    private void autoSave() {
        MapJson.MapMeta meta = loadedMeta != null ? loadedMeta
                : new MapJson.MapMeta("Main Campus", 3000, 2400, "Admin", "Auto-save");
        saver.save(campusMap.getMutableBuildings(),
                campusMap.getPaths(), campusMap.getEntrances(), meta);
    }

    public void undo() {
        List<BuildingData> snap = state.popUndo();
        if (snap == null) { state.showStatus("Nothing to undo"); return; }
        campusMap.replaceAllBuildings(snap);
        state.setSelectedBuilding(null); state.clearMultiSelect();
        state.showStatus("Undone");
    }

    public void deleteSelectedPath() {
        PathEditState pe = state.getPathEdit();
        if (!pe.isActive()) { state.showStatus("Select a path first (R tool)"); return; }
        PathData toDelete = pe.getSource();
        pe.endEdit(); campusMap.removePath(toDelete);
        autoSave(); state.showStatus("Path deleted: " + toDelete.name);
    }

    public ResizeHandle[] getResizeHandles() { return resizeHandles; }

    private BuildingData makePlaceholder(float wx, float wz) {
        return new BuildingData("NEW BLDG " + nameCounter, wx, wz,
                EditorState.DEFAULT_WIDTH, EditorState.DEFAULT_DEPTH,
                EditorState.DEFAULT_FLOORS, EditorState.DEFAULT_COLOR,
                true, EditorState.DEFAULT_TAG);
    }

    private BuildingData hitTest(float wx, float wz) {
        List<BuildingData> all = campusMap.getMutableBuildings();
        LayerManager lm = state.getLayerManager();
        for (int i = all.size() - 1; i >= 0; i--) {
            BuildingData b = all.get(i);
            if (!lm.isSelectable(b.tag)) continue;
            if (!b.isPolygon() && wx >= b.x && wx <= b.maxX() && wz >= b.z && wz <= b.maxZ()) return b;
            if (b.isPolygon()) {
                int minX = min(b.polygonX), maxX = max(b.polygonX),
                        minZ = min(b.polygonZ), maxZ = max(b.polygonZ);
                if (wx >= minX && wx <= maxX && wz >= minZ && wz <= maxZ) return b;
            }
        }
        return null;
    }

    private PathData hitTestPath(float wx, float wz) {
        for (PathData p : campusMap.getPaths())
            for (int i = 0; i < p.points.size() - 1; i++)
                if (distToSeg(wx, wz, p.points.get(i)[0], p.points.get(i)[1],
                        p.points.get(i+1)[0], p.points.get(i+1)[1]) < 30f) return p;
        return null;
    }

    private float distToSeg(float px, float pz, float ax, float az, float bx, float bz) {
        float dx = bx-ax, dz = bz-az, l = dx*dx+dz*dz;
        if (l == 0) return (float) Math.hypot(px-ax, pz-az);
        float t = Math.max(0, Math.min(1, ((px-ax)*dx+(pz-az)*dz)/l));
        return (float) Math.hypot(px-(ax+t*dx), pz-(az+t*dz));
    }

    private BuildingData buildFromShape(ShapeEditState se, BuildingData src) {
        int[] vx = se.getVerticesX(), vz = se.getVerticesZ();
        if (!se.isPolygonMode() || isAxisAlignedRect(vx, vz)) {
            int minX = min(vx), maxX = max(vx), minZ = min(vz), maxZ = max(vz);
            return new BuildingData(src.name, minX, minZ, maxX-minX, maxZ-minZ,
                    src.floors, src.colorARGB, src.rotationDegrees, src.collisionEnabled, src.tag);
        }
        return new BuildingData(src.name, vx.clone(), vz.clone(),
                src.floors, src.colorARGB, src.rotationDegrees, src.collisionEnabled, src.tag);
    }

    private boolean isAxisAlignedRect(int[] vx, int[] vz) {
        if (vx.length != 4) return false;
        return java.util.Arrays.stream(vx).distinct().count() == 2
                && java.util.Arrays.stream(vz).distinct().count() == 2;
    }

    public int[][] projectToScreen(int[] vx, int[] vz) {
        int n = vx.length; int[] sx = new int[n], sy = new int[n];
        for (int i = 0; i < n; i++) { sx[i] = camera.worldToScreenX(vx[i]); sy[i] = camera.worldToScreenY(vz[i]); }
        return new int[][]{sx, sy};
    }

    public int[][] projectPathToScreen(PathEditState pe) {
        float[] wx = pe.getWx(), wz = pe.getWz(); int n = wx.length;
        int[] sx = new int[n], sy = new int[n];
        for (int i = 0; i < n; i++) { sx[i] = camera.worldToScreenX(wx[i]); sy[i] = camera.worldToScreenY(wz[i]); }
        return new int[][]{sx, sy};
    }

    private float snap(float v)  { float g = state.getGridSnap(); return Math.round(v/g)*g; }
    private int   min(int[] arr) { int m = arr[0]; for (int v : arr) if (v < m) m = v; return m; }
    private int   max(int[] arr) { int m = arr[0]; for (int v : arr) if (v > m) m = v; return m; }
}