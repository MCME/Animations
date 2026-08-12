package com.ivan1pl.animations.editor;

import com.ivan1pl.animations.AnimationsPlugin;
import com.ivan1pl.animations.constants.OperationResult;
import com.ivan1pl.animations.data.Animations;
import com.ivan1pl.animations.data.AnimationsLocation;
import com.ivan1pl.animations.data.MCMEStoragePlotFrame;
import com.ivan1pl.animations.data.Selection;
import com.ivan1pl.animations.data.StationaryAnimation;
import com.ivan1pl.animations.exceptions.InvalidSelectionException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;

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
    private final Map<UUID, BukkitTask> previewTasks = new HashMap<>();

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
        pasteInto(sel, session, 1);
        prepareScreen(session);
        msg(
                player,
                "Plot session '" + name + "' started (" + sx + "x" + sy + "x" + sz + "). Frame 1 = your selection.");
        teleportToFrame(player, session, 1);
    }

    /** Adds a new frame plot and teleports to it. */
    public void newFrame(Player player) {
        EditSession session = require(player);
        if (session == null) {
            return;
        }
        int previous = session.frames().size();
        session.frames().add();
        int n = session.frames().size();
        preparePlotForFrame(session, n);
        pasteInto(plotSelection(session, previous), session, n);
        msg(player, "Frame " + n + " added (copied from frame " + previous + ").");
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
        stopPreviewTask(player.getUniqueId());
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

    /** A selection over a frame's plot region in the edit world (used for copy-forward). */
    private Selection plotSelection(EditSession session, int frameIndex) {
        World world = EditWorld.ensure(editWorldName);
        PlotBounds b = geometry.frameBounds(
                session.laneIndex(), frameIndex, session.sizeX(), session.sizeY(), session.sizeZ());
        loadChunks(world, b);
        Selection sel = new Selection();
        sel.setPoint1(new Location(world, b.minX(), b.minY(), b.minZ()));
        sel.setPoint2(new Location(world, b.maxX(), b.maxY(), b.maxZ()));
        return sel;
    }

    /** Copies the blocks of {@code source} into a frame's plot (the real selection, or a prior plot). */
    private void pasteInto(Selection source, EditSession session, int frameIndex) {
        if (!Selection.isValid(source)) {
            return;
        }
        loadSelectionChunks(source);
        World world = EditWorld.ensure(editWorldName);
        PlotBounds dst = geometry.frameBounds(
                session.laneIndex(), frameIndex, session.sizeX(), session.sizeY(), session.sizeZ());
        loadChunks(world, dst);
        MCMEStoragePlotFrame frame = MCMEStoragePlotFrame.fromSelection(source);
        if (frame == null) {
            return;
        }
        frame.relocate(editWorldName, dst.minX(), dst.minY(), dst.minZ());
        frame.show();
    }

    private void loadSelectionChunks(Selection sel) {
        AnimationsLocation p1 = sel.getPoint1();
        AnimationsLocation p2 = sel.getPoint2();
        World w = p1.getWorld();
        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                w.getChunkAt(cx, cz);
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
        PlotBounds b = geometry.frameBounds(
                session.laneIndex(), frameIndex, session.sizeX(), session.sizeY(), session.sizeZ());
        buildPlatform(
                b,
                Component.text(session.animationName()).appendNewline().append(Component.text("Frame " + frameIndex)));
    }

    /** Builds the reserved "screen" platform at the head of a session's lane (the local preview area). */
    private void prepareScreen(EditSession session) {
        PlotBounds b = geometry.screenBounds(session.laneIndex(), session.sizeX(), session.sizeY(), session.sizeZ());
        buildPlatform(b, Component.text(session.animationName()).appendNewline().append(Component.text("SCREEN")));
    }

    /** Clears a plot column, lays a bordered floor one block below, and (re)places a floating label. */
    private void buildPlatform(PlotBounds b, Component label) {
        World world = EditWorld.ensure(editWorldName);
        loadChunks(world, b);
        int floorY = b.minY() - 1;
        for (int x = b.minX(); x <= b.maxX(); x++) {
            for (int z = b.minZ(); z <= b.maxZ(); z++) {
                for (int yy = floorY; yy <= b.maxY(); yy++) {
                    world.getBlockAt(x, yy, z).setType(Material.AIR, false);
                }
                boolean border = x == b.minX() || x == b.maxX() || z == b.minZ() || z == b.maxZ();
                world.getBlockAt(x, floorY, z)
                        .setType(border ? Material.POLISHED_ANDESITE : Material.SMOOTH_STONE, false);
            }
        }
        Location labelLoc = new Location(world, b.minX() + b.sizeX() / 2.0, b.maxY() + 2, b.minZ() + b.sizeZ() / 2.0);
        world.getNearbyEntities(labelLoc, 1.5, 1.5, 1.5).forEach(entity -> {
            if (entity.getScoreboardTags().contains("anim_plot_label")) {
                entity.remove();
            }
        });
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

    /**
     * Snapshots the current frames and loops them on the lane's screen so the animation can be
     * validated in the void world before {@code /anim play} ever touches the real map. The snapshot
     * is taken now; edit a plot and re-run {@code preview} to refresh it.
     */
    public void preview(Player player) {
        EditSession session = require(player);
        if (session == null) {
            return;
        }
        int n = session.frames().size();
        if (n == 0) {
            msg(player, "Nothing to preview yet.");
            return;
        }
        World editWorld = EditWorld.ensure(editWorldName);
        PlotBounds screen =
                geometry.screenBounds(session.laneIndex(), session.sizeX(), session.sizeY(), session.sizeZ());
        List<MCMEStoragePlotFrame> frames = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            PlotBounds b =
                    geometry.frameBounds(session.laneIndex(), i, session.sizeX(), session.sizeY(), session.sizeZ());
            loadChunks(editWorld, b);
            Selection plotSel = new Selection();
            plotSel.setPoint1(new Location(editWorld, b.minX(), b.minY(), b.minZ()));
            plotSel.setPoint2(new Location(editWorld, b.maxX(), b.maxY(), b.maxZ()));
            MCMEStoragePlotFrame frame = MCMEStoragePlotFrame.fromSelection(plotSel);
            if (frame != null) {
                frame.relocate(editWorldName, screen.minX(), screen.minY(), screen.minZ());
                frames.add(frame);
            }
        }
        if (frames.isEmpty()) {
            msg(player, "Nothing to preview.");
            return;
        }
        loadChunks(editWorld, screen);
        stopPreviewTask(player.getUniqueId());
        int interval =
                Math.max(1, AnimationsPlugin.getPluginInstance().getConfig().getInt("editor.plot.previewInterval", 10));
        UUID id = player.getUniqueId();
        int[] idx = {0};
        BukkitTask task = Bukkit.getScheduler()
                .runTaskTimer(
                        AnimationsPlugin.getPluginInstance(),
                        () -> {
                            if (!player.isOnline()) {
                                stopPreviewTask(id);
                                return;
                            }
                            frames.get(idx[0]).show();
                            idx[0] = (idx[0] + 1) % frames.size();
                        },
                        0L,
                        interval);
        previewTasks.put(id, task);
        player.teleport(new Location(
                editWorld, screen.minX() + screen.sizeX() / 2.0, screen.minY(), screen.minZ() + screen.sizeZ() / 2.0));
        msg(player, "Previewing " + frames.size() + " frame(s) on the screen. /anim plot preview stop to stop.");
    }

    /** Stops a running preview and resets the screen. */
    public void previewStop(Player player) {
        if (previewTasks.get(player.getUniqueId()) == null) {
            msg(player, "No preview running.");
            return;
        }
        stopPreviewTask(player.getUniqueId());
        EditSession session = sessions.resume(player.getUniqueId());
        if (session != null) {
            prepareScreen(session);
        }
        msg(player, "Preview stopped.");
    }

    private void stopPreviewTask(UUID player) {
        BukkitTask task = previewTasks.remove(player);
        if (task != null) {
            task.cancel();
        }
    }

    private void msg(Player player, String text) {
        player.sendMessage(Component.text(text));
    }
}
