package com.softsynth.dsp;

/**
 * Convert sample rate.
 * 
 * @author (C) 2003 Phil Burk, All Rights Reserved
 */

public class SampleRateConverter extends BufferedSignalProcessor
{

	private float phaseIncrement = 1.0f;
	private float phase = 0.0f;
	private float S0 = 0.0f;
	private float S1 = 0.0f;
	private float S2 = 0.0f;
	private float S3 = 0.0f;
	private boolean unity = true;

	/**
	 * @param ratio
	 *            old sample rate over target sample rate
	 */
	public SampleRateConverter(float ratio)
	{
		phaseIncrement = ratio;
	}

	public void setRatio( float ratio )
	{
		// Is it really close to no conversion at all?
		unity = Math.abs( ratio - 1.0 ) < 0.0001;
		if( unity )
		{
			ratio = 0.0f;
			reset();
		}
		this.phaseIncrement = ratio;
	}

	public float getRatio()
	{
		return this.phaseIncrement;
	}

	/**
	 * Generate a new array of floats by resampling an input array. Reduce
	 * aliasing noise by using a third order, five point interpolation.
	 */

	public void write( float[] data, int offset, int numSamples )
	{
		if( unity  )
		{
			// Just pass the data through with no conversion so we don't get any creepy phase effects
			// or waste CPU cycles.
			for( int i = 0; i < numSamples; i++ )
			{
				output( data[ i + offset ] );
			}
		}
		else
		{
			// don't increment i because we advance by output samples
			for( int i = 0; i < numSamples; )
			{
				if( phase < 1.0 )
				{
					// Third order polynomial interpolation from Music-DSP
					// archive by Josh Scholar
					// I had to break it up into intermediate calculation
					// because Microsoft JVM
					// got confused and generated zero when it was one big
					// equation! Very scary.
					float da = ((S2 - S1) * 6) + ((S0 - S3) * 2);
					float db = ((S1 - S2) * 15) + ((S3 - S0) * 5)
							+ (phase * da);
					float dc = ((S2 - S1) * 9) + ((S0 - S3) * 3) + (phase * db);
					float outSample = S1
							+ 0.5f
							* phase
							* (S2 - S0 + phase
									* (S2 + S1 * (-2) + S0 + phase * dc));

					output( outSample );
					// Advance phase and step through input points as needed.
					phase += phaseIncrement;
				}

				while( (phase >= 1.0) && (i < numSamples) )
				{
					phase -= 1.0;
					S0 = S1;
					S1 = S2;
					S2 = S3;
					S3 = (float) data[offset + i++];
				}
			}
		}
	}

	public void flush()
	{
		super.flush();
		reset();
	}

	private void reset()
	{
		S0 = 0.0f;
		S1 = 0.0f;
		S2 = 0.0f;
		S3 = 0.0f;
		phase = 0.0f;
	}
}
