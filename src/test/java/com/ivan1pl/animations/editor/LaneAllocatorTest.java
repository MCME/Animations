package com.ivan1pl.animations.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LaneAllocatorTest {

    @Test
    void firstClaimIsLaneZero() {
        assertEquals(0, new LaneAllocator().claim());
    }

    @Test
    void consecutiveClaimsAreDistinctAndAscending() {
        LaneAllocator a = new LaneAllocator();
        assertEquals(0, a.claim());
        assertEquals(1, a.claim());
        assertEquals(2, a.claim());
    }

    @Test
    void releasedLaneIsReusedByTheNextClaim() {
        LaneAllocator a = new LaneAllocator();
        a.claim(); // 0
        a.claim(); // 1
        a.release(0);
        assertEquals(0, a.claim(), "next claim reuses the lowest free index");
    }

    @Test
    void markUsedIsSkippedByClaim() {
        LaneAllocator a = new LaneAllocator();
        a.markUsed(0);
        a.markUsed(1);
        assertEquals(2, a.claim(), "claim skips lanes restored via markUsed");
        assertTrue(a.isUsed(0));
        assertTrue(a.isUsed(2));
    }
}
