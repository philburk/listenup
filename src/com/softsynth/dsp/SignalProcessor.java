package com.softsynth.dsp;

/**
 * A module in a signal processing chain.
 *
 * @author (C) 2003 Phil Burk, All Rights Reserved
 */

public interface SignalProcessor
{
	/**
	 * Process data and pass it to the next modules in the chain. 
	 * @param data
	 * @param offset
	 * @param numSamples
	 */
	public void write( float[] data, int offset, int numSamples );
	
	/**
	 * Flush any remaining data in the system with zeros.
	 */
	public void flush();
}
