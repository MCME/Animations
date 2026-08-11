package com.ivan1pl.animations.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionRegistryTest {

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    @Test
    void openClaimsLaneZeroForTheFirstEditor() {
        SessionRegistry reg = new SessionRegistry();
        EditSession s = reg.open(alice, "world-gate");
        assertEquals(0, s.laneIndex());
        assertEquals("world-gate", s.animationName());
        assertEquals(0, s.frames().size());
    }

    @Test
    void twoEditorsGetDistinctLanes() {
        SessionRegistry reg = new SessionRegistry();
        assertEquals(0, reg.open(alice, "a").laneIndex());
        assertEquals(1, reg.open(bob, "b").laneIndex());
    }

    @Test
    void resumeReturnsTheSameOpenSession() {
        SessionRegistry reg = new SessionRegistry();
        EditSession opened = reg.open(alice, "a");
        assertSame(opened, reg.resume(alice), "disconnect != exit: the session persists");
    }

    @Test
    void resumeIsNullWhenNoSessionIsOpen() {
        assertNull(new SessionRegistry().resume(alice));
    }

    @Test
    void closeReleasesTheLaneForReuse() {
        SessionRegistry reg = new SessionRegistry();
        reg.open(alice, "a"); // lane 0
        reg.close(alice);
        assertNull(reg.resume(alice));
        assertEquals(0, reg.open(bob, "b").laneIndex(), "freed lane 0 is reused");
    }

    @Test
    void openingWhileAlreadyInASessionIsRejected() {
        SessionRegistry reg = new SessionRegistry();
        reg.open(alice, "a");
        assertThrows(IllegalStateException.class, () -> reg.open(alice, "b"));
    }
}
