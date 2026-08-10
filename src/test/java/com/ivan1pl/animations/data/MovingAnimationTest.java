package com.ivan1pl.animations.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

/**
 * Regression test for the background-selection logic of {@link MovingAnimation#load} (audit
 * finding H5).
 *
 * <p>The full {@code load(ConfigurationSection)} path cannot be exercised in a unit test: it
 * builds plot-storage frames through PluginUtils, which reads {@code Chunk#getTileEntities} and
 * so needs a real server. The clone-before-expand behaviour that the fix restored is therefore
 * factored into {@link MovingAnimation#expandedBackgroundSelection}, which is pure and tested
 * here directly.
 */
class MovingAnimationTest {

    @Test
    void expandedBackgroundSelectionLeavesTheFrameSelectionUntouched() {
        Selection frame = selection(0, 0, 0, 4, 4, 4);

        // stepX * frameCount = 1 * 10 = 10
        Selection background = MovingAnimation.expandedBackgroundSelection(frame, 1, 0, 0, 10);

        // Before the fix, load() aliased and expanded the stored selection in place, so it grew
        // by step * frameCount every load/save cycle. The frame selection must stay put.
        assertEquals(4, frame.getPoint2().getBlockX(), "frame selection must not be mutated (H5)");
        assertEquals(
                14,
                background.getPoint2().getBlockX(),
                "background must be the frame selection expanded by step * frameCount");
    }

    private static Selection selection(int x1, int y1, int z1, int x2, int y2, int z2) {
        Selection selection = new Selection();
        selection.setPoint1(new Location(null, x1, y1, z1));
        selection.setPoint2(new Location(null, x2, y2, z2));
        return selection;
    }
}
