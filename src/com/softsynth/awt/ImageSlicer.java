package com.softsynth.awt;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;

/**
 * A Component that can be used to slice an Image up into pieces. This is useful
 * for loading skins.
 * 
 * @author (C) 2003 Phil Burk, All Rights Reserved
 */

public class ImageSlicer
{
	Image image;
	Component visibleComponent;

	public ImageSlicer(Component visibleComponent, Image image)
	{
		this.image = image;
		this.visibleComponent = visibleComponent;
	}

	private Image createSubImage( int x, int y, int width, int height )
	{
		// Create a buffered image that supports transparency
		Image subImage = SafeImageFactory.createImage( visibleComponent, width, height );
		if( subImage == null )
		{
			throw new NullPointerException( "SafeImageFactory.createImage(" + width
					+ ", " + height + ") failed." );
		}
		Graphics g = subImage.getGraphics();
		g.drawImage( image, -x, -y, visibleComponent );
		return subImage;
	}


	/** Slice image into a complete series of sub images. */
	public Image[] createSubImageRow( int rowIndex, int numRows, int numColumns )
	{
		int numSlices = numColumns;
		Image[] images = new Image[numSlices];
		int subWidth = image.getWidth( null ) / numColumns;
		int subHeight = image.getHeight( null ) / numRows;
		int y = subHeight * rowIndex;
		for( int ic = 0; ic < numColumns; ic++ )
		{
			int x = subWidth * ic;
			images[ic] = createSubImage( x, y, subWidth, subHeight );
		}
		return images;
	}

	/** Slice image into a complete series of sub images. */
	public Image[] createAllSubImages( int numRows, int numColumns )
	{
		int numSlices = numRows * numColumns;
		Image[] images = new Image[numSlices];
		int subWidth = image.getWidth( null ) / numColumns;
		int subHeight = image.getHeight( null ) / numRows;
		int idx = 0;
		for( int ir = 0; ir < numRows; ir++ )
		{
			int y = subHeight * ir;
			for( int ic = 0; ic < numColumns; ic++ )
			{
				int x = subWidth * ic;
				images[idx++] = createSubImage( x, y, subWidth, subHeight );
			}
		}
		return images;
	}

}
