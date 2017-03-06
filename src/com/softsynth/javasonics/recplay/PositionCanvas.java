package com.softsynth.javasonics.recplay;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Show value between 0.0 and 1.0.
 *
 * @author (C) 1997 Phil Burk, SoftSynth.com, All Rights Reserved
 */

/* ========================================================================== */
public class PositionCanvas extends Canvas
{
	ObservableProxy   observable;
	double            value = 0.0;  // Always between 0.0 and 1.0
    int               oldWidth = 0;
    int               oldHeight = 0;
    Dimension         minSize = new Dimension(100, 20);

	Integer           dragged = new Integer(0);
	Integer           released = new Integer(1);
	
	public PositionCanvas()
	{
		observable = new ObservableProxy();

		addMouseListener( new MouseAdapter()
			{
				public void mousePressed(MouseEvent e)
				{
					handleMouseDown( e, released );
				}
				public void mouseReleased(MouseEvent e)
				{
					handleMouseReleased( e, released );
				}
			} );
		addMouseMotionListener( new MouseMotionAdapter()
			{
				public void mouseDragged(MouseEvent e)
				{
					handleMouseDragged( e, dragged );
				}
			} );
	}

	void handleMouseDown( MouseEvent e, Object arg )
	{
        if( isEnabled() )
        {
		    setValue( xToValue( e.getX() ) );
		    observable.changed( arg );
        }
	}
	void handleMouseDragged( MouseEvent e, Object arg )
	{
		handleMouseDown( e, arg );
	}
	void handleMouseReleased( MouseEvent e, Object arg )
	{
		handleMouseDown( e, arg );
	}

/* Make new class to get around the fact that setChanged is protected. */
	class ObservableProxy extends Observable
	{
		void changed( Object arg )
		{
			{
				setChanged();
				notifyObservers( arg );
				clearChanged();
			}
		}
	}

	int valueToX( double value )
	{
		return (int) (bounds().width * value);
	}

	double xToValue( int x )
	{
		return ((double) x) / bounds().width;
	}

/** Add an Observer that will receive update() calls when the slider is moved using the mouse.
 */
	public void addObserver( Observer o )
	{
		observable.addObserver( o );
	}
	public void deleteObserver( Observer o )
	{
		observable.deleteObserver( o );
	}

/** Get current value, in range of 0.0 to 1.0 */
	public double getValue()
	{
		return value;
	}

	public void setValue( double val )
	{
	// clip to useable range
		if( val < 0.0 ) val = 0.0;
		else if( val > 1.0 ) val = 1.0;
		value = val;
		if( !((value >= 0) && (value <= 1)) ) new RuntimeException("setValue is " + value ).printStackTrace();
		repaint();
	}

	public void setEnabled( boolean flag )
	{
		super.setEnabled( flag );
		repaint();
	}

	/**
	 * @param player
	 * @return
	 */
	public double getMaxTime( Player player )
	{
		return player.getMaxTime();
	}
	
    protected void sizeChanged( int width, int height )
    {
        oldWidth = width;
        oldHeight = height;
    }

/* Override default paint action. */
	public void checkSize()
	{
		int width = bounds().width;
		int height = bounds().height;
        if( (width != oldWidth) || (height != oldHeight) )
        {
            sizeChanged( width, height );
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
