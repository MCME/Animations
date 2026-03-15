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
package com.ivan1pl.animations.triggers;

import com.ivan1pl.animations.constants.MouseButton;
import com.ivan1pl.animations.data.AnimationsLocation;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Don't store whole trigger in animation, just object of this class.
 *
 * When animation is loaded it will use it to create a trigger.
 * @author Ivan1pl
 */
public class TriggerBuilderData implements Serializable {

    private static final long serialVersionUID = -955742584558670327L;

    private final TriggerType type;
    private final int range;
    private final String password;
    private final List<AnimationsLocation> triggerBlocks;
    private final List<MouseButton> triggerButtons;
    private final String animationName;
    private final int frame;

    public TriggerBuilderData(
            TriggerType type,
            int range,
            String password,
            List<AnimationsLocation> triggerBlocks,
            List<MouseButton> triggerButtons,
            String animationName,
            int frame) {
        this.type = type;
        this.range = range;
        this.password = password;
        this.triggerBlocks = triggerBlocks;
        this.triggerButtons = triggerButtons;
        this.animationName = animationName;
        this.frame = frame;
    }

    public TriggerBuilderData(TriggerType type, int range, String password) {
        this(type, range, password, null, null, null, -1);
    }

    public TriggerBuilderData(
            TriggerType type, int range, AnimationsLocation triggerBlock1, MouseButton triggerButton1) {
        this(type, range, null, List.of(triggerBlock1), List.of(triggerButton1), null, -1);
    }

    public TriggerBuilderData(TriggerType type, int range) {
        this(type, range, null, null, null, null, -1);
    }

    public TriggerBuilderData(TriggerType type, int range, String animationName, int frame) {
        this(type, range, null, null, null, animationName, frame);
    }

    public TriggerType getType() {
        return type;
    }

    public int getRange() {
        return range;
    }

    public String getPassword() {
        return password;
    }

    public List<AnimationsLocation> getTriggerBlocks() {
        return triggerBlocks;
    }

    public List<MouseButton> getTriggerButtons() {
        return triggerButtons;
    }

    public String getAnimationName() {
        return animationName;
    }

    public int getFrame() {
        return frame;
    }

    public void save(ConfigurationSection config) {
        ConfigurationSection section = config.createSection("TriggerBuilderData");
        section.set("TriggerType", type.name());
        section.set("Range", range);
        if (password != null) section.set("Password", password);
        section.set("Frame", frame);
        section.set("AnimationName", animationName);
        if (triggerBlocks != null) {
            ConfigurationSection blockSection = section.createSection("TriggerBlocks");
            for (int i = 0; i < triggerBlocks.size(); i++) {
                triggerBlocks.get(i).save("" + i, blockSection);
            }
        }
        if (triggerButtons != null) {
            ConfigurationSection buttonSection = section.createSection("TriggerButtons");
            for (int i = 0; i < triggerButtons.size(); i++) {
                buttonSection.set("" + i, triggerButtons.get(i).name());
            }
        }
    }

    public static TriggerBuilderData load(ConfigurationSection config) {
        ConfigurationSection section = config.getConfigurationSection("TriggerBuilderData");
        if (section == null) {
            return null;
        } else {

            List<AnimationsLocation> triggerBlocks = null;
            ConfigurationSection blockSection = section.getConfigurationSection("TriggerBlocks");
            if (blockSection != null) {
                triggerBlocks = new ArrayList<>();
                int i = 0;
                while (blockSection.isSet("" + i)) {
                    triggerBlocks.add(AnimationsLocation.load("" + i, blockSection));
                    i++;
                }
            }
            List<MouseButton> triggerButtons = null;
            ConfigurationSection buttonSection = section.getConfigurationSection("TriggerButtons");
            if (buttonSection != null) {
                triggerButtons = new ArrayList<>();
                int i = 0;
                while (buttonSection.isSet("" + i)) {
                    triggerButtons.add(MouseButton.valueOf(buttonSection.getString("" + i)));
                    i++;
                }
            }
            return new TriggerBuilderData(
                    TriggerType.valueOf(section.getString("TriggerType")),
                    section.getInt("Range"),
                    section.getString("Password", null),
                    triggerBlocks,
                    triggerButtons,
                    section.getString("AnimationName", null),
                    section.getInt("Frame", -1));
        }
    }
}
