package com.campusgame.editor;

import com.campusgame.map.CampusMap;
import com.campusgame.map.data.BuildingData;
import com.campusgame.map.data.PathData;
import com.campusgame.map.io.MapJson;
import com.campusgame.map.io.MapLoader;
import com.campusgame.map.io.MapSaver;
import com.campusgame.renderer.Camera;

import java.util.List;

public class EditorMode {

    private final CampusMap   campusMap;
    private final Camera      camera;
    private final EditorState state  = new EditorState();
    private final MapSaver    saver  = new MapSaver();

    public static final float GRID_SNAP = 20f;

    private MapJson.MapMeta loadedMeta  = null;
    private int nameCounter     = 1;
    private int pathNameCounter = 1;

    public EditorMode(CampusMap campusMap, Camera camera) {
        this.campusMap = campusMap;
        this.camera    = camera;
        MapLoader.LoadResult lr = campusMap.getLastLoadResult();
        if (lr != null) loadedMeta = lr.meta;
    }

    public void        toggleEditor() { state.toggle(); }
    public boolean     isActive()     { return state.isActive(); }
    public EditorState getState()     { return state; }

    public void switchToPlace()      { state.setCurrentTool(EditorState.Tool.PLACE);  }
    public void switchToDelete()     { state.setCurrentTool(EditorState.Tool.DELETE); }
    public void switchToSelect()     { state.setCurrentTool(EditorState.Tool.SELECT); }

    public void switchToShapeEdit() {
        BuildingData sel = state.getSelectedBuilding();
        if (sel == null) { state.showStatus("Select a building first (S key), then press V"); return; }
        state.setCurrentTool(EditorState.Tool.SHAPE_EDIT);
        state.getShapeEdit().beginEdit(sel);
        state.showStatus("SHAPE EDIT — drag verts | 1-5 presets | Enter=commit | Esc=cancel");
    }

    public void switchToPathEdit() {
        state.setCurrentTool(EditorState.Tool.PATH_EDIT);
        state.getPathEdit().endEdit();
        state.showStatus("PATH EDIT — Click=new/add point | Mid=insert | R=delete | Enter=finish");
    }

    public void onMouseMoved(int screenX, int screenY) {
        if (!state.isActive()) return;
        float wx = snap(camera.screenToWorldX(screenX));
        float wz = snap(camera.screenToWorldY(screenY));
        state.setCursorWorld(wx, wz);

        switch (state.getCurrentTool()) {
            case PLACE -> state.setGhostBuilding(new BuildingData(
                    "NEW BLDG " + nameCounter, wx, wz,
                    EditorState.DEFAULT_WIDTH, EditorState.DEFAULT_DEPTH,
                    EditorState.DEFAULT_FLOORS, EditorState.DEFAULT_COLOR,
                    true, EditorState.DEFAULT_TAG));
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

    public void onMouseDragged(int screenX, int screenY) {
        if (!state.isActive()) return;
        state.setCursorWorld(snap(camera.screenToWorldX(screenX)), snap(camera.screenToWorldY(screenY)));
        switch (state.getCurrentTool()) {
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

    public void onLeftClick(int screenX, int screenY) {
        if (!state.isActive()) return;
        float wx = snap(camera.screenToWorldX(screenX));
        float wz = snap(camera.screenToWorldY(screenY));
        switch (state.getCurrentTool()) {
            case PLACE      -> place(wx, wz);
            case SELECT     -> select(wx, wz);
            case DELETE     -> delete(wx, wz);
            case SHAPE_EDIT -> shapeEditPress(screenX, screenY);
            case PATH_EDIT  -> pathEditClick(wx, wz, screenX, screenY);
        }
    }

    public void onLeftRelease(int screenX, int screenY) {
        if (!state.isActive()) return;
        switch (state.getCurrentTool()) {
            case SHAPE_EDIT -> {
                ShapeEditState se = state.getShapeEdit();
                if (se.isDragging()) { se.setDragging(false); se.setSelectedVertex(-1);
                    state.showStatus("Vertex moved — Enter to commit, Esc to cancel"); }
            }
            case PATH_EDIT -> {
                PathEditState pe = state.getPathEdit();
                if (pe.isDragging()) { pe.setDragging(false); pe.setSelectedPoint(-1);
                    state.showStatus("Point moved — Enter to finish, Esc to cancel"); }
            }
            default -> {}
        }
    }

    public void onRightClick(int screenX, int screenY) {
        if (!state.isActive()) return;
        switch (state.getCurrentTool()) {
            case SHAPE_EDIT -> {
                ShapeEditState se = state.getShapeEdit();
                if (!se.isActive()) return;
                int[][] sv = projectToScreen(se.getVerticesX(), se.getVerticesZ());
                int idx = se.findNearestVertex(screenX, screenY, sv[0], sv[1]);
                if (idx >= 0) { se.deleteVertex(idx);
                    state.showStatus("Vertex deleted (" + se.getVerticesX().length + " remain)"); }
            }
            case PATH_EDIT -> {
                PathEditState pe = state.getPathEdit();
                if (!pe.isActive()) return;
                int[][] sv = projectPathToScreen(pe);
                int idx = pe.findNearestPoint(screenX, screenY, sv[0], sv[1]);
                if (idx >= 0) { pe.deletePoint(idx); state.showStatus("Point deleted"); }
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
                int edge = se.findNearestEdgeMidpoint(screenX, screenY, sv[0], sv[1]);
                if (edge >= 0) { se.insertVertexAfterEdge(edge);
                    state.showStatus("Vertex inserted — now " + se.getVerticesX().length + " verts"); }
            }
            case PATH_EDIT -> {
                PathEditState pe = state.getPathEdit();
                if (!pe.isActive()) return;
                int[][] sv = projectPathToScreen(pe);
                int edge = pe.findNearestEdgeMidpoint(screenX, screenY, sv[0], sv[1]);
                if (edge >= 0) { pe.insertPointAfter(edge);
                    state.showStatus("Point inserted (" + pe.getWx().length + " points)"); }
            }
            default -> {}
        }
    }

    private void shapeEditPress(int screenX, int screenY) {
        ShapeEditState se = state.getShapeEdit();
        if (!se.isActive()) return;
        int[][] sv = projectToScreen(se.getVerticesX(), se.getVerticesZ());
        int vertIdx = se.findNearestVertex(screenX, screenY, sv[0], sv[1]);
        if (vertIdx >= 0) { se.setSelectedVertex(vertIdx); se.setDragging(true); return; }
        int edgeIdx = se.findNearestEdgeMidpoint(screenX, screenY, sv[0], sv[1]);
        if (edgeIdx >= 0) { se.insertVertexAfterEdge(edgeIdx);
            state.showStatus("Vertex inserted (" + se.getVerticesX().length + " verts)"); }
    }

    private void pathEditClick(float wx, float wz, int screenX, int screenY) {
        PathEditState pe = state.getPathEdit();
        if (pe.isActive()) {
            int[][] sv = projectPathToScreen(pe);
            int vtx = pe.findNearestPoint(screenX, screenY, sv[0], sv[1]);
            if (vtx >= 0) { pe.setSelectedPoint(vtx); pe.setDragging(true); return; }
            int edge = pe.findNearestEdgeMidpoint(screenX, screenY, sv[0], sv[1]);
            if (edge >= 0) { pe.insertPointAfter(edge);
                state.showStatus("Point inserted (" + pe.getWx().length + " points)"); return; }
            pe.appendPoint(wx, wz);
            state.showStatus("Waypoint added (" + pe.getWx().length + " points) — Enter to finish");
            return;
        }
        PathData hit = hitTestPath(wx, wz);
        if (hit != null) {
            pe.beginEdit(hit);
            state.showStatus("Editing: " + hit.name + " — drag points | Enter=done");
        } else {
            PathData np = new PathData("PATH " + pathNameCounter++, 55f, "#FFDCD7C8");
            np.addPoint(wx, wz);
            campusMap.addPath(np);
            pe.beginEdit(np);
            state.showStatus("New path started — click to add waypoints, Enter to finish");
        }
    }

    public void applyShapePreset(ShapeEditState.ShapePreset preset) {
        ShapeEditState se = state.getShapeEdit();
        if (!se.isActive()) return;
        se.applyPreset(preset, se.getSource());
        state.showStatus("Preset: " + preset + " — Enter to commit, Esc to cancel");
    }

    public void commitShapeEdit() {
        ShapeEditState se = state.getShapeEdit();
        if (!se.isActive()) { state.showStatus("Nothing to commit"); return; }
        BuildingData original = se.getSource();
        state.pushUndo(campusMap.getMutableBuildings());
        BuildingData updated = buildFromWorkingCopy(se, original);
        campusMap.replaceBuilding(original, updated);
        state.setSelectedBuilding(updated);
        se.endEdit();
        state.setCurrentTool(EditorState.Tool.SELECT);
        autoSave();
        state.showStatus("Shape committed: " + updated.name);
    }

    public void cancelShapeEdit() {
        ShapeEditState se = state.getShapeEdit();
        if (!se.isActive()) return;
        se.endEdit();
        state.setCurrentTool(EditorState.Tool.SELECT);
        state.showStatus("Shape edit cancelled");
    }

    public void commitPathEdit() {
        PathEditState pe = state.getPathEdit();
        if (!pe.isActive()) { state.showStatus("Nothing to commit"); return; }
        pe.applyTo(pe.getSource());
        pe.endEdit();
        autoSave();
        state.showStatus("Path saved");
    }

    public void cancelPathEdit() {
        PathEditState pe = state.getPathEdit();
        if (!pe.isActive()) { state.showStatus("Nothing to cancel"); return; }
        if (pe.getSource().points.size() < 2) campusMap.removePath(pe.getSource());
        pe.endEdit();
        state.showStatus("Path edit cancelled");
    }

    private void place(float wx, float wz) {
        state.pushUndo(campusMap.getMutableBuildings());
        String n = "NEW BLDG " + nameCounter++;
        campusMap.addBuilding(new BuildingData(n, wx, wz,
                EditorState.DEFAULT_WIDTH, EditorState.DEFAULT_DEPTH,
                EditorState.DEFAULT_FLOORS, EditorState.DEFAULT_COLOR, true, EditorState.DEFAULT_TAG));
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
        state.showStatus("Undone");
    }

    public void resizeSelected(float dw, float dd) {
        BuildingData sel = state.getSelectedBuilding();
        if (sel == null) { state.showStatus("No building selected"); return; }
        if (sel.isPolygon()) { state.showStatus("Use SHAPE EDIT (V) for polygon resize"); return; }
        state.pushUndo(campusMap.getMutableBuildings());
        BuildingData updated = new BuildingData(sel.name, sel.x, sel.z,
                Math.max(20, sel.width+dw), Math.max(20, sel.depth+dd),
                sel.floors, sel.colorARGB, sel.rotationDegrees, sel.collisionEnabled, sel.tag);
        campusMap.replaceBuilding(sel, updated);
        state.setSelectedBuilding(updated);
        autoSave();
        state.showStatus("Resized: " + updated.name);
    }

    public void rotateSelected(float degrees) {
        BuildingData sel = state.getSelectedBuilding();
        if (sel == null) { state.showStatus("No building selected"); return; }
        if (sel.isPolygon()) { state.showStatus("Use SHAPE EDIT (V) for polygon rotation"); return; }
        state.pushUndo(campusMap.getMutableBuildings());
        BuildingData updated = new BuildingData(sel.name, sel.x, sel.z, sel.width, sel.depth,
                sel.floors, sel.colorARGB, sel.rotationDegrees+degrees, sel.collisionEnabled, sel.tag);
        campusMap.replaceBuilding(sel, updated);
        state.setSelectedBuilding(updated);
        autoSave();
        state.showStatus("Rotated: " + updated.name);
    }

    private BuildingData hitTest(float wx, float wz) {
        List<BuildingData> all = campusMap.getMutableBuildings();
        for (int i=all.size()-1; i>=0; i--) {
            BuildingData b = all.get(i);
            if (!b.isPolygon() && wx>=b.x && wx<=b.maxX() && wz>=b.z && wz<=b.maxZ()) return b;
            if (b.isPolygon()) {
                int minX=min(b.polygonX),maxX=max(b.polygonX),minZ=min(b.polygonZ),maxZ=max(b.polygonZ);
                if (wx>=minX && wx<=maxX && wz>=minZ && wz<=maxZ) return b;
            }
        }
        return null;
    }

    private PathData hitTestPath(float wx, float wz) {
        float threshold = 30f;
        for (PathData p : campusMap.getPaths()) {
            for (int i=0; i<p.points.size()-1; i++) {
                if (distToSegment(wx,wz,p.points.get(i)[0],p.points.get(i)[1],
                        p.points.get(i+1)[0],p.points.get(i+1)[1]) < threshold) return p;
            }
        }
        return null;
    }

    private float distToSegment(float px,float pz,float ax,float az,float bx,float bz) {
        float dx=bx-ax,dz=bz-az,lenSq=dx*dx+dz*dz;
        if (lenSq==0) return (float)Math.hypot(px-ax,pz-az);
        float t=Math.max(0,Math.min(1,((px-ax)*dx+(pz-az)*dz)/lenSq));
        return (float)Math.hypot(px-(ax+t*dx),pz-(az+t*dz));
    }

    public int[][] projectToScreen(int[] vx, int[] vz) {
        int n=vx.length; int[] sx=new int[n],sy=new int[n];
        for (int i=0;i<n;i++){sx[i]=camera.worldToScreenX(vx[i]);sy[i]=camera.worldToScreenY(vz[i]);}
        return new int[][]{sx,sy};
    }

    public int[][] projectPathToScreen(PathEditState pe) {
        float[] wx=pe.getWx(),wz=pe.getWz(); int n=wx.length;
        int[] sx=new int[n],sy=new int[n];
        for (int i=0;i<n;i++){sx[i]=camera.worldToScreenX(wx[i]);sy[i]=camera.worldToScreenY(wz[i]);}
        return new int[][]{sx,sy};
    }

    private BuildingData buildFromWorkingCopy(ShapeEditState se, BuildingData src) {
        int[] vx=se.getVerticesX(), vz=se.getVerticesZ();
        if (!se.isPolygonMode() || isAxisAlignedRect(vx,vz)) {
            int minX=min(vx),maxX=max(vx),minZ=min(vz),maxZ=max(vz);
            return new BuildingData(src.name,minX,minZ,maxX-minX,maxZ-minZ,
                    src.floors,src.colorARGB,src.rotationDegrees,src.collisionEnabled,src.tag);
        }
        return new BuildingData(src.name,vx.clone(),vz.clone(),
                src.floors,src.colorARGB,src.rotationDegrees,src.collisionEnabled,src.tag);
    }

    private boolean isAxisAlignedRect(int[] vx, int[] vz) {
        if (vx.length!=4) return false;
        long dx=java.util.Arrays.stream(vx).distinct().count();
        long dz=java.util.Arrays.stream(vz).distinct().count();
        return dx==2 && dz==2;
    }

    private float snap(float v)  { return Math.round(v/GRID_SNAP)*GRID_SNAP; }
    private int   min(int[] arr) { int m=arr[0]; for(int v:arr) if(v<m) m=v; return m; }
    private int   max(int[] arr) { int m=arr[0]; for(int v:arr) if(v>m) m=v; return m; }
}