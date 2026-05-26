package com.campusgame.input;

import com.campusgame.editor.EditorMode;
import com.campusgame.editor.EditorState;
import com.campusgame.editor.InteriorEditorMode;
import com.campusgame.editor.Renameable;
import com.campusgame.editor.ShapeEditState;
import com.campusgame.editor.layer.LayerManager;
import com.campusgame.entities.Player;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * INPUT HANDLER (input/InputHandler.java)
 *
 * Phase 5 : added setOnInteract ([E] key) and setMovementBlocked (during transitions).
 * Phase 6 : added InteriorEditorMode support; rerouted N key through Renameable;
 *           ENTRANCE tool now routes N → renameSelectedEntrance instead of re-opening tool.
 */
public class InputHandler extends KeyAdapter {

    private boolean up, down, left, right, sprint;

    private EditorMode         editor          = null;
    private InteriorEditorMode interiorEditor  = null;
    private Component          windowComponent = null;   // JOptionPane parent

    private Runnable onInteractPressed = null;
    private boolean  movementBlocked   = false;

    // ── Wiring ────────────────────────────────────────────────────────

    public void setEditor(EditorMode editor)             { this.editor          = editor;          }
    public void setInteriorEditor(InteriorEditorMode ie) { this.interiorEditor  = ie;              }
    public void setWindowComponent(Component c)          { this.windowComponent = c;               }
    public void setOnInteract(Runnable r)                { this.onInteractPressed = r;             }
    public void setMovementBlocked(boolean blocked)      { this.movementBlocked   = blocked;       }

    private com.campusgame.editor.ControlsOverlay controlsOverlay = null;
    public void setControlsOverlay(com.campusgame.editor.ControlsOverlay o) {
        this.controlsOverlay = o;
    }
    // ── Key pressed ───────────────────────────────────────────────────

    @Override
    public void keyPressed(KeyEvent e) {
        int     key  = e.getKeyCode();
        boolean ctrl = e.isControlDown();
        boolean alt  = e.isAltDown();

        // F1: toggle editor (interior takes priority if active)
        if (key == KeyEvent.VK_F1) {
            if (interiorEditor != null && interiorEditor.isActive()) {
                interiorEditor.toggle(null);
                return;
            }
            if (editor != null) { editor.toggleEditor(); return; }
        }

        if (key == KeyEvent.VK_F2) {
            if (controlsOverlay != null) controlsOverlay.toggleGameplay();
            return;
        }
        if (key == KeyEvent.VK_F3) {
            if (controlsOverlay != null) controlsOverlay.toggleEditor();
            return;
        }

        // Interior editor
        if (interiorEditor != null && interiorEditor.isActive()) {
            handleInteriorEditorKeys(key);
            return;
        }

        // Exterior editor
        if (editor != null && editor.isActive()) {
            handleExteriorEditorKeys(key, ctrl, alt);
            return;
        }

        // Normal gameplay
        handleGameplayKeys(key);
    }

    // ── Interior editor ───────────────────────────────────────────────

    private void handleInteriorEditorKeys(int key) {
        switch (key) {
            case KeyEvent.VK_P      -> interiorEditor.switchToPlace();
            case KeyEvent.VK_S      -> interiorEditor.switchToSelect();
            case KeyEvent.VK_X      -> interiorEditor.switchToDelete();
            case KeyEvent.VK_G      -> interiorEditor.cycleGridSnap();
            case KeyEvent.VK_ESCAPE -> interiorEditor.escape();
            case KeyEvent.VK_DELETE -> interiorEditor.deleteSelected();
            case KeyEvent.VK_N      -> dispatchRename(interiorEditor);
        }
    }

    // ── Exterior editor ───────────────────────────────────────────────

