package com.campusgame.editor;

import com.campusgame.renderer.Camera;
import com.campusgame.world.interior.InteriorRoom;
import com.campusgame.world.interior.InteriorScene;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * INTERIOR EDITOR MODE  (editor/InteriorEditorMode.java)
 *
 * Mirrors the exterior EditorMode but operates on InteriorScene rooms.
 * Activated by pressing F1 while the player is indoors.
 *
 * Tools
 * -----
 *   P  — PLACE   : click to drop a new room rectangle
 *   S  — SELECT  : click to select; drag to move; handles to resize
 *   X  — DELETE  : click to remove a room
 *   N  — RENAME  : (called externally) JOptionPane rename of selected room
 *   Esc          : deselect / cancel
 *
 * Architecture notes
 * ------------------
 *  - Works entirely in interior scene coordinate space (same coords the Camera uses
 *    while indoors, i.e. worldX/worldZ in [0..scene.width, 0..scene.height]).
 *  - Does NOT touch campus.json — interior layouts are registered in
 *    InteriorSceneRegistry (code).  A JSON round-trip for rooms is future work;
 *    the hook points are marked TODO_SAVE.
 *  - The four resize handles (NW / NE / SW / SE) are 10×10 squares drawn by
 *    InteriorEditorOverlayRenderer and hit-tested here.
 */
public class InteriorEditorMode implements Renameable {

    public enum Tool { PLACE, SELECT, DELETE }

    // ── State ─────────────────────────────────────────────────────────
    private boolean       active      = false;
    private Tool          currentTool = Tool.PLACE;
    private InteriorScene scene       = null;   // scene being edited

    private InteriorRoom selected = null;
    private InteriorRoom ghost    = null;   // preview while placing

    // Drag-move
    private boolean dragActive  = false;
    private float   dragStartWX, dragStartWZ;   // world click origin
    private float   roomOriginX, roomOriginZ;   // room's position at drag start

    // Resize  (handle indices 0=NW 1=NE 2=SW 3=SE)
    private int   activeHandle  = -1;
    private float resizeAnchorX, resizeAnchorZ;  // world position of opposite corner
    private float resizeDragX,   resizeDragZ;    // current drag world pos

    // Grid snap
    private float gridSnap = 20f;

    // Room counter for default names
    private int roomCounter = 1;

    // Status message (shown by overlay)
    private String statusMsg    = "";
    private long   statusExpiry = 0L;

    // Default room appearance
    private static final Color DEFAULT_FLOOR = new Color(215, 208, 196);
    private static final Color DEFAULT_WALL  = new Color(100, 85, 70);
    private static final float DEFAULT_W     = 200f;
    private static final float DEFAULT_D     = 160f;

    // Resize handle hit-test radius (world units)
    public static final float HANDLE_HIT = 12f;

    private final Camera camera;

    public InteriorEditorMode(Camera camera) {
        this.camera = camera;
    }

    // ── Activation ────────────────────────────────────────────────────

    public void toggle(InteriorScene currentScene) {
        active = !active;
        if (active) {
            scene        = currentScene;
            selected     = null;
            ghost        = null;
            dragActive   = false;
            activeHandle = -1;
            currentTool  = Tool.PLACE;
            roomCounter  = (scene != null ? scene.rooms.size() : 0) + 1;
            showStatus("INTERIOR EDITOR  P=Place  S=Select  X=Delete  N=Rename  F1=Exit  F3=Controls");
        } else {
            scene    = null;
            selected = null;
            ghost    = null;
            showStatus("Interior editor OFF");
        }
    }

    public boolean       isActive()      { return active;      }
    public Tool          getCurrentTool(){ return currentTool; }
    public InteriorScene getScene()      { return scene;       }
    public InteriorRoom  getSelected()   { return selected;    }
    public InteriorRoom  getGhost()      { return ghost;       }
    public int           getActiveHandle(){ return activeHandle; }
    public float         getGridSnap()   { return gridSnap;    }

    // ── Tool switching ────────────────────────────────────────────────

    public void switchToPlace() {
        currentTool  = Tool.PLACE;
        selected     = null;
        dragActive   = false;
        activeHandle = -1;
        showStatus("PLACE — click to drop a room");
    }

    public void switchToSelect() {
        currentTool = Tool.SELECT;
        ghost       = null;
        showStatus("SELECT — click room to select  drag to move");
    }

    public void switchToDelete() {
        currentTool = Tool.DELETE;
        selected    = null;
        ghost       = null;
        showStatus("DELETE — click a room to remove it");
    }

    public void escape() {
        selected     = null;
        ghost        = null;
        dragActive   = false;
        activeHandle = -1;
        showStatus("Deselected");
    }

    // ── Mouse events ──────────────────────────────────────────────────

