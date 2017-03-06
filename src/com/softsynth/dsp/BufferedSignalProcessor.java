package com.softsynth.dsp;

/**
 * A module in a signal processing chain that has its own output buffer.
 *
 * @author (C) 2003 Phil Burk, All Rights Reserved
 */

public abstract class BufferedSignalProcessor implements SignalProcessor
{
	private SignalProcessor next;
	private float[] buffer;
	private int numInBuffer;
	
	/**
	 * 
	 */
	public BufferedSignalProcessor()
	{
		this( 256 );
	}

	/**
	 * @param bufferSize
	 */
	public BufferedSignalProcessor(int bufferSize)
	{
		buffer = new float[ bufferSize ];
	}

	public void output( float value )
	{
		buffer[ numInBuffer++ ] = value;
		if( numInBuffer == buffer.length )
		{
			sendBuffer();
		}
	}
	/* (non-Javadoc)
	 * @see com.softsynth.dsp.SignalProcessorSink#flush()
	 */
	private void sendBuffer()
	{
		int temp = numInBuffer;
		numInBuffer = 0;
		next.write( buffer, 0, temp );
	}
	/* (non-Javadoc)
	 * @see com.softsynth.dsp.SignalProcessorSink#flush()
	 */
	public void flush()
	{
		if( numInBuffer > 0 ) sendBuffer();
		next.flush();
	}
	
	/**
	 * @return
	 */
	public SignalProcessor getNext() {
		return next;
	}

	/**
	 * @param processor
	 */
	public void setNext(SignalProcessor processor) {
		next = processor;
	}

}
