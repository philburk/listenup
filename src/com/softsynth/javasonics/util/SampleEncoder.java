package com.softsynth.javasonics.util;
import java.io.*;

/**
 * SampleDecoder is used to read samples from an encoded byte stream.
 *
 * @author (C) 2001 Phil Burk, SoftSynth.com, All Rights Reserved
 */

interface SampleEncoder
{
/** Calculate size in bytes required to store numSamples */
    public int calculateSize( int numSamples );

/** Assume stream is positioned within the sample data area.
 */
	public void write( OutputStream stream, short[] shorts, int offset, int numShorts ) throws IOException;

/** If any samples are stuck in a buffer, pad the buffer with zeroes, and encode last buffer.
 *  Only call this when you are done encoding samples.
 */
   	public void finish( OutputStream stream ) throws IOException;

}
