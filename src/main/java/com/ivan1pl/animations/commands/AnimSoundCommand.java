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
import com.ivan1pl.animations.data.Animation;
import com.ivan1pl.animations.data.Animations;
import com.ivan1pl.animations.data.SoundData;
import com.ivan1pl.animations.utils.MessageUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AnimSoundCommand {

    private static final int SUGGESTION_LIMIT = 50;
    private static final List<String> sounds;

    static {
        List<String> soundList = new ArrayList<>();
        for (Sound sound : Registry.SOUNDS) {
            NamespacedKey key = Registry.SOUNDS.getKey(sound);
            if (key != null) {
                soundList.add(key.toString());
            }
        }
        Collections.sort(soundList);
        sounds = Collections.unmodifiableList(soundList);
    }

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
                            getFilteredSounds(builder.getRemainingLowerCase()).forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("block-range", IntegerArgumentType.integer(1))
                                // "In Java Edition, values less than 0.5 are equivalent to 0.5"
                                .then(Commands.argument("pitch", FloatArgumentType.floatArg(0.5f, 2))
                                        .then(Commands.literal("begin")
                                                .executes(ctx -> executeSet(ctx, SoundPlayMode.BEGIN)))
                                        .then(Commands.literal("end")
                                                .executes(ctx -> executeSet(ctx, SoundPlayMode.END)))
                                        .then(Commands.literal("all")
                                                .executes(ctx -> executeSet(ctx, SoundPlayMode.ALL_FRAMES))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> removeSubcommand() {
        return Commands.literal("remove").executes(ctx -> {
            Animation animation = AnimationArgumentType.getAnimation(ctx, "name");
            animation.setSoundData(null);
            Animations.saveAnimation(animation.getName());
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
                        sender,
                        Messages.MSG_SOUND_INFO,
                        sd.getName(),
                        sd.getPlayMode(),
                        sd.getRange(),
                        sd.getPitch() / 100f);
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
        int range = IntegerArgumentType.getInteger(ctx, "block-range");
        float pitch = FloatArgumentType.getFloat(ctx, "pitch");

        // Volume specifies the distance that the sound can be heard, default: 1 - a 16 block radius
        // The animation plugin stores volume x100 (to avoid floating point)
        int volume = Math.round(range * 100.0f / 16);

        player.playSound(player.getLocation(), soundKey.toString(), volume / 100.f, pitch);

        SoundData sd = new SoundData();
        sd.setName(soundKey.toString());
        sd.setRange(range);
        // "Values lower than 1 will lower the pitch and increase the duration" vice versa
        sd.setPitch((int) (pitch * 100));
        sd.setVolume(volume);
        sd.setPlayMode(mode);
        animation.setSoundData(sd);
        Animations.saveAnimation(animation.getName());

        MessageUtil.sendInfoMessage(sender, Messages.MSG_SOUND_ADD_SUCCESS, soundKey, mode);
        return Command.SINGLE_SUCCESS;
    }

    private static List<String> getFilteredSounds(String prefix) {
        String lower = prefix.toLowerCase();
        return sounds.stream()
                // removing the minecraft namespace from suggestions
                .map(s -> s.startsWith("minecraft:") ? s.substring("minecraft:".length()) : s)
                .filter(s -> s.contains(lower))
                .limit(SUGGESTION_LIMIT)
                .collect(Collectors.toList());
    }
}
