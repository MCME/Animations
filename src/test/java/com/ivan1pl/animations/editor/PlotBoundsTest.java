package com.ivan1pl.animations.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlotBoundsTest {

    // base (0,64,0), laneSpacing 2048, frameGap 8
    private final LaneGeometry geo = new LaneGeometry(0, 64, 0, 2048, 8);

    @Test
    void boundsExposeMinMaxAndSize() {
        PlotBounds b = new PlotBounds(10, 64, 20, 5, 3, 4);
        assertEquals(10, b.minX());
        assertEquals(64, b.minY());
        assertEquals(20, b.minZ());
        assertEquals(14, b.maxX()); // 10 + 5 - 1
        assertEquals(66, b.maxY()); // 64 + 3 - 1
        assertEquals(23, b.maxZ()); // 20 + 4 - 1
    }

    @Test
    void aBoxOverlapsItself() {
        PlotBounds b = new PlotBounds(0, 64, 0, 10, 10, 10);
        assertTrue(b.overlaps(b));
    }

    @Test
    void screenBoundsSitAtTheLaneOrigin() {
        PlotBounds screen = geo.screenBounds(0, 10, 5, 6);
        assertEquals(0, screen.minX());
        assertEquals(64, screen.minY());
        assertEquals(0, screen.minZ());
        assertEquals(9, screen.maxX());
    }

    @Test
    void adjacentFramesInALaneDoNotOverlap() {
        PlotBounds f1 = geo.frameBounds(0, 1, 10, 5, 6);
        PlotBounds f2 = geo.frameBounds(0, 2, 10, 5, 6);
        assertFalse(f1.overlaps(f2), "the frameGap must keep consecutive frames apart");
    }

    @Test
    void screenDoesNotOverlapTheFirstFrame() {
        PlotBounds screen = geo.screenBounds(0, 10, 5, 6);
        PlotBounds f1 = geo.frameBounds(0, 1, 10, 5, 6);
        assertFalse(screen.overlaps(f1));
    }

    @Test
    void differentLanesNeverOverlap() {
        // depth 6 is far smaller than laneSpacing 2048, so lanes cannot touch
        PlotBounds laneA = geo.frameBounds(0, 1, 10, 5, 6);
        PlotBounds laneB = geo.frameBounds(1, 1, 10, 5, 6);
        assertFalse(laneA.overlaps(laneB), "lanes are separated on Z by more than any plot depth");
    }
}
