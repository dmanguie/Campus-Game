package com.campusgame.engine;

import com.campusgame.core.GameWorld;
import com.campusgame.editor.EditorInputAdapter;
import com.campusgame.editor.EditorMode;
import com.campusgame.editor.EditorOverlayRenderer;
import com.campusgame.entities.Player;
import com.campusgame.input.InputHandler;
import com.campusgame.map.CampusMap;
import com.campusgame.physics.CollisionManager;
import com.campusgame.renderer.Camera;
import com.campusgame.renderer.Renderer;
import com.campusgame.renderer.api.IRenderer;
import com.campusgame.renderer.swing2d.SwingRenderer2D;

import javax.swing.*;

public class GameLoop implements Runnable {

    public static final int    WINDOW_WIDTH  = 1280;
    public static final int    WINDOW_HEIGHT = 720;
    public static final String TITLE         = "Campus Game - Phase 3";
    public static final int    TARGET_FPS    = 60;

    private JFrame   frame;
    private boolean  running = false;
    private Thread   gameThread;

    // Core subsystems
    private CampusMap        campusMap;
    private Player           player;
    private Camera           camera;
    private InputHandler     inputHandler;
    private CollisionManager collisionManager;
    private GameWorld        world;
    private IRenderer        activeRenderer;
    private Renderer         swingPanel;

    // Phase 3 — editor
    private EditorMode         editorMode;
    private EditorInputAdapter editorInput;

    public void start() {
        init();
        running    = true;
        gameThread = new Thread(this, "GameThread");
        gameThread.start();
    }

    private void init() {
        // Core
        campusMap        = new CampusMap();
        player           = new Player(1000, 1000);
        camera           = new Camera(WINDOW_WIDTH, WINDOW_HEIGHT);
        inputHandler     = new InputHandler();
        collisionManager = new CollisionManager(campusMap);
        world            = new GameWorld();
        world.add(player);

        // Renderer
        swingPanel     = new Renderer(campusMap, player, camera);
        activeRenderer = new SwingRenderer2D(swingPanel);
        activeRenderer.init();

        // ── Phase 3: wire editor ──────────────────────────────────
        editorMode  = new EditorMode(campusMap, camera);
        editorInput = new EditorInputAdapter(editorMode);

        // 1. Keyboard: F1 / P / X / Ctrl+S / Ctrl+Z
        inputHandler.setEditor(editorMode);

        // 2. Drawing: editor banner + ghost + selection overlay
        EditorOverlayRenderer overlay = new EditorOverlayRenderer(
                editorMode.getState(), campusMap, camera);
        swingPanel.setEditorOverlay(overlay);

        // 3. Mouse: click-to-place / delete
        swingPanel.addMouseListener(editorInput);
        swingPanel.addMouseMotionListener(editorInput);
        // ─────────────────────────────────────────────────────────

        // Window
        frame = new JFrame(TITLE + "  [" + activeRenderer.getBackendName() + "]");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(swingPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Focus must be on swingPanel so keyboard events reach it
        swingPanel.addKeyListener(inputHandler);
        swingPanel.requestFocusInWindow();

        // Snap camera to player on start
        camera.snapTo(player);
    }

    @Override
    public void run() {
        long lastTime  = System.nanoTime();
        long msPerTick = 1_000_000_000L / TARGET_FPS;

        while (running) {
            long  now   = System.nanoTime();
            float delta = (now - lastTime) / 1_000_000_000f;
            lastTime    = now;

            update(delta);
            activeRenderer.render(world, player, camera);

            long sleep = (msPerTick - (System.nanoTime() - now)) / 1_000_000;
            if (sleep > 0) try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
        }
        activeRenderer.dispose();
    }

    private void update(float delta) {
        inputHandler.applyToPlayer(player);
        world.update(delta);
        collisionManager.resolve(player);
        camera.follow(player);
    }

    public void stop() { running = false; }
}