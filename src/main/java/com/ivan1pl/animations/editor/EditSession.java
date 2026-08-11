package com.ivan1pl.animations.editor;

/** One editor's in-progress session: which animation, which lane, and its frame bookkeeping. */
public final class EditSession {

    private final String animationName;
    private final int laneIndex;
    private final FrameSequence frames = new FrameSequence();

    public EditSession(String animationName, int laneIndex) {
        this.animationName = animationName;
        this.laneIndex = laneIndex;
    }

    public String animationName() {
        return animationName;
    }

    public int laneIndex() {
        return laneIndex;
    }

    public FrameSequence frames() {
        return frames;
    }

    // Target region in the real world (where the animation plays) + plot size, set at create from
    // the wand selection. Stored as primitives + world name so this class stays Bukkit-free.
    private String targetWorld;
    private int targetX;
    private int targetY;
    private int targetZ;
    private int sizeX;
    private int sizeY;
    private int sizeZ;

    public void setTarget(String targetWorld, int targetX, int targetY, int targetZ, int sizeX, int sizeY, int sizeZ) {
        this.targetWorld = targetWorld;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    public String targetWorld() {
        return targetWorld;
    }

    public int targetX() {
        return targetX;
    }

    public int targetY() {
        return targetY;
    }

    public int targetZ() {
        return targetZ;
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
}