    private void handleExteriorEditorKeys(int key, boolean ctrl, boolean alt) {
        EditorState      state = editor.getState();
        EditorState.Tool tool  = state.getCurrentTool();

        // SHAPE EDIT sub-mode
        if (tool == EditorState.Tool.SHAPE_EDIT) {
            if (ctrl && key == KeyEvent.VK_Z) { editor.undo();    return; }
            if (ctrl && key == KeyEvent.VK_S) { editor.saveMap(); return; }
            switch (key) {
                case KeyEvent.VK_1      -> editor.applyShapePreset(ShapeEditState.ShapePreset.SQUARE);
                case KeyEvent.VK_2      -> editor.applyShapePreset(ShapeEditState.ShapePreset.RECTANGLE);
                case KeyEvent.VK_3      -> editor.applyShapePreset(ShapeEditState.ShapePreset.CIRCLE_8);
                case KeyEvent.VK_4      -> editor.applyShapePreset(ShapeEditState.ShapePreset.CIRCLE_16);
                case KeyEvent.VK_5      -> editor.applyShapePreset(ShapeEditState.ShapePreset.POLYGON);
                case KeyEvent.VK_ENTER  -> editor.commitShapeEdit();
                case KeyEvent.VK_ESCAPE -> editor.cancelShapeEdit();
                case KeyEvent.VK_DELETE -> {
                    int hov = state.getShapeEdit().getHoveredVertex();
                    if (hov >= 0) state.getShapeEdit().deleteVertex(hov);
                }
            }
            return;
        }

        // PATH EDIT sub-mode
        if (tool == EditorState.Tool.PATH_EDIT) {
            if (ctrl && key == KeyEvent.VK_Z) { editor.undo();    return; }
            if (ctrl && key == KeyEvent.VK_S) { editor.saveMap(); return; }
            switch (key) {
                case KeyEvent.VK_ENTER  -> { editor.commitPathEdit();  return; }
                case KeyEvent.VK_ESCAPE -> { editor.cancelPathEdit();  return; }
                case KeyEvent.VK_N      -> { dispatchRename(editor);   return; }
                case KeyEvent.VK_DELETE -> {
                    int hov = state.getPathEdit().getHoveredPoint();
                    if (hov >= 0) state.getPathEdit().deletePoint(hov);
                    else          editor.deleteSelectedPath();
                    return;
                }
            }
        }

        // ENTRANCE sub-mode
        if (tool == EditorState.Tool.ENTRANCE) {
            if (ctrl && key == KeyEvent.VK_S) { editor.saveMap(); return; }
            if (ctrl && key == KeyEvent.VK_Z) { editor.undo();    return; }
            switch (key) {
                case KeyEvent.VK_DELETE -> { editor.deleteSelectedEntrance(); return; }
                case KeyEvent.VK_ESCAPE -> { editor.escapeEntrance();         return; }
                case KeyEvent.VK_C      -> { editor.openScenePicker();        return; }
                case KeyEvent.VK_N      -> { dispatchRename(editor);          return; }
            }
            return;
        }

        // Rotate keys — only active when ROTATE tool is selected
        if (tool == EditorState.Tool.ROTATE) {
            if (key == KeyEvent.VK_Q) { editor.rotateSelected(-5); return; }
            if (key == KeyEvent.VK_E) { editor.rotateSelected( 5); return; }
        }

        // Layer hotkeys (Alt+1-5 = lock, 1-5 = visibility)
        LayerManager lm = state.getLayerManager();   // ← only ONE declaration, the duplicate is deleted
        if (alt) {
            String msg = switch (key) {
                case KeyEvent.VK_1 -> lm.handleLockHotkey(1);
                case KeyEvent.VK_2 -> lm.handleLockHotkey(2);
                case KeyEvent.VK_3 -> lm.handleLockHotkey(3);
                case KeyEvent.VK_4 -> lm.handleLockHotkey(4);
                case KeyEvent.VK_5 -> lm.handleLockHotkey(5);
                default            -> null;
            };
            if (msg != null) { state.showStatus(msg); return; }
        } else if (tool != EditorState.Tool.ENTRANCE) {
            String msg = switch (key) {
                case KeyEvent.VK_1 -> lm.handleVisibilityHotkey(1);
                case KeyEvent.VK_2 -> lm.handleVisibilityHotkey(2);
                case KeyEvent.VK_3 -> lm.handleVisibilityHotkey(3);
                case KeyEvent.VK_4 -> lm.handleVisibilityHotkey(4);
                case KeyEvent.VK_5 -> lm.handleVisibilityHotkey(5);
                default            -> null;
            };
            if (msg != null) { state.showStatus(msg); return; }
        }

        // General editor keys
        switch (key) {
            case KeyEvent.VK_Q -> editor.switchToSelect();
            case KeyEvent.VK_W -> editor.switchToMove();
            case KeyEvent.VK_E -> editor.switchToResize();
            case KeyEvent.VK_R -> editor.switchToRotate();
            case KeyEvent.VK_T -> editor.switchToPathEdit();
            case KeyEvent.VK_P -> editor.switchToPlace();
            case KeyEvent.VK_X -> editor.switchToDelete();
            case KeyEvent.VK_V -> editor.switchToShapeEdit();
            case KeyEvent.VK_S -> { if (ctrl) editor.saveMap(); else editor.switchToSelect(); }
            case KeyEvent.VK_G -> editor.cycleGridSnap();
            case KeyEvent.VK_Z -> { if (ctrl) editor.undo(); }
            case KeyEvent.VK_DELETE    -> editor.deleteSelected();
            case KeyEvent.VK_ESCAPE    -> editor.escape();
            case KeyEvent.VK_PAGE_UP   -> editor.nextFloor();
            case KeyEvent.VK_PAGE_DOWN -> editor.prevFloor();
            case KeyEvent.VK_N -> {
                if (state.getSelectedBuilding() != null) {
                    dispatchRename(editor);
                } else {
                    editor.switchToEntrance();
                }
            }
        }
    }

