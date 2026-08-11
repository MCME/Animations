package com.ivan1pl.animations.editor;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Tracks which lane indices are in use and hands out the lowest free one. Thread-safe so two
 * simultaneous session starts cannot receive the same lane. Pure — no Bukkit.
 */
public final class LaneAllocator {

    private final SortedSet<Integer> used = new TreeSet<>();

    /** Claims and returns the lowest free lane index. */
    public synchronized int claim() {
        int i = 0;
        while (used.contains(i)) {
            i++;
        }
        used.add(i);
        return i;
    }

    public synchronized void release(int laneIndex) {
        used.remove(laneIndex);
    }

    /** Marks a lane used without claiming a new one (restart restore). */
    public synchronized void markUsed(int laneIndex) {
        used.add(laneIndex);
    }

    public synchronized boolean isUsed(int laneIndex) {
        return used.contains(laneIndex);
    }
}
