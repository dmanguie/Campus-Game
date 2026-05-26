package com.campusgame.map.data;

/**
 * ENTRANCE DATA (map/data/EntranceData.java)
 * -------------------------------------------
 * Data record for one campus building entrance / door trigger.
 *
 * Fields:
 *   id              — stable internal key, never renamed
 *   buildingName    — source building name
 *   label           — legacy / internal label (kept for backward compat)
 *   displayName     — visible door label shown in editor overlay  ← NEW
 *   targetDisplayName — text shown in the [E] interaction prompt  ← NEW
 *   worldX/Z        — trigger position on the exterior campus
 *   triggerRadius   — activation radius in world units
 *   interiorSceneId — key into InteriorSceneRegistry
 *   interiorSpawnX/Z— where the player spawns inside the scene
 *   exteriorSpawnX/Z— reserved; InteriorManager restores the entry position
 */
public class EntranceData {

    public String id;
    public String buildingName;
    public String label;            // kept for backward compat; mirrors displayName on load

    // ── NEW rename fields ─────────────────────────────────────────────
    /** Human-readable door label shown in the editor overlay. */
    public String displayName;

    /**
     * Text shown in the [E] interaction prompt, e.g. "Registrar Office".
     * Falls back to displayName → label if blank.
     */
    public String targetDisplayName;

    // ── Position / trigger ────────────────────────────────────────────
    public float worldX;
    public float worldZ;
    public float triggerRadius = 55f;

    // ── Interior transition ───────────────────────────────────────────
    public String interiorSceneId;
    public float  interiorSpawnX;
    public float  interiorSpawnZ;

    public float  exteriorSpawnX;
    public float  exteriorSpawnZ;

    /** No-arg constructor for Gson and editor ghost creation. */
    public EntranceData() {}

    /** Full constructor (legacy — displayName mirrors label). */
    public EntranceData(String id,
                        String buildingName,
                        String label,
                        float  worldX,
                        float  worldZ,
                        String interiorSceneId,
                        float  interiorSpawnX,
                        float  interiorSpawnZ,
                        float  exteriorSpawnX,
                        float  exteriorSpawnZ) {
        this.id               = id;
        this.buildingName     = buildingName;
        this.label            = label;
        this.displayName      = label;   // mirror on construction
        this.targetDisplayName = "";     // empty = fall back to displayName
        this.worldX           = worldX;
        this.worldZ           = worldZ;
        this.interiorSceneId  = interiorSceneId;
        this.interiorSpawnX   = interiorSpawnX;
        this.interiorSpawnZ   = interiorSpawnZ;
        this.exteriorSpawnX   = exteriorSpawnX;
        this.exteriorSpawnZ   = exteriorSpawnZ;
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /** True if this entrance has been linked to an interior scene. */
    public boolean hasScene() {
        return interiorSceneId != null && !interiorSceneId.isEmpty();
    }

    /**
     * Returns the best available display name for the editor overlay.
     * Priority: displayName → label → id.
     */
    public String effectiveDisplayName() {
        if (displayName != null && !displayName.isBlank()) return displayName;
        if (label       != null && !label.isBlank())       return label;
        return id;
    }

    /**
     * Returns the text to show inside the [E] interaction prompt.
     * Priority: targetDisplayName → displayName → label → id.
     */
    public String effectivePromptName() {
        if (targetDisplayName != null && !targetDisplayName.isBlank()) return targetDisplayName;
        return effectiveDisplayName();
    }

    @Override
    public String toString() {
        return String.format("EntranceData{id='%s', display='%s', scene='%s', pos=(%.0f,%.0f)}",
                id, effectiveDisplayName(), interiorSceneId, worldX, worldZ);
    }
}