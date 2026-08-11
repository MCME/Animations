package com.ivan1pl.animations.editor;

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
}
