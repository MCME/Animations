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

import com.ivan1pl.animations.constants.Messages;
import com.ivan1pl.animations.constants.Permissions;
import com.ivan1pl.animations.constants.SoundPlayMode;
import com.ivan1pl.animations.conversations.SelectSoundConversationPrompt;
import com.ivan1pl.animations.data.Animation;
import com.ivan1pl.animations.data.SoundData;
import com.ivan1pl.animations.utils.MessageUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AnimSoundCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> subcommand() {
        return Commands.literal("sound")
                .requires(src -> src.getSender().hasPermission(Permissions.PERMISSION_ADMIN))
                .then(Commands.argument("name", AnimationArgumentType.animation())
                        .then(setSubcommand())
                        .then(removeSubcommand())
                        .then(infoSubcommand()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> setSubcommand() {
        return Commands.literal("set")
                .then(Commands.argument("sound", ArgumentTypes.namespacedKey())
                        .suggests((ctx, builder) -> {
                            SelectSoundConversationPrompt.getFilteredSounds(builder.getRemainingLowerCase())
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("block-radius", IntegerArgumentType.integer(1))
                                // "In Java Edition, values less than 0.5 are equivalent to 0.5"
                                .then(Commands.argument("pitch", IntegerArgumentType.integer(50, 200))
                                        .then(Commands.literal("begin")
                                                .executes(ctx -> executeSet(ctx, SoundPlayMode.BEGIN)))
                                        .then(Commands.literal("end")
                                                .executes(ctx -> executeSet(ctx, SoundPlayMode.END)))
                                        .then(Commands.literal("all")
                                                .executes(ctx -> executeSet(ctx, SoundPlayMode.ALL_FRAMES))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> removeSubcommand() {
        return Commands.literal("remove").executes(ctx -> {
            AnimationArgumentType.getAnimation(ctx, "name").setSoundData(null);
            MessageUtil.sendInfoMessage(ctx.getSource().getSender(), Messages.MSG_SOUND_REMOVE_SUCCESS);
            return Command.SINGLE_SUCCESS;
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> infoSubcommand() {
        return Commands.literal("info").executes(ctx -> {
            CommandSender sender = ctx.getSource().getSender();
            SoundData sd = AnimationArgumentType.getAnimation(ctx, "name").getSoundData();
            if (sd == null) {
                MessageUtil.sendInfoMessage(sender, Messages.MSG_SOUND_NOT_SET);
            } else {
                MessageUtil.sendInfoMessage(
                        sender, Messages.MSG_SOUND_INFO, sd.getName(), sd.getPlayMode(), sd.getRange(), sd.getPitch());
            }
            return Command.SINGLE_SUCCESS;
        });
    }

    private static int executeSet(CommandContext<CommandSourceStack> ctx, SoundPlayMode mode) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            MessageUtil.sendErrorMessage(sender, Messages.MSG_PLAYER_ONLY);
            return 0;
        }

        Animation animation = AnimationArgumentType.getAnimation(ctx, "name");
        NamespacedKey soundKey = ctx.getArgument("sound", NamespacedKey.class);
        int radius = IntegerArgumentType.getInteger(ctx, "block-radius");
        int pitch = IntegerArgumentType.getInteger(ctx, "pitch");

        // Volume specifies the distance that the sound can be heard, default: 1 - a 16 block radius
        // The animation plugin stores volume x100 (to avoid floating point)
        int volume = Math.round(radius * 100.0f / 16);

        SoundData sd = new SoundData();
        sd.setName(soundKey.toString());
        sd.setRange(radius);
        sd.setPitch(pitch); // "Values lower than 1 lower the pitch and increase the duration" vice versa
        sd.setVolume(volume);
        sd.setPlayMode(mode);
        animation.setSoundData(sd);

        player.playSound(player.getLocation(), soundKey.toString(), volume / 100.f, pitch / 100.f);
        MessageUtil.sendInfoMessage(sender, Messages.MSG_SOUND_ADD_SUCCESS, soundKey, mode);
        return Command.SINGLE_SUCCESS;
    }
}
