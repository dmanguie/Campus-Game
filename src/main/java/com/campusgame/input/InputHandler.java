package com.campusgame.input;

import com.campusgame.editor.EditorMode;
import com.campusgame.editor.EditorState;
import com.campusgame.editor.PathEditState;
import com.campusgame.editor.ShapeEditState;
import com.campusgame.entities.Player;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * INPUT HANDLER  (input/InputHandler.java)
 * ------------------------------------------
 * Key bindings:
 *
 *   Always in editor:
 *     F1          → toggle editor on/off
 *     P           → PLACE tool
 *     S / Ctrl+S  → SELECT tool / save
 *     X           → DELETE tool
 *     V           → SHAPE_EDIT tool (needs a building selected first)
 *     R           → PATH_EDIT tool
 *     Ctrl+Z      → undo
 *     [ ] - =     → resize selected building (rect only)
 *     Q / E       → rotate selected building (rect only)
 *
 *   Extra keys active only while SHAPE_EDIT tool is selected:
 *     1           → preset Square
 *     2           → preset Rectangle
 *     3           → preset Circle 8-pt
 *     4           → preset Circle 16-pt
 *     5           → preset New Polygon
 *     Enter       → commit shape
 *     Escape      → cancel shape edit
 *     Delete      → delete hovered vertex
 *
 *   Extra keys active only while PATH_EDIT tool is selected:
 *     Enter       → finish / commit path
 *     Escape      → cancel path edit
 *     Delete      → delete hovered waypoint
 */
public class InputHandler extends KeyAdapter {

    private boolean up, down, left, right, sprint;
    private EditorMode editor = null;

    public void setEditor(EditorMode editor) { this.editor = editor; }

    @Override
    public void keyPressed(KeyEvent e) {
        int     key  = e.getKeyCode();
        boolean ctrl = e.isControlDown();

        // ── F1: toggle editor (always checked first) ──────────────────
        if (key == KeyEvent.VK_F1 && editor != null) {
            editor.toggleEditor();
            return;
        }

        if (editor != null && editor.isActive()) {

            // ── Shape-edit sub-keys (only when SHAPE_EDIT tool active) ─
            if (editor.getState().getCurrentTool() == EditorState.Tool.SHAPE_EDIT) {
                switch (key) {
                    case KeyEvent.VK_1      -> editor.applyShapePreset(ShapeEditState.ShapePreset.SQUARE);
                    case KeyEvent.VK_2      -> editor.applyShapePreset(ShapeEditState.ShapePreset.RECTANGLE);
                    case KeyEvent.VK_3      -> editor.applyShapePreset(ShapeEditState.ShapePreset.CIRCLE_8);
                    case KeyEvent.VK_4      -> editor.applyShapePreset(ShapeEditState.ShapePreset.CIRCLE_16);
                    case KeyEvent.VK_5      -> editor.applyShapePreset(ShapeEditState.ShapePreset.POLYGON);
                    case KeyEvent.VK_ENTER  -> editor.commitShapeEdit();
                    case KeyEvent.VK_ESCAPE -> editor.cancelShapeEdit();
                    case KeyEvent.VK_DELETE -> {
                        int hov = editor.getState().getShapeEdit().getHoveredVertex();
                        if (hov >= 0) editor.getState().getShapeEdit().deleteVertex(hov);
                    }
                }
                if (ctrl && key == KeyEvent.VK_Z) { editor.undo(); return; }
                if (ctrl && key == KeyEvent.VK_S) { editor.saveMap(); return; }
                return;
            }

            // ── Path-edit sub-keys (only when PATH_EDIT tool active) ──
            // Does NOT return at the end — tool-switching keys (P/S/X/V/R)
            // fall through to the general switch so you can leave PATH_EDIT.
            if (editor.getState().getCurrentTool() == EditorState.Tool.PATH_EDIT) {
                if (ctrl && key == KeyEvent.VK_Z) { editor.undo(); return; }
                if (ctrl && key == KeyEvent.VK_S) { editor.saveMap(); return; }
                switch (key) {
                    case KeyEvent.VK_ENTER  -> { editor.commitPathEdit(); return; }
                    case KeyEvent.VK_ESCAPE -> { editor.cancelPathEdit(); return; }
                    case KeyEvent.VK_A -> {
                        PathEditState pe = editor.getState().getPathEdit();
                        if (pe.isActive()) {
                            boolean nowAppend = !pe.isAppendMode();
                            pe.setAppendMode(nowAppend);
                            editor.getState().showStatus(nowAppend
                                    ? "Append mode ON — click to add waypoints, Enter=finish"
                                    : "Append mode OFF — drag to move points, Del=delete path");
                        }
                        return;
                    }
                    case KeyEvent.VK_DELETE -> {
                        int hov = editor.getState().getPathEdit().getHoveredPoint();
                        if (hov >= 0) {
                            editor.getState().getPathEdit().deletePoint(hov);
                        } else {
                            editor.deleteSelectedPath();
                        }
                        return;
                    }
                }
                // Any other key (P, S, X, V, R) falls through to the general switch
            }

            // ── General editor keys ───────────────────────────────────
            switch (key) {
                case KeyEvent.VK_P -> editor.switchToPlace();
                case KeyEvent.VK_S -> { if (ctrl) editor.saveMap(); else editor.switchToSelect(); }
                case KeyEvent.VK_X -> editor.switchToDelete();
                case KeyEvent.VK_V -> editor.switchToShapeEdit();
                case KeyEvent.VK_R -> editor.switchToPathEdit();
                case KeyEvent.VK_Z -> { if (ctrl) editor.undo(); }

                case KeyEvent.VK_OPEN_BRACKET  -> editor.resizeSelected(-10,   0);
                case KeyEvent.VK_CLOSE_BRACKET -> editor.resizeSelected( 10,   0);
                case KeyEvent.VK_MINUS         -> editor.resizeSelected(  0, -10);
                case KeyEvent.VK_EQUALS        -> editor.resizeSelected(  0,  10);

                case KeyEvent.VK_Q -> editor.rotateSelected(-5);
                case KeyEvent.VK_E -> editor.rotateSelected( 5);
            }
            return;
        }

        // ── Normal player movement ────────────────────────────────────
        switch (key) {
            case KeyEvent.VK_W, KeyEvent.VK_UP    -> up     = true;
            case KeyEvent.VK_S, KeyEvent.VK_DOWN  -> down   = true;
            case KeyEvent.VK_A, KeyEvent.VK_LEFT  -> left   = true;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> right  = true;
            case KeyEvent.VK_SHIFT                -> sprint = true;
        }
    }

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

    public void applyToPlayer(Player player) {
        if (editor != null && editor.isActive()) {
            player.vx = 0; player.vy = 0; return;
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

    public boolean isUp()     { return up;     }
    public boolean isDown()   { return down;   }
    public boolean isLeft()   { return left;   }
    public boolean isRight()  { return right;  }
    public boolean isSprint() { return sprint; }
}