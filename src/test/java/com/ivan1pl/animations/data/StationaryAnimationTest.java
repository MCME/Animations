package com.ivan1pl.animations.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression test for orphan frame-file cleanup (audit finding M9).
 *
 * <p>{@code save()} writes {@code frame_0..N-1} but historically never removed higher-numbered
 * files left behind when frames were deleted, so a directory could report more {@code .mcme}
 * files on disk than the animation declared (8 such directories were found in production). The
 * sweep is factored into the pure {@link StationaryAnimation#deleteOrphanFrames} helper and
 * tested here directly; wiring it into {@code save()} needs a real server and is verified by
 * inspection.
 */
class StationaryAnimationTest {

    @TempDir
    Path tempDir;

    @Test
    void deleteOrphanFramesRemovesFramesAtOrAboveTheKeepCount() throws Exception {
        createFrames(6); // frame_0..frame_5

        StationaryAnimation.deleteOrphanFrames(tempDir.toFile(), 3);

        assertTrue(frame(0).exists(), "kept frames below the count must survive");
        assertTrue(frame(1).exists());
        assertTrue(frame(2).exists());
        assertFalse(frame(3).exists(), "the frame at the keep count is the first orphan");
        assertFalse(frame(4).exists());
        assertFalse(frame(5).exists());
    }

    @Test
    void deleteOrphanFramesLeavesAContiguousSetUntouched() throws Exception {
        createFrames(3); // frame_0..frame_2, exactly the declared count

        StationaryAnimation.deleteOrphanFrames(tempDir.toFile(), 3);

        assertTrue(frame(0).exists());
        assertTrue(frame(1).exists());
        assertTrue(frame(2).exists());
    }

    @Test
    void deleteOrphanFramesRemovesEverythingWhenNoFramesRemain() throws Exception {
        createFrames(4); // an animation whose frames were all deleted

        StationaryAnimation.deleteOrphanFrames(tempDir.toFile(), 0);

        assertFalse(frame(0).exists());
        assertFalse(frame(1).exists());
        assertFalse(frame(2).exists());
        assertFalse(frame(3).exists());
    }

    private void createFrames(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            frame(i).createNewFile();
        }
    }

    private File frame(int index) {
        return new File(tempDir.toFile(), "frame_" + index + ".mcme");
    }
}
