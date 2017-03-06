package com.softsynth.javasonics.recplay;

/** Something that acts like a tape recorder.
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */
public interface Recorder extends Player
{
	/** Begin recording. */
	public void recordAudio();

	/** Can we record anything? */
	public boolean isRecordable();
	
    public void record( short[] samples, int firstIndex, int numSamples);

    public void flush();
}

