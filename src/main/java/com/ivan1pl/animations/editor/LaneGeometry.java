package com.ivan1pl.animations.editor;

/**
 * Maps a lane index and frame position to world coordinates in the edit world. Lanes are
 * separated on Z; within a lane a reserved screen sits at the origin and frame plots tile on X.
 * Pure arithmetic — no Bukkit. See PLOT-EDITOR-DESIGN.md sections 5-6.
 */
public final class LaneGeometry {

    private final int baseX;
    private final int baseY;
    private final int baseZ;
    private final int laneSpacing;
    private final int frameGap;

    public LaneGeometry(int baseX, int baseY, int baseZ, int laneSpacing, int frameGap) {
        this.baseX = baseX;
        this.baseY = baseY;
        this.baseZ = baseZ;
        this.laneSpacing = laneSpacing;
        this.frameGap = frameGap;
    }

    /** Z origin of the given lane. */
    public int laneZ(int laneIndex) {
        return baseZ + laneIndex * laneSpacing;
    }

    /** X origin of the screen (reserved head of every lane). */
    public int screenX() {
        return baseX;
    }

    public int baseY() {
        return baseY;
    }

    /**
     * X origin of a builder-facing frame (1-based). Frame 1 sits after the screen (one plot
     * width) plus a gap; each subsequent frame is one (width + gap) stride further.
     */
    public int frameX(int frameIndex, int width) {
        return baseX + width + frameGap + (frameIndex - 1) * (width + frameGap);
    }
}
