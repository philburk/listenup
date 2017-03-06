package com.softsynth.javasonics;

/**
 * Output stream for samples.
 * Samples can be written to an audio device associated with this stream.
 * The sample rate, pan and gain can be controlled.
 * @author Phil Burk (C) 2001 SoftSynth.com
 * @version 0.1
 */

public interface AudioOutputStream extends AudioStream
{
/** Write an array of samples to be played. The samples will be copied into a buffer.
 *  If there is not enough room in the buffer, then this method will block until
 *  all of the data has been written. You may overwrite the array at any time after
 *  this routine returns.
 *  @param samples linear PCM audio samples, stereo samples should be interleaved left/right
 *  @param offset index of first sample in array to be used
 *  @param numShorts number of samples to write
 *  @return number of shorts written
 * @throws InterruptedException 
 * @throws DeviceUnavailableException 
 */
	public int write( short[] samples, int offset, int numShorts );
	public int write( short[] samples );

/** Set volume of this stream in decibels.
 *  The default is 0.0 for normal volume.
 *  A value of -6.0 will be at half amplitude.
 */
	public void setGain( float dB );
	public float getGain();

/** Set left/right position of this stream in a stereo field.
 *  The default center value is 0.0. A value of -1.0 is full left
 *  and +1.0 is full right.
 *  Note that if a signal is panned away from center, then you may have to turn down
 *  the gain to prevent clipping.
 */
	public void setPan( float pan );
	public float getPan();

/** Set sample rate for playback of this stream.
 *  Values range from 0.0 to 48000.0 Hz.
 */
	public void setSampleRate( float rate );

/** Get the number of unplayed samples currently in the buffer.
 *  This can be used to implement your own version of drain().
 */
    public int getRemainingSampleCount();
}