    public void onMouseMoved(int screenX, int screenY) {
        if (!active) return;
        float wx = snap(camera.screenToWorldX(screenX));
        float wz = snap(camera.screenToWorldY(screenY));
        ghost = (currentTool == Tool.PLACE) ? makeGhost(wx, wz) : null;
    }

    public void onMouseDragged(int screenX, int screenY) {
        if (!active) return;
        float wx = snap(camera.screenToWorldX(screenX));
        float wz = snap(camera.screenToWorldY(screenY));

        if (activeHandle >= 0 && selected != null) {
            resizeDragX = wx;
            resizeDragZ = wz;
            applyResizePreview();
            return;
        }

        if (dragActive && selected != null) {
            float dx = wx - dragStartWX;
            float dz = wz - dragStartWZ;
            selected = mutateRoom(selected,
                    snap(roomOriginX + dx), snap(roomOriginZ + dz),
                    selected.width, selected.depth);
            replaceSelectedInScene();
        }
    }

    public void onLeftClick(int screenX, int screenY) {
        if (!active) return;
        float wx = snap(camera.screenToWorldX(screenX));
        float wz = snap(camera.screenToWorldY(screenY));

        switch (currentTool) {
            case PLACE  -> placeRoom(wx, wz);
            case SELECT -> selectOrStartDrag(wx, wz, screenX, screenY);
            case DELETE -> deleteAt(wx, wz);
        }
    }

    public void onLeftRelease(int screenX, int screenY) {
        if (!active) return;
        if (activeHandle >= 0) {
            activeHandle = -1;
            showStatus("Resized — N=rename  Del=delete  Esc=deselect");
        }
        dragActive = false;
    }

    // ── Rename (Renameable interface, N key) ──────────────────────────

    /**
     * Renames the currently selected InteriorRoom.
     * InteriorRoom is immutable so mutateRoom() produces a replacement instance.
     * Called by InputHandler via the Renameable interface.
     */
    @Override
    public void renameSelected(java.awt.Component parent) {
        if (selected == null) {
            showStatus("Select a room first (S), then press N");
            return;
        }
        String input = JOptionPane.showInputDialog(
                parent, "Rename room:", selected.displayName);
        if (input == null || input.isBlank()) return;
        String trimmed = input.trim();
        selected = mutateRoom(selected,
                selected.x, selected.z, selected.width, selected.depth,
                trimmed, selected.floorColor, selected.wallColor);
        replaceSelectedInScene();
        showStatus("Renamed to: " + trimmed);
        // TODO_SAVE: wire to interior JSON persistence when that system is extended
    }

    // ── Delete selected ───────────────────────────────────────────────

    public void deleteSelected() {
        if (selected == null) {
            showStatus("No room selected — click one first (S)");
            return;
        }
        if (scene != null) scene.rooms.remove(selected);
        showStatus("Deleted room: " + selected.displayName);
        selected = null;
        // TODO_SAVE: persist
    }

    // ── Internal helpers ──────────────────────────────────────────────

    private void placeRoom(float wx, float wz) {
        if (scene == null) return;
        String name = "Room " + roomCounter++;
        InteriorRoom r = new InteriorRoom(
                "room_new_" + (roomCounter - 1), name,
                wx, wz, DEFAULT_W, DEFAULT_D,
                DEFAULT_FLOOR, DEFAULT_WALL);
        scene.rooms.add(r);
        selected = r;
        showStatus("Placed: " + name + "  —  S=select  N=rename");
        // TODO_SAVE: persist
    }

    private void selectOrStartDrag(float wx, float wz, int screenX, int screenY) {
        // 1. Check resize handles on currently selected room first
        if (selected != null) {
            int hi = hitTestHandles(selected, screenX, screenY);
            if (hi >= 0) {
                activeHandle  = hi;
                resizeDragX   = wx;
                resizeDragZ   = wz;
                resizeAnchorX = (hi == 0 || hi == 2) ? selected.x + selected.width : selected.x;
                resizeAnchorZ = (hi == 0 || hi == 1) ? selected.z + selected.depth : selected.z;
                return;
            }
        }

        // 2. Hit-test rooms
        InteriorRoom hit = hitTest(wx, wz);
        if (hit != null) {
            selected    = hit;
            dragActive  = true;
            dragStartWX = wx;
            dragStartWZ = wz;
            roomOriginX = hit.x;
            roomOriginZ = hit.z;
            showStatus("Selected: " + hit.displayName
                    + "  drag=move  N=rename  Del=delete");
        } else {
            selected   = null;
            dragActive = false;
        }
    }

