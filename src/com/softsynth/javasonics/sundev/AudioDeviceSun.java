package com.softsynth.javasonics.sundev;

import javax.sound.sampled.*;

import com.softsynth.javasonics.AudioDevice;
import com.softsynth.javasonics.AudioInputStream;
import com.softsynth.javasonics.AudioOutputStream;
import com.softsynth.javasonics.DeviceUnavailableException;
import com.softsynth.javasonics.util.Logger;

/**
 * Implement AudioDevice using SUN's JavaSound
 * 
 * @author Phil Burk (C) 2001 SoftSynth.com
 * @version 0.1
 */

public class AudioDeviceSun implements AudioDevice
{
	private int inputSuccessCount;
	private int inputFailureCount;
	private int outputFailureCount;
	private int outputSuccessCount;

	/** Free any resources associated with this device. */
	public void destroy()
	{
	}

	private DataLine.Info isLineSupported( Class lineClass, float frameRate,
			int samplesPerFrame, int bitsPerSample )
	{
		// float frameRate, int sampleSizeInBits, int channels, boolean signed,
		// boolean bigEndian
		AudioFormat format = new AudioFormat( (float) frameRate, bitsPerSample,
				samplesPerFrame, true, false );
		DataLine.Info info = new DataLine.Info( lineClass, format );
		// format is an AudioFormat object
		if( AudioSystem.isLineSupported( info ) )
			return info;
		return null;
	}

	/**
	 * Is there support for an AudioOutputStream capable of playing audio in the
	 * specified format?
	 * 
	 * @param frameRate
	 *            number of sample frames per second, typically 44100.0 or
	 *            22050.0
	 * @param samplesperFrame
	 *            one for mono, two for stereo
	 * @param bitsPerSample
	 *            typically 16
	 */
	public boolean isOutputSupported( float frameRate, int samplesPerFrame,
			int bitsPerSample )
	{
		return (isLineSupported( SourceDataLine.class, frameRate,
				samplesPerFrame, bitsPerSample ) != null);
	}

	public AudioOutputStream getOutputStream( float frameRate,
			int samplesPerFrame, int bitsPerSample )
			throws DeviceUnavailableException
	{
		AudioOutputStreamSun outStream = null;
		SourceDataLine outLine;
		DataLine.Info info = isLineSupported( SourceDataLine.class, frameRate,
				samplesPerFrame, bitsPerSample );
		if( info != null )
		{
			// Create the outLine.
			try
			{
				AudioFormat format = new AudioFormat( frameRate, bitsPerSample,
						samplesPerFrame, true, false );
				outLine = (SourceDataLine) AudioSystem.getLine( info );
				outStream = new AudioOutputStreamSun( outLine, format,
						samplesPerFrame, bitsPerSample );
			} catch( LineUnavailableException ex )
			{
				throw new DeviceUnavailableException(
						"Could not open an audio device for playback.", ex );
			}
		}

		return (AudioOutputStream) outStream;
	}

	/**
	 * Is there support for an AudioInputStream capable of playing audio in the
	 * specified format?
	 * 
	 * @param frameRate
	 *            number of sample frames per second, typically 44100.0 or
	 *            22050.0
	 * @param samplesperFrame
	 *            one for mono, two for stereo
	 * @param bitsPerSample
	 *            typically 16
	 */
	public boolean isInputSupported( float frameRate, int samplesPerFrame,
			int bitsPerSample )
	{
		return (isLineSupported( TargetDataLine.class, frameRate,
				samplesPerFrame, bitsPerSample ) != null);
	}

	public com.softsynth.javasonics.AudioInputStream getInputStream(
			float frameRate, int samplesPerFrame, int bitsPerSample )
			throws DeviceUnavailableException
	{
		boolean success = false;
		try
		{
			AudioInputStreamSun inStream = null;
			TargetDataLine inLine;
			DataLine.Info info = isLineSupported( TargetDataLine.class,
					frameRate, samplesPerFrame, bitsPerSample );
			if( info != null )
			{
				// Create the inLine.
				try
				{
					AudioFormat format = new AudioFormat( frameRate,
							bitsPerSample, samplesPerFrame, true, false );
					inLine = (TargetDataLine) AudioSystem.getLine( info );

					inStream = new AudioInputStreamSun( inLine, format,
							samplesPerFrame, bitsPerSample );
					success = true;
					return (com.softsynth.javasonics.AudioInputStream) inStream;
				} catch( LineUnavailableException ex )
				{
					throw new DeviceUnavailableException(
							"Could not open an audio device for recording.", ex );
				}
			}
			else
			{
				throw new DeviceUnavailableException(
						"Audio format not supported. FrameRate = " + frameRate );
			}
		} finally
		{
			if( success )
			{
				inputSuccessCount += 1;
			}
			else
			{
				inputFailureCount += 1;
			}
		}
	}

	public static String lineClassToString( Class lineClass )
	{
		if( lineClass == TargetDataLine.class )
		{
			return "input";
		}
		else if( lineClass == SourceDataLine.class )
		{
			return "output";
		}
		else
		{
			return "unknown";
		}
	}

	class SmartInputStreamOpener extends SmartStreamOpener
	{
		public AudioStreamSun createStream( Line dataline, AudioFormat format,
				int samplesPerFrame, int bitsPerSample )
		{
			return new AudioInputStreamSun( (TargetDataLine) dataline, format,
					samplesPerFrame, bitsPerSample );
		}
	}

	class SmartOutputStreamOpener extends SmartStreamOpener
	{
		public AudioStreamSun createStream( Line dataline, AudioFormat format,
				int samplesPerFrame, int bitsPerSample )
		{
			return new AudioOutputStreamSun( (SourceDataLine) dataline, format,
					samplesPerFrame, bitsPerSample );
		}
	}

	public AudioInputStream startSmartInputStream( double frameRate,
			int samplesPerFrame, int latencyInFrames )
			throws DeviceUnavailableException
	{
		AudioInputStream result = null;
		try
		{
			SmartStreamOpener opener = new SmartInputStreamOpener();
			result = (AudioInputStream) opener.startSmart(
					TargetDataLine.class, frameRate, samplesPerFrame,
					latencyInFrames );

		} finally
		{
			if( result == null )
			{
				if( (inputFailureCount==0) && (outputFailureCount==0) )
				{
					AudioDeviceSun.logAudioInformation();
				}
				inputFailureCount += 1;
			}
			else
			{
				inputSuccessCount += 1;
			}
		}
		return result;
	}

	public AudioOutputStream startSmartOutputStream( double frameRate,
			int samplesPerFrame, int latencyInFrames )
			throws DeviceUnavailableException
	{
		AudioOutputStream result = null;
		try
		{
			SmartStreamOpener opener = new SmartOutputStreamOpener();
			result = (AudioOutputStream) opener.startSmart(
					SourceDataLine.class, frameRate, samplesPerFrame,
					latencyInFrames );

		} finally
		{
			if( result == null )
			{
				if( (inputFailureCount==0) && (outputFailureCount==0) )
				{
					AudioDeviceSun.logAudioInformation();
				}
				outputFailureCount += 1;
			}
			else
			{
				outputSuccessCount += 1;
			}
		}
		return result;
	}

	public static void logAudioInformation()
	{
		final int level = 0;
		Logger.println(level, "Audio Mixers Available ===========================" );
		Mixer.Info[] infoArray = AudioSystem.getMixerInfo();
		for( int i = 0; i < infoArray.length; i++ )
		{
			Mixer.Info info = infoArray[i];
			Logger.println(level, "Mixer: " + info.getName() );
			Logger.println(level, "       " + info.getDescription() );
			Logger.println(level, "       " + info.getClass() );
			Logger.println(level, "----------------------------" );
		}
	}

}