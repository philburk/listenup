package com.softsynth.javasonics.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Phil Burk (C) 2004
 */
public abstract class SampleDecoderBasic implements SampleDecoder
{

	public abstract int read( InputStream stream, short[] shorts, int offset,
			int numSamples ) throws IOException;
	
	/* Convert ulaw bytes in WAV file to signed shorts.
	 * @see com.softsynth.javasonics.util.SampleDecoder#read(java.io.InputStream, short[], int, int)
	 */
	public int read(InputStream stream, float[] floats, int offset, int numSamples) throws IOException
	{
		/* Read data into a short array, then convert to floats.
		 * This will be the default behavior for 4,8,16 bit samples. */
		short[] shortData = new short[ numSamples ];
		read( stream, shortData, 0, numSamples );

		int limit = offset + numSamples;
		float scalar = 1.0f / AudioSampleLoader.FLOAT_CONVERSION_LIMIT;
		for( int i=offset; i<limit; i++ )
		{
			floats[i] = shortData[i - offset] * scalar;
		}
		
		return numSamples;
	}

}
