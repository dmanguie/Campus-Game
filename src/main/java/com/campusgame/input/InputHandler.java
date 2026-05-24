package com.campusgame.input;

import com.campusgame.entities.Player;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * INPUT HANDLER (input/InputHandler.java)
 * -----------------------------------------
 * Captures keyboard state and maps it to player velocity.
 *
 * Responsibilities:
 *  - Listens for KeyPressed / KeyReleased events via Swing KeyAdapter
 *  - Maintains a boolean flag for each key of interest
 *  - applyToPlayer() converts current key state into player vx/vy each frame
 *
 * Why flags instead of direct movement?
 *  - Flag-based input allows multiple keys simultaneously (diagonal movement)
 *  - Clean separation: input reads keys, GameLoop decides when to apply them
 *
 * WASD mapping:
 *   W → move up    (vy negative)
 *   S → move down  (vy positive)
 *   A → move left  (vx negative)
 *   D → move right (vx positive)
 *
 * Future expansion:
 *  - Add sprint (Shift), interact (E), map toggle (M)
 *  - Support gamepad via polling
 */
public class InputHandler extends KeyAdapter {

    // Key state flags
    private boolean up, down, left, right;
    private boolean sprint;

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
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

    /**
     * Called every frame by GameLoop.
     * Sets player velocity based on current key state.
     * Normalizes diagonal movement so speed is consistent.
     */
    public void applyToPlayer(Player player) {
        float speed = sprint ? Player.SPEED * 1.8f : Player.SPEED;

        float vx = 0, vy = 0;
        if (left)  vx -= speed;
        if (right) vx += speed;
        if (up)    vy -= speed;
        if (down)  vy += speed;

        // Normalize diagonal movement (prevent ~40% speed boost)
        if (vx != 0 && vy != 0) {
            float mag = (float) Math.sqrt(vx * vx + vy * vy);
            vx = (vx / mag) * speed;
            vy = (vy / mag) * speed;
        }

        player.vx = vx;
        player.vy = vy;
    }

    // Getters (useful for UI or debugging)
    public boolean isUp()     { return up;     }
    public boolean isDown()   { return down;   }
    public boolean isLeft()   { return left;   }
    public boolean isRight()  { return right;  }
    public boolean isSprint() { return sprint; }
}