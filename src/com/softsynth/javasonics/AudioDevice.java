package com.softsynth.javasonics;

/**
 * Basic audio services for Java VMs with or without the JavaSound capability.
 * @author Phil Burk (C) 2001 SoftSynth.com
 * @version 0.1
 */

public interface AudioDevice
{
/** Free any resources associated with this device. */
	public void destroy();

/** Is there support for an AudioOutputStream capable of playing audio in the specified format?
 *  @param frameRate number of sample frames per second, typically 44100.0 or 22050.0
 *  @param samplesPerFrame one for mono, two for stereo
 *  @param bitsPerSample typically 16
 */
	public boolean isOutputSupported( float frameRate, int samplesPerFrame,
		int bitsPerSample );

/** Create an AudioOutputStream capable of playing audio in the specified format.
 *  @param frameRate number of sample frames per second, typically 44100.0 or 22050.0
 *  @param samplesPerFrame one for mono, two for stereo
 *  @param bitsPerSample typically 16
 */
	public AudioOutputStream getOutputStream( float frameRate, int samplesPerFrame,
		int bitsPerSample ) throws DeviceUnavailableException;

/** Is there support for an AudioInputStream capable of recording audio in the specified format?
 *  @param frameRate number of sample frames per second, typically 44100.0 or 22050.0
 *  @param samplesPerFrame one for mono, two for stereo
 *  @param bitsPerSample typically 16
 */
	public boolean isInputSupported( float frameRate, int samplesPerFrame,
		int bitsPerSample );

/** Create an AudioInputStream capable of recording audio in the specified format.
 *  @param frameRate number of sample frames per second, typically 44100.0 or 22050.0
 *  @param samplesPerFrame one for mono, two for stereo
 *  @param bitsPerSample typically 16
 */
	public AudioInputStream getInputStream( float frameRate, int samplesPerFrame,
		int bitsPerSample ) throws DeviceUnavailableException;

/** Create, open and start an AudioInputStream that matches as closely as possible
 * specified format.  We do this all together because we may need to do some tricks to
 * open and start a working stream.
 * 
 * @param frameRate
 * @param samplesPerFrame
 * @param latencyInFrames
 * @return stream already started
 * @throws DeviceUnavailableException 
 */
public AudioInputStream startSmartInputStream( double frameRate,
		int samplesPerFrame, int latencyInFrames ) throws DeviceUnavailableException;

/** Create, open and start an AudioOutputStream that matches as closely as possible
 * specified format.  We do this all together because we may need to do some tricks to
 * open and start a working stream.
 * 
 * @param frameRate
 * @param samplesPerFrame
 * @param latencyInFrames
 * @return stream already started
 * @throws DeviceUnavailableException 
 * @throws DeviceUnavailableException 
 */
public AudioOutputStream startSmartOutputStream( double frameRate,
		int samplesPerFrame, int latencyInFrames ) throws DeviceUnavailableException;

}
