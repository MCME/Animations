/*
 *  Copyright (C) 2016 Ivan1pl
 *
 *  This file is part of Animations.
 *
 *  Animations is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Animations is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Animations.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.ivan1pl.animations.data;

import com.ivan1pl.animations.AnimationsPlugin;
import com.ivan1pl.animations.constants.Messages;
import com.ivan1pl.animations.constants.OperationResult;
import com.ivan1pl.animations.tasks.AnimationTask;
import com.ivan1pl.animations.triggers.Trigger;
import com.ivan1pl.animations.triggers.TriggerBuilder;
import com.ivan1pl.animations.utils.MessageUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 *
 * @author Ivan1pl, Eriol_Eandur
 */
public class Animations {

    private static final Map<String, Animation> animations = new HashMap<>();
    private static final Map<Animation, Trigger> triggers = new HashMap<>();
    private static final Map<UUID, Selection> selections = new HashMap<>();
    private static final Map<UUID, Location> blockSelections = new HashMap<>();

    private static final File PLUGIN_DIR =
            new File(AnimationsPlugin.getPluginInstance().getDataFolder() + File.separator + "animations");

    private static Material wandMaterial = null;
    private static Material blockSelectorMaterial = null;

    private static final Set<AnimationTask> runningTasks = new HashSet<>();

    private static final int PAGE_SIZE = 10;

    private static final int SUGGESTION_LIMIT = 50;

    private static boolean debugMode = false;

    private static int editorTimeout;
    private static String editorEscapeString;

    private static int maxFrameSize = 0;
    private static int maxRunningAnimations = 0;
    private static int maxProcessedBlocks = 0;
    private static int currentSize = 0;

    static {
        if (!PLUGIN_DIR.exists()) {
            PLUGIN_DIR.mkdirs();
        }
    }

    private Animations() {}

    public static void reload() {
        debugMode = AnimationsPlugin.getPluginInstance().getConfig().getBoolean("debug.enabled");

        String wand = AnimationsPlugin.getPluginInstance().getConfig().getString("wand");
        wandMaterial = parseMaterial(wand);

        String blockSelectorWand =
                AnimationsPlugin.getPluginInstance().getConfig().getString("blockSelectorWand");
        blockSelectorMaterial = parseMaterial(blockSelectorWand);

        editorTimeout = AnimationsPlugin.getPluginInstance().getConfig().getInt("editor.timeout");
        editorEscapeString = AnimationsPlugin.getPluginInstance().getConfig().getString("editor.escapeString");

        maxFrameSize = AnimationsPlugin.getPluginInstance().getConfig().getInt("limits.maxFrameSize");
        maxRunningAnimations = AnimationsPlugin.getPluginInstance().getConfig().getInt("limits.maxRunningAnimations");
        maxProcessedBlocks = AnimationsPlugin.getPluginInstance().getConfig().getInt("limits.maxProcessedBlocks");

        if (wandMaterial == null) {
            wandMaterial = Material.BLAZE_POWDER;
            AnimationsPlugin.getPluginInstance()
                    .getLogger()
                    .info(MessageUtil.formatMessage(
                            Messages.INFO_INVALID_MATERIAL, wand, Material.BLAZE_POWDER.toString()));
        }

        if (blockSelectorMaterial == null) {
            blockSelectorMaterial = Material.BLAZE_ROD;
            AnimationsPlugin.getPluginInstance()
                    .getLogger()
                    .info(MessageUtil.formatMessage(
                            Messages.INFO_INVALID_MATERIAL, blockSelectorWand, Material.BLAZE_ROD.toString()));
        }

        // Stop anything still running before dropping the animations it refers to, otherwise
        // orphaned tasks keep pasting frames for animations that no longer exist (audit finding M6).
        for (AnimationTask task : new ArrayList<>(runningTasks)) {
            task.stop();
        }
        runningTasks.clear();
        currentSize = 0;

        animations.clear();
        for (Trigger t : triggers.values()) {
            t.unregister();
        }
        triggers.clear();

        File[] animationDirs = PLUGIN_DIR.listFiles(File::isDirectory);
        if (animationDirs != null) {
            for (File f : animationDirs) {
                reloadAnimation(f.getName());
            }
        }
    }

