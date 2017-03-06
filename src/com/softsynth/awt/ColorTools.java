package com.softsynth.awt;
import java.awt.*;

/**
 * Tools for manipulating color.
 *
 * @author (C) 2003 Phil Burk, All Rights Reserved
 */

public class ColorTools {

    /** Generate new color that is brighter than original.
     * @param degree interpolation factor, 0.0 yields original color, 1.0 yields white.
     */

    public static Color brighter( Color color, double degree) {
        float[] hsb = new float[3];
        Color.RGBtoHSB( color.getRed(), color.getGreen(), color.getBlue(), hsb );
        hsb[2] = (float) (hsb[2] + ((1.0 - hsb[2]) * degree));
        int rgb = Color.HSBtoRGB( hsb[0], hsb[1], hsb[2] );
        return new Color( rgb );
    }

    /** Generate new color that is darker than original.
     * @param degree interpolation factor, 0.0 yields original color, 1.0 yields black.
     */
    public static Color darker( Color color, double degree) {
        float[] hsb = new float[3];
        Color.RGBtoHSB( color.getRed(), color.getGreen(), color.getBlue(), hsb );
        hsb[2] = (float) (hsb[2] * (1.0 - degree));
        int rgb = Color.HSBtoRGB( hsb[0], hsb[1], hsb[2] );
        return new Color( rgb );
    }

}
