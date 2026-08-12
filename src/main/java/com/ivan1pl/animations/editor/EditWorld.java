package com.ivan1pl.animations.editor;

import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;

/**
 * Provisions and provides the single private void world where plot-per-frame animations are built.
 * Editors are separated into lanes within this one world (PLOT-EDITOR-DESIGN.md §5); it never holds
 * real terrain, so building in it destroys nothing.
 */
public final class EditWorld {

    private EditWorld() {}

    /** Returns the editing world, creating it as a void world if it does not already exist. */
    public static World ensure(String worldName) {
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            return existing;
        }
        World world = new WorldCreator(worldName)
                .generator(new VoidChunkGenerator())
                .environment(World.Environment.NORMAL)
                .createWorld();
        if (world != null) {
            configure(world);
        }
        return world;
    }

    /** Editing-friendly gamerules (frozen time/weather, no mobs) and a spawn landing platform. */
    private static void configure(World world) {
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setTime(6000); // fixed noon
        world.setStorm(false);
        world.setThundering(false);
        // Small landing platform at spawn, well clear of the build lanes (which start at +X/+Z).
        for (int x = -3; x <= 3; x++) {
            for (int z = -19; z <= -13; z++) {
                world.getBlockAt(x, 64, z).setType(Material.SMOOTH_STONE, false);
            }
        }
        world.setSpawnLocation(0, 65, -16);
    }

    /**
     * Regenerates the edit world from scratch: unloads it (without saving), deletes its folder, and
     * recreates it empty. Called on enable because editing sessions are in-memory only, so any plots
     * left on disk from a previous run are stale scratch. Refuses a suspicious world name so it can
     * never delete anything outside the world container.
     */
    public static World reset(String worldName) {
        if (worldName == null
                || worldName.isBlank()
                || worldName.contains("/")
                || worldName.contains("\\")
                || worldName.contains("..")) {
            return ensure(worldName);
        }
        // Load (or create) the world first, then ask IT for its real folder — computing the path from
        // getWorldContainer() + name is unreliable on custom/symlinked server layouts.
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = ensure(worldName);
        }
        if (world == null) {
            Bukkit.getLogger().warning("[Animations] edit-world reset: could not open '" + worldName + "'.");
            return null;
        }
        File folder = world.getWorldFolder();
        boolean unloaded = Bukkit.unloadWorld(world, false);
        boolean existed = folder.isDirectory();
        if (unloaded && existed) {
            deleteRecursively(folder);
        }
        boolean deletedOk = existed && !folder.exists();
        Bukkit.getLogger()
                .info("[Animations] edit-world reset: folder=" + folder.getAbsolutePath() + " unloaded=" + unloaded
                        + " folderExisted=" + existed + " deletedOk=" + deletedOk);
        return ensure(worldName);
    }

    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
