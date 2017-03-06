package com.softsynth.javasonics.recplay;
import java.awt.*;

/****************************************************************************
 * Bar Graph displayed as a set of circular LEDs.
 * Useful for VU meters, etc.
 * @author (C) 2000 Phil Burk, All Rights Reserved
 */
public class LEDBarDisplay extends BarGraphDisplay
{
    int LEDWidth;
    int LEDHeight;

    Color[] colors = { Color.blue, Color.green, Color.green, Color.green, Color.green, Color.orange, Color.red };
    Color[] darkerColors;

	public LEDBarDisplay( int orientation, double value, double min, double max )
	{
        super( orientation, value, min, max );
        setColors( colors );
	}

/** Specify the colors for the LEDs.
 *  The number of LEDs will be determined by the length of the array.
 *  The default is six LEDs with 1 blue, 3 green, 1 orange and 1 red.
 */
    public void setColors( Color[] colors )
    {
        this.colors = colors;
        darkerColors = new Color[ colors.length ];
        for( int i=0; i<colors.length; i++ )
        {
            darkerColors[i] = colors[i].darker().darker();
        }
    }

    public Color[] getColors()
    {
        return colors;
    }

/* Override default paint action. Draw "LEDs". */
	public void paint( Graphics g )
	{
		int dx,dy;
        int diameter;
		int width = bounds().width;
		int height = bounds().height;
        int numLEDs = colors.length;
		if( orientation == VERTICAL )
		{
            dx = 0;
            dy = height / numLEDs;
		    LEDWidth = width;
            LEDHeight = (dy * 4) / 5;
		}
		else
		{
            dx = width / numLEDs;
            dy = 0;
		    LEDWidth = (dx * 4) / 5;
            LEDHeight = height;
		}

        int x = 0;
        int y = 0;
        int firstDarkIndex = (int) (0.5 + (numLEDs * (value - min) * scalar));
        for( int i=0; i<numLEDs; i++ )
        {
            if( i >= firstDarkIndex ) g.setColor( darkerColors[i] );
            else g.setColor( colors[i] );

            g.fillRect( x, y, LEDWidth, LEDHeight );
            x += dx;
            y += dy;
        }
	}
}
