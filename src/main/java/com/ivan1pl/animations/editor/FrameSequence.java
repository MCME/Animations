package com.ivan1pl.animations.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Per-session bookkeeping between builder-facing frame positions (1-based) and physical plot
 * slots. Physical slots are assigned monotonically and never reused within a session, so the
 * block data in a slot never has to move; reordering is purely a permutation. Pure — no Bukkit.
 */
public final class FrameSequence {

    private final List<Integer> order = new ArrayList<>(); // logical position (0-based) -> physical slot
    private int nextSlot = 0;

    /** Appends a new frame, returns the physical slot allocated for it. */
    public int add() {
        int slot = nextSlot++;
        order.add(slot);
        return slot;
    }

    public int size() {
        return order.size();
    }

    /** Physical slot backing a 1-based builder position. */
    public int slotFor(int position) {
        return order.get(position - 1);
    }

    /** Removes a 1-based position; later positions shift down. */
    public void removeAt(int position) {
        order.remove(position - 1);
    }

    /** Swaps two 1-based positions. */
    public void swap(int a, int b) {
        Collections.swap(order, a - 1, b - 1);
    }

    /** Physical slots in current logical order (for capture on save). */
    public List<Integer> slotsInOrder() {
        return List.copyOf(order);
    }
}
