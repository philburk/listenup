package com.softsynth.javasonics.recplay;

import com.softsynth.javasonics.util.Logger;
import com.softsynth.storage.*;

/**
 * An audio recording that can be any length.
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */
public class DynamicRecording extends FixedRecording
{
	public DynamicRecording(int maxSamplesRecordable, boolean editable,
			boolean useFileCache)
	{
		super( createShortArray( editable, useFileCache ), maxSamplesRecordable );
		setEditable( editable );
	}

	private static VirtualShortArray createShortArray( boolean editable,
			boolean useFileCache )
	{
		if( editable )
		{
			if( useFileCache )
			{
				return (VirtualShortArray) new LayeredShortArray();
			}
			else
			{
				return (VirtualShortArray) new EditableSegmentedShortArray();
			}
		}
		else
		{
			if( useFileCache )
			{
				return (VirtualShortArray) new FileBasedShortArray();
			}
			else
			{
				return (VirtualShortArray) new SegmentedShortArray();
			}
		}
	}

	/**
	 * Destroy any existing encoder, probably because we edited message.
	 */
	protected void deleteEncoder()
	{
	}

	/**
	 * @param index
	 * @param count
	 */
	public void delete( int index, int count )
	{
		if( shorts instanceof EditableShortArray )
		{
			// This used to crash before V1.87 is called for PlayerApplet
			((EditableShortArray) shorts).delete( index, count );
			deleteEncoder();
		}
	}

	/** Write shorts to internal buffer. */
	public synchronized void insert( int writeIndex, short[] samples,
			int firstIndex, int numSamples )
	{
		if( writeIndex > shorts.length() )
		{
			// throw new RuntimeException("Attempt to write beyond end of
			// recording. writeIndex = " + writeIndex + " > " +
			// shorts.length());
			// Try to recover from misplaced write pointer.
			Logger.println( 0,
					"Attempt to write beyond end of recording. writeIndex = "
							+ writeIndex + " > " + shorts.length() );
			writeIndex = shorts.length();
		}
		if( writeIndex == shorts.length() )
		{
			write( samples, firstIndex, numSamples );
		}
		else
		{
			((EditableShortArray) shorts).insert( writeIndex, samples,
					firstIndex, numSamples );
			deleteEncoder();
		}
		// Perform peak cache work after updating data in case we resize the
		// cache.
		insertPeaks( writeIndex, numSamples );
	}
}
