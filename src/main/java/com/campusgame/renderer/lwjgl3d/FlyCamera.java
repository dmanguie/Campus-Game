package com.campusgame.renderer.lwjgl3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/**
 * FlyCamera
 * ──────────────────────────────────────────────────────────────────────────
 * First-person camera with two modes:
 *
 *   WALK mode (default)
 *   ───────────────────
 *   W / S          – move forward / back  (XZ plane only, ignores pitch)
 *   A / D          – strafe left / right
 *   Left Shift     – sprint (3× speed)
 *   Mouse          – look around (cursor captured, ESC releases)
 *   Scroll         – dolly forward/back along XZ only
 *   F              – toggle fly mode
 *
 *   FLY mode  (editor / overview)
 *   ──────────────────────────────
 *   Same as above, plus:
 *   Space          – move up
 *   Left Ctrl      – move down
 *   Movement follows full 3-D look direction (pitch included)
 *   F              – toggle back to walk mode
 *
 * Eye height in walk mode is fixed at WALK_EYE_HEIGHT above ground (y = 0).
 */
public class FlyCamera {

    // ── Constants ─────────────────────────────────────────────────────
    private static final float WALK_EYE_HEIGHT = 5.5f;   // eye level above ground
    private static final float PITCH_LIMIT_UP  = 70f;    // max look-up angle
    private static final float PITCH_LIMIT_DOWN = -70f;  // max look-down angle

    // ── State ─────────────────────────────────────────────────────────
    public final Vector3f position = new Vector3f(0f, WALK_EYE_HEIGHT, 100f);
    public float yaw   = 180f;   // degrees; 0 = +Z, 90 = +X
    public float pitch =  -15f;  // degrees; clamped by PITCH_LIMIT constants

    // ── Mode ──────────────────────────────────────────────────────────
    private boolean flyMode = false;

    // ── Settings ──────────────────────────────────────────────────────
    public float walkSpeed  = 30f;   // world units / second in walk mode
    public float flySpeed   = 60f;   // world units / second in fly mode
    public float mouseSens  = 0.10f; // degrees per pixel (lower = smoother)
    public float sprintMult = 3f;    // speed multiplier when holding Shift

    // ── Mouse state ───────────────────────────────────────────────────
    private double  lastMouseX = Double.NaN;
    private double  lastMouseY = Double.NaN;
    private boolean firstMove  = true;  // skip first delta to avoid snap on attach

    // ── Key edge detection (F key toggle) ─────────────────────────────
    private boolean fKeyWasDown = false;

    // ──────────────────────────────────────────────────────────────────

    /**
     * Call once after the GLFW window is created to register input callbacks.
     * Captures the cursor so mouse movement always rotates the camera.
     * Press ESC to release the cursor (which also closes the window).
     */
    public void attachTo(long window) {

        // Capture cursor — hides it and locks it to the window centre
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);

        // Cursor position — always active, no click required
        GLFW.glfwSetCursorPosCallback(window, (win, mx, my) -> {
            if (firstMove) {
                lastMouseX = mx;
                lastMouseY = my;
                firstMove  = false;
                return;
            }
            float dx = (float)(mx - lastMouseX) * mouseSens;
            float dy = (float)(my - lastMouseY) * mouseSens;
            lastMouseX = mx;
            lastMouseY = my;

            yaw -= dx;
            pitch -= dy;   // inverted Y: mouse up = look up
            pitch  = Math.max(PITCH_LIMIT_DOWN, Math.min(PITCH_LIMIT_UP, pitch));
        });

        // Scroll wheel – dolly along ground plane (XZ) regardless of pitch
        GLFW.glfwSetScrollCallback(window, (win, xoff, yoff) -> {
            float speed = flyMode ? flySpeed : walkSpeed;
            Vector3f flat = flatForward();
            position.x += flat.x * (float) yoff * speed * 0.3f;
            position.z += flat.z * (float) yoff * speed * 0.3f;
            if (!flyMode) position.y = WALK_EYE_HEIGHT;
        });
    }

    /**
     * Process keyboard movement.  Call every frame before rendering.
     *
     * @param window GLFW window handle
     * @param dt     delta time in seconds
     */
    public void update(long window, float dt) {

        // ── F key: toggle fly mode (edge-triggered) ───────────────────
        boolean fKeyDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F) == GLFW.GLFW_PRESS;
        if (fKeyDown && !fKeyWasDown) {
            flyMode = !flyMode;
            if (!flyMode) {
                // Snap back to ground on mode switch
                position.y = WALK_EYE_HEIGHT;
            }
            System.out.println("[Camera] " + (flyMode ? "FLY mode" : "WALK mode"));
        }
        fKeyWasDown = fKeyDown;

        // ── Speed ─────────────────────────────────────────────────────
        float baseSpeed = flyMode ? flySpeed : walkSpeed;
        boolean sprinting = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
        float moveSpeed = baseSpeed * dt * (sprinting ? sprintMult : 1f);

        // ── Direction vectors ─────────────────────────────────────────
        Vector3f moveDir; // forward vector used for W/S movement
        if (flyMode) {
            moveDir = fullForward();   // follows pitch in fly mode
        } else {
            moveDir = flatForward();   // locked to XZ in walk mode
        }
        Vector3f right = new Vector3f(moveDir).cross(new Vector3f(0, 1, 0)).normalize();

        // ── WASD movement ─────────────────────────────────────────────
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS)
            position.add(new Vector3f(moveDir).mul(moveSpeed));
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS)
            position.sub(new Vector3f(moveDir).mul(moveSpeed));
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS)
            position.sub(new Vector3f(right).mul(moveSpeed));
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS)
            position.add(new Vector3f(right).mul(moveSpeed));

        // ── Vertical movement (fly mode only) ─────────────────────────
        if (flyMode) {
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS)
                position.y += moveSpeed;
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS)
                position.y -= moveSpeed;
        }

        // ── Lock Y to ground in walk mode ─────────────────────────────
        if (!flyMode) {
            position.y = WALK_EYE_HEIGHT;
        }
    }

    // ── Public helpers ────────────────────────────────────────────────

    /** @return true if currently in fly/editor mode */
    public boolean isFlyMode() { return flyMode; }

    /** Programmatically set fly mode (e.g. from editor) */
    public void setFlyMode(boolean enabled) {
        flyMode = enabled;
        if (!flyMode) position.y = WALK_EYE_HEIGHT;
    }

    /**
     * Builds a view matrix and writes it into the provided Matrix4f.
     */
    public void buildViewMatrix(Matrix4f out) {
        Vector3f target = new Vector3f(position).add(fullForward());
        out.identity().lookAt(position, target, new Vector3f(0, 1, 0));
    }

    // ── Private direction helpers ─────────────────────────────────────

    /**
     * Full 3-D forward vector (includes pitch). Used for fly mode and
     * view matrix construction.
     */
    private Vector3f fullForward() {
        float yawR   = (float) Math.toRadians(yaw);
        float pitchR = (float) Math.toRadians(pitch);
        return new Vector3f(
                (float)(Math.cos(pitchR) * Math.sin(yawR)),
                (float) Math.sin(pitchR),
                (float)(Math.cos(pitchR) * Math.cos(yawR))
        ).normalize();
    }

    /**
     * Horizontal-only forward vector (pitch = 0). Used for walk-mode
     * movement so looking up/down never lifts or sinks the camera.
     */
    private Vector3f flatForward() {
        float yawR = (float) Math.toRadians(yaw);
        return new Vector3f(
                (float) Math.sin(yawR),
                0f,
                (float) Math.cos(yawR)
        ).normalize();
    }
}