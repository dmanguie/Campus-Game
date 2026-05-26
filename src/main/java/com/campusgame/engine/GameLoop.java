package com.campusgame.engine;

import com.campusgame.core.GameWorld;
import com.campusgame.editor.EditorInputAdapter;
import com.campusgame.editor.EditorMode;
import com.campusgame.editor.EditorOverlayRenderer;
import com.campusgame.entities.DoorTrigger;
import com.campusgame.entities.Player;
import com.campusgame.input.InputHandler;
import com.campusgame.interaction.Interactable;
import com.campusgame.interaction.InteractionContext;
import com.campusgame.interaction.InteractionPrompt;
import com.campusgame.interaction.InteractionSystem;
import com.campusgame.map.CampusMap;
import com.campusgame.map.data.EntranceData;
import com.campusgame.physics.CollisionManager;
import com.campusgame.renderer.Camera;
import com.campusgame.renderer.Renderer;
import com.campusgame.renderer.api.IRenderer;
import com.campusgame.renderer.swing2d.SwingRenderer2D;
import com.campusgame.world.interior.InteriorManager;
import com.campusgame.world.interior.InteriorSceneRegistry;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class GameLoop implements Runnable {

    public static final int    WINDOW_WIDTH  = 1280;
    public static final int    WINDOW_HEIGHT = 720;
    public static final String TITLE         = "Campus Game — Phase 5";
    public static final int    TARGET_FPS    = 60;

    private JFrame   frame;
    private boolean  running = false;
    private Thread   gameThread;

    // Core
    private CampusMap        campusMap;
    private Player           player;
    private Camera           camera;
    private InputHandler     inputHandler;
    private CollisionManager collisionManager;
    private GameWorld        world;
    private IRenderer        activeRenderer;
    private Renderer         swingPanel;

    // Editor
    private EditorMode         editorMode;
    private EditorInputAdapter editorInput;

    // Phase 5: interior + interaction
    private InteriorSceneRegistry sceneRegistry;
    private InteractionSystem     interactionSystem;
    private InteractionPrompt     interactionPrompt;
    private InteriorManager       interiorManager;
    private InteractionContext    interactionContext;

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

        // Scene registry
        sceneRegistry = new InteriorSceneRegistry();

        // Door triggers from campus.json entrances
        List<Interactable> doorTriggers = new ArrayList<>();
        for (EntranceData e : campusMap.getEntrances()) {
            doorTriggers.add(new DoorTrigger(e));
        }

        // Interaction system
        interactionSystem = new InteractionSystem();
        interactionSystem.setInteractables(doorTriggers);
        interactionPrompt = new InteractionPrompt();

        // Interior manager
        interiorManager   = new InteriorManager(sceneRegistry, interactionSystem, doorTriggers);
        interactionContext = new InteractionContext(player, camera, interiorManager);

        // Wire [E] key
        inputHandler.setOnInteract(
                () -> interactionSystem.tryInteract(interactionContext));

        // Editor
        editorMode  = new EditorMode(campusMap, camera, sceneRegistry);
        editorInput = new EditorInputAdapter(editorMode);
        inputHandler.setEditor(editorMode);

        EditorOverlayRenderer overlay = new EditorOverlayRenderer(
                editorMode.getState(), campusMap, camera);
        swingPanel.setEditorOverlay(overlay);
        swingPanel.setInteriorManager(interiorManager);
        swingPanel.setInteractionSystem(interactionSystem);
        swingPanel.setInteractionPrompt(interactionPrompt);

        swingPanel.addMouseListener(editorInput);
        swingPanel.addMouseMotionListener(editorInput);

        // Window
        frame = new JFrame(TITLE + "  [" + activeRenderer.getBackendName() + "]");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(swingPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        swingPanel.addKeyListener(inputHandler);
        swingPanel.requestFocusInWindow();
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
        boolean transitioning = interiorManager.isTransitioning();
        inputHandler.setMovementBlocked(transitioning);
        inputHandler.applyToPlayer(player);
        world.update(delta);
        if (!interiorManager.isIndoors()) collisionManager.resolve(player);
        camera.follow(player);
        interactionSystem.update(player);
        interiorManager.update(delta, player, camera);
        interactionPrompt.update(delta);
    }

    public void stop() { running = false; }
}