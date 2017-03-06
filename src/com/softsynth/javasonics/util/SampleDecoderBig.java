package com.softsynth.javasonics.util;
import java.io.*;

/**
 * Decode 24 bit or other extended precision data.
 *
 * @author (C) 2001 Phil Burk, SoftSynth.com, All Rights Reserved
 */

abstract class SampleDecoderBig implements SampleDecoder
{
	/* Read data as little endian. */
	public abstract int readFXP32( InputStream stream ) throws IOException;
	
/** Load 16 bit data.
 */
	public int read( InputStream stream, short[] shorts, int offset, int numShorts ) throws IOException
    {
        int limit = offset + numShorts;
		for( int i=offset; i<limit; i++ )
		{
			shorts[i] = (short) (readFXP32( stream ) >> 16);
		}
        return numShorts;
	}
	
	public int read( InputStream stream, float[] floats, int offset, int numSamples ) throws IOException
    {
        int limit = offset + numSamples;
		float scalar = 1.0f / Integer.MAX_VALUE;

		for( int i=offset; i<limit; i++ )
		{
					
			floats[i] = ((float)readFXP32( stream )) * scalar;
		}
        return numSamples;
	}
}
