package com.lost.database.game.world;

public enum TileType {
    // Old types left for compatibility
    WATER,
    SAND,
    GRASS,
    FOREST,
    MOUNTAIN,
    BUNKER_WALL,
    BUNKER_FLOOR,
    BUNKER_DOOR,

    // Platformer-specific tiles
    GROUND, // solid ground / grass
    FLOATING_PLATFORM, // suspended platforms (solid)
    SPIKES, // hazardous - causes respawn/damage
    DECORATION, // purely visual, no collision

    // Items & Enemies
    HEALTH_PACK, // restores HP when picked up
    FOOD_ITEM, // restores small HP
    ENEMY_PATROL, // enemy spawn point (patrols left-right)

    // Legacy jungle types (kept for compatibility)
    JUNGLE_DIRT,
    JUNGLE_TREE,
    JUNGLE_ROCK,
    COCKPIT_WRECKAGE
}
