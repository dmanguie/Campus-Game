package com.campusgame.editor;

import com.campusgame.editor.gizmo.ResizeHandle;
import com.campusgame.editor.layer.EditorLayer;
import com.campusgame.editor.layer.LayerManager;
import com.campusgame.map.CampusMap;
import com.campusgame.map.data.BuildingData;
import com.campusgame.map.data.PathData;
import com.campusgame.map.io.MapJson;
import com.campusgame.map.io.MapLoader;
import com.campusgame.map.io.MapSaver;
import com.campusgame.renderer.Camera;

import java.util.List;

/**
 * EDITOR MODE (editor/EditorMode.java)
 * Phase 4: drag-to-move, resize handles, multi-select, layer system,
 * property panel commit, box-select rubber band.
 *
 * Key bindings:
 *   F1        toggle editor
 *   P/S/X/V/R tool switch
 *   G         cycle grid snap size
 *   PageUp/Dn floor up/down
 *   Ctrl+S    save
 *   Ctrl+Z    undo
 *   Delete    delete selected
 *   Esc       deselect / cancel
 *   Tab       cycle property panel fields (when panel visible)
 *   Enter     commit property panel / shape / path
 *   1-5       shape presets (in SHAPE_EDIT)
 *   [ ] - =   resize (in SELECT with rect building selected)
 *   Q / E     rotate
 */
public class EditorMode {

    private final CampusMap   campusMap;
    private final Camera      camera;
    private final EditorState state  = new EditorState();
    private final MapSaver    saver  = new MapSaver();

    // Resize handles — 8 for a rectangle building
    private final ResizeHandle[] resizeHandles = ResizeHandle.all();
    // Which handle is being dragged, and the drag origin
    private float resizeDragOriginX, resizeDragOriginZ;
    private float resizeDragOrigW,   resizeDragOrigD;
    private float resizeDragStartWX, resizeDragStartWZ;

    private MapJson.MapMeta loadedMeta  = null;
    private List<MapJson.PathJson> loadedPaths = null;

    private int nameCounter     = 1;
    private int pathNameCounter = 1;

    public EditorMode(CampusMap campusMap, Camera camera) {
        this.campusMap = campusMap;
        this.camera    = camera;
        MapLoader.LoadResult lr = campusMap.getLastLoadResult();
        if (lr != null) { loadedMeta = lr.meta; loadedPaths = null; }
        // Count existing NEW BLDGs so auto-names don't collide
        for (BuildingData b : campusMap.getMutableBuildings())
            if (b.name.startsWith("NEW BLDG")) nameCounter++;
    }

    public void        toggleEditor() { state.toggle(); }
    public boolean     isActive()     { return state.isActive(); }
    public EditorState getState()     { return state; }

    // ── Tool switching ────────────────────────────────────────────────
    public void switchToPlace()      { state.setCurrentTool(EditorState.Tool.PLACE);  }
    public void switchToDelete()     { state.setCurrentTool(EditorState.Tool.DELETE); }
    public void switchToSelect()     { state.setCurrentTool(EditorState.Tool.SELECT); }

    public void switchToShapeEdit() {
        BuildingData sel = state.getSelectedBuilding();
        if (sel == null) { state.showStatus("Select a building first (S), then press V"); return; }
        state.setCurrentTool(EditorState.Tool.SHAPE_EDIT);
        state.getShapeEdit().beginEdit(sel);
        state.showStatus("SHAPE EDIT | drag verts | 1-5=presets | Enter=commit | Esc=cancel");
    }

    public void switchToPathEdit() {
        state.setCurrentTool(EditorState.Tool.PATH_EDIT);
        state.getPathEdit().endEdit();
        state.showStatus("PATH EDIT | click=new/add | Mid=insert | R=delete | Enter=finish");
    }

    // ── Grid snap cycle ───────────────────────────────────────────────
    public void cycleGridSnap() {
        state.cycleGridSnap();
        state.showStatus("Grid snap: " + (int)state.getGridSnap() + " units");
    }

    // ── Floor controls ────────────────────────────────────────────────
    public void nextFloor() { state.nextFloor(); }
    public void prevFloor() { state.prevFloor(); }

    // ── Delete selected ───────────────────────────────────────────────
    public void deleteSelected() {
        if (state.hasMultiSelect()) {
            state.pushUndo(campusMap.getMutableBuildings());
            for (BuildingData b : state.getMultiSelect()) campusMap.removeBuilding(b);
            state.clearMultiSelect();
            state.setSelectedBuilding(null);
            state.showStatus("Deleted " + state.getMultiSelect().size() + " buildings");
            autoSave();
        } else if (state.getSelectedBuilding() != null) {
            delete(state.getCursorWorldX(), state.getCursorWorldZ());
        }
    }

    // ── Escape ────────────────────────────────────────────────────────
    public void escape() {
        if (state.getCurrentTool() == EditorState.Tool.SHAPE_EDIT) { cancelShapeEdit(); return; }
        if (state.getCurrentTool() == EditorState.Tool.PATH_EDIT)  { cancelPathEdit();  return; }
        state.setSelectedBuilding(null);
        state.clearMultiSelect();
        state.dragState.cancel();
        state.showStatus("Deselected");
    }

    // ─────────────────────────────────────────────────────────────────
    // MOUSE MOVE
    // ─────────────────────────────────────────────────────────────────
    public void onMouseMoved(int screenX, int screenY) {
        if (!state.isActive()) return;
        float wx = snap(camera.screenToWorldX(screenX));
        float wz = snap(camera.screenToWorldY(screenY));
        state.setCursorWorld(wx, wz);

        switch (state.getCurrentTool()) {
            case PLACE -> state.setGhostBuilding(makePlaceholder(wx, wz));

            case SELECT -> {
                state.setGhostBuilding(null);
                BuildingData sel = state.getSelectedBuilding();
                if (sel != null && !sel.isPolygon()) {
                    updateResizeHandles(sel);
                    state.setHoveredHandleIdx(findHoveredHandle(screenX, screenY));
                } else {
                    state.setHoveredHandleIdx(-1);
                }
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
                // Resize handle drag
                if (state.getActiveHandleIdx() >= 0) {
                    applyResizeDrag(wx, wz);
                    return;
                }
                // Building move drag
                if (state.dragState.active) {
                    state.dragState.update(wx, wz);
                    return;
                }
                // Box select update
                if (state.boxSelecting) {
                    state.boxSelectCurSX = screenX;
                    state.boxSelectCurSY = screenY;
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
        float wx = snap(camera.screenToWorldX(screenX));
        float wz = snap(camera.screenToWorldY(screenY));

        switch (state.getCurrentTool()) {
            case PLACE      -> place(wx, wz);
            case DELETE     -> delete(wx, wz);
            case SELECT     -> selectPress(wx, wz, screenX, screenY, false);
            case SHAPE_EDIT -> shapeEditPress(screenX, screenY);
            case PATH_EDIT  -> pathEditClick(wx, wz, screenX, screenY);
        }
    }

    /** Called with Shift held — adds to multi-select. */
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

            case SELECT -> {
                // Finish resize drag
                if (state.getActiveHandleIdx() >= 0) {
                    commitResizeDrag();
                    return;
                }
                // Commit building move
                if (state.dragState.active) {
                    commitDragMove();
                    return;
                }
                // Finish box select
                if (state.boxSelecting) {
                    commitBoxSelect(screenX, screenY);
                    state.boxSelecting = false;
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
    // SELECT PRESS LOGIC
    // ─────────────────────────────────────────────────────────────────
    private void selectPress(float wx, float wz, int sx, int sy, boolean addToMulti) {
        BuildingData current = state.getSelectedBuilding();

        // 1. Resize handle?
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

        // 2. Hit test building
        BuildingData hit = hitTest(wx, wz);

        if (hit != null) {
            if (addToMulti) {
                state.addToMultiSelect(hit);
                state.showStatus("Multi-select: " + state.getMultiSelect().size() + " buildings");
                return;
            }
            // Single select + begin drag
            if (!state.isMultiSelected(hit)) state.clearMultiSelect();
            state.setSelectedBuilding(hit);
            state.dragState.begin(wx, wz, hit.x, hit.z);
            state.showStatus("Selected: " + hit.name + "  (drag to move)");
        } else {
            // Click empty → start box select
            state.setSelectedBuilding(null);
            state.clearMultiSelect();
            state.boxSelecting     = true;
            state.boxSelectStartSX = sx; state.boxSelectStartSY = sy;
            state.boxSelectCurSX   = sx; state.boxSelectCurSY   = sy;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // DRAG MOVE COMMIT
    // ─────────────────────────────────────────────────────────────────
    private void commitDragMove() {
        BuildingData sel = state.getSelectedBuilding();
        if (sel == null) { state.dragState.cancel(); return; }

        float nx = snap(state.dragState.currentX);
        float nz = snap(state.dragState.currentZ);

        // Don't commit if barely moved
        if (Math.abs(nx - sel.x) < 1f && Math.abs(nz - sel.z) < 1f) {
            state.dragState.cancel(); return;
        }

        state.pushUndo(campusMap.getMutableBuildings());
        BuildingData moved = movedBuilding(sel, nx, nz);
        campusMap.replaceBuilding(sel, moved);
        state.setSelectedBuilding(moved);
        state.dragState.cancel();
        autoSave();
        state.showStatus("Moved: " + moved.name);
    }

    private BuildingData movedBuilding(BuildingData b, float nx, float nz) {
        if (b.isPolygon()) {
            int dx = (int)(nx - b.x), dz = (int)(nz - b.z);
            int[] px = b.polygonX.clone(), pz = b.polygonZ.clone();
            for (int i=0; i<px.length; i++) { px[i]+=dx; pz[i]+=dz; }
            return new BuildingData(b.name, px, pz, b.floors, b.colorARGB,
                    b.rotationDegrees, b.collisionEnabled, b.tag);
        }
        return new BuildingData(b.name, nx, nz, b.width, b.depth,
                b.floors, b.colorARGB, b.rotationDegrees, b.collisionEnabled, b.tag);
    }

    // ─────────────────────────────────────────────────────────────────
    // RESIZE HANDLES
    // ─────────────────────────────────────────────────────────────────
    private void updateResizeHandles(BuildingData b) {
        for (ResizeHandle h : resizeHandles)
            h.updatePosition(b.x, b.z, b.width, b.depth);
    }

    private int findHoveredHandle(int screenX, int screenY) {
        for (int i = 0; i < resizeHandles.length; i++) {
            ResizeHandle h = resizeHandles[i];
            int hsx = camera.worldToScreenX(h.worldX);
            int hsy = camera.worldToScreenY(h.worldZ);
            if (h.hitTest(screenX, screenY, hsx, hsy)) return i;
        }
        return -1;
    }

    private void applyResizeDrag(float wx, float wz) {
        BuildingData sel = state.getSelectedBuilding();
        if (sel == null) return;
        ResizeHandle h = resizeHandles[state.getActiveHandleIdx()];
        float dWX = wx - resizeDragStartWX;
        float dWZ = wz - resizeDragStartWZ;
        float[] bounds = h.applyDrag(resizeDragOriginX, resizeDragOriginZ,
                resizeDragOrigW,   resizeDragOrigD, dWX, dWZ);
        // Live preview — replace without undo push (undo on release)
        BuildingData preview = new BuildingData(sel.name, bounds[0], bounds[1],
                bounds[2], bounds[3], sel.floors, sel.colorARGB,
                sel.rotationDegrees, sel.collisionEnabled, sel.tag);
        campusMap.replaceBuilding(sel, preview);
        state.setSelectedBuilding(preview);
        updateResizeHandles(preview);
    }

    private void commitResizeDrag() {
        state.setActiveHandleIdx(-1);
        BuildingData sel = state.getSelectedBuilding();
        if (sel != null) { autoSave(); state.showStatus("Resized: " + sel.name); }
    }

    // ─────────────────────────────────────────────────────────────────
    // BOX SELECT
    // ─────────────────────────────────────────────────────────────────
    private void commitBoxSelect(int curSX, int curSY) {
        int x1 = Math.min(state.boxSelectStartSX, curSX);
        int y1 = Math.min(state.boxSelectStartSY, curSY);
        int x2 = Math.max(state.boxSelectStartSX, curSX);
        int y2 = Math.max(state.boxSelectStartSY, curSY);

        if (x2 - x1 < 5 && y2 - y1 < 5) return; // too small = normal click miss

        state.clearMultiSelect();
        for (BuildingData b : campusMap.getMutableBuildings()) {
            if (!state.getLayerManager().isSelectable(b.tag)) continue;
            int bsx = camera.worldToScreenX(b.x);
            int bsy = camera.worldToScreenY(b.z);
            if (bsx >= x1 && bsx <= x2 && bsy >= y1 && bsy <= y2)
                state.addToMultiSelect(b);
        }
        if (state.hasMultiSelect())
            state.showStatus("Box selected: " + state.getMultiSelect().size() + " buildings");
    }

    // ─────────────────────────────────────────────────────────────────
    // PLACE / DELETE / SELECT
    // ─────────────────────────────────────────────────────────────────
    private void place(float wx, float wz) {
        state.pushUndo(campusMap.getMutableBuildings());
        String n = "NEW BLDG " + nameCounter++;
        String tag = state.getLayerManager().getActiveLayer() == EditorLayer.WALLS ? "wall"
                : state.getLayerManager().getActiveLayer() == EditorLayer.PE    ? "pe"
                : "building";
        BuildingData bd = new BuildingData(n, wx, wz,
                EditorState.DEFAULT_WIDTH, EditorState.DEFAULT_DEPTH,
                EditorState.DEFAULT_FLOORS, EditorState.DEFAULT_COLOR, true, tag);
        campusMap.addBuilding(bd);
        state.showStatus("Placed: " + n);
        autoSave();
    }

    private void delete(float wx, float wz) {
        BuildingData hit = hitTest(wx, wz);
        if (hit == null) { state.showStatus("Nothing to delete here"); return; }
        state.pushUndo(campusMap.getMutableBuildings());
        campusMap.removeBuilding(hit);
        if (state.getSelectedBuilding() == hit) state.setSelectedBuilding(null);
        state.showStatus("Deleted: " + hit.name);
        autoSave();
    }

    // ── Property panel commit ─────────────────────────────────────────
    public void commitPropertyPanel() {
        BuildingData updated = state.getPropertyPanel().buildUpdated();
        if (updated == null) return;
        BuildingData original = state.getPropertyPanel().getSource();
        state.pushUndo(campusMap.getMutableBuildings());
        campusMap.replaceBuilding(original, updated);
        state.setSelectedBuilding(updated);
        autoSave();
        state.showStatus("Properties updated: " + updated.name);
    }

    // ── Shape edit ────────────────────────────────────────────────────
    private void shapeEditPress(int screenX, int screenY) {
        ShapeEditState se = state.getShapeEdit();
        if (!se.isActive()) return;
        int[][] sv = projectToScreen(se.getVerticesX(), se.getVerticesZ());
        int vi = se.findNearestVertex(screenX, screenY, sv[0], sv[1]);
        if (vi >= 0) { se.setSelectedVertex(vi); se.setDragging(true); return; }
        int ei = se.findNearestEdgeMidpoint(screenX, screenY, sv[0], sv[1]);
        if (ei >= 0) { se.insertVertexAfterEdge(ei);
            state.showStatus("Vertex inserted (" + se.getVerticesX().length + " verts)"); }
    }

    public void applyShapePreset(ShapeEditState.ShapePreset preset) {
        ShapeEditState se = state.getShapeEdit();
        if (!se.isActive()) return;
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
        state.setSelectedBuilding(updated);
        se.endEdit();
        state.setCurrentTool(EditorState.Tool.SELECT);
        autoSave();
        state.showStatus("Shape committed: " + updated.name);
    }

    public void cancelShapeEdit() {
        state.getShapeEdit().endEdit();
        state.setCurrentTool(EditorState.Tool.SELECT);
        state.showStatus("Shape edit cancelled");
    }

    // ── Path edit ─────────────────────────────────────────────────────
    private void pathEditClick(float wx, float wz, int screenX, int screenY) {
        PathEditState pe = state.getPathEdit();

        if (pe.isActive()) {

            if (pe.isAppendMode()) {
                // In append mode — clicks add waypoints
                int[][] sv = projectPathToScreen(pe);
                int vi = pe.findNearestPoint(screenX, screenY, sv[0], sv[1]);
                if (vi >= 0) { pe.setSelectedPoint(vi); pe.setDragging(true); return; }
                int ei = pe.findNearestEdgeMidpoint(screenX, screenY, sv[0], sv[1]);
                if (ei >= 0) {
                    pe.insertPointAfter(ei);
                    state.showStatus("Point inserted (" + pe.getWx().length + ")");
                    return;
                }
                pe.appendPoint(wx, wz);
                state.showStatus("Waypoint added (" + pe.getWx().length + ") — Enter=finish");
                return;
            }

            // Not in append mode — handle vertex drag only
            int[][] sv = projectPathToScreen(pe);
            int vi = pe.findNearestPoint(screenX, screenY, sv[0], sv[1]);
            if (vi >= 0) { pe.setSelectedPoint(vi); pe.setDragging(true); return; }

            // Click on empty space = deselect current path, try to select another
            PathData hit = hitTestPath(wx, wz);
            if (hit != null && hit != pe.getSource()) {
                pe.endEdit();
                pe.beginEdit(hit);
                state.showStatus("Selected: " + hit.name + " — drag points | Del=delete path | A=add points");
            } else if (hit == null) {
                pe.endEdit();
                state.showStatus("Path deselected");
            }
            return;
        }

        // No path active — try to select one
        PathData hit = hitTestPath(wx, wz);
        if (hit != null) {
            pe.beginEdit(hit);
            state.showStatus("Selected: " + hit.name + " — drag points | Del=delete path | A=add points");
        } else {
            // Click empty with nothing selected = start new path in append mode
            PathData np = new PathData("PATH " + pathNameCounter++, 55f, "#FFDCD7C8");
            np.addPoint(wx, wz);
            campusMap.addPath(np);
            pe.beginEdit(np);
            pe.setAppendMode(true);
            state.showStatus("New path — click to add waypoints, Enter to finish");
        }
    }

    public void commitPathEdit() {
        PathEditState pe = state.getPathEdit();
        if (!pe.isActive()) { state.showStatus("Nothing to commit"); return; }
        pe.applyTo(pe.getSource());
        pe.setAppendMode(false);
        pe.endEdit();
        autoSave();
        state.showStatus("Path saved");
    }

    public void cancelPathEdit() {
        PathEditState pe = state.getPathEdit();
        if (!pe.isActive()) return;
        if (pe.getSource().points.size() < 2) campusMap.removePath(pe.getSource());
        pe.endEdit();
        state.showStatus("Path edit cancelled");
    }

    // ── Right / middle click ──────────────────────────────────────────
    public void onRightClick(int screenX, int screenY) {
        if (!state.isActive()) return;
        switch (state.getCurrentTool()) {
            case SHAPE_EDIT -> {
                ShapeEditState se = state.getShapeEdit();
                if (!se.isActive()) return;
                int[][] sv = projectToScreen(se.getVerticesX(), se.getVerticesZ());
                int i = se.findNearestVertex(screenX, screenY, sv[0], sv[1]);
                if (i >= 0) { se.deleteVertex(i);
                    state.showStatus("Vertex deleted (" + se.getVerticesX().length + " remain)"); }
            }
            case PATH_EDIT -> {
                PathEditState pe = state.getPathEdit();
                if (!pe.isActive()) return;
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
                ShapeEditState se = state.getShapeEdit();
                if (!se.isActive()) return;
                int[][] sv = projectToScreen(se.getVerticesX(), se.getVerticesZ());
                int e = se.findNearestEdgeMidpoint(screenX, screenY, sv[0], sv[1]);
                if (e >= 0) { se.insertVertexAfterEdge(e);
                    state.showStatus("Vertex inserted"); }
            }
            case PATH_EDIT -> {
                PathEditState pe = state.getPathEdit();
                if (!pe.isActive()) return;
                int[][] sv = projectPathToScreen(pe);
                int e = pe.findNearestEdgeMidpoint(screenX, screenY, sv[0], sv[1]);
                if (e >= 0) { pe.insertPointAfter(e); state.showStatus("Point inserted"); }
            }
            default -> {}
        }
    }

    // ── Resize / Rotate via keyboard ──────────────────────────────────
    public void resizeSelected(float dw, float dd) {
        BuildingData sel = state.getSelectedBuilding();
        if (sel == null) { state.showStatus("No building selected"); return; }
        if (sel.isPolygon()) { state.showStatus("Use SHAPE EDIT (V) for polygon"); return; }
        state.pushUndo(campusMap.getMutableBuildings());
        BuildingData u = new BuildingData(sel.name, sel.x, sel.z,
                Math.max(20, sel.width+dw), Math.max(20, sel.depth+dd),
                sel.floors, sel.colorARGB, sel.rotationDegrees, sel.collisionEnabled, sel.tag);
        campusMap.replaceBuilding(sel, u);
        state.setSelectedBuilding(u);
        autoSave();
    }

    public void rotateSelected(float deg) {
        BuildingData sel = state.getSelectedBuilding();
        if (sel == null || sel.isPolygon()) return;
        state.pushUndo(campusMap.getMutableBuildings());
        BuildingData u = new BuildingData(sel.name, sel.x, sel.z, sel.width, sel.depth,
                sel.floors, sel.colorARGB, sel.rotationDegrees+deg, sel.collisionEnabled, sel.tag);
        campusMap.replaceBuilding(sel, u);
        state.setSelectedBuilding(u);
        autoSave();
    }

    // ── Save / Undo ───────────────────────────────────────────────────
    public void saveMap() {
        MapJson.MapMeta meta = loadedMeta != null ? loadedMeta
                : new MapJson.MapMeta("Main Campus", 3000, 2400, "Admin", "Editor save");
        boolean ok = saver.save(campusMap.getMutableBuildings(), campusMap.getPaths(), meta);
        state.showStatus(ok ? "✓ Saved to campus.json" : "✗ Save FAILED!");
    }

    private void autoSave() {
        MapJson.MapMeta meta = loadedMeta != null ? loadedMeta
                : new MapJson.MapMeta("Main Campus", 3000, 2400, "Admin", "Auto-save");
        saver.save(campusMap.getMutableBuildings(), campusMap.getPaths(), meta);
    }

    public void undo() {
        List<BuildingData> snap = state.popUndo();
        if (snap == null) { state.showStatus("Nothing to undo"); return; }
        campusMap.replaceAllBuildings(snap);
        state.setSelectedBuilding(null);
        state.clearMultiSelect();
        state.showStatus("Undone");
    }

    // ── Accessors for renderer ────────────────────────────────────────
    public ResizeHandle[] getResizeHandles() { return resizeHandles; }

    // ── Helpers ───────────────────────────────────────────────────────
    private BuildingData makePlaceholder(float wx, float wz) {
        return new BuildingData("NEW BLDG " + nameCounter, wx, wz,
                EditorState.DEFAULT_WIDTH, EditorState.DEFAULT_DEPTH,
                EditorState.DEFAULT_FLOORS, EditorState.DEFAULT_COLOR,
                true, EditorState.DEFAULT_TAG);
    }

    private BuildingData hitTest(float wx, float wz) {
        List<BuildingData> all = campusMap.getMutableBuildings();
        LayerManager lm = state.getLayerManager();
        for (int i = all.size()-1; i >= 0; i--) {
            BuildingData b = all.get(i);
            if (!lm.isSelectable(b.tag)) continue;
            if (!b.isPolygon() && wx>=b.x && wx<=b.maxX() && wz>=b.z && wz<=b.maxZ()) return b;
            if (b.isPolygon()) {
                int minX=min(b.polygonX),maxX=max(b.polygonX),minZ=min(b.polygonZ),maxZ=max(b.polygonZ);
                if (wx>=minX && wx<=maxX && wz>=minZ && wz<=maxZ) return b;
            }
        }
        return null;
    }

    public void deleteSelectedPath() {
        PathEditState pe = state.getPathEdit();
        if (!pe.isActive()) {
            state.showStatus("Select a path first (R tool), then press Delete");
            return;
        }
        PathData toDelete = pe.getSource();
        pe.endEdit();
        campusMap.removePath(toDelete);
        autoSave();
        state.showStatus("Path deleted: " + toDelete.name);
    }

    private PathData hitTestPath(float wx, float wz) {
        for (PathData p : campusMap.getPaths()) {
            for (int i=0; i<p.points.size()-1; i++) {
                if (distToSeg(wx,wz, p.points.get(i)[0],p.points.get(i)[1],
                        p.points.get(i+1)[0],p.points.get(i+1)[1]) < 30f) return p;
            }
        }
        return null;
    }

    private float distToSeg(float px,float pz,float ax,float az,float bx,float bz) {
        float dx=bx-ax,dz=bz-az,l=dx*dx+dz*dz;
        if(l==0) return (float)Math.hypot(px-ax,pz-az);
        float t=Math.max(0,Math.min(1,((px-ax)*dx+(pz-az)*dz)/l));
        return (float)Math.hypot(px-(ax+t*dx),pz-(az+t*dz));
    }

    private BuildingData buildFromShape(ShapeEditState se, BuildingData src) {
        int[] vx=se.getVerticesX(), vz=se.getVerticesZ();
        if (!se.isPolygonMode() || isAxisAlignedRect(vx, vz)) {
            int minX=min(vx),maxX=max(vx),minZ=min(vz),maxZ=max(vz);
            return new BuildingData(src.name,minX,minZ,maxX-minX,maxZ-minZ,
                    src.floors,src.colorARGB,src.rotationDegrees,src.collisionEnabled,src.tag);
        }
        return new BuildingData(src.name,vx.clone(),vz.clone(),
                src.floors,src.colorARGB,src.rotationDegrees,src.collisionEnabled,src.tag);
    }

    private boolean isAxisAlignedRect(int[] vx, int[] vz) {
        if(vx.length!=4) return false;
        return java.util.Arrays.stream(vx).distinct().count()==2
                && java.util.Arrays.stream(vz).distinct().count()==2;
    }

    public int[][] projectToScreen(int[] vx, int[] vz) {
        int n=vx.length; int[] sx=new int[n],sy=new int[n];
        for(int i=0;i<n;i++){sx[i]=camera.worldToScreenX(vx[i]);sy[i]=camera.worldToScreenY(vz[i]);}
        return new int[][]{sx,sy};
    }

    public int[][] projectPathToScreen(PathEditState pe) {
        float[] wx=pe.getWx(),wz=pe.getWz(); int n=wx.length;
        int[] sx=new int[n],sy=new int[n];
        for(int i=0;i<n;i++){sx[i]=camera.worldToScreenX(wx[i]);sy[i]=camera.worldToScreenY(wz[i]);}
        return new int[][]{sx,sy};
    }

    private float snap(float v)  { float g=state.getGridSnap(); return Math.round(v/g)*g; }
    private int   min(int[] arr) { int m=arr[0]; for(int v:arr) if(v<m) m=v; return m; }
    private int   max(int[] arr) { int m=arr[0]; for(int v:arr) if(v>m) m=v; return m; }
}