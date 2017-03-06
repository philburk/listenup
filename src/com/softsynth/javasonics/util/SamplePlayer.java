package com.softsynth.javasonics.util;
import com.softsynth.javasonics.*;

/**
 * Play an AudioSample from memory in a Thread.
 * @author Phil Burk (C) 2001 SoftSynth.com
 * @version 0.6
 */

public class SamplePlayer extends AudioOutputStreamPlayer
{
	private boolean           loop;
	private AudioSample       sample;

	public SamplePlayer( AudioOutputStream stream, AudioSample sample )
	{
		super( stream );
		this.sample = sample;
	}

/** @return the sample that was passed to the constructor.
 */
    public AudioSample getSample()
    {
        return sample;
    }

/** Start playing the sample from the beginning.
 *  Plays until stop()ped or util the end of the sample. */
	public void start()
	{
        loop = false;
        super.start();
	}

/** Start playing the sample and continue.
 *  Plays the sample forever in a loop until the stop() method is called.
 */
	public void startLoop()
	{
        loop = true;
        super.start();
	}

/** Write the sample to the output stream.
 *  This is called internally by the AudioOutputStreamPlayer's run() method.
 *  You probably don't want to call this method directly.
 *  But you may want to redefine it to extend the play functionality.
 */
	public void play()
	{
        short samples[] = sample.getShorts();
        int blockSize = 1024;
        do
		{
            int cursor = 0;
            int numLeft = samples.length;

            while( SafeThread.getGo() && (numLeft > 0) )
			{
                int numToWrite = (numLeft < blockSize) ? numLeft : blockSize;
                getStream().write( samples, cursor, numToWrite );
                cursor += numToWrite;
                numLeft -= numToWrite;
			}

		} while( SafeThread.getGo() && loop );
	}
}
