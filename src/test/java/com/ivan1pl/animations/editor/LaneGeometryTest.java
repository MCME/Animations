package com.ivan1pl.animations.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LaneGeometryTest {

    // base (0,64,0), laneSpacing 2048, frameGap 8
    private final LaneGeometry geo = new LaneGeometry(0, 64, 0, 2048, 8);

    @Test
    void laneZIsBasePlusIndexTimesSpacing() {
        assertEquals(0, geo.laneZ(0));
        assertEquals(2048, geo.laneZ(1));
        assertEquals(4096, geo.laneZ(2));
    }

    @Test
    void screenSitsAtTheLaneOrigin() {
        assertEquals(0, geo.screenX());
        assertEquals(64, geo.baseY());
    }

    @Test
    void frameOneSitsAfterTheScreenPlusGap() {
        // width 10: screen occupies [0,10); gap 8; frame 1 at 18
        assertEquals(18, geo.frameX(1, 10));
    }

    @Test
    void framesTileWithUniformStride() {
        // frame i at screenWidth + gap + (i-1)*(width+gap)
        assertEquals(18, geo.frameX(1, 10)); // 10 + 8
        assertEquals(36, geo.frameX(2, 10)); // + (10+8)
        assertEquals(54, geo.frameX(3, 10)); // + (10+8)
    }

    @Test
    void screenAndFirstFrameDoNotOverlap() {
        int width = 10;
        int screenEnd = geo.screenX() + width; // exclusive
        assertTrue(geo.frameX(1, width) >= screenEnd + 8, "gap must separate screen and frame 1");
    }
}
