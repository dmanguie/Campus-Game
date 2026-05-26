package com.campusgame.world.interior;

import java.awt.*;

/**
 * SCENE TRANSITION (world/interior/SceneTransition.java)
 * Black-fade transition: FADE_OUT → SWITCH → FADE_IN → IDLE.
 *
 * Usage each frame:
 *   transition.update(delta);
 *   if (transition.isReadyToSwitch()) { doSwap(); transition.notifySwitched(); }
 *   transition.draw(g, w, h);
 */
public class SceneTransition {

    public enum Phase { IDLE, FADE_OUT, SWITCH, FADE_IN }

    private static final float HALF_DURATION = 0.45f;

    private Phase   phase    = Phase.IDLE;
    private float   alpha    = 0f;
    private boolean switched = false;

    public void start() {
        phase    = Phase.FADE_OUT;
        alpha    = 0f;
        switched = false;
    }

    public void update(float delta) {
        switch (phase) {
            case FADE_OUT -> {
                alpha += delta / HALF_DURATION;
                if (alpha >= 1f) { alpha = 1f; phase = Phase.SWITCH; }
            }
            case SWITCH -> { if (switched) phase = Phase.FADE_IN; }
            case FADE_IN -> {
                alpha -= delta / HALF_DURATION;
                if (alpha <= 0f) { alpha = 0f; phase = Phase.IDLE; }
            }
            case IDLE -> {}
        }
    }

    public boolean isReadyToSwitch() { return phase == Phase.SWITCH && !switched; }
    public void    notifySwitched()  { switched = true; }
    public boolean isActive()        { return phase != Phase.IDLE; }
    public float   getAlpha()        { return alpha; }

    public void draw(Graphics2D g, int screenW, int screenH) {
        if (phase == Phase.IDLE || alpha <= 0f) return;
        g.setColor(new Color(0, 0, 0, (int)(alpha * 255)));
        g.fillRect(0, 0, screenW, screenH);
    }
}