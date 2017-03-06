package com.softsynth.javasonics;

/**
 * AudioStream base class that tracks open and close state.
 * @author Phil Burk (C) 2001 SoftSynth.com
 * @version 0.1
 */

public abstract class AudioStreamBase
{
	private boolean opened = false;
	private boolean dead = false;
	protected int samplesPerFrame;
/** Used internally for permission validation. */
    public static final String inputPermissionFilename = "javasonics_in_perm.txt";

    public AudioStreamBase( int samplesPerFrame )
    {
        this.samplesPerFrame = samplesPerFrame;
    }

/** A stereo frame contains two samples. A mono frame contains one sample. */
	public int getSamplesPerFrame()
    {
        return samplesPerFrame;
    }

/** Called by subclasses when open()ed to check state.
 */
	protected void checkOpen() throws DeviceUnavailableException
	{
		if( opened ) throw new IllegalStateException("Audio already open.");
		else if( dead ) throw new DeviceUnavailableException("Attempt to re-open() audio after close().");
		else opened = true;
	}

/** Is the stream currently open? */
	public boolean isOpen()
	{
		return opened;
	}

/** Called by subclasses when close()d to check state.
 */
	protected void checkClose()
	{
		if( dead ) throw new IllegalStateException("Audio already close()d.");
		else if( !opened ) throw new IllegalStateException("Attempt to close() audio before open.");
		else
		{
			dead = true;
			opened = false;
		}
	}
}
