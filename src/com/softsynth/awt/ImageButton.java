package com.softsynth.awt;

import java.awt.*;
import java.awt.event.*;

import com.softsynth.javasonics.util.Logger;

/**
 * A button that uses an Image instead of text.
 * 
 * @author (C) 2003 Phil Burk, All Rights Reserved
 */

public class ImageButton extends ImageLabel
{

	Image enabledImage;
	Image disabledImage;
	Image overImage;
	Image downImage;

	boolean isOver = false;
	boolean isDown = false;
	boolean wasClicked = false;

	ActionListener actionListener = null;
	String command;

	public ImageButton(Image enabledImage, Image disabledImage,
			Image overImage, Image downImage)
	{
		this.enabledImage = enabledImage;
		this.disabledImage = disabledImage;
		this.overImage = overImage;
		this.downImage = downImage;

		selectImage();
		addMouseListener( createMouseListener() );
	}

	public ImageButton(Image[] images)
	{
		this( images[0], images[1], images[2], images[3] );
	}

	public synchronized void addActionListener( ActionListener l )
	{
		actionListener = AWTEventMulticaster.add( actionListener, l );
	}

	public synchronized void removeActionListener( ActionListener l )
	{
		actionListener = AWTEventMulticaster.remove( actionListener, l );
	}

	protected void triggerActionEvent()
	{
		// Cache this to be thread safe.
		ActionListener listener = actionListener;
		Logger.println( 2, "ImageButton.triggerActionEvent(): listener = "
				+ listener );
		if( listener != null )
		{
			listener.actionPerformed( new ActionEvent( this,
					ActionEvent.ACTION_PERFORMED, command ) );
		}
	}

	protected MouseListener createMouseListener()
	{
		// It is tricky to get the right behavior.
		// On Windows Java 1.6, a mouseMoved inside the button will cause the
		// mouseClicked() method to not get called.
		// On Mac, the mouseEntered is often not called.
		// And if you drag outside the image then the mouseExited is not called
		// until after you release the mouse.
		final boolean debug = false;
		return new MouseAdapter()
		{
			// Warning! Sometimes this will not get called.
			public void mouseEntered( MouseEvent me )
			{
				isOver = true;
				wasClicked = false;
				if( debug )
				{
					Logger
							.println( 0,
									"ImageButton mouseEntered: isOver set to "
											+ isOver );
				}
				selectImage();
			}

			// Warning! Sometimes this will not get called.
			public void mouseExited( MouseEvent me )
			{
				isOver = false;
				if( debug )
				{
					Logger.println( 0,
							"ImageButton mouseExited: isOver set to " + isOver );
				}
				selectImage();
			}

			public void mousePressed( MouseEvent me )
			{
				isDown = true;
				isOver = true; // In case mouseEntered not called.
				wasClicked = false;
				if( debug )
				{
					Logger.println( 0,
							"ImageButton mousePressed: isDown set to " + isDown
									+ ", wasClicked set to " + wasClicked );
				}
				selectImage();
			}

			public void mouseReleased( MouseEvent me )
			{
				if( isDown && isOver )
				{
					// Check to see if we are still over the image because the
					// Mac does not always call mouseExited.
					Component compSource = me.getComponent();
					if( compSource != null )
					{
						if( (me.getX() >= 0)
								&& (me.getX() <= compSource.getBounds().width)
								&& (me.getY() >= 0)
								&& (me.getY() <= compSource.getBounds().height) )
						{
							triggerActionEvent();
							wasClicked = true;
						}
					}
				}
				isDown = false;
				if( debug )
				{
					Logger.println( 0,
							"ImageButton mouseReleased: isDown set to "
									+ isDown + ", wasClicked set to "
									+ wasClicked );
				}
				selectImage();
			}

			public void mouseClicked( MouseEvent me )
			{
				isDown = false;
				if( debug )
				{
					Logger.println( 0, "ImageButton mouseClicked()" );
				}
				if( !wasClicked )
				{
					triggerActionEvent();
				}
				wasClicked = false;
			}
		};
	}

	protected void selectImage()
	{
		Image nextImage = null;
		if( isEnabled() )
		{
			if( isDown )
				nextImage = downImage;
			else if( isOver )
				nextImage = overImage;
			else
				nextImage = enabledImage;
		}
		else
		{
			nextImage = disabledImage;
		}

		if( nextImage != null )
		{
			setImage( nextImage );
		}
	}

	public void setEnabled( boolean flag )
	{
		super.setEnabled( flag );
		selectImage();
	}

	public void setActionCommand( String command )
	{
		this.command = command;
	}

	public String getActionCommand()
	{
		return command;
	}
}
