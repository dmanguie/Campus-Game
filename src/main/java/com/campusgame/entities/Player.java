package com.campusgame.entities;

import com.campusgame.core.GameObject;

import java.awt.*;

/**
 * PLAYER (entities/Player.java)
 * ------------------------------
 * Represents the player character on the campus map.
 *
 * PHASE 2 CHANGE:
 *   Now extends GameObject, giving it a shared identity, tag, and
 *   world position (x, y, z) compatible with the 3D coordinate system.
 *
 *   In 2D rendering: x → screen X, z → screen Y (y is ignored).
 *   In 3D rendering: x, y, z used directly.
 *
 * BACKWARD COMPATIBILITY:
 *   getCenterX() and getCenterY() still return the 2D screen coords
 *   so Renderer.java and Camera.java are UNCHANGED.
 *
 *   CollisionManager still works — getBounds() still returns a Rectangle
 *   based on x (world X) and z (world Z mapped to screen Y).
 */
public class Player extends GameObject {

    // --- Movement ---
    public float vx, vy;        // velocity (pixels/second) — vy maps to world Z velocity

    public static final float SPEED = 200f; // pixels per second

    // --- Visual / Collision Size ---
    public static final int WIDTH  = 20;
    public static final int HEIGHT = 20;

    // --- Visual Style ---
    private static final Color BODY_COLOR    = new Color(52, 152, 219);
    private static final Color OUTLINE_COLOR = new Color(21, 67, 101);
    private static final Color DOT_COLOR     = new Color(236, 240, 241);

    // --- Facing direction ---
    private float facingAngle = 0f;

    public Player(float startX, float startZ) {
        // PHASE 2: call GameObject constructor with tag "player"
        // startX → world X, startZ → world Z (screen Y in 2D)
        super(startX, startZ, "player");
        // Note: GameObject sets this.x = startX, this.z = startZ
        // In 2D rendering, we use x for screen X and z for screen Y
    }

    /**
     * Called every frame by GameLoop.
     * Moves player by velocity * delta time.
     * vx → world X movement, vy → world Z movement (screen Y in 2D).
     */
    @Override
    public void update(float delta) {
        x += vx * delta;   // world X (screen X in 2D)
        z += vy * delta;   // world Z (screen Y in 2D) — vy is the "up/down" input

        if (vx != 0 || vy != 0) {
            facingAngle = (float) Math.toDegrees(Math.atan2(vy, vx)) + 90f;
        }
    }

    /**
     * Returns the AABB collision rectangle (world space, 2D).
     * Uses x (world X) and z (world Z = screen Y in 2D).
     */
    public Rectangle getBounds() {
        return new Rectangle(
                (int)(x - WIDTH  / 2f),
                (int)(z - HEIGHT / 2f),   // z maps to screen Y in 2D
                WIDTH,
                HEIGHT
        );
    }

    /**
     * Draws the player at its camera-adjusted screen position.
     */
    public void draw(Graphics2D g, int screenX, int screenY) {
        g.setColor(new Color(0, 0, 0, 60));
        g.fillOval(screenX - WIDTH/2 + 2, screenY - HEIGHT/2 + 4, WIDTH, HEIGHT);

        g.setColor(BODY_COLOR);
        g.fillOval(screenX - WIDTH/2, screenY - HEIGHT/2, WIDTH, HEIGHT);

        g.setColor(OUTLINE_COLOR);
        g.setStroke(new BasicStroke(2));
        g.drawOval(screenX - WIDTH/2, screenY - HEIGHT/2, WIDTH, HEIGHT);

        double rad = Math.toRadians(facingAngle - 90);
        int dotX = (int)(screenX + Math.cos(rad) * 5);
        int dotY = (int)(screenY + Math.sin(rad) * 5);
        g.setColor(DOT_COLOR);
        g.fillOval(dotX - 3, dotY - 3, 6, 6);

        g.setStroke(new BasicStroke(1));
    }

    // --- 2D-compatible getters (Renderer and Camera unchanged) ---

    /** Screen X in 2D = world X */
    public float getCenterX() { return x; }

    /** Screen Y in 2D = world Z */
    public float getCenterY() { return z; }

    public void setPosition(float nx, float nz) {
        this.x = nx;
        this.z = nz;
    }
}