package com.softsynth.javasonics.util;
import java.io.*;

/**
 * Parse PC style RIFF File
 * RIFF is a modification of the IFF standard for Little Endian
 * Intel based machines.
 *
 * @see AudioSampleWAV
 * @author (C) 1997 Phil Burk, SoftSynth.com, All Rights Reserved
 */
public class RIFFParser extends IFFParser
{
	public static final int RIFF_ID = ('R'<<24) | ('I'<<16) | ('F'<<8) | 'F';

	public RIFFParser( InputStream stream )
	{
		super(stream);
	}

/** Read 32 bit signed short assuming Little Endian byte order. */
	public int readChunkSize()  throws IOException
	{
		return readIntLittle();
	}

	public boolean isForm( int ckid )   throws IOException
	{
		switch(ckid)
		{
		case LIST_ID:
		case RIFF_ID:
			return true;
		default:
			return false;
		}
	}
}
