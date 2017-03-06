package com.softsynth.javasonics.util;
import java.io.*;

/**
 * Read 16 bit Big Endian PCM from a stream.
 *
 * @author (C) 2001 Phil Burk, SoftSynth.com, All Rights Reserved
 */

class SampleDecoder_Big24 extends SampleDecoderBig
{
	/* Read data as big endian. */
	public int readFXP32( InputStream stream ) throws IOException
	{
		int high_byte = stream.read();
		int mid_byte = stream.read();
		int low_byte = stream.read();
		int combined =
				((low_byte & 0xFF) << 8) |
				((mid_byte & 0xFF) << 16) |
				(high_byte << 24);
		return combined;
	}
	
}
