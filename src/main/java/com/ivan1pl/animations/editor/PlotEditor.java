package com.ivan1pl.animations.editor;

import com.ivan1pl.animations.data.Animations;
import com.ivan1pl.animations.data.AnimationsLocation;
import com.ivan1pl.animations.data.Selection;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

/**
 * Ties per-player editing sessions to the void edit world: opens/closes sessions, adds frame plots,
 * and teleports the builder between them. The pure geometry/allocation/bookkeeping lives in
 * {@link LaneGeometry}/{@link SessionRegistry}/{@link FrameSequence}; this class is the Bukkit glue
 * that drives them (PLOT-EDITOR-DESIGN.md §9-10). Phase 2a: session + navigation only; capture and
 * the screen come in later increments.
 */
public final class PlotEditor {

    private final SessionRegistry sessions = new SessionRegistry();
    private final LaneGeometry geometry;
    private final String editWorldName;

    public PlotEditor(String editWorldName, int baseY, int laneSpacing, int frameGap) {
        this.editWorldName = editWorldName;
        this.geometry = new LaneGeometry(0, baseY, 0, laneSpacing, frameGap);
    }

    /** Starts a session from the player's current wand selection (the real target region). */
    public void create(Player player, String name) {
        if (sessions.resume(player.getUniqueId()) != null) {
            msg(player, "You're already editing — /anim plot exit first.");
            return;
        }
        Selection sel = Animations.getSelection(player);
        if (!Selection.isValid(sel)) {
            msg(player, "Select the target region with the wand first.");
            return;
        }
        if (!Animations.validateSelectionSize(sel)) {
            msg(player, "Selection is larger than the configured maxFrameSize.");
            return;
        }
        AnimationsLocation p1 = sel.getPoint1();
        AnimationsLocation p2 = sel.getPoint2();
        int sx = Math.abs(p1.getBlockX() - p2.getBlockX()) + 1;
        int sy = Math.abs(p1.getBlockY() - p2.getBlockY()) + 1;
        int sz = Math.abs(p1.getBlockZ() - p2.getBlockZ()) + 1;
        int tx = Math.min(p1.getBlockX(), p2.getBlockX());
        int ty = Math.min(p1.getBlockY(), p2.getBlockY());
        int tz = Math.min(p1.getBlockZ(), p2.getBlockZ());

        EditSession session = sessions.open(player.getUniqueId(), name);
        session.setTarget(p1.getWorld().getName(), tx, ty, tz, sx, sy, sz);
        session.frames().add();
        preparePlotForFrame(session, 1);
        msg(player, "Plot session '" + name + "' started (" + sx + "x" + sy + "x" + sz + "). Frame 1 ready.");
        teleportToFrame(player, session, 1);
    }

    /** Adds a new frame plot and teleports to it. */
    public void newFrame(Player player) {
        EditSession session = require(player);
        if (session == null) {
            return;
        }
        session.frames().add();
        int n = session.frames().size();
        preparePlotForFrame(session, n);
        msg(player, "Frame " + n + " added.");
        teleportToFrame(player, session, n);
    }

    /** Teleports to an existing frame plot (1-based). */
    public void gotoFrame(Player player, int frameIndex) {
        EditSession session = require(player);
        if (session == null) {
            return;
        }
        if (frameIndex < 1 || frameIndex > session.frames().size()) {
            msg(player, "No frame " + frameIndex + " (have " + session.frames().size() + ").");
            return;
        }
        teleportToFrame(player, session, frameIndex);
    }

    /** Closes the session and frees its lane. */
    public void exit(Player player) {
        if (sessions.resume(player.getUniqueId()) == null) {
            msg(player, "You're not editing.");
            return;
        }
        sessions.close(player.getUniqueId());
        msg(player, "Left the plot editor.");
    }

    private EditSession require(Player player) {
        EditSession session = sessions.resume(player.getUniqueId());
        if (session == null) {
            msg(player, "You're not editing — /anim plot create <name> first.");
        }
        return session;
    }

    private void teleportToFrame(Player player, EditSession session, int frameIndex) {
        World world = EditWorld.ensure(editWorldName);
        PlotBounds b = geometry.frameBounds(
                session.laneIndex(), frameIndex, session.sizeX(), session.sizeY(), session.sizeZ());
        Location loc =
                new Location(world, b.minX() + session.sizeX() / 2.0, b.minY(), b.minZ() + session.sizeZ() / 2.0);
        player.teleport(loc);
        msg(
                player,
                "Frame " + frameIndex + " plot @ " + b.minX() + "," + b.minY() + "," + b.minZ() + " (lane "
                        + session.laneIndex() + ").");
    }

    /**
     * Lays a visible floor one block BELOW the plot (y = minY - 1, outside the captured region
     * [minY..maxY]) so the builder can see the footprint and stand on it. The perimeter is a
     * contrasting block so the plot edges read clearly.
     */
    private void preparePlotForFrame(EditSession session, int frameIndex) {
        World world = EditWorld.ensure(editWorldName);
        PlotBounds b = geometry.frameBounds(
                session.laneIndex(), frameIndex, session.sizeX(), session.sizeY(), session.sizeZ());
        int y = b.minY() - 1;
        for (int x = b.minX(); x <= b.maxX(); x++) {
            for (int z = b.minZ(); z <= b.maxZ(); z++) {
                boolean border = x == b.minX() || x == b.maxX() || z == b.minZ() || z == b.maxZ();
                world.getBlockAt(x, y, z).setType(border ? Material.POLISHED_ANDESITE : Material.SMOOTH_STONE, false);
            }
        }

        // Floating label above the plot so frames can be told apart. Tagged for later cleanup.
        Location labelLoc =
                new Location(world, b.minX() + session.sizeX() / 2.0, b.maxY() + 2, b.minZ() + session.sizeZ() / 2.0);
        Component label =
                Component.text(session.animationName()).appendNewline().append(Component.text("Frame " + frameIndex));
        world.spawn(labelLoc, TextDisplay.class, display -> {
            display.text(label);
            display.setBillboard(Display.Billboard.CENTER);
            display.addScoreboardTag("anim_plot_label");
        });
    }

    private void msg(Player player, String text) {
        player.sendMessage(Component.text(text));
    }
}
