package com.softsynth.javasonics.recplay;

import java.awt.*;

/*******************************************************************************
 * Bar Graph extracted from Wire. Useful for VU meters, etc.
 * 
 * @author (C) 2000 Phil Burk, All Rights Reserved
 */
public class BarGraphDisplay extends Canvas
{
	public final static int HORIZONTAL = 0;
	public final static int VERTICAL = 1;
	protected double value;
	protected double min = 0.0;
	protected double max = 1.0;
	protected int orientation = VERTICAL;
	protected Dimension minSize;
	protected double scalar = 1.0;
	private int numDivisions = 5;

	public BarGraphDisplay(int orientation, double value, double min, double max)
	{
		this.orientation = orientation;
		this.value = value;
		this.min = min;
		this.max = max;
		if( orientation == VERTICAL )
		{
			minSize = new Dimension( 14, 50 );
		}
		else
		{
			minSize = new Dimension( 70, 16 );
		}
		updateScalar();
	}

	/**
	 * Set number of scale divisions for background. If 0 then do not draw
	 * background divisions.
	 */
	public void setNumDivisions( int divisions )
	{
		numDivisions = divisions;
	}

	public int getNumDivisions()
	{
		return numDivisions;
	}

	void updateScalar()
	{
		scalar = 1.0 / (max - min);
	}

	public void set( double newValue )
	{
		if( newValue < min )
			newValue = min;
		else if( newValue > max )
			newValue = max;
		if( value != newValue )
		{
			value = newValue;
			repaint();
		}
	}

	public double get()
	{
		return value;
	}

	int valueToY( double value )
	{
		return 1 + (int) ((getHeight() - 2) * (value - min) * scalar);
	}

	int valueToX( double value )
	{
		return 1 + (int) ((getWidth() - 1) * (value - min) * scalar);
	}

	/* Override default paint action. Draw fader. */
	public void paint( Graphics g )
	{
		int y;
        int width = getWidth();
        int height = getHeight();

		// Draw background bars.
		if( numDivisions > 0 )
		{
			Color savedColor = g.getColor();
			g.setColor( getForeground() );
			if( orientation == VERTICAL )
			{
				int x = width / 2;
				g.drawLine( x, 0, x, height );
				for( int i = 0; i <= numDivisions; i++ )
				{
					y = (int) ((height * i) / numDivisions);
					g.drawLine( 0, y, width, y );
				}
			}
			else
			{
				y = height / 2;
				g.drawLine( 0, y, width, y );
				for( int i = 0; i <= numDivisions; i++ )
				{
					int x = (int) (((width - 1) * i) / numDivisions);
					g.drawLine( x, 0, x, height );
				}

			}
			g.setColor( savedColor );
		}

		if( orientation == VERTICAL )
		{
			y = valueToY( value );
			g.fill3DRect( 0, height - y, width, y, true );
		}
		else
		{
			g.fill3DRect( 0, 0, valueToX( value ), height, true );
		}
	}

	public Dimension minimumSize()
	{
		return minSize;
	}

	public Dimension getMinimumSize()
	{
		return minSize;
	}

	public Dimension preferredSize()
	{
		return minSize;
	}

	public Dimension getPreferredSize()
	{
		return minSize;
	}
}
