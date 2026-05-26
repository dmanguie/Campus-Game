package com.campusgame.world.interior;

import com.campusgame.map.data.EntranceData;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * INTERIOR SCENE REGISTRY (world/interior/InteriorSceneRegistry.java)
 * Holds all hand-built interior scenes.
 * The editor scene picker (C key) reads getSceneIds() to list options.
 * InteriorManager calls get(sceneId) to load a scene on entry.
 */
public class InteriorSceneRegistry {

    private final Map<String, InteriorScene> scenes = new HashMap<>();

    public InteriorSceneRegistry() {
        register(buildLibrary());
        register(buildMainHall());
        register(buildGym());
        register(buildSalBldg());
    }

    public void          register(InteriorScene s) { scenes.put(s.id, s); }
    public InteriorScene get(String id)            { return scenes.get(id); }
    public boolean       has(String id)            { return scenes.containsKey(id); }
    public Collection<InteriorScene> all()         { return scenes.values(); }

    public List<String> getSceneIds() {
        List<String> ids = new ArrayList<>(scenes.keySet());
        ids.sort(String::compareTo);
        return ids;
    }

    // ── Scene builders ────────────────────────────────────────────────

    private InteriorScene buildLibrary() {
        InteriorScene s = new InteriorScene("library_interior", "University Library",
                840f, 620f, new Color(212, 202, 186));
        s.defaultSpawnX = 420f; s.defaultSpawnZ = 545f;
        s.addRoom(new InteriorRoom("reading_hall", "Main Reading Hall",
                40, 40, 760, 280, new Color(218, 208, 192), new Color(105, 85, 65)));
        s.addRoom(new InteriorRoom("reference", "Reference Section",
                40, 345, 360, 190, new Color(205, 196, 182), new Color(105, 85, 65)));
        s.addRoom(new InteriorRoom("periodicals", "Periodicals",
                440, 345, 360, 190, new Color(205, 196, 182), new Color(105, 85, 65)));
        EntranceData exit = new EntranceData("library_exit", "University Library",
                "University Library", 420f, 570f, null, 0, 0, 0, 0);
        exit.triggerRadius = 50f;
        s.addExit(exit);
        return s;
    }

    private InteriorScene buildMainHall() {
        InteriorScene s = new InteriorScene("main_hall_interior", "Main Administration Hall",
                1020f, 720f, new Color(218, 212, 200));
        s.defaultSpawnX = 510f; s.defaultSpawnZ = 635f;
        s.addRoom(new InteriorRoom("lobby", "Lobby",
                80, 40, 860, 210, new Color(228, 222, 210), new Color(115, 95, 75)));
        s.addRoom(new InteriorRoom("registrar", "Registrar's Office",
                80, 270, 380, 210, new Color(210, 204, 194), new Color(115, 95, 75)));
        s.addRoom(new InteriorRoom("cashier", "Cashier",
                480, 270, 380, 210, new Color(210, 204, 194), new Color(115, 95, 75)));
        s.addRoom(new InteriorRoom("corridor", "Main Corridor",
                80, 500, 860, 160, new Color(200, 194, 184), new Color(115, 95, 75)));
        EntranceData exit = new EntranceData("main_hall_exit", "Main Administration Hall",
                "Main Hall", 510f, 660f, null, 0, 0, 0, 0);
        exit.triggerRadius = 60f;
        s.addExit(exit);
        return s;
    }

    private InteriorScene buildGym() {
        InteriorScene s = new InteriorScene("gym_interior", "Sports Complex",
                920f, 720f, new Color(172, 205, 172));
        s.defaultSpawnX = 460f; s.defaultSpawnZ = 630f;
        s.addRoom(new InteriorRoom("basketball_court", "Basketball Court",
                60, 40, 800, 420, new Color(188, 158, 118), new Color(75, 55, 35)));
        s.addRoom(new InteriorRoom("locker_m", "Men's Locker Room",
                60, 480, 370, 190, new Color(168, 184, 168), new Color(75, 95, 75)));
        s.addRoom(new InteriorRoom("locker_f", "Women's Locker Room",
                490, 480, 370, 190, new Color(168, 184, 168), new Color(75, 95, 75)));
        EntranceData exit = new EntranceData("gym_exit", "Sports Complex",
                "Sports Complex", 460f, 660f, null, 0, 0, 0, 0);
        exit.triggerRadius = 55f;
        s.addExit(exit);
        return s;
    }

    private InteriorScene buildSalBldg() {
        InteriorScene s = new InteriorScene("sal_interior", "SAL Building",
                880f, 700f, new Color(210, 205, 195));
        s.defaultSpawnX = 440f; s.defaultSpawnZ = 620f;
        s.addRoom(new InteriorRoom("hallway_gf", "Ground Floor Hallway",
                40, 40, 800, 140, new Color(220, 215, 205), new Color(100, 85, 70)));
        s.addRoom(new InteriorRoom("room_101", "Room 101",
                40, 200, 360, 180, new Color(215, 208, 196), new Color(100, 85, 70)));
        s.addRoom(new InteriorRoom("room_102", "Room 102",
                480, 200, 360, 180, new Color(215, 208, 196), new Color(100, 85, 70)));
        s.addRoom(new InteriorRoom("room_103", "Room 103",
                40, 400, 360, 180, new Color(215, 208, 196), new Color(100, 85, 70)));
        s.addRoom(new InteriorRoom("room_104", "Room 104",
                480, 400, 360, 180, new Color(215, 208, 196), new Color(100, 85, 70)));
        EntranceData exit = new EntranceData("sal_exit", "SAL Building",
                "SAL Building", 440f, 645f, null, 0, 0, 0, 0);
        exit.triggerRadius = 55f;
        s.addExit(exit);
        return s;
    }
}