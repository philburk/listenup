package com.softsynth.javasonics.sundev;
import javax.sound.sampled.*;

import com.softsynth.javasonics.DeviceUnavailableException;

/**
 * Implement AudioInputStream using SUN's JavaSound
 * @author Phil Burk (C) 2001 SoftSynth.com
 * @version 0.1
 */

class AudioInputStreamSun extends AudioStreamSun implements com.softsynth.javasonics.AudioInputStream
{
    TargetDataLine inLine;

    protected AudioInputStreamSun(TargetDataLine inLine, 
    		AudioFormat format,
    		int samplesPerFrame,
            int bitsPerSample) {
        super( (DataLine) inLine, format, samplesPerFrame, bitsPerSample);
        this.inLine = inLine;
    }

	public void open() throws DeviceUnavailableException
	{
		open(-1);
	}
	
    public void open(int bufferSizeInFrames) throws DeviceUnavailableException
    {
        checkOpen();
        try {
            if (bufferSizeInFrames < 0)
            {
                inLine.open(getFormat());
            }
            else
            {
            	inLine.open(getFormat(),
                            bufferSizeInFrames * samplesPerFrame *
                            bytesPerSample);
            }
            //System.out.println("Buffer size = " + inLine.getBufferSize() + " bytes." );
            if( false )
            {
            	AudioFormat actualFormat = inLine.getFormat();
            	System.out.println("AudioInputStreamSun requested frameRate = " + getFormat().getFrameRate() );

        		DataLine.Info info = new DataLine.Info(TargetDataLine.class, getFormat());
        		// format is an AudioFormat object
        		System.out.println("   supported = " + AudioSystem.isLineSupported(info) );
            	System.out.println("   actual frameRate = " + actualFormat.getFrameRate() );
            }
        }
        catch (LineUnavailableException ex) {
            throw new DeviceUnavailableException( "opening input, " + ex.toString() );
        }
    }

	public synchronized int read( short[] data, int offset, int numShorts )
	{
	// read short data in bursts that will fit in byte array
		int numShortsRead = 0;
		int numLeft = numShorts;
		while( numLeft > 0 )
		{
		// don't read past end of buffer
			int numToRead = (numLeft > FRAMES_PER_BUFFER) ? FRAMES_PER_BUFFER : numLeft;
			int numRead = readLittleShorts( data, offset, numToRead );
			if( numRead == 0 ) break; // in case line stopped
			numLeft -= numRead;
			offset += numRead;
			numShortsRead += numRead;
		}
		return numShortsRead;
	}

	public int read( short[] data ) throws InterruptedException, DeviceUnavailableException
	{
		return read( data, 0, data.length );
	}

/* ***************************************************************************/
/* ********* Private *********************************************************/
/* ***************************************************************************/

/** Read shorts from byte stream in LittleEndian format.
 * @throws InterruptedException 
 * @throws DeviceUnavailableException 
 */
	private int readLittleShorts( short[] data, int offset, int numShorts )
	{
		int bi = 0;
		int numBytesToRead = numShorts*2;
		if( avoidBlockingIO )
		{
			sleepUntilAvailable( numBytesToRead );
		}
		int numBytesRead = inLine.read( bytes, 0, numBytesToRead );
		int numShortsRead = numBytesRead / 2;
		for( int i=offset; i<(offset+numShortsRead); i++ )
		{
			int temp = ((int)bytes[bi++]) & 0x000000FF; // little end first
			temp |= bytes[bi++] << 8; // big end last
			data[i] = (short) temp;
		}
		return numShortsRead;
	}

    public void checkPermission() throws SecurityException {} // The Java Sandbox will check for us.

}