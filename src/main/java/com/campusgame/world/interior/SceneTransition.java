package com.campusgame.world.interior;

import java.awt.*;

/**
 * Black-fade transition controller.
 *
 * Lifecycle:
 *   FADE_OUT  (alpha 0→1 over DURATION seconds)
 *     → SWITCH  (one frame: caller does the actual teleport/scene swap)
 *     → FADE_IN  (alpha 1→0 over DURATION seconds)
 *     → DONE
 *
 * Usage:
 *   transition.start();
 *   // each frame:
 *   transition.update(delta);
 *   if (transition.isReadyToSwitch()) {
 *       doSwap();
 *       transition.notifySwitched();
 *   }
 *   transition.draw(g, w, h);
 */
public class SceneTransition {

    public enum Phase { IDLE, FADE_OUT, SWITCH, FADE_IN }

    private static final float HALF_DURATION = 0.45f;  // seconds per half

    private Phase   phase    = Phase.IDLE;
    private float   alpha    = 0f;
    private boolean switched = false;

    // ── Control ───────────────────────────────────────────────────────

    public void start() {
        phase    = Phase.FADE_OUT;
        alpha    = 0f;
        switched = false;
    }

    // ── Update ────────────────────────────────────────────────────────

    public void update(float delta) {
        switch (phase) {
            case FADE_OUT -> {
                alpha += delta / HALF_DURATION;
                if (alpha >= 1f) { alpha = 1f; phase = Phase.SWITCH; }
            }
            case SWITCH -> {
                // Stays here until caller calls notifySwitched()
                if (switched) phase = Phase.FADE_IN;
            }
            case FADE_IN -> {
                alpha -= delta / HALF_DURATION;
                if (alpha <= 0f) { alpha = 0f; phase = Phase.IDLE; }
            }
            case IDLE -> {}
        }
    }

    // ── Queries ───────────────────────────────────────────────────────

    /** True for exactly one frame — caller must do the scene/player swap now. */
    public boolean isReadyToSwitch() { return phase == Phase.SWITCH && !switched; }

    /** Call after performing the swap. */
    public void notifySwitched()     { switched = true; }

    public boolean isActive() { return phase != Phase.IDLE; }
    public boolean isDone()   { return phase == Phase.IDLE; }
    public float   getAlpha() { return alpha; }

    // ── Draw ──────────────────────────────────────────────────────────

    public void draw(Graphics2D g, int screenW, int screenH) {
        if (phase == Phase.IDLE || alpha <= 0f) return;
        g.setColor(new Color(0, 0, 0, (int)(alpha * 255)));
        g.fillRect(0, 0, screenW, screenH);
    }
}