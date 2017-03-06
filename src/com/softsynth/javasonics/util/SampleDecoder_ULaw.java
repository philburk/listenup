package com.softsynth.javasonics.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Phil Burk (C) 2004
 */
public class SampleDecoder_ULaw extends SampleDecoderBasic
{

	/* Convert ulaw bytes in WAV file to signed shorts.
	 * @see com.softsynth.javasonics.util.SampleDecoder#read(java.io.InputStream, short[], int, int)
	 */
	public int read(InputStream stream, short[] shorts, int offset, int numShorts) throws IOException
	{
		int limit = offset + numShorts;
		for( int i=offset; i<limit; i++ )
		{
			int nextUVal = stream.read();
			shorts[i] = (short) ULaw_Codec.convertULawToLinear(nextUVal);
		}
		return numShorts;
	}

}
