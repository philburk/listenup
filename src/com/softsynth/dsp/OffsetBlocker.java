package com.softsynth.dsp;

/**
 * A module in a signal processing chain.
 *
 * @author (C) 2003 Phil Burk, All Rights Reserved
 */

public class OffsetBlocker
	extends BufferedSignalProcessor
{
	float coefficient = 0.9996f;
	float yn_1 = 0.0f;
	float xn_1 = 0.0f;
	boolean initialized = false;
	
	/** Remove DC offset from a signal using a differentiator
	 * and a leaky integrator.
	 */
	public void write( float[] data, int offset, int numSamples )
	{
		// Initialize offset to first sample.
		if( !initialized )
		{
			if( numSamples > 0 ) xn_1 = data[0];
			initialized = true;
		}
		
		for( int i=0; i<numSamples; i++ )
		{
			// get next sample
			float xn = data[i + offset];
			// differentiate
			float diff = xn - xn_1;
			// leaky integrator
			float yn = (coefficient * yn_1) + diff;
			
			output( yn );

			// delay input and output
			xn_1 = xn;
			yn_1 = yn;
		}
	}

	public void flush()
	{
		super.flush();
		yn_1 = 0.0f;
		xn_1 = 0.0f;
		initialized = false;
	}
}
