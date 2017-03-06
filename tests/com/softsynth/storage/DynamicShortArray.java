package com.softsynth.storage.test;

import com.softsynth.storage.FixedShortArray;

/**
 * @author Phil Burk (C) 2004
 */
public class DynamicShortArray extends FixedShortArray
{
	private final static int SIZE_INCREMENT = 64 * 1024;

	/**
	 *  
	 */
	public DynamicShortArray()
	{
		super( SIZE_INCREMENT );
	}

	/* Make sure there is room for more data. */
	protected void checkRoom( int numAdditional )
	{
		int sizeNeeded = length() + numAdditional;
		if( sizeNeeded > getData().length )
		{
			// decide on new size
			int nextSize = sizeNeeded + SIZE_INCREMENT;
			short[] newBuffer = new short[nextSize];
			System.arraycopy( getData(), 0, newBuffer, 0, getData().length );
			setData( newBuffer );
		}
	}


	/** Write shorts to internal buffer. */
	public synchronized void write( short[] array, int firstIndex, int numShorts )
	{
		checkRoom( numShorts );
		super.write( array, firstIndex, numShorts );
	}

}