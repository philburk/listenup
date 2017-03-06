package com.softsynth.javasonics.util;

import java.io.*;

/**
 * SampleDecoder is used to read samples from an encoded byte stream. Assume
 * stream is positioned within the sample data area. You can use: <br>
 * stream.skip( sample.getDataPosition() );<br>
 * on a freshly opened stream to seek to the beginning of data.
 * 
 * @author (C) 2001 Phil Burk, SoftSynth.com, All Rights Reserved
 */

interface SampleDecoder
{
	public int read( InputStream stream, short[] shorts, int offset,
			int numSamples ) throws IOException;

	public int read( InputStream stream, float[] floats, int offset,
			int numSamples ) throws IOException;
}
