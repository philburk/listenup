package com.softsynth.javasonics.recplay;

/**
 * Create a recording object with the appropriate encoding.
 * @author Phil Burk (C) 2004
 */
public interface RecordingFactory
{
	Recording createRecording( short[] data );
	Recording createRecording( int maxSamples );
	Recording createRecording();
}
