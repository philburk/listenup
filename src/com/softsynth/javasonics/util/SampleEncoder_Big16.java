package com.softsynth.javasonics.util;
import java.io.*;

/**
 * Read 16 bit Little Endian PCM from a stream.
 *
 * @author (C) 2001 Phil Burk, SoftSynth.com, All Rights Reserved
 */

class SampleEncoder_Big16 implements SampleEncoder
{
/** Calculate size in bytes required to store numSamples */
    public int calculateSize( int numSamples )
    {
        return numSamples * 2;
    }

/** Assume stream is positioned within the sample data area.
 */
	public void write( OutputStream stream, short[] shorts, int offset, int numShorts ) throws IOException
    {
        int limit = offset + numShorts;
		for( int i=offset; i<limit; i++ )
		{
            short sample = shorts[i];
            stream.write( sample >> 8 ); // high byte
            stream.write( sample ); // low byte
		}
	}

/** No samples are buffer by this encoder. */
   	public void finish( OutputStream stream ) throws IOException
    {
    }
}
