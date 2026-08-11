package com.ivan1pl.animations.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class FrameSequenceTest {

    @Test
    void addAppendsNewPhysicalSlotsInCreationOrder() {
        FrameSequence s = new FrameSequence();
        assertEquals(0, s.add()); // returns the physical slot it created
        assertEquals(1, s.add());
        assertEquals(2, s.add());
        assertEquals(3, s.size());
        assertEquals(List.of(0, 1, 2), s.slotsInOrder());
    }

    @Test
    void slotForReturnsPhysicalSlotForA1BasedPosition() {
        FrameSequence s = new FrameSequence();
        s.add();
        s.add();
        assertEquals(0, s.slotFor(1));
        assertEquals(1, s.slotFor(2));
    }

    @Test
    void removeAtDropsThePositionAndShiftsLaterFramesDown() {
        FrameSequence s = new FrameSequence();
        s.add(); // slot 0 -> pos 1
        s.add(); // slot 1 -> pos 2
        s.add(); // slot 2 -> pos 3
        s.removeAt(2); // drop pos 2 (slot 1)
        assertEquals(2, s.size());
        assertEquals(List.of(0, 2), s.slotsInOrder());
        assertEquals(2, s.slotFor(2), "old position 3 is now position 2");
    }

    @Test
    void swapExchangesTwoPositions() {
        FrameSequence s = new FrameSequence();
        s.add(); // 0
        s.add(); // 1
        s.add(); // 2
        s.swap(1, 3);
        assertEquals(List.of(2, 1, 0), s.slotsInOrder());
    }

    @Test
    void newSlotAfterRemoveDoesNotReuseAFreedSlotNumber() {
        FrameSequence s = new FrameSequence();
        s.add(); // 0
        s.add(); // 1
        s.removeAt(1); // drop slot 0
        assertEquals(2, s.add(), "physical slots are monotonic so blocks never move");
        assertEquals(List.of(1, 2), s.slotsInOrder());
    }

    @Test
    void outOfRangePositionsThrow() {
        FrameSequence s = new FrameSequence();
        s.add();
        assertThrows(IndexOutOfBoundsException.class, () -> s.slotFor(2));
        assertThrows(IndexOutOfBoundsException.class, () -> s.removeAt(0));
    }
}