    // ── Gameplay ──────────────────────────────────────────────────────

    private void handleGameplayKeys(int key) {
        switch (key) {
            case KeyEvent.VK_W, KeyEvent.VK_UP    -> up     = true;
            case KeyEvent.VK_S, KeyEvent.VK_DOWN  -> down   = true;
            case KeyEvent.VK_A, KeyEvent.VK_LEFT  -> left   = true;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> right  = true;
            case KeyEvent.VK_SHIFT                -> sprint = true;
            case KeyEvent.VK_E -> { if (onInteractPressed != null) onInteractPressed.run(); }
        }
    }

    // ── Rename dispatch ───────────────────────────────────────────────

    private void dispatchRename(Renameable target) {
        target.renameSelected(windowComponent);   // null is fine: dialog centres on screen
    }

    // ── Key released ──────────────────────────────────────────────────

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W, KeyEvent.VK_UP    -> up     = false;
            case KeyEvent.VK_S, KeyEvent.VK_DOWN  -> down   = false;
            case KeyEvent.VK_A, KeyEvent.VK_LEFT  -> left   = false;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> right  = false;
            case KeyEvent.VK_SHIFT                -> sprint = false;
        }
    }

    // ── Player movement ───────────────────────────────────────────────

    public void applyToPlayer(Player player) {
        if ((editor         != null && editor.isActive())
                || (interiorEditor != null && interiorEditor.isActive())
                || movementBlocked) {
            player.vx = 0; player.vy = 0;
            return;
        }
        float speed = sprint ? Player.SPEED * 1.8f : Player.SPEED;
        float vx = 0, vy = 0;
        if (left)  vx -= speed;
        if (right) vx += speed;
        if (up)    vy -= speed;
        if (down)  vy += speed;
        if (vx != 0 && vy != 0) {
            float mag = (float) Math.sqrt(vx * vx + vy * vy);
            vx = vx / mag * speed;
            vy = vy / mag * speed;
        }
        player.vx = vx;
        player.vy = vy;
    }

    // ── Accessors ─────────────────────────────────────────────────────

    public boolean isUp()     { return up;     }
    public boolean isDown()   { return down;   }
    public boolean isLeft()   { return left;   }
    public boolean isRight()  { return right;  }
    public boolean isSprint() { return sprint; }
}