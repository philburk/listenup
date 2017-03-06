package com.softsynth.javasonics.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Phil Burk (C) 2004
 */
public class SampleEncoder_ULaw implements SampleEncoder
{

	/* One byte per sample
	 * @see com.softsynth.javasonics.util.SampleEncoder#calculateSize(int)
	 */
	public int calculateSize(int numSamples)
	{
		return numSamples;
	}

	/* Just convert
	 * @see com.softsynth.javasonics.util.SampleEncoder#write(java.io.OutputStream, short[], int, int)
	 */
	public void write(OutputStream stream, short[] shorts, int offset, int numShorts) throws IOException
	{
		int limit = offset + numShorts;
		for( int i=offset; i<limit; i++ )
		{
			int sample = shorts[i];
			int u8 = ULaw_Codec.convertLinearToULaw( sample );
			stream.write( u8 ); //  unsigned byte
		}
	}

	/* no buffering so nothing to do
	 * @see com.softsynth.javasonics.util.SampleEncoder#finish(java.io.OutputStream)
	 */
	public void finish(OutputStream stream) throws IOException
	{
	}

}
