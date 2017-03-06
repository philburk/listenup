package com.softsynth.dsp;

import com.softsynth.javasonics.DeviceUnavailableException;

/**
 * Convert floats between -1.0 and 0.9999.. to short.
 *
 * @author (C) 2003 Phil Burk, All Rights Reserved
 */

public abstract class FloatToShortProcessor implements SignalProcessor
{
	
	/** Just copy directly to output.
	 * @throws InterruptedException 
	 * @throws DeviceUnavailableException 
	 */
	public abstract void write( short[] data, int offset, int numSamples );

	public int convertFloatToS16( float sample )
	{
		int result = (int) (32768.0f * sample);
		if( result > Short.MAX_VALUE ) result = Short.MAX_VALUE;
		else if( result < Short.MIN_VALUE ) result = Short.MIN_VALUE;
		return result;
	}

	/* Write floats to  write(short[]) method through buffer. */
	private short[] tempBuffer;
	public void write( float[] samples, int firstIndex, int numSamples )
	{
		if( tempBuffer == null ) tempBuffer = new short[256];
		int idx = firstIndex;
		while( numSamples > 0 )
		{
			int numToWrite = ( numSamples > tempBuffer.length ) ?
				tempBuffer.length : numSamples;
			for( int i=0; i<numToWrite; i++ )
			{
				tempBuffer[i] = (short) convertFloatToS16( samples[idx++] );
			}
			write( tempBuffer, 0, numToWrite );
			numSamples -= numToWrite;
		}
	}

}
