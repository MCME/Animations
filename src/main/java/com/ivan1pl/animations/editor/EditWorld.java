package com.ivan1pl.animations.editor;

import java.io.File;
import org.bukkit.Bukkit;
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
        return new WorldCreator(worldName)
                .generator(new VoidChunkGenerator())
                .environment(World.Environment.NORMAL)
                .createWorld();
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
