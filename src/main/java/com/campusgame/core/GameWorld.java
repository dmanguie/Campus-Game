package com.campusgame.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GAME WORLD (core/GameWorld.java)
 * ----------------------------------
 * The scene/entity manager. Owns all active GameObjects.
 *
 * Responsibilities:
 *  - Maintains the master list of all live GameObjects
 *  - Provides add/remove with deferred processing (avoids ConcurrentModificationException)
 *  - Calls update() on all active objects each frame
 *  - Provides query methods: getByTag(), getAll(), etc.
 *
 * GameLoop owns one GameWorld and calls world.update(delta) each frame.
 *
 * Future expansion:
 *  - Layers/zones (outdoor campus, indoor room, hallway)
 *  - Spatial partitioning (grid cells) for large NPC counts
 *  - Scene loading/unloading for interior transitions
 */
public class GameWorld {

    private final List<GameObject> objects        = new ArrayList<>();
    private final List<GameObject> pendingAdd     = new ArrayList<>();
    private final List<GameObject> pendingRemove  = new ArrayList<>();

    // ---------------------------------------------------------------
    // ENTITY MANAGEMENT
    // ---------------------------------------------------------------

    /**
     * Schedules a GameObject for addition next frame.
     * Safe to call during update().
     */
    public void add(GameObject obj) {
        pendingAdd.add(obj);
    }

    /**
     * Schedules a GameObject for removal next frame.
     */
    public void remove(GameObject obj) {
        pendingRemove.add(obj);
    }

    // ---------------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------------

    /**
     * Called every frame by GameLoop.
     * Flushes pending adds/removes, then updates all active objects.
     */
    public void update(float delta) {
        // Flush pending additions
        for (GameObject obj : pendingAdd) {
            obj.init();
            objects.add(obj);
        }
        pendingAdd.clear();

        // Flush pending removals
        for (GameObject obj : pendingRemove) {
            obj.destroy();
            objects.remove(obj);
        }
        pendingRemove.clear();

        // Update all active objects
        for (GameObject obj : objects) {
            if (obj.active) {
                obj.update(delta);
            }
        }
    }

    // ---------------------------------------------------------------
    // QUERIES
    // ---------------------------------------------------------------

    /** All current objects (unmodifiable view). */
    public List<GameObject> getAll() {
        return Collections.unmodifiableList(objects);
    }

    /** All objects with the given tag. */
    public List<GameObject> getByTag(String tag) {
        List<GameObject> result = new ArrayList<>();
        for (GameObject obj : objects) {
            if (tag.equals(obj.tag)) result.add(obj);
        }
        return result;
    }

    /** First object with the given tag, or null. */
    public GameObject findByTag(String tag) {
        for (GameObject obj : objects) {
            if (tag.equals(obj.tag)) return obj;
        }
        return null;
    }

    /** Object count (active + inactive). */
    public int size() { return objects.size(); }

    /** True if no objects in world. */
    public boolean isEmpty() { return objects.isEmpty(); }
}