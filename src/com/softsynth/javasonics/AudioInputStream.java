package com.softsynth.javasonics;

/**
 * Input stream for samples.
 * Samples can be read from an audio device associated with this stream.
 * @author Phil Burk (C) 2001 SoftSynth.com
 * @version 0.1
 */

public interface AudioInputStream extends AudioStream
{
/** Read an array of samples.
 *  If there is not enough data to be read, then this method will block until
 *  all of the data has been read.
 *  @param samples linear PCM audio samples, stereo samples should be interleaved left/right
 *  @param offset index of first sample in array to be used
 *  @param numShorts number of samples to read
 *  @return the number of samples actually read. May be zero if stream is not open and started.
 * @throws InterruptedException 
 * @throws DeviceUnavailableException 
 */
	public int read( short[] samples, int offset, int numShorts );

/** Check to make sure we have permission to use this stream on this computer. */
    public void checkPermission() throws SecurityException;
}
