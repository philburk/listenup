package com.softsynth.javasonics.util;
import java.io.*;

/**
 * Write 8 bit unsigned data to a stream.
 * Dither using triangular high pass random 
 * values with 1 LSB amplitude.
 * Also apply second order noise shaping.
 *
 * @author (C) 2001 Phil Burk, SoftSynth.com, All Rights Reserved
 */

class SampleEncoder_Unsigned8 extends SampleEncoder_Big16
{
	private int random16 = 123;
	private int previous = 0;
	private int err1 = 0;
	private int err2 = 0;
	
	/** Generate random dither signal with triangular distribution. */
	private int nextDither()
	{
		// Use linear congruential algorithm to generate two random numbers.
		// Constants are from Hal Chamberlain's " Musical Applications
		// of Microprocessors".
		random16 = ((random16 * 13709) + 13849) & 0x0000FFFF;
		// High pass filter for triangular distribution
		int highPass = random16 - previous; // signed 17 bit
		previous = random16;
		return highPass >> 9; // shift down to +/- 128 range
	}
	
/** Calculate size in bytes required to store numSamples */
    public int calculateSize( int numSamples )
    {
        return numSamples;
    }

	/** Convert a signed short to an unsigned byte sample. */
	public int convertS16toU8( int signed16 )
	{
		// Add second order noise shaping function
		signed16 += (err1 + err1 - err2) >> 1;
		// Dither, round and shift to byte.
		int ditheredS8 = (signed16 + nextDither() + 0x0080) >> 8;
		err2 = err1;
		err1 = signed16 - (ditheredS8 << 8);
		int u8 = ditheredS8 + 128; //  unsigned byte
		if( u8 > 255 )u8 = 255;
		else if( u8 < 0 )u8 = 0;
		return u8;
	}
	
/** Assume stream is positioned within the sample data area.
 */
	public void write( OutputStream stream, short[] shorts, int offset, int numShorts ) throws IOException
    {
        int limit = offset + numShorts;
		for( int i=offset; i<limit; i++ )
		{
            int sample = shorts[i];
            int u8 = convertS16toU8( sample );
            stream.write( u8 ); //  unsigned byte
		}
	}
/*
	public static void main( String[] args )
	{
		SampleEncoder_Unsigned8 enc = new SampleEncoder_Unsigned8();
		int maxDither = -1000000;
		int minDither = 1000000;
		for( int i=0; i<500; i++ )
		{
			int dither = enc.nextDither();
			if( dither > maxDither ) maxDither = dither;
			else if( dither < minDither ) minDither = dither;
			System.out.println( "dither = " + dither);
		}
		System.out.println( "maxDither = " + maxDither);
		System.out.println( "minDither = " + minDither);
	}
*/
}
