package com.softsynth.dsp;
import java.util.*;

/**
 * Split a signal out to multiple other signal processors.
 * Implements multiple fan-out.
 *
 * @author (C) 2003 Phil Burk, All Rights Reserved
 */

public class SignalProcessorSplitter
	implements SignalProcessor
{
	Vector sinks = new Vector();

	/**
	 * Add another sink to the processing chain. 
	 * @param sink
	 */
	public void addSink(SignalProcessor sink)
	{
		sinks.addElement( sink );
	}
	
	/**
	 * Remove sink from the processing chain. 
	 * @param sink
	 */
	public void removeSink(SignalProcessor sink)
	{
		sinks.removeElement( sink );
	}
	
	public void write(float[] data, int offset, int numSamples)
	{
		Enumeration sinkers = sinks.elements();
		while( sinkers.hasMoreElements() )
		{
			SignalProcessor sink = (SignalProcessor) sinkers.nextElement();
			sink.write( data, offset, numSamples );
		}
	}

	/* (non-Javadoc)
	 * @see com.softsynth.dsp.SignalProcessorSink#flush()
	 */
	public void flush()
	{
		Enumeration sinkers = sinks.elements();
		while( sinkers.hasMoreElements() )
		{
			SignalProcessor sink = (SignalProcessor) sinkers.nextElement();
			sink.flush();
		}
	}
}
