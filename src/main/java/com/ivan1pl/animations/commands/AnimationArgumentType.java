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

import com.ivan1pl.animations.data.Animation;
import com.ivan1pl.animations.data.Animations;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;

public class AnimationArgumentType implements CustomArgumentType.Converted<Animation, String> {

    private static final DynamicCommandExceptionType UNKNOWN_ANIMATION =
            new DynamicCommandExceptionType(val -> MessageComponentSerializer.message()
                    .serialize(Component.text("Unable to find an animation matching '" + val + "'")));

    public static AnimationArgumentType animation() {
        return new AnimationArgumentType();
    }

    public static Animation getAnimation(CommandContext<?> ctx, String argName) {
        return ctx.getArgument(argName, Animation.class);
    }

    @Override
    public @NonNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public Animation convert(String nativeValue) throws CommandSyntaxException {
        Animation animation = Animations.getAnimation(nativeValue);
        if (animation == null) {
            throw UNKNOWN_ANIMATION.create(nativeValue);
        }
        return animation;
    }

    @Override
    public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(
            CommandContext<S> ctx, SuggestionsBuilder builder) {
        Animations.getFilteredAnimationNames(builder.getRemainingLowerCase()).forEach(builder::suggest);
        return builder.buildFuture();
    }
}
