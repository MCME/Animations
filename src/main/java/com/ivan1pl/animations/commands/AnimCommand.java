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
package com.ivan1pl.animations.commands;

import com.ivan1pl.animations.AnimationsPlugin;
import com.ivan1pl.animations.constants.Messages;
import com.ivan1pl.animations.constants.Permissions;
import com.ivan1pl.animations.data.Animations;
import com.ivan1pl.animations.utils.MessageUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Ivan1pl
 */
public class AnimCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("anim")
            .requires(src -> src.getSender().hasPermission(Permissions.PERMISSION_ADMIN))
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    for (String name : Animations.getAnimationNames()) builder.suggest(name);
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player player)) {
                        MessageUtil.sendErrorMessage(sender, Messages.MSG_PLAYER_ONLY);
                        return 0;
                    }
                    String name = StringArgumentType.getString(ctx, "name");
                    AnimationsPlugin.getPluginInstance().getConversationFactory().startConversation(player, name);
                    return Command.SINGLE_SUCCESS;
                }));
    }
}
