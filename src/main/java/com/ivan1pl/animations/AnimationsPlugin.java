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
package com.ivan1pl.animations;

import com.ivan1pl.animations.commands.AnimCommand;
import com.ivan1pl.animations.constants.Messages;
import com.ivan1pl.animations.conversations.EditAnimationConversationFactory;
import com.ivan1pl.animations.data.Animations;
import com.ivan1pl.animations.editor.EditWorld;
import com.ivan1pl.animations.editor.PlotEditor;
import com.ivan1pl.animations.listeners.PlayerListener;
import com.ivan1pl.animations.triggers.RangeTriggerListener;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.logging.Level;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Ivan1pl
 */
public class AnimationsPlugin extends JavaPlugin {

    private static AnimationsPlugin pluginInstance;
    private EditAnimationConversationFactory conversationFactory;
    private PlotEditor plotEditor;

    @Override
    public void onEnable() {
        pluginInstance = this;
        this.saveDefaultConfig();

        // Provision the private void world the plot-per-frame editor builds in. Fail-soft: if it
        // cannot be created the rest of the plugin still loads (audit M2/M3 posture).
        String editWorldName = getConfig().getString("editor.plot.world", "animations_edit");
        try {
            World editWorld = EditWorld.ensure(editWorldName);
            if (editWorld == null) {
                getLogger().warning("Plot editor: could not create edit world '" + editWorldName + "'.");
            } else {
                getLogger().info("Plot editor: edit world '" + editWorldName + "' ready.");
            }
        } catch (Exception | LinkageError ex) {
            getLogger().log(Level.SEVERE, "Plot editor: failed to provision the edit world.", ex);
        }
        plotEditor = new PlotEditor(
                editWorldName,
                getConfig().getInt("editor.plot.baseY", 64),
                getConfig().getInt("editor.plot.laneSpacing", 2048),
                getConfig().getInt("editor.plot.frameGap", 8));

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            final Commands registrar = commands.registrar();
            registrar.register(AnimCommand.command().build());
        });

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new RangeTriggerListener(), this);

        try {
            Animations.reload();
        } catch (Exception | LinkageError ex) {
            getLogger().log(Level.SEVERE, "Failed to load animations; the plugin will run with none loaded.", ex);
        }
        conversationFactory = new EditAnimationConversationFactory(this);

        getLogger().info(Messages.INFO_ENABLED);
    }

    public static AnimationsPlugin getPluginInstance() {
        return pluginInstance;
    }

    public EditAnimationConversationFactory getConversationFactory() {
        return conversationFactory;
    }

    public PlotEditor getPlotEditor() {
        return plotEditor;
    }
}
