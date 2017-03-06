package com.softsynth.javasonics.recplay;

import com.softsynth.storage.FixedShortArray;
import com.softsynth.storage.VirtualShortArray;

/**
 * An audio recording of a fixed maximum length.
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */
public class FixedRecording extends Recording
{
	VirtualShortArray shorts;

	public FixedRecording(VirtualShortArray shorts, int maxSamplesRecordable )
	{
		this.maxSamplesRecordable = maxSamplesRecordable;
		this.shorts = shorts;
	}

	public FixedRecording( int maxSamples )
	{
		this( new FixedShortArray( maxSamples ), maxSamples );
	}
	/**
	 *  Create a recording with preset data.
	 */
	public FixedRecording( short[] data )
	{
		this( new FixedShortArray( data ), data.length );
	}

	/** Clean up any temporary structures created during the editing process. */
	public void finalizeEdits()
	{
		shorts.flatten();
	}
	
	/** Write shorts to end of internal buffer. */
	public void write( short[] samples, int firstIndex,
			int numSamples )
	{
		shorts.write( samples, firstIndex, numSamples );
	}

	/** Write shorts to internal buffer. */
	public synchronized void insert( int writeIndex, short[] samples, int firstIndex,
			int numSamples )
	{
		if( writeIndex != shorts.length() ) throw new RuntimeException("Can only append to FixedRecording!");
		shorts.write( samples, firstIndex, numSamples );
	}

	/** Read shorts from internal buffer. */
	public void read( int rdIndex, short[] samples, int firstIndex,
			int numSamples )
	{
		shorts.read( rdIndex, samples, firstIndex, numSamples );
	}

	private void reverseArray( short[] samples )
	{
		for( int i = 0; i < samples.length / 2; i++ )
		{
			int swapIndex = samples.length - i - 1;
			short temp = samples[i];
			samples[i] = samples[swapIndex];
			samples[swapIndex] = temp;
		}
	}

	/** Read shorts from internal buffer. Backwards version. */
	public synchronized void readBackwards( int readIndex, short[] samples, int firstIndex,
			int numSamples )
	{
		shorts.read( readIndex, samples, firstIndex, numSamples );
		reverseArray( samples );
	}

	/* For SignalProcessor interface. */
	public void flush()
	{
	}

	/* (non-Javadoc)
	 * @see com.softsynth.javasonics.recplay.Recording#getMaxSamplesPlayable()
	 */
	public int getMaxSamplesPlayable()
	{
		return shorts.length();
	}

    /** Delete any previously recorded material. */
    public synchronized void erase()
    {
    	shorts.clear();
    }
}