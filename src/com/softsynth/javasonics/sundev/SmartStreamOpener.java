package com.softsynth.javasonics.sundev;

import javax.sound.sampled.*;

import com.softsynth.javasonics.DeviceUnavailableException;
import com.softsynth.javasonics.util.Logger;

/**
 * Try harder to open an audio stream. User every trick possible and log errors
 * for debugging.
 * 
 * @author Phil Burk (C) 2010 Mobileer Inc
 */
abstract class SmartStreamOpener
{

	private final static int BITS_PER_SAMPLE = 16; // most likely to work with
													// JavaSound
	private static int formatDebugLevel = 0;

	/**
	 * Try each mixer. Open and start the stream to make sure it really works.
	 */
	public AudioStreamSun startSmart( Class lineClass, double frameRate,
			int samplesPerFrame, int latencyInFrames )
			throws DeviceUnavailableException
	{
		try
		{
			Mixer.Info[] mi = AudioSystem.getMixerInfo();
			for( int i = 0; i < mi.length; i++ )
			{
				Mixer mixer = AudioSystem.getMixer( mi[i] );
				Logger.println( 1, "Try mixer " + mi[i] );
				if( isLineClassSupported( mixer, lineClass ) )
				{
					AudioStreamSun inStream = tryMultipleFormats( mixer, lineClass, frameRate,
							samplesPerFrame, latencyInFrames );
					if( inStream != null )
					{
						Logger.println( formatDebugLevel , "Audio using: \"" + mi[i] + "\", format = "
								+ inStream.getFormat() );
						// Only log it verbosely the first time.
						if( formatDebugLevel == 0 ) 
						{
							formatDebugLevel = 1;
						}
						return inStream;
					}
				}
			}
		} catch( Exception ex )
		{
			// Unexpected exception!
			ex.printStackTrace();
			throw new DeviceUnavailableException(
					"Failure when opening an audio device for "
							+ AudioDeviceSun.lineClassToString( lineClass ), ex );
		}
		throw new DeviceUnavailableException(
				"Could not open any audio device for "
						+ AudioDeviceSun.lineClassToString( lineClass )
						+ " at " + frameRate + "Hz" );

	}

	/** Scan the lines available on the mixer to see if the class of line is supported.
	 * This will help us avoid getting IllegalArgumentExceptions.
	 * @param mixer
	 * @param lineClass
	 * @return true if supported
	 */
	private boolean isLineClassSupported( Mixer mixer, Class lineClass )
	{
		if( checkLineArray( lineClass, mixer.getSourceLineInfo() ) )
		{
			return true;
		}
		else
		{
			return checkLineArray( lineClass, mixer.getTargetLineInfo() );
		}
	}

	private boolean checkLineArray( Class lineClass, Line.Info[] lines )
	{
		for( int i = 0; i < lines.length; i++ )
		{
			Logger.println( 2, "     line:  " + lines[i] );
			if( lines[i].getLineClass().equals( lineClass ) )
			{
				return true;
			}
		}
		return false;
	}

	/** Try opening a stream at the desired rate then try other common rates. */
	private AudioStreamSun tryMultipleFormats( Mixer mixer, Class lineClass,
			double requestedRate, int samplesPerFrame, int latencyInFrames )
	{
		// Try various sample rates.
		// Leave hole at beginning for desiredRate.
		// Start with 44100 because that is the common Macintosh rate.
		final double rates[] = { 0.0f, 44100.0, 48000.0, 32000.0, 24000.0,
				22050.0, 16000.0, 11025.0, 8000.0 };

		AudioStreamSun inStream = null;
		double rate = 0;
		int latency = 0;
		// start with the requested rate
		rates[0] = requestedRate;
		for( int ir = 0; ((ir < rates.length) && (inStream == null)); ir++ )
		{
			rate = rates[ir];
			AudioFormat format = new AudioFormat( (float) rate,
					BITS_PER_SAMPLE, samplesPerFrame, true, false );
			Logger.println( 2, "     try: " + format );
			DataLine.Info info = new DataLine.Info( lineClass, format );
			if( mixer.isLineSupported( info ) )
			{
				latency = latencyInFrames;
				// Try with specified latency.
				inStream = openStartStream( mixer, info, format, latency );
				if( (inStream == null) && (latencyInFrames > 0) )
				{
					// Try with unspecified latency.
					latency = -1;
					inStream = openStartStream( mixer, info, format, latency );
				}
			}
		}

		return inStream;
	}

	private AudioStreamSun openStartStream( Mixer mixer, DataLine.Info info,
			AudioFormat format, int latencyInFrames )
	{
		AudioStreamSun inStream = null;
		Line dataline = null;
		String mixerDump = mixer.getMixerInfo().getDescription() + ", "
				+ mixer.getMixerInfo().getName();
		try
		{
			dataline = (Line) mixer.getLine( info );
			inStream = createStream( dataline, format, format.getChannels(),
					format.getSampleSizeInBits() );
			inStream.open( latencyInFrames );
			inStream.start();
			return inStream; // SUCCESS!!

		} catch( Exception exc )
		{
			Logger.println( 0, "For rate " + format.getFrameRate() + " Hz on "
					+ mixerDump + ", caught " + exc );
			exc.printStackTrace();
		}
		if( inStream != null )
		{
			inStream.close();
			inStream = null;
		}
		return null;
	}

	public abstract AudioStreamSun createStream( Line dataline,
			AudioFormat format, int samplesPerFrame, int bitsPerSample );

}