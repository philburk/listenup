package com.softsynth.javasonics.recplay;

/**
 * Result of a peak measurement of a recording.
 * @author Phil Burk (C) 2004
 */
public class PeakMeasurements
{
	public short[] mins;
	public short[] maxs;
	public int numSamples;
	
	public PeakMeasurements( int numBins )
	{
		mins = new short[numBins];
		maxs = new short[numBins];
	}
	
}
