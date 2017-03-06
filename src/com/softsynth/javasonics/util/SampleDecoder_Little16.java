package com.softsynth.javasonics.util;
import java.io.*;

/**
 * Read 16 bit Little Endian PCM from a stream.
 *
 * @author (C) 2001 Phil Burk, SoftSynth.com, All Rights Reserved
 */

class SampleDecoder_Little16 extends SampleDecoderBasic
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
            int low_byte = stream.read();
            int high_byte = stream.read();
			shorts[i] = (short) ((low_byte & 0xFF) | (high_byte << 8));
		}
        return numShorts;
	}
}
