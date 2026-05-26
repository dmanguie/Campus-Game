package com.campusgame.map.data;

/**
 * ENTRANCE DATA (map/data/EntranceData.java)
 * -------------------------------------------
 * Data record for one campus building entrance / door trigger.
 *
 * Placed in the world by the ENTRANCE editor tool (N key).
 * Saved to campus.json under "entrances".
 * Loaded at startup by MapLoader → CampusMap.entrances list.
 * Converted to DoorTrigger entities by GameLoop on startup.
 *
 * Fields:
 *   id              — unique identifier e.g. "entrance_1"
 *   buildingName    — name of the parent building
 *   label           — shown in interaction prompt "[E] to enter Library"
 *   worldX/Z        — trigger position on the exterior campus
 *   triggerRadius   — activation radius in world units
 *   interiorSceneId — key into InteriorSceneRegistry (e.g. "library_interior")
 *   interiorSpawnX/Z— where the player spawns inside the scene
 *   exteriorSpawnX/Z— reserved; InteriorManager restores the entry position
 */
public class EntranceData {

    public String id;
    public String buildingName;
    public String label;

    public float worldX;
    public float worldZ;
    public float triggerRadius = 55f;

    public String interiorSceneId;
    public float  interiorSpawnX;
    public float  interiorSpawnZ;

    public float  exteriorSpawnX;
    public float  exteriorSpawnZ;

    /** No-arg constructor for Gson and editor ghost creation. */
    public EntranceData() {}

    /** Full constructor. */
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
        this.id              = id;
        this.buildingName    = buildingName;
        this.label           = label;
        this.worldX          = worldX;
        this.worldZ          = worldZ;
        this.interiorSceneId = interiorSceneId;
        this.interiorSpawnX  = interiorSpawnX;
        this.interiorSpawnZ  = interiorSpawnZ;
        this.exteriorSpawnX  = exteriorSpawnX;
        this.exteriorSpawnZ  = exteriorSpawnZ;
    }

    /** True if this entrance has been linked to an interior scene. */
    public boolean hasScene() {
        return interiorSceneId != null && !interiorSceneId.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("EntranceData{id='%s', scene='%s', pos=(%.0f,%.0f)}",
                id, interiorSceneId, worldX, worldZ);
    }
}