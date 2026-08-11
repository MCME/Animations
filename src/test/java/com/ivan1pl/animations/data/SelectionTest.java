package com.ivan1pl.animations.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Regression test for {@link Selection#volumeOf} (audit finding M5).
 *
 * <p>The volume used to be computed in {@code int}; a large selection overflowed to a negative
 * value and slipped past the {@code maxFrameSize} check. It is now computed in {@code long} and
 * clamped to {@link Integer#MAX_VALUE}.
 */
class SelectionTest {

    @Test
    void volumeOfSmallBoxIsExact() {
        // inclusive 10x10x10 box
        assertEquals(1000, Selection.volumeOf(0, 0, 0, 9, 9, 9));
    }

    @Test
    void volumeOfHugeBoxClampsInsteadOfOverflowing() {
        // 2001^3 ≈ 8.0e9, well past Integer.MAX_VALUE (~2.1e9). The old int math overflowed
        // (often to a negative), which validateSelectionSize would then wrongly accept.
        assertEquals(Integer.MAX_VALUE, Selection.volumeOf(0, 0, 0, 2000, 2000, 2000));
    }
}
