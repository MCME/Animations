package com.ivan1pl.animations.editor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maps a player to their open {@link EditSession}, backed by a {@link LaneAllocator}. A session
 * survives disconnects (resume returns it); only {@link #close} releases the lane.
 */
public final class SessionRegistry {

    private final LaneAllocator allocator = new LaneAllocator();
    private final Map<UUID, EditSession> sessions = new HashMap<>();

    /** Starts a session, claiming a lane. Rejects a second concurrent session for the same player. */
    public synchronized EditSession open(UUID player, String animationName) {
        if (sessions.containsKey(player)) {
            throw new IllegalStateException("player already has an open editing session");
        }
        EditSession session = new EditSession(animationName, allocator.claim());
        sessions.put(player, session);
        return session;
    }

    /** Returns the player's open session, or null if none. */
    public synchronized EditSession resume(UUID player) {
        return sessions.get(player);
    }

    /** Closes the session and frees its lane. */
    public synchronized void close(UUID player) {
        EditSession session = sessions.remove(player);
        if (session != null) {
            allocator.release(session.laneIndex());
        }
    }
}
