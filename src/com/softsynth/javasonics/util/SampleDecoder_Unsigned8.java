package com.softsynth.javasonics.util;
import java.io.*;

/**
 * Read 8 bit unsigned byte samples from a stream.
 *
 * @author (C) 2001 Phil Burk, SoftSynth.com, All Rights Reserved
 */

class SampleDecoder_Unsigned8 extends SampleDecoderBasic
{
/** Assume stream is positioned within the sample data area.
 *  You can use:
 *  <br>
 *  stream.skip( sample.getDataPosition() );
 *  <br>
 *  on a freshly opened stream to seek to the beginning of data.
 */
	public int read( InputStream stream, short[] shorts, int offset, int numShorts ) throws IOException
    {
        int limit = offset + numShorts;
		for( int i=offset; i<limit; i++ )
		{
			shorts[i] = (short) ((stream.read() - 128) << 8);
		}
        return numShorts;
	}
}
