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
import com.ivan1pl.animations.data.Animation;
import com.ivan1pl.animations.data.Animations;
import com.ivan1pl.animations.utils.MessageUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AnimCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("anim")
                .requires(src -> src.getSender().hasPermission(Permissions.PERMISSION_ADMIN))
                .then(Commands.literal("create")
                        .then(Commands.argument("world-project-name", StringArgumentType.word())
                                .executes(ctx ->
                                        openEditor(ctx, StringArgumentType.getString(ctx, "world-project-name")))))
                .then(Commands.literal("edit")
                        .then(Commands.argument("name", AnimationArgumentType.animation())
                                .executes(ctx -> openEditor(
                                        ctx,
                                        AnimationArgumentType.getAnimation(ctx, "name")
                                                .getName()))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", AnimationArgumentType.animation())
                                .executes(ctx -> {
                                    CommandSender sender = ctx.getSource().getSender();
                                    Animation animation = AnimationArgumentType.getAnimation(ctx, "name");
                                    animation.stop();
                                    String name = animation.getName();
                                    boolean success = Animations.deleteAnimation(name);
                                    if (success) {
                                        MessageUtil.sendInfoMessage(sender, Messages.MSG_ANIMATION_DELETED);
                                    } else {
                                        MessageUtil.sendErrorMessage(sender, Messages.MSG_DELETE_FAILED, name);
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("play")
                        .then(Commands.argument("name", AnimationArgumentType.animation())
                                .executes(ctx -> {
                                    AnimationArgumentType.getAnimation(ctx, "name")
                                            .play();
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("list")
                        .executes(ctx -> executeList(ctx.getSource().getSender(), 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> executeList(
                                        ctx.getSource().getSender(), IntegerArgumentType.getInteger(ctx, "page")))))
                .then(AnimSoundCommand.subcommand());
    }

    private static int openEditor(CommandContext<CommandSourceStack> ctx, String name) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            MessageUtil.sendErrorMessage(sender, Messages.MSG_PLAYER_ONLY);
            return 0;
        }
        AnimationsPlugin.getPluginInstance().getConversationFactory().startConversation(player, name);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeList(CommandSender sender, int page) {
        MessageUtil.sendInfoMessage(sender, Messages.MSG_DISPLAYING_PAGE, (long) page, (long) Animations.countPages());
        List<String> list = Animations.getPage(page);
        for (String item : list) {
            Animation anim = Animations.getAnimation(item);
            Location center = anim.getSelection().getCenter();
            MessageUtil.sendInfoMessage(
                    sender,
                    Messages.MSG_ITEM,
                    item + " (" + center.getX() + "," + center.getY() + "," + center.getZ() + ")");
        }
        return Command.SINGLE_SUCCESS;
    }
}
