/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ivan1pl.animations.data;

import org.bukkit.Location;

/**
 *
 * @author Eriol_Eandur
 */
public interface IFrame {

    int getSizeX();

    int getSizeY();

    int getSizeZ();

    void show();

    void show(int offsetX, int offsetY, int offsetZ);

    boolean isInside(Location location, int offsetX, int offsetY, int offsetZ);

    Selection toSelection();

    Location getCenter();

    boolean isOutdated();
}
