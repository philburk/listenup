package com.softsynth.awt;

import java.awt.*;

/**
 * A passive label that uses an Image instead of text.
 * 
 * @author (C) 2003 Phil Burk, All Rights Reserved
 */

public class ImageLabel extends Canvas
{
	Image image;

	public ImageLabel()
	{
		super();
	}

	public ImageLabel(Image image)
	{
		setImage( image );
	}

	public void setImage( Image image )
	{
		if( this.image != image )
		{
			this.image = image;
			setSize( image.getWidth( this ), image.getHeight( this ) );
			repaint();
		}
	}

	public Image getImage()
	{
		return image;
	}

	public void paint( Graphics g )
	{
		g.drawImage( image, 0, 0, this );
	}
}
