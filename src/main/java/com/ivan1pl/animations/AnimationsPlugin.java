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
import com.ivan1pl.animations.listeners.PlayerListener;
import com.ivan1pl.animations.triggers.RangeTriggerListener;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Ivan1pl
 */
public class AnimationsPlugin extends JavaPlugin {

    private static AnimationsPlugin pluginInstance;
    private EditAnimationConversationFactory conversationFactory;

    @Override
    public void onEnable() {
        pluginInstance = this;
        this.saveDefaultConfig();

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
}
