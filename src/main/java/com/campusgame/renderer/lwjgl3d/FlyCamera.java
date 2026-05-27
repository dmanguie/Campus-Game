package com.campusgame.renderer.lwjgl3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class FlyCamera {

    // ── Constants ─────────────────────────────────────────────────────
    private static final float WALK_EYE_HEIGHT  =  5.5f;
    private static final float PITCH_LIMIT_UP   =  70f;
    private static final float PITCH_LIMIT_DOWN = -70f;

    // ── State ─────────────────────────────────────────────────────────
    public final Vector3f position = new Vector3f(0f, WALK_EYE_HEIGHT, 100f);
    public float yaw   = 180f;
    public float pitch = -15f;

    // ── Mode ──────────────────────────────────────────────────────────
    private boolean flyMode  = false;
    private boolean pickMode = false;   // Phase 7.1: cursor free for ray picking

    // ── Settings ──────────────────────────────────────────────────────
    public float walkSpeed  = 30f;
    public float flySpeed   = 60f;
    public float mouseSens  = 0.10f;
    public float sprintMult = 3f;

    // ── Mouse state ───────────────────────────────────────────────────
    private double  lastMouseX = Double.NaN;
    private double  lastMouseY = Double.NaN;
    private boolean firstMove  = true;

    // ── Key edge detection ────────────────────────────────────────────
    private boolean fKeyWasDown = false;

    // ─────────────────────────────────────────────────────────────────

    public void attachTo(long window) {
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);

        GLFW.glfwSetCursorPosCallback(window, (win, mx, my) -> {
            if (pickMode) {
                // In pick mode just track position — don't rotate camera
                lastMouseX = mx;
                lastMouseY = my;
                return;
            }
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

            yaw   -= dx;
            pitch -= dy;
            pitch  = Math.max(PITCH_LIMIT_DOWN, Math.min(PITCH_LIMIT_UP, pitch));
        });

        GLFW.glfwSetScrollCallback(window, (win, xoff, yoff) -> {
            if (pickMode) return; // scroll disabled in pick mode
            float speed = flyMode ? flySpeed : walkSpeed;
            Vector3f flat = flatForward();
            position.x += flat.x * (float) yoff * speed * 0.3f;
            position.z += flat.z * (float) yoff * speed * 0.3f;
            if (!flyMode) position.y = WALK_EYE_HEIGHT;
        });
    }

    public void update(long window, float dt) {
        // F key: toggle fly mode (edge-triggered)
        boolean fKeyDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F) == GLFW.GLFW_PRESS;
        if (fKeyDown && !fKeyWasDown) {
            flyMode = !flyMode;
            if (!flyMode) position.y = WALK_EYE_HEIGHT;
            System.out.println("[Camera] " + (flyMode ? "FLY mode" : "WALK mode"));
        }
        fKeyWasDown = fKeyDown;

        float baseSpeed  = flyMode ? flySpeed : walkSpeed;
        boolean sprinting = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
        float moveSpeed  = baseSpeed * dt * (sprinting ? sprintMult : 1f);

        Vector3f moveDir = flyMode ? fullForward() : flatForward();
        Vector3f right   = new Vector3f(moveDir).cross(new Vector3f(0, 1, 0)).normalize();

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS)
            position.add(new Vector3f(moveDir).mul(moveSpeed));
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS)
            position.sub(new Vector3f(moveDir).mul(moveSpeed));
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS)
            position.sub(new Vector3f(right).mul(moveSpeed));
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS)
            position.add(new Vector3f(right).mul(moveSpeed));

        if (flyMode) {
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS)
                position.y += moveSpeed;
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS)
                position.y -= moveSpeed;
        } else {
            position.y = WALK_EYE_HEIGHT;
        }
    }

    // ── Phase 7.1: Pick mode ──────────────────────────────────────────

    /**
     * Toggle between captured-cursor (fly/look) and free-cursor (pick) mode.
     * Called when the player presses Tab.
     */
    public void togglePickMode(long window) {
        pickMode = !pickMode;
        if (pickMode) {
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        } else {
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
            firstMove = true; // avoid rotation snap when re-entering fly mode
        }
    }

    public boolean isPickMode()  { return pickMode; }
    public boolean isFlyMode()   { return flyMode;  }
    public double  getMouseX()   { return lastMouseX; }
    public double  getMouseY()   { return lastMouseY; }

    public void setFlyMode(boolean enabled) {
        flyMode = enabled;
        if (!flyMode) position.y = WALK_EYE_HEIGHT;
    }

    public void buildViewMatrix(Matrix4f out) {
        Vector3f target = new Vector3f(position).add(fullForward());
        out.identity().lookAt(position, target, new Vector3f(0, 1, 0));
    }

    // ── Direction helpers ─────────────────────────────────────────────

    private Vector3f fullForward() {
        float yawR   = (float) Math.toRadians(yaw);
        float pitchR = (float) Math.toRadians(pitch);
        return new Vector3f(
                (float)(Math.cos(pitchR) * Math.sin(yawR)),
                (float) Math.sin(pitchR),
                (float)(Math.cos(pitchR) * Math.cos(yawR))
        ).normalize();
    }

    private Vector3f flatForward() {
        float yawR = (float) Math.toRadians(yaw);
        return new Vector3f(
                (float) Math.sin(yawR),
                0f,
                (float) Math.cos(yawR)
        ).normalize();
    }
}