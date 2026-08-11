package com.ivan1pl.animations.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression test for the frame data read path (audit findings H4 and M3).
 *
 * <p>{@code load(File)} previously read the gzip stream with a length-guarded loop
 * ({@code do { n = in.read(buf); ... } while (n == buf.length)}). Because
 * {@link java.util.zip.GZIPInputStream} hands back data in chunks smaller than a
 * 1&nbsp;KiB request, that loop ended after the first short read (silently truncating
 * the frame) or, at EOF, called {@code write(buf, 0, -1)} and threw. The sizes below
 * straddle that boundary so a round-trip catches both failure modes.
 *
 * <p>For M3, {@code load(File)} used to return {@code void} and swallow an
 * {@link java.io.IOException}, leaving {@code frameNBTData} null and surfacing the failure
 * only later as a mystery NPE at playback. It now returns a success flag so the caller can
 * skip a broken animation cleanly.
 */
class MCMEStoragePlotFrameLoadTest {

    @TempDir
    Path tempDir;

    @Test
    void loadReturnsTrueOnAWellFormedFile() throws Exception {
        byte[] original = pattern(512);
        File file = tempDir.resolve("valid.mcme").toFile();
        MCMEStoragePlotFrame saver = new MCMEStoragePlotFrame();
        setFrameNbtData(saver, original);
        saver.save(file);

        MCMEStoragePlotFrame loader = new MCMEStoragePlotFrame();
        assertTrue(loader.load(file), "a readable frame file must report success");
    }

    @Test
    void loadReturnsFalseWhenTheFileCannotBeRead() {
        File missing = tempDir.resolve("does_not_exist.mcme").toFile();
        MCMEStoragePlotFrame loader = new MCMEStoragePlotFrame();
        assertFalse(loader.load(missing), "an unreadable frame file must report failure, not swallow it (M3)");
    }

    @Test
    void loadReturnsExactlyWhatSaveWrote() throws Exception {
        for (int size : new int[] {0, 100, 512, 1024, 5000, 70000}) {
            byte[] original = pattern(size);
            File file = tempDir.resolve("frame_" + size + ".mcme").toFile();

            MCMEStoragePlotFrame saver = new MCMEStoragePlotFrame();
            setFrameNbtData(saver, original);
            saver.save(file);

            MCMEStoragePlotFrame loader = new MCMEStoragePlotFrame();
            loader.load(file);

            assertArrayEquals(
                    original, getFrameNbtData(loader), "round-trip must preserve every byte for payload size " + size);
        }
    }

    private static byte[] pattern(int size) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) ((i * 31 + 7) & 0xFF);
        }
        return data;
    }

    private static void setFrameNbtData(MCMEStoragePlotFrame frame, byte[] data) throws Exception {
        Field field = MCMEStoragePlotFrame.class.getDeclaredField("frameNBTData");
        field.setAccessible(true);
        field.set(frame, data);
    }

    private static byte[] getFrameNbtData(MCMEStoragePlotFrame frame) throws Exception {
        Field field = MCMEStoragePlotFrame.class.getDeclaredField("frameNBTData");
        field.setAccessible(true);
        return (byte[]) field.get(frame);
    }
}
