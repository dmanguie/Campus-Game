package com.campusgame.input;

import com.campusgame.editor.EditorMode;
import com.campusgame.entities.Player;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * INPUT HANDLER (input/InputHandler.java)
 * Phase 3 changes:
 *  - F1          → toggleEditor()
 *  - P           → editor PLACE tool
 *  - X           → editor DELETE tool
 *  - Ctrl+S      → editor saveMap()
 *  - Ctrl+Z      → editor undo()
 *  - WASD blocked while editor is active (player stays still)
 * Everything else UNCHANGED.
 */
public class InputHandler extends KeyAdapter {

    private boolean up, down, left, right, sprint;

    // Phase 3: optional editor reference (null = no editor wired)
    private EditorMode editor = null;

    public void setEditor(EditorMode editor) { this.editor = editor; }

    @Override
    public void keyPressed(KeyEvent e) {
        System.out.println("Key pressed: " + e.getKeyCode());
        int  key  = e.getKeyCode();
        boolean ctrl = e.isControlDown();

        // ── Editor hotkeys ────────────────────────────────────────────
        if (key == KeyEvent.VK_F1 && editor != null) {
            editor.toggleEditor();
            return;
        }
        if (editor != null && editor.isActive()) {
            switch (key) {
                case KeyEvent.VK_P                -> editor.switchToPlace();
                case KeyEvent.VK_X                -> editor.switchToDelete();
                case KeyEvent.VK_S  -> { if (ctrl) editor.saveMap(); }
                case KeyEvent.VK_Z  -> { if (ctrl) editor.undo();    }
            }
            // Block WASD while editor is open
            return;
        }

        // ── Normal movement ───────────────────────────────────────────
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
        // Stop player while editor is open
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