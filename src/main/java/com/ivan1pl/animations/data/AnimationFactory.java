package com.ivan1pl.animations.data;

import com.ivan1pl.animations.exceptions.InvalidSelectionException;
import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class AnimationFactory {

    public static Animation loadAnimation(File file) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
            AnimationType type = AnimationType.valueOf(config.getString("AnimationType"));
            Animation animation =
                    switch (type) {
                        case STATIONARY -> StationaryAnimation.load(config);
                        case MOVING -> MovingAnimation.load(config);
                    };
            animation.setName(file.getParentFile().getName());
            return animation;
        } catch (IOException | InvalidConfigurationException | InvalidSelectionException e) {
            e.printStackTrace();
        }
        return null;
    }
}