    private void deleteAt(float wx, float wz) {
        if (scene == null) return;
        InteriorRoom hit = hitTest(wx, wz);
        if (hit == null) { showStatus("No room there"); return; }
        scene.rooms.remove(hit);
        if (selected == hit) selected = null;
        showStatus("Deleted: " + hit.displayName);
        // TODO_SAVE: persist
    }

    /**
     * Applies in-progress resize to the selected room using the drag world position.
     * Does NOT commit until mouse release — but we update the room reference live
     * so the renderer always shows the current size.
     */
    private void applyResizePreview() {
        if (selected == null || scene == null) return;

        float ax = resizeAnchorX, az = resizeAnchorZ;
        float bx = snap(resizeDragX), bz = snap(resizeDragZ);

        float newX = Math.min(ax, bx);
        float newZ = Math.min(az, bz);
        float newW = Math.max(40f, Math.abs(bx - ax));
        float newD = Math.max(40f, Math.abs(bz - az));

        selected = mutateRoom(selected, newX, newZ, newW, newD);
        replaceSelectedInScene();
    }

    // ── Hit testing ───────────────────────────────────────────────────

    private InteriorRoom hitTest(float wx, float wz) {
        if (scene == null) return null;
        List<InteriorRoom> rooms = scene.rooms;
        for (int i = rooms.size() - 1; i >= 0; i--) {
            InteriorRoom r = rooms.get(i);
            if (wx >= r.x && wx <= r.x + r.width
                    && wz >= r.z && wz <= r.z + r.depth) return r;
        }
        return null;
    }

    /**
     * Returns handle index (0=NW 1=NE 2=SW 3=SE) if the given screen pixel
     * is within HANDLE_HIT pixels of a corner of {@code room}, or -1.
     */
    public int hitTestHandles(InteriorRoom room, int screenX, int screenY) {
        int[][] corners = handleScreenCorners(room);
        for (int i = 0; i < corners.length; i++) {
            int dx = corners[i][0] - screenX;
            int dz = corners[i][1] - screenY;
            if (dx * dx + dz * dz <= (int)(HANDLE_HIT * HANDLE_HIT)) return i;
        }
        return -1;
    }

    /** Screen positions of the four resize handles for a room. */
    public int[][] handleScreenCorners(InteriorRoom room) {
        int x1 = camera.worldToScreenX(room.x);
        int z1 = camera.worldToScreenY(room.z);
        int x2 = camera.worldToScreenX(room.x + room.width);
        int z2 = camera.worldToScreenY(room.z + room.depth);
        return new int[][]{{x1, z1}, {x2, z1}, {x1, z2}, {x2, z2}};
    }

    // ── Mutation helpers (InteriorRoom is immutable by design) ────────

    /**
     * Creates a replacement room with new position/size but copies
     * id, displayName, and colors from {@code src}.
     */
    private InteriorRoom mutateRoom(InteriorRoom src,
                                    float x, float z, float w, float d) {
        return new InteriorRoom(src.id, src.displayName,
                x, z, w, d, src.floorColor, src.wallColor);
    }

    private InteriorRoom mutateRoom(InteriorRoom src,
                                    float x, float z, float w, float d,
                                    String newName, Color floor, Color wall) {
        return new InteriorRoom(src.id, newName, x, z, w, d, floor, wall);
    }

    /** Replaces the old instance of {@code selected} in the scene room list. */
    private void replaceSelectedInScene() {
        if (scene == null || selected == null) return;
        List<InteriorRoom> rooms = scene.rooms;
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).id.equals(selected.id)) {
                rooms.set(i, selected);
                return;
            }
        }
        // Not found (e.g. just placed) — add it
        rooms.add(selected);
    }

    // ── Ghost ─────────────────────────────────────────────────────────

    private InteriorRoom makeGhost(float wx, float wz) {
        return new InteriorRoom("__ghost__", "New Room",
                wx, wz, DEFAULT_W, DEFAULT_D, DEFAULT_FLOOR, DEFAULT_WALL);
    }

    // ── Grid snap ─────────────────────────────────────────────────────

    private float snap(float v) { return Math.round(v / gridSnap) * gridSnap; }

    public void cycleGridSnap() {
        float[] sizes = {5f, 10f, 20f, 40f, 80f};
        for (int i = 0; i < sizes.length; i++) {
            if (Math.abs(gridSnap - sizes[i]) < 1f) {
                gridSnap = sizes[(i + 1) % sizes.length];
                showStatus("Grid snap: " + (int) gridSnap);
                return;
            }
        }
        gridSnap = 20f;
    }

    // ── Status ────────────────────────────────────────────────────────

    public void showStatus(String msg) {
        statusMsg    = msg;
        statusExpiry = System.nanoTime() + 3_500_000_000L;
    }

    public String getStatusMessage() {
        return System.nanoTime() < statusExpiry ? statusMsg : "";
    }
}