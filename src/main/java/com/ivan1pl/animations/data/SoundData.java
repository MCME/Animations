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

import com.ivan1pl.animations.constants.SoundPlayMode;
import java.io.Serializable;
import org.bukkit.configuration.ConfigurationSection;

/**
 *
 * @author Ivan1pl
 */
public class SoundData implements Serializable {

    private static final long serialVersionUID = -8235280102289513906L;

    private String name = null;
    private SoundPlayMode playMode = null;
    private int range;
    private int pitch;
    private int volume;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SoundPlayMode getPlayMode() {
        return playMode;
    }

    public void setPlayMode(SoundPlayMode playMode) {
        this.playMode = playMode;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public int getPitch() {
        return pitch;
    }

    public void setPitch(int pitch) {
        this.pitch = pitch;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public void save(ConfigurationSection config) {
        ConfigurationSection section = config.createSection("SoundData");
        if (name != null) section.set("Name", name);
        if (playMode != null) section.set("PlayMode", playMode.name());
        section.set("Range", range);
        section.set("Pitch", pitch);
        section.set("Volume", volume);
    }

    public static SoundData load(ConfigurationSection config) {
        if (config.isConfigurationSection("SoundData")) {
            ConfigurationSection section = config.getConfigurationSection("SoundData");
            SoundData soundData = new SoundData();
            if (section.isSet("Name")) soundData.setName(section.getString("Name"));
            if (section.isSet("PlayMode")) soundData.setPlayMode(SoundPlayMode.valueOf(section.getString("PlayMode")));
            if (section.isSet("Range")) soundData.setRange(section.getInt("Range"));
            if (section.isSet("Pitch")) soundData.setPitch(section.getInt("Pitch"));
            if (section.isSet("Volume")) soundData.setVolume(section.getInt("Volume"));
            return soundData;
        }
        return null;
    }
}
