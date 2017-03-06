package com.softsynth.dsp;

/**
 * A module in a signal processing chain.
 *
 * @author (C) 2003 Phil Burk, All Rights Reserved
 */

public class SilenceTrimmer
	extends BufferedSignalProcessor
	implements SignalProcessor
{
	// Threshold determines what is considered silence.
	private float threshold = (float) 0.05;
	// How many silent samples will we tolerate before trimming.
	private int   countdownSilent = 0;
	// If we have more than this many silent samples then turn off recording.
	private int   maxSilent = 1000;

	// How many will we trim before disabling trimming?
	private int   countdownTrimmed = 0;
	// After a certain number of samples are trimmed start recording again.
	private int maxTrimmed = 100000;
	private boolean enableTrim = true;
	private static double MAX_TRIMMED_SECONDS = 60.0; // for security, to prevent eavesdropping
	/**
	 * @param trusted 
	 * @param d
	 */
	public SilenceTrimmer( double sampleRate, boolean trusted )
	{
		maxSilent = (int) (2.0 * sampleRate);
		maxTrimmed = trusted ? Integer.MAX_VALUE : (int) (MAX_TRIMMED_SECONDS * sampleRate);
		countdownTrimmed = maxTrimmed;
	}
	
	/** Trim silent sections from a stream.
	 */
	public void write( float[] data, int offset, int numSamples )
	{
		for( int i=0; i<numSamples; i++ )
		{
			float sample = data[ i + offset ];
			
			// Reset countdownSilent every time we see a valid sample.
			if( sample > threshold )
			{
				countdownSilent = maxSilent;
			}

			if( (countdownSilent > 0) || !enableTrim )
			{
				output( sample );
				countdownSilent -= 1;
				countdownTrimmed = maxTrimmed;
			} else
			{
				 // trimmed
				if( countdownTrimmed--  < 0 )
				{
					enableTrim = false;
				}
			}
		}
	}
	
	public void flush()
	{
		super.flush();
		countdownSilent = 0;
		enableTrim = true;
		countdownTrimmed = maxTrimmed;
	}
	
	/**
	 * @return threshold below which audio will be cutoff.
	 */
	public float getThreshold() {
		return threshold;
	}

	/**
	 * @param threshold below which audio will be cutoff
	 */
	public void setThreshold( float threshold ) {
		this.threshold = threshold;
	}

}
