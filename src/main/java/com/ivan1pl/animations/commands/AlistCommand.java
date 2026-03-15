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
import com.ivan1pl.animations.data.Animation;
import com.ivan1pl.animations.data.Animations;
import com.ivan1pl.animations.utils.MessageUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

/**
 *
 * @author Ivan1pl
 */
public class AlistCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("alist")
                .requires(src -> src.getSender().hasPermission(Permissions.PERMISSION_ADMIN))
                .executes(ctx -> executeList(ctx.getSource().getSender(), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx ->
                                executeList(ctx.getSource().getSender(), IntegerArgumentType.getInteger(ctx, "page"))));
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
