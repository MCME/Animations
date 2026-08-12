package com.ivan1pl.animations.editor;

import java.util.Random;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

/**
 * A generator that produces nothing — every chunk is air. Used for the private editing world so
 * plots sit in empty space with no terrain to destroy or collide with (PLOT-EDITOR-DESIGN.md §5).
 */
public class VoidChunkGenerator extends ChunkGenerator {

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        // Clear of the build lanes, which start at the origin and grow +X / +Z.
        return new Location(world, 0, 65, -16);
    }
}
