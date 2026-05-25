package com.campusgame.map.data;

public class EntranceData {

    public String id;
    public String label;
    public String buildingName;

    public float worldX;
    public float worldZ;
    public float triggerRadius = 55f;

    public String interiorSceneId;
    public float  interiorSpawnX;
    public float  interiorSpawnZ;

    public float exteriorSpawnX;
    public float exteriorSpawnZ;

    public EntranceData() {}

    public EntranceData(String id, String buildingName, String label,
                        float worldX,          float worldZ,
                        String interiorSceneId,
                        float interiorSpawnX,  float interiorSpawnZ,
                        float exteriorSpawnX,  float exteriorSpawnZ) {
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
}