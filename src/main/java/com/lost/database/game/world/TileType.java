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
    SLOPE_LEFT, // slope rising to the right (e.g. tile 121)
    SLOPE_LEFT_2, // slope rising to the right (steep)
    SLOPE_RIGHT, // slope falling to the right (first half)
    SLOPE_RIGHT_2, // slope falling to the right (second half)
    SLOPE_RIGHT_GENTLE, // gentle slope down (first half)
    SLOPE_RIGHT_GENTLE_2, // gentle slope down (second half)

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
