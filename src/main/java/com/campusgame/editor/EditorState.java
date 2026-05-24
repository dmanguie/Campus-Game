package com.campusgame.editor;

import com.campusgame.map.data.BuildingData;
import java.util.ArrayList;
import java.util.List;

/**
 * EDITOR STATE (editor/EditorState.java)
 * Pure data bag — no rendering, no Swing.
 * Holds all mutable editor UI state consumed by EditorMode (logic)
 * and EditorOverlayRenderer (drawing).
 */
public class EditorState {

    public enum Tool { PLACE, SELECT, DELETE }

    // ── Active ────────────────────────────────────────────────────────
    private boolean active = false;
    public boolean  isActive()           { return active; }
    public void     setActive(boolean v) { active = v; }
    public void     toggle() {
        active = !active;
        showStatus(active ? "[ EDITOR ON ]  P=Place  X=Delete  Ctrl+S=Save  Ctrl+Z=Undo" : "[ EDITOR OFF ]");
    }

    // ── Tool ──────────────────────────────────────────────────────────
    private Tool currentTool = Tool.PLACE;
    public Tool  getCurrentTool()       { return currentTool; }
    public void  setCurrentTool(Tool t) { currentTool = t; showStatus("Tool: " + t); }

    // ── Default new-building template ─────────────────────────────────
    public static final float  DEFAULT_WIDTH  = 120f;
    public static final float  DEFAULT_DEPTH  = 100f;
    public static final int    DEFAULT_FLOORS = 2;
    public static final int    DEFAULT_COLOR  = 0xFF888888;
    public static final String DEFAULT_TAG    = "new";

    // ── Ghost building (placement preview) ────────────────────────────
    private BuildingData ghostBuilding = null;
    public BuildingData getGhostBuilding()              { return ghostBuilding; }
    public void         setGhostBuilding(BuildingData b){ ghostBuilding = b; }

    // ── Cursor world position ─────────────────────────────────────────
    private float cursorWorldX, cursorWorldZ;
    public float getCursorWorldX() { return cursorWorldX; }
    public float getCursorWorldZ() { return cursorWorldZ; }
    public void  setCursorWorld(float x, float z) { cursorWorldX = x; cursorWorldZ = z; }

    // ── Selection ─────────────────────────────────────────────────────
    private BuildingData selectedBuilding = null;
    public BuildingData getSelectedBuilding()              { return selectedBuilding; }
    public void         setSelectedBuilding(BuildingData b){ selectedBuilding = b; }

    // ── Undo stack ────────────────────────────────────────────────────
    private final List<List<BuildingData>> undoStack = new ArrayList<>();
    private static final int MAX_UNDO = 20;

    public void pushUndo(List<BuildingData> snapshot) {
        if (undoStack.size() >= MAX_UNDO) undoStack.remove(0);
        undoStack.add(new ArrayList<>(snapshot));
    }
    public List<BuildingData> popUndo() {
        return undoStack.isEmpty() ? null : undoStack.remove(undoStack.size() - 1);
    }
    public boolean canUndo() { return !undoStack.isEmpty(); }

    // ── Status message (shown in HUD for 3s) ─────────────────────────
    private String statusMsg    = "";
    private long   statusExpiry = 0L;

    public void   showStatus(String msg) {
        statusMsg    = msg;
        statusExpiry = System.nanoTime() + 3_000_000_000L;
    }
    public String getStatusMessage() {
        return System.nanoTime() < statusExpiry ? statusMsg : "";
    }
}