package com.softsynth.javasonics.sundev;
import com.softsynth.javasonics.*;

import javax.sound.sampled.*;

/**
 * Implement AudioOutputStream using SUN's JavaSound
 * @author Phil Burk (C) 2001 SoftSynth.com
 * @version 0.1
 */

class AudioOutputStreamSun extends AudioStreamSun implements AudioOutputStream
{
	SourceDataLine outLine;
	private FloatControl gainControl = null;
	private FloatControl panControl = null;
	private FloatControl rateControl = null;

	protected AudioOutputStreamSun(
		SourceDataLine outLine,
		AudioFormat format,
		int samplesPerFrame,
		int bitsPerSample)
	{
		super((DataLine) outLine, format, samplesPerFrame, bitsPerSample);

		this.outLine = outLine;

		try
		{
			gainControl =
				(FloatControl) outLine.getControl(
					FloatControl.Type.MASTER_GAIN);
		} catch (IllegalArgumentException ex)
		{
			//System.err.println("MASTER_GAIN control not supported in this version of JavaSound!");
		}
		try
		{
			panControl =
				(FloatControl) outLine.getControl(FloatControl.Type.PAN);

		} catch (IllegalArgumentException ex)
		{
			//System.err.println("PAN control not supported in this version of JavaSound!");
		}
		try
		{
			rateControl =
				(FloatControl) line.getControl(FloatControl.Type.SAMPLE_RATE);
		} catch (IllegalArgumentException ex)
		{
			//System.err.println("SAMPLE_RATE control not supported in this version of JavaSound!");
		}
	}

	public void open() throws DeviceUnavailableException
	{
		open(-1);
	}
	public void open(int bufferSizeInFrames) throws DeviceUnavailableException
	{
		checkOpen();
		try
		{
			// Note: do not use outLine.getFormat() because it may not be
			// the format the line was created with! JDK 1.5.0 beta 2 was broken
			// and returned a different format.
			if (bufferSizeInFrames < 0)
				outLine.open(getFormat());
			else
				outLine.open(
					getFormat(),
					bufferSizeInFrames * samplesPerFrame * bytesPerSample);
		} catch (LineUnavailableException ex)
		{
			throw new DeviceUnavailableException( "opening output, " + ex.toString() );
		}
	}

	public synchronized int write(short[] data, int offset, int numShorts)
	{
		// write short data in bursts that will fit in byte array
		int numLeft = numShorts;
		while (numLeft > 0)
		{
			// don't write past end of buffer
			int numToWrite =
				(numLeft > FRAMES_PER_BUFFER) ? FRAMES_PER_BUFFER : numLeft;
			
			writeLittleShorts(data, offset, numToWrite);
			numLeft -= numToWrite;
			offset += numToWrite;
		}
		return numShorts;
	}

	public int write(short[] data)
	{
		return write(data, 0, data.length);
	}

	public void setGain(float dB)
	{
		if (gainControl != null)
		{
			gainControl.setValue(dB);
		}
	}

	public float getGain()
	{
		if (gainControl != null)
			return gainControl.getValue();
		else
			return 1.0f;
	}

	public void setPan(float pan)
	{
		if (panControl != null)
		{
			panControl.setValue(pan);
		}
	}
	public float getPan()
	{
		if (panControl != null)
			return panControl.getValue();
		else
			return 0.5f;
	}

	public void setSampleRate(float rate)
	{
		if (rateControl != null)
		{
			rateControl.setValue(rate);
		}
	}

	public int getRemainingSampleCount()
	{
		return (getBufferSizeInFrames() * getSamplesPerFrame())
			- availableSamples();
	}

	/* ***************************************************************************/
	/* ********* Private *********************************************************/
	/* ***************************************************************************/

	/** Write shorts to byte stream in LittleEndian format.
	 * @throws InterruptedException 
	 * @throws DeviceUnavailableException 
	 */
	private void writeLittleShorts(short[] data, int offset, int numShorts)
	{
		int bi = 0;
		for (int i = offset; i < (offset + numShorts); i++)
		{
			short s16 = data[i];
			bytes[bi++] = (byte) s16; // little end first
			bytes[bi++] = (byte) (s16 >> 8); // big end
		}
		int numToWrite = numShorts * 2;
		if( avoidBlockingIO )
		{
			sleepUntilAvailable( numToWrite );
		}
		outLine.write( bytes, 0, numToWrite );
	}

	public double getFrameRate()
	{
		return outLine.getFormat().getSampleRate();
	}
}