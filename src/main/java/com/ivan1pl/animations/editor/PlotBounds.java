package com.ivan1pl.animations.editor;

/**
 * An immutable inclusive block region (a min corner plus a size). Bridges lane/frame indices to
 * the actual block cuboid a plot occupies, and answers whether two plots share any block. Pure —
 * no Bukkit.
 */
public final class PlotBounds {

    private final int minX;
    private final int minY;
    private final int minZ;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    public PlotBounds(int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    public int minX() {
        return minX;
    }

    public int minY() {
        return minY;
    }

    public int minZ() {
        return minZ;
    }

    public int maxX() {
        return minX + sizeX - 1;
    }

    public int maxY() {
        return minY + sizeY - 1;
    }

    public int maxZ() {
        return minZ + sizeZ - 1;
    }

    public int sizeX() {
        return sizeX;
    }

    public int sizeY() {
        return sizeY;
    }

    public int sizeZ() {
        return sizeZ;
    }

    /** True if this region shares at least one block with {@code other}. */
    public boolean overlaps(PlotBounds other) {
        return minX <= other.maxX()
                && other.minX <= maxX()
                && minY <= other.maxY()
                && other.minY <= maxY()
                && minZ <= other.maxZ()
                && other.minZ <= maxZ();
    }
}
