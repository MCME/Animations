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

import com.ivan1pl.animations.constants.Messages;
import com.ivan1pl.animations.exceptions.InvalidSelectionException;
import com.ivan1pl.animations.utils.SerializationUtils;
import java.io.File;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 *
 * @author Ivan1pl, Eriol_Eandur
 */
public class MovingAnimation extends Animation {

    private IFrame frame;

    private IFrame background;

    private final Selection selection;

    private int stepX;
    private int stepY;
    private int stepZ;
    private int maxDistance;

    public MovingAnimation(Selection selection) throws InvalidSelectionException {
        if (!Selection.isValid(selection)) {
            throw new InvalidSelectionException();
        }
        this.selection = selection;
        frame = MCMEStoragePlotFrame.fromSelection(selection);
        background = MCMEStoragePlotFrame.fromSelection(selection);
    }

    public void updateBackground() {
        Selection s = SerializationUtils.clone(selection);
        s.expand(stepX * getFrameCount(), stepY * getFrameCount(), stepZ * getFrameCount());
        background = MCMEStoragePlotFrame.fromSelection(s);
    }

    @Override
    public boolean showFrame(int index) {
        if (index < 0 || index >= getFrameCount()) {
            return false;
        }
        background.show();
        frame.show(stepX * index, stepY * index, stepZ * index);
        return true;
    }

    @Override
    public int getFrameCount() {
        return maxDistance;
    }

    public void movePlayers(int index, boolean reverse) {
        List<Player> allPlayers = selection.getPoint1().getWorld().getPlayers();
        for (Player player : allPlayers) {
            Animations.debug(
                    Messages.DEBUG_MOVING_ANIMATION_CHECKING_PLAYER,
                    this.getClass().getName(),
                    player.getName());
            if (frame.isInside(player.getLocation(), stepX * index, stepY * index, stepZ * index)
                    && player.isOnGround()) {
                Animations.debug(Messages.DEBUG_MOVING_PLAYER, this.getClass().getName(), player.getName());
                if (reverse) {
                    player.teleport(player.getLocation().add(-stepX, -stepY, -stepZ));
                } else {
                    player.teleport(player.getLocation().add(stepX, stepY, stepZ));
                }
            }
        }
    }

    @Override
    public boolean isPlayerInRange(Player player, int range) {
        Selection s = background.toSelection();
        return player.getWorld().equals(s.getPoint1().getWorld())
                ? s.getDistance(player.getLocation()) <= range
                : false;
    }

    @Override
    public int getSizeInBlocks() {
        return frame.getSizeX() * frame.getSizeY() * frame.getSizeZ()
                + background.getSizeX() * background.getSizeY() * background.getSizeZ();
    }

    @Override
    protected Location getCenter() {
        return background.getCenter();
    }

    @Override
    public boolean prepare(File folder) {
        if (!folder.exists()) {
            folder.mkdir();
        }
        if (background instanceof MCMEStoragePlotFrame) {
            ((MCMEStoragePlotFrame) background).load(new File(folder, "background.mcme"));
        }
        if (frame instanceof MCMEStoragePlotFrame) {
            ((MCMEStoragePlotFrame) frame).load(new File(folder, "frame.mcme"));
        }
        return true;
    }

    public Selection getSelection() {
        return selection;
    }

    public int getStepX() {
        return stepX;
    }

    public void setStepX(int stepX) {
        this.stepX = stepX;
    }

    public int getStepY() {
        return stepY;
    }

    public void setStepY(int stepY) {
        this.stepY = stepY;
    }

    public int getStepZ() {
        return stepZ;
    }

    public void setStepZ(int stepZ) {
        this.stepZ = stepZ;
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
    }

    @Override
    public void save(File folder, ConfigurationSection config) {
        super.save(folder, config);
        config.set("AnimationType", AnimationType.MOVING.name());
        selection.save(config);
        config.set("StepX", stepX);
        config.set("StepY", stepY);
        config.set("StepZ", stepZ);
        config.set("MaxDistance", maxDistance);
        if (background instanceof MCMEStoragePlotFrame) {
            if (!folder.exists()) {
                folder.mkdir();
            }
            ((MCMEStoragePlotFrame) background).save(new File(folder, "background.mcme"));
        }
        if (frame instanceof MCMEStoragePlotFrame) {
            if (!folder.exists()) {
                folder.mkdir();
            }
            ((MCMEStoragePlotFrame) frame).save(new File(folder, "frame.mcme"));
        }
    }

    public static MovingAnimation load(ConfigurationSection config) throws InvalidSelectionException {
        MovingAnimation animation = new MovingAnimation(Selection.load(config));
        Animation.load(animation, config);
        animation.setStepX(config.getInt("StepX", 0));
        animation.setStepY(config.getInt("StepY", 0));
        animation.setStepZ(config.getInt("StepZ", 0));
        animation.setMaxDistance(config.getInt("MaxDistance", 0));
        animation.frame = MCMEStoragePlotFrame.fromSelection(animation.getSelection(), true);
        Selection backgroundSelection = expandedBackgroundSelection(
                animation.selection, animation.stepX, animation.stepY, animation.stepZ, animation.getFrameCount());
        animation.background = MCMEStoragePlotFrame.fromSelection(backgroundSelection, true);
        return animation;
    }

    /**
     * Builds the (larger) background selection from the frame selection. The frame selection
     * must be left untouched: {@link Selection#expand} mutates in place, so this clones first.
     * Aliasing the stored selection here used to grow it by {@code step * frameCount} on every
     * load/save cycle (audit finding H5).
     */
    static Selection expandedBackgroundSelection(Selection selection, int stepX, int stepY, int stepZ, int frameCount) {
        Selection expanded = SerializationUtils.clone(selection);
        expanded.expand(stepX * frameCount, stepY * frameCount, stepZ * frameCount);
        return expanded;
    }
}
