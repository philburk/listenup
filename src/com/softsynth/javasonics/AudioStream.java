package com.softsynth.javasonics;

/**
 * Stream for samples.
 * @author Phil Burk (C) 2001 SoftSynth.com
 * @version 0.1
 */

public interface AudioStream
{
/** Open the audio line and allocate its associated hardware resources.
 * @throws DeviceUnavailableException if the line cannot be opened because of a hardware resource restriction, or if the line has already been closed.
 * @throws IllegalStateException if already open.
*/
	public void open() throws DeviceUnavailableException;

/** Open the audio line and allocate its associated hardware resources.
 * Specify the size of the FIFO buffer in frames.
 * Note that if the buffer is too small then the audio may glitch periodically.
 * If the buffer is very large then there will be a very noticeable delay
 * between when the audio is written and when it is heard.
 * If you want to use the default size, then call open(),
 * or call open( SonicSystem.NOT_SPECIFIED ).
 * @version 0.6
 * @throws DeviceUnavailableException if the line cannot be opened because of a hardware resource restriction, or if the line has already been closed.
 * @throws IllegalStateException if already open.
*/
	public void open( int bufferSizeInFrames ) throws DeviceUnavailableException;

/** Is the stream currently open? */
	public boolean isOpen();

/** Start outputting audio. */
	public void start();

/** Wait for the previously written audio data to be played. If the stream is stopped, then this may wait forever. */
	public void drain();

/** Flush any pending data in the audio buffer. This will have no effect unless the stream is stopped. */
	public void flush();

/** Stop outputting audio. */
	public void stop();

/** Close the audio line and free its associated hardware resources.
 *  In order to ensure consistency with JavaSound based implementations,
 *  this cannot be reopened once closed.
 */
	public void close();

/** Return the number of frames that have been played so far. */
	public int getFramePosition();

/** How many samples can we read() or write() without blocking? */
	public int availableSamples();

/** Get size of audio FIFO buffer in frames. Set by open( int bufferSizeInFrames ). */
	public int getBufferSizeInFrames();

/** A stereo frame contains two samples. A mono frame contains one sample. */
	public int getSamplesPerFrame();

	public float getSampleRate();
}
