package com.softsynth.awt;

import java.awt.Component;
import java.awt.Image;
import java.awt.image.BufferedImage;

import com.softsynth.javasonics.util.Logger;

/**
 * Try to create an image even if the Applet is not yet viewable.
 * @author Phil Burk (C) 2009 Mobileer Inc
 */
public class SafeImageFactory
{
	public static Image createImage( Component visibleComponent, int width,
			int height )
	{
		Image image = visibleComponent.createImage( width, height );
		if( image == null )
		{
			Logger
					.println( 0,
							"Could not createImage from Applet so used BufferedImage()." );
			image = new BufferedImage( width, height, visibleComponent
					.getColorModel().getTransferType() );
			if( image == null )
			{
				Logger
						.println( 0,
								"Could not get ColorModel from Applet so use TYPE_INT_ARGB." );
				image = new BufferedImage( width, height,
						BufferedImage.TYPE_INT_RGB );
			}
		}
		return image;
	}

}
