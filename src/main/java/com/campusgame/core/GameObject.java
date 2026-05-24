package com.campusgame.core;

/**
 * GAME OBJECT (core/GameObject.java)
 * -------------------------------------
 * Base class for every entity in the game world.
 *
 * In Phase 1, only Player exists as an entity.
 * In Phase 2+, NPCs, items, triggers, and decorations will extend this.
 *
 * Why a GameObject system?
 *  - GameLoop can iterate a single List<GameObject> to update/render all entities
 *  - Consistent interface for physics, rendering, and logic
 *  - Future: Component-based extension (add a PhysicsComponent, RenderComponent, etc.)
 *
 * Coordinate system (matches BuildingData):
 *   x = east-west  (world units)
 *   y = elevation  (0 = ground)
 *   z = north-south (world units)
 *
 * For 2D rendering: x maps to screen X, z maps to screen Y.
 * For 3D rendering: x, y, z used directly in the scene graph.
 *
 * LIFECYCLE:
 *   init()   → called once when object is added to the world
 *   update() → called every frame by GameLoop
 *   destroy()→ called when object is removed from the world
 */
public abstract class GameObject {

    // ---------------------------------------------------------------
    // IDENTITY
    // ---------------------------------------------------------------

    /** Unique ID auto-assigned on creation */
    public final int id;
    private static int nextId = 0;

    /** Human-readable tag for debugging ("player", "npc_guard", etc.) */
    public final String tag;

    /** Whether this object participates in update/render cycles */
    public boolean active = true;

    // ---------------------------------------------------------------
    // TRANSFORM (world-space position and size)
    // ---------------------------------------------------------------

    /** World X position (east-west, center of object) */
    public float x;

    /** World Y position (elevation; 0 = ground) */
    public float y;

    /** World Z position (north-south, center of object) — maps to screen Y in 2D */
    public float z;

    // ---------------------------------------------------------------
    // CONSTRUCTOR
    // ---------------------------------------------------------------

    protected GameObject(float x, float z, String tag) {
        this.id  = nextId++;
        this.x   = x;
        this.y   = 0f;
        this.z   = z;
        this.tag = tag;
    }

    // ---------------------------------------------------------------
    // LIFECYCLE METHODS — override in subclasses
    // ---------------------------------------------------------------

    /**
     * Called once when the object enters the world.
     * Load resources, set initial state here.
     */
    public void init() {}

    /**
     * Called every frame by GameLoop.
     * @param delta seconds since last frame
     */
    public abstract void update(float delta);

    /**
     * Called when the object is removed from the world.
     * Release resources, stop sounds, etc.
     */
    public void destroy() {}

    // ---------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------

    /**
     * Distance to another GameObject (2D, ignoring Y elevation).
     * Useful for NPC detection radius, interaction range, etc.
     */
    public float distanceTo(GameObject other) {
        float dx = this.x - other.x;
        float dz = this.z - other.z;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Distance to another GameObject in full 3D.
     */
    public float distanceTo3D(GameObject other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        float dz = this.z - other.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Teleport to a new position (no interpolation). */
    public void setPosition(float x, float z) {
        this.x = x;
        this.z = z;
    }

    /** Teleport to a full 3D position. */
    public void setPosition3D(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return String.format("GameObject{id=%d, tag='%s', x=%.1f, z=%.1f, active=%b}",
                id, tag, x, z, active);
    }
}