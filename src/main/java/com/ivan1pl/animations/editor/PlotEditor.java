package com.ivan1pl.animations.editor;

import com.ivan1pl.animations.constants.OperationResult;
import com.ivan1pl.animations.data.Animations;
import com.ivan1pl.animations.data.AnimationsLocation;
import com.ivan1pl.animations.data.MCMEStoragePlotFrame;
import com.ivan1pl.animations.data.Selection;
import com.ivan1pl.animations.data.StationaryAnimation;
import com.ivan1pl.animations.exceptions.InvalidSelectionException;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
        EditSession session = sessions.resume(player.getUniqueId());
        if (session == null) {
            msg(player, "You're not editing.");
            return;
        }
        cleanupSession(session);
        sessions.close(player.getUniqueId());
        msg(player, "Left the plot editor. Plots cleared.");
    }

    /**
     * Captures each plot's blocks into frame files stored at the session's real target location so
     * the animation plays back where the wand selection was made (PLOT-EDITOR-DESIGN.md §7). A
     * frame's paste position comes from the animation's selection, so building the animation over the
     * target region and adding the plot-captured frames is all that is needed — no relocation.
     */
    public void save(Player player) {
        EditSession session = require(player);
        if (session == null) {
            return;
        }
        World targetWorld = Bukkit.getWorld(session.targetWorld());
        if (targetWorld == null) {
            msg(player, "Target world '" + session.targetWorld() + "' is not loaded.");
            return;
        }
        World editWorld = EditWorld.ensure(editWorldName);
        Selection targetSel = new Selection();
        targetSel.setPoint1(new Location(targetWorld, session.targetX(), session.targetY(), session.targetZ()));
        targetSel.setPoint2(new Location(
                targetWorld,
                session.targetX() + session.sizeX() - 1,
                session.targetY() + session.sizeY() - 1,
                session.targetZ() + session.sizeZ() - 1));
        try {
            StationaryAnimation anim = new StationaryAnimation(targetSel);
            anim.setInterval(10);
            int n = session.frames().size();
            for (int i = 1; i <= n; i++) {
                PlotBounds b =
                        geometry.frameBounds(session.laneIndex(), i, session.sizeX(), session.sizeY(), session.sizeZ());
                loadChunks(editWorld, b);
                Selection plotSel = new Selection();
                plotSel.setPoint1(new Location(editWorld, b.minX(), b.minY(), b.minZ()));
                plotSel.setPoint2(new Location(editWorld, b.maxX(), b.maxY(), b.maxZ()));
                anim.addFrame(MCMEStoragePlotFrame.fromSelection(plotSel));
            }
            Animations.setAnimation(session.animationName(), anim);
            OperationResult result = Animations.saveAnimation(session.animationName());
            if (result == OperationResult.SUCCESS) {
                Animations.reloadAnimation(session.animationName());
                msg(
                        player,
                        "Saved '" + session.animationName() + "' (" + n + " frame(s)). Play it with /anim play "
                                + session.animationName() + ".");
            } else {
                msg(player, "Save failed: " + result + ".");
            }
        } catch (InvalidSelectionException ex) {
            msg(player, "Could not build the animation (invalid selection).");
        }
    }

    /** Force-loads the chunks a plot spans so capture reads real blocks even if the builder walked away. */
    private void loadChunks(World world, PlotBounds b) {
        for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++) {
            for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) {
                world.getChunkAt(cx, cz);
            }
        }
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

    /**
     * Clears a session's plots (floor + build region) and floating labels from the edit world.
     * Clearing the region via {@code getBlockAt} loads each plot's chunk, which also makes the
     * label removal reliable (the hologram sits in the same chunk column).
     */
    private void cleanupSession(EditSession session) {
        World world = EditWorld.ensure(editWorldName);
        for (int i = 1; i <= session.frames().size(); i++) {
            PlotBounds b =
                    geometry.frameBounds(session.laneIndex(), i, session.sizeX(), session.sizeY(), session.sizeZ());
            for (int x = b.minX(); x <= b.maxX(); x++) {
                for (int z = b.minZ(); z <= b.maxZ(); z++) {
                    world.getBlockAt(x, b.minY() - 1, z).setType(Material.AIR, false);
                    for (int y = b.minY(); y <= b.maxY(); y++) {
                        world.getBlockAt(x, y, z).setType(Material.AIR, false);
                    }
                }
            }
            Location labelLoc = new Location(
                    world, b.minX() + session.sizeX() / 2.0, b.maxY() + 2, b.minZ() + session.sizeZ() / 2.0);
            world.getNearbyEntities(labelLoc, 1.5, 1.5, 1.5).forEach(entity -> {
                if (entity.getScoreboardTags().contains("anim_plot_label")) {
                    entity.remove();
                }
            });
        }
    }

    private void msg(Player player, String text) {
        player.sendMessage(Component.text(text));
    }
}
