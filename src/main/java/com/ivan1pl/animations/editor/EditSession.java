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
}
