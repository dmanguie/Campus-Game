package com.campusgame.engine;

import com.campusgame.core.GameWorld;
import com.campusgame.entities.Player;
import com.campusgame.input.InputHandler;
import com.campusgame.map.CampusMap;
import com.campusgame.physics.CollisionManager;
import com.campusgame.renderer.Camera;
import com.campusgame.renderer.Renderer;
import com.campusgame.renderer.api.IRenderer;
import com.campusgame.renderer.swing2d.SwingRenderer2D;

import javax.swing.*;

/**
 * GAME LOOP (engine/GameLoop.java)
 * ---------------------------------
 * PHASE 2 CHANGES:
 *  - Now owns a GameWorld (entity registry) alongside the existing subsystems
 *  - Uses IRenderer interface instead of direct Renderer reference
 *  - SwingRenderer2D wraps the existing Renderer — 2D game still works identically
 *  - To switch to 3D later: change ONE line (new LwjglRenderer3D(...))
 *
 * UNCHANGED:
 *  - Fixed-timestep loop
 *  - Window creation
 *  - update() order: input → move → collide → camera
 */
public class GameLoop implements Runnable {

    public static final int    WINDOW_WIDTH  = 1280;
    public static final int    WINDOW_HEIGHT = 720;
    public static final String TITLE         = "Campus Game - Phase 2";
    public static final int    TARGET_FPS    = 60;

    private JFrame  frame;
    private boolean running = false;
    private Thread  gameThread;

    // Core subsystems
    private CampusMap        campusMap;
    private Player           player;
    private Camera           camera;
    private InputHandler     inputHandler;
    private CollisionManager collisionManager;

    // PHASE 2: entity scene graph
    private GameWorld world;

    // PHASE 2: renderer accessed through interface — swap for 3D later
    private IRenderer    activeRenderer;
    private Renderer     swingPanel;  // kept for adding to JFrame

    public void start() {
        init();
        running    = true;
        gameThread = new Thread(this, "GameThread");
        gameThread.start();
    }

    private void init() {
        campusMap = new CampusMap();
        player    = new Player(300, 300);
        camera    = new Camera(WINDOW_WIDTH, WINDOW_HEIGHT);
        inputHandler     = new InputHandler();
        collisionManager = new CollisionManager(campusMap);

        // PHASE 2: create world and register player
        world = new GameWorld();
        world.add(player);

        // PHASE 2: renderer via interface
        // To switch to 3D: replace this line with new LwjglRenderer3D(...)
        swingPanel     = new Renderer(campusMap, player, camera);
        activeRenderer = new SwingRenderer2D(swingPanel);
        activeRenderer.init();

        frame = new JFrame(TITLE + " [" + activeRenderer.getBackendName() + "]");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(swingPanel);
        frame.addKeyListener(inputHandler);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
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
            activeRenderer.render(world, player, camera);  // PHASE 2: via interface

            long elapsed = System.nanoTime() - now;
            long sleep   = (msPerTick - elapsed) / 1_000_000;
            if (sleep > 0) {
                try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
            }
        }
        activeRenderer.dispose();
    }

    private void update(float delta) {
        inputHandler.applyToPlayer(player);
        world.update(delta);              // PHASE 2: world updates all GameObjects
        collisionManager.resolve(player);
        camera.follow(player);
    }

    public void stop() { running = false; }
}