package com.softsynth.dsp;

/**
 * Convert short to float in range of -1.0 to 0.9999..
 *
 * @author (C) 2003 Phil Burk, All Rights Reserved
 */

public class ShortToFloatProcessor
	extends BufferedSignalProcessor
{
	
	/** Just copy directly to output.
	 */
	public void write( float[] data, int offset, int numSamples )
	{
		for( int i=0; i<numSamples; i++ )
		{
			output( data[i + offset] );
		}
	}
	/** Convert short to float in range of -1.0 to 0.9999.
	 */
	public void write( short[] data, int offset, int numSamples )
	{
		for( int i=0; i<numSamples; i++ )
		{
			float fpdata = (data[i + offset] * (1.0f/32768.0f));
			output( fpdata );
		}
	}

}