    public static Animation getAnimation(String name) {
        return animations.get(name);
    }

    public static void setAnimation(String name, Animation animation) {
        animations.put(name, animation);
    }

    public static OperationResult saveAnimation(String name) {
        Animation animation = animations.get(name);
        if (animation != null) {
            if (!saveAnimation(name, animation)) {
                return OperationResult.INTERNAL_ERROR;
            }
        } else {
            return OperationResult.NOT_FOUND;
        }
        return OperationResult.SUCCESS;
    }

    private static boolean saveAnimation(String name, Animation animation) {
        boolean result = true;
        try {
            File folder = new File(PLUGIN_DIR, name);
            if (!folder.exists()) {
                folder.mkdir();
            }
            File f = new File(folder, "animation.yml");
            Files.deleteIfExists(f.toPath());

            YamlConfiguration config = new YamlConfiguration();
            animation.save(folder, config);
            config.save(f);
        } catch (IOException ex) {
            Logger.getLogger(Animations.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }
        return result;
    }

    public static boolean deleteAnimation(String name) {
        File folder = new File(PLUGIN_DIR, name);
        boolean retval = new File(folder, "animation.yml").delete();
        for (File file : folder.listFiles()) {
            retval = retval && file.delete();
        }
        retval = retval && folder.delete();
        if (retval) {
            Animation animation = animations.get(name);
            if (animation != null) {
                Trigger t = triggers.get(animation);
                if (t != null) {
                    t.unregister();
                }
                triggers.remove(animation);
            }
            animations.remove(name);
        }
        return retval;
    }

    public static Selection getSelection(Player p) {
        if (p == null) {
            return null;
        }

        Selection s = selections.get(p.getUniqueId());
        if (s == null) {
            selections.put(p.getUniqueId(), new Selection());
            s = selections.get(p.getUniqueId());
        }

        return s;
    }

    public static Location getBlockSelection(Player p) {
        if (p == null) {
            return null;
        }

        return blockSelections.get(p.getUniqueId());
    }

    public static void setBlockSelection(Player p, Location selection) {
        blockSelections.put(p.getUniqueId(), selection);
    }

    public static void reloadAnimation(String name) {
        File f = new File(new File(PLUGIN_DIR, name), "animation.yml");
        try {
            Animation animation = AnimationFactory.loadAnimation(f);
            if (animation != null) {
                Logger.getLogger(Animations.class.getName()).log(Level.INFO, "Loading Animation: " + name);
                if (!animation.prepare(new File(PLUGIN_DIR, name))) {
                    Logger.getLogger(Animations.class.getName())
                            .log(Level.WARNING, "Error while preparing Animation: " + name);
                    return;
                }
                Animation oldAnimation = animations.get(name);
                if (oldAnimation != null) {
                    Trigger t = triggers.get(oldAnimation);
                    if (t != null) {
                        t.unregister();
                    }
                    triggers.remove(oldAnimation);
                }
                animations.put(name, animation);
                if (animation.getTriggerBuilderData() != null) {
                    Trigger t = new TriggerBuilder(animation)
                            .setTriggerType(animation.getTriggerBuilderData().getType())
                            .setRange(animation.getTriggerBuilderData().getRange())
                            .setPassword(animation.getTriggerBuilderData().getPassword())
                            .setTriggerBlocks(animation.getTriggerBuilderData().getTriggerBlocks())
                            .setTriggerButtons(animation.getTriggerBuilderData().getTriggerButtons())
                            .setAnimationName(animation.getTriggerBuilderData().getAnimationName())
                            .setFrame(animation.getTriggerBuilderData().getFrame())
                            .create();
                    t.register();
                    triggers.put(animation, t);
                }
                AnimationsPlugin.getPluginInstance().getLogger().info(Messages.INFO_ANIMATION_LOADED + name);
            }
        } catch (Exception | LinkageError ex) {
            // Isolate a single bad animation so it cannot disable the whole plugin. LinkageError is
            // caught alongside Exception because a version-mismatched dependency surfaces as a
            // NoSuchMethodError/NoClassDefFoundError (an Error, not an Exception) — audit finding M2.
            Logger.getLogger(Animations.class.getName()).log(Level.SEVERE, "Failed to load animation: " + name, ex);
        }
    }

    private static Material parseMaterial(String name) {
        if (name == null) {
            return null;
        }
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static boolean registerTask(AnimationTask task) {
        if ((runningTasks.size() < maxRunningAnimations || maxRunningAnimations == -1)
                && (task.getAnimation().getSizeInBlocks() + currentSize <= maxProcessedBlocks
                        || maxProcessedBlocks == -1)) {
            currentSize += task.getAnimation().getSizeInBlocks();
            runningTasks.add(task);
            return true;
        }
        return false;
    }

    public static AnimationTask retrieveTask(Animation animation) {
        for (AnimationTask task : runningTasks) {
            if (task.getAnimation() == animation) {
                return task;
            }
        }
        return null;
    }

    public static void deleteTask(AnimationTask task) {
        currentSize -= task.getAnimation().getSizeInBlocks();
        runningTasks.remove(task);
    }

    public static int countPages() {
        return Math.max(1, (animations.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    public static List<String> getPage(int page) {
        Set<String> animationSet = animations.keySet();
        List<String> list = new ArrayList<>(animationSet);
        Collections.sort(list);
        int from = (page - 1) * PAGE_SIZE;
        int to = page * PAGE_SIZE;
        if (from > list.size()) {
            from = list.size();
        }
        if (to > list.size()) {
            to = list.size();
        }
        return list.subList(from, to);
    }

    public static void debug(String message) {
        if (debugMode) {
            Logger.getLogger("DEBUG").log(Level.INFO, "[DEBUG] " + message);
        }
    }

    public static void debug(String message, Object... parameters) {
        debug(MessageFormat.format(message, parameters));
    }

    public static boolean validateSelectionSize(Selection s) {
        return s.getVolume() <= maxFrameSize || maxFrameSize == -1;
    }

    public static void callEvent(Event event) {
        Bukkit.getServer().getPluginManager().callEvent(event);
    }

    public static String[] getAnimationNames() {
        Set<String> names = animations.keySet();
        return names.toArray(new String[names.size()]);
    }

    public static List<String> getFilteredAnimationNames(String searchTerm) {
        return animations.keySet().stream()
                .filter(name -> matchesAnimationFilter(name, searchTerm))
                .limit(SUGGESTION_LIMIT)
                .collect(java.util.stream.Collectors.toList());
    }

    private static boolean matchesAnimationFilter(String name, String searchTerm) {
        if (searchTerm.isEmpty()) return true;
        String[] parts = name.toLowerCase().split("-");
        int partsLength = parts.length;

        // segment = world-project-name, project-name, name
        for (int i = 0; i < partsLength; i++) {
            String segment = String.join("-", Arrays.copyOfRange(parts, i, partsLength));
            if (segment.startsWith(searchTerm)) {
                return true;
            }
        }
        return false;
    }

    public static Material getWandMaterial() {
        return wandMaterial;
    }

    public static Material getBlockSelectorMaterial() {
        return blockSelectorMaterial;
    }

    public static int getEditorTimeout() {
        return editorTimeout;
    }

    public static String getEditorEscapeString() {
        return editorEscapeString;
    }

    public static int getMaxFrameSize() {
        return maxFrameSize;
    }
}
