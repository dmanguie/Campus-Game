package com.campusgame.editor;

import com.campusgame.editor.layer.LayerManager;
import com.campusgame.editor.panel.PropertyPanel;
import com.campusgame.map.data.BuildingData;
import com.campusgame.map.data.EntranceData;

import java.util.*;

public class EditorState {

    public enum Tool { PLACE, SELECT, DELETE, SHAPE_EDIT, PATH_EDIT, ENTRANCE, MOVE, RESIZE, ROTATE }

    private boolean active = false;
    public boolean  isActive()           { return active; }
    public void     setActive(boolean v) { active = v; }
    public void toggle() {
        active = !active;
        if (!active) { multiSelect.clear(); dragState.cancel(); }
        showStatus(active
                ? "EDITOR ON  |  Q=Select  W=Move  E=Resize  R=Rotate  T=Path  P=Place  X=Delete  F1=Exit"
                : "[ EDITOR OFF ]");
    }

    private Tool currentTool = Tool.PLACE;
    public Tool  getCurrentTool()       { return currentTool; }
    public void  setCurrentTool(Tool t) {
        if (currentTool == Tool.PATH_EDIT  && t != Tool.PATH_EDIT)  pathEdit.endEdit();
        if (currentTool == Tool.SHAPE_EDIT && t != Tool.SHAPE_EDIT) shapeEdit.endEdit();
        if (currentTool == Tool.ENTRANCE   && t != Tool.ENTRANCE)   entranceEdit.clear();
        currentTool = t;
        if (t != Tool.SELECT) { multiSelect.clear(); dragState.cancel(); }
        showStatus("Tool: " + t);
    }

    public static final float  DEFAULT_WIDTH  = 120f;
    public static final float  DEFAULT_DEPTH  = 100f;
    public static final int    DEFAULT_FLOORS = 2;
    public static final int    DEFAULT_COLOR  = 0xFF888888;
    public static final String DEFAULT_TAG    = "building";

    private BuildingData ghostBuilding = null;
    public BuildingData getGhostBuilding()               { return ghostBuilding; }
    public void         setGhostBuilding(BuildingData b) { ghostBuilding = b; }

    private float cursorWorldX, cursorWorldZ;
    public float getCursorWorldX() { return cursorWorldX; }
    public float getCursorWorldZ() { return cursorWorldZ; }
    public void  setCursorWorld(float x, float z) { cursorWorldX = x; cursorWorldZ = z; }

    private BuildingData selectedBuilding = null;
    public BuildingData getSelectedBuilding()               { return selectedBuilding; }
    public void         setSelectedBuilding(BuildingData b) {
        selectedBuilding = b;
        if (b != null) propertyPanel.load(b);
        else           propertyPanel.hide();
    }

    private final List<BuildingData> multiSelect = new ArrayList<>();
    public List<BuildingData> getMultiSelect()        { return multiSelect; }
    public void addToMultiSelect(BuildingData b)      { if (!multiSelect.contains(b)) multiSelect.add(b); }
    public void removeFromMultiSelect(BuildingData b) { multiSelect.remove(b); }
    public void clearMultiSelect()                    { multiSelect.clear(); }
    public boolean isMultiSelected(BuildingData b)    { return multiSelect.contains(b); }
    public boolean hasMultiSelect()                   { return !multiSelect.isEmpty(); }

    public final DragMoveState dragState = new DragMoveState();

    public static class DragMoveState {
        public boolean active      = false;
        public float   startWorldX = 0, startWorldZ = 0;
        public float   originX     = 0, originZ     = 0;
        public float   currentX    = 0, currentZ    = 0;

        public void begin(float startWX, float startWZ, float bOriginX, float bOriginZ) {
            active = true;
            startWorldX = startWX; startWorldZ = startWZ;
            originX = bOriginX;    originZ = bOriginZ;
            currentX = bOriginX;   currentZ = bOriginZ;
        }
        public void update(float wx, float wz) {
            currentX = originX + (wx - startWorldX);
            currentZ = originZ + (wz - startWorldZ);
        }
        public void cancel() { active = false; }
    }

    private int hoveredHandleIdx = -1;
    private int activeHandleIdx  = -1;
    public int  getHoveredHandleIdx()      { return hoveredHandleIdx; }
    public void setHoveredHandleIdx(int i) { hoveredHandleIdx = i; }
    public int  getActiveHandleIdx()       { return activeHandleIdx; }
    public void setActiveHandleIdx(int i)  { activeHandleIdx = i; }

    public boolean boxSelecting     = false;
    public int     boxSelectStartSX = 0, boxSelectStartSY = 0;
    public int     boxSelectCurSX   = 0, boxSelectCurSY   = 0;

    private float gridSnap = 20f;
    public float  getGridSnap()        { return gridSnap; }
    public void   setGridSnap(float v) { gridSnap = Math.max(5f, v); }
    public void   cycleGridSnap() {
        float[] sizes = {5f, 10f, 20f, 40f, 80f};
        for (int i = 0; i < sizes.length; i++) {
            if (Math.abs(gridSnap - sizes[i]) < 1f) {
                gridSnap = sizes[(i + 1) % sizes.length]; return;
            }
        }
        gridSnap = 20f;
    }

    private int currentFloor = 1;
    public int  getCurrentFloor()      { return currentFloor; }
    public void setCurrentFloor(int f) { currentFloor = Math.max(1, f); }
    public void nextFloor()            { currentFloor++; showStatus("Floor: " + currentFloor); }
    public void prevFloor()            { if (currentFloor > 1) { currentFloor--; showStatus("Floor: " + currentFloor); } }

    private final ShapeEditState shapeEdit = new ShapeEditState();
    public  ShapeEditState getShapeEdit()  { return shapeEdit; }

    private final PathEditState pathEdit = new PathEditState();
    public  PathEditState getPathEdit()   { return pathEdit; }

    // ── Entrance edit state ───────────────────────────────────────────
    public static class EntranceEditState {
        public EntranceData selected      = null;
        public EntranceData ghost         = null;
        public boolean      dragging      = false;

        // Scene picker
        public boolean        pickerOpen  = false;
        public List<String>   sceneIds    = new ArrayList<>();
        public int            pickerHover = -1;   // index of hovered item in picker

        public void clear() {
            selected = null; ghost = null; dragging = false;
            pickerOpen = false; pickerHover = -1;
        }
        public void openPicker(List<String> ids) {
            pickerOpen = true; sceneIds = ids; pickerHover = -1;
        }
        public void closePicker() { pickerOpen = false; pickerHover = -1; }
    }

    private final EntranceEditState entranceEdit = new EntranceEditState();
    public  EntranceEditState getEntranceEdit()  { return entranceEdit; }

    private final LayerManager layerManager = new LayerManager();
    public  LayerManager getLayerManager()  { return layerManager; }

    private final PropertyPanel propertyPanel = new PropertyPanel();
    public  PropertyPanel getPropertyPanel()  { return propertyPanel; }

    private final List<List<BuildingData>> undoStack = new ArrayList<>();
    private static final int MAX_UNDO = 30;

    public void pushUndo(List<BuildingData> snapshot) {
        if (undoStack.size() >= MAX_UNDO) undoStack.remove(0);
        undoStack.add(new ArrayList<>(snapshot));
    }
    public List<BuildingData> popUndo() {
        return undoStack.isEmpty() ? null : undoStack.remove(undoStack.size() - 1);
    }
    public boolean canUndo() { return !undoStack.isEmpty(); }

    private String statusMsg    = "";
    private long   statusExpiry = 0L;
    public void   showStatus(String msg) { statusMsg = msg; statusExpiry = System.nanoTime() + 3_000_000_000L; }
    public String getStatusMessage()     { return System.nanoTime() < statusExpiry ? statusMsg : ""; }
}