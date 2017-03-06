package com.softsynth.javasonics.recplay;

import com.softsynth.dsp.BufferedSignalProcessor;
import com.softsynth.dsp.FloatToShortProcessor;
import com.softsynth.javasonics.AudioInputStream;
import com.softsynth.javasonics.DeviceUnavailableException;
import com.softsynth.javasonics.util.Logger;

/**
 * Record and Playback sound using JavaSonics. Has no GUI. Requires external
 * controller.
 * 
 * Advanced version of Recorder in com.softsynth.javasonics.recorder
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */
public class JSRecorder extends JSPlayer implements Recorder
{
	private boolean testSignalEnabled = false;
	private String testSignalSpec;
	private SignalGenerator signalGenerator;

	private RecorderProcessingChain recorderProcessingChain;

	private AudioInputStream inStream;
	private double framesReadFromInputStream;
	// FadeIn when recording to prevent pops.
	private int fadeInRemaining = 0;
	private int fadeInDelta = 0;
	private double fadeInTime = 0.01; // seconds
	private int writeIndex = 0;

	public JSRecorder(boolean preferNative, double frameRate, int numChannels)
			throws DeviceUnavailableException
	{
		super( preferNative, frameRate, numChannels );
		recorderProcessingChain = new RecorderProcessingChain();
		updateSampleRateConverter();
	}

	class RecorderProcessingChain extends PlayerProcessingChain
	{

		protected FloatToShortProcessor createFloatToShortProcessor()
		{
			return new FloatToShortProcessor()
			{
				public void write( short[] data, int offset, int numSamples )
				{
					JSRecorder.this.record( data, offset, numSamples );
				}

				public void flush()
				{
					//System.out.println("JSRecorder chain flushed.");
					recording.flush();
				}
			};
		}
	}

	protected void updateSampleRateConverter()
	{
		super.updateSampleRateConverter();
		if( recording != null )
		{
			float compoundRatio = (float) (getFrameRate() / recording
					.getFrameRate());
			Logger.println( 2, "JSRecorder: SRC ratio = " + compoundRatio + " = "
					+ recording.getFrameRate() + " / " + getFrameRate() );
			recorderProcessingChain.setRatio( compoundRatio );
		}
	}

	/**
	 * Does the audio device support this sample rate for the current
	 * samplesPerFrame?
	 */
	public boolean isRateSupported( double frameRate )
	{
		return device.isInputSupported( (float) frameRate, numChannels, 16 )
				&& super.isRateSupported( frameRate );
	}

	public void setRecordChain( BufferedSignalProcessor first, BufferedSignalProcessor last )
	{
		recorderProcessingChain.insertAfterRateConverter( first, last );
	}

	public void startRecordTask() throws DeviceUnavailableException
	{
		framesReadFromInputStream = 0.0;

		clipReadIndex();

		if( recording.isEditable() )
		{
			deleteSelectedRange();
			writeIndex = readIndex;
		}
		else
		{
			writeIndex = recording.getMaxSamplesPlayable();
		}

		leftLevel = 0.0f;
		rightLevel = 0.0f;
		resetFadeIn();

		inStream = device.startSmartInputStream( getRecording().getFrameRate(),
				numChannels, getLatencyInFrames() );

		Logger.println( 2, "JSRecorder.startRecordTask(): Latency in frames = "
				+ inStream.getBufferSizeInFrames() );
		Logger.println( 1,
				"JSRecorder.startRecordTask(): stream sample rate = "
						+ inStream.getSampleRate() );

		setFrameRate( inStream.getSampleRate() );
		positionTracker.start();

		signalGenerator = new SignalGenerator( testSignalSpec );
		signalGenerator.setFrameRate( getFrameRate() );
	}

	private void resetFadeIn()
	{
		fadeInRemaining = 32767;
		fadeInDelta = (int) ((double) fadeInRemaining / (getFrameRate() * fadeInTime));
		// Prevent staying stuck at zero gain.
		if( fadeInDelta <= 0 )
			fadeInDelta = 1;
	}

	/** @return true if done 
	 * @throws InterruptedException */
	protected boolean doRecordTask() throws DeviceUnavailableException, InterruptedException
	{
		int numLeft = recording.getMaxSamplesRecordable()
				- recording.getMaxSamplesPlayable();
		if( numLeft > 0 )
		{
			int numRead;
			if( testSignalEnabled )
			{
				signalGenerator.generate( sampleBuffer, sampleBuffer.length );
				// generateTestSignal2();
				numRead = sampleBuffer.length;
			}
			else
			{
				numRead = inStream.read( sampleBuffer, 0, sampleBuffer.length );
				// Test adding noise to the recorded audio stream to test the
				// compressor.
//				if( false )
//				{
//					for( int i = 0; i < sampleBuffer.length; i++ )
//					{
//						final int noiseLevel = 5000;
//						int sample = sampleBuffer[i];
//						double noise = ((Math.random() * noiseLevel) - (noiseLevel / 2));
//						sample += (int) noise;
//						if( sample > Short.MAX_VALUE )
//							sample = Short.MAX_VALUE;
//						else if( sample < Short.MIN_VALUE )
//							sample = Short.MIN_VALUE;
//						sampleBuffer[i] = (short) sample;
//					}
//				}
//				// Test replacing audio with smooth signal at audio rates.
//				else if( false )
//				{
//					signalGenerator.generate( sampleBuffer, numRead );
//				}
			}

			if( fadeInRemaining > 0 )
			{
				for( int ifade = 0; (ifade < numRead) && (fadeInRemaining > 0); ifade++ )
				{
					int sample = sampleBuffer[ifade];
					int gain = 32767 - fadeInRemaining;
					fadeInRemaining -= fadeInDelta;
					sample = (sample * gain) >> 15;
					sampleBuffer[ifade] = (short) sample;
				}
			}

			// accumulate double # frames read in from stream
			// calc timestamp by dividing by hardware sample rate
			// getFrameRate()
			double timeStamp = framesReadFromInputStream / getFrameRate();
			positionTracker.addTimeStamp( writeIndex, timeStamp );
			framesReadFromInputStream += numRead;

			if( numRead < 0 )
			{
				throw new RuntimeException(
						"Attempted to read more than size of array!" );
			}
			int numWrite = (numLeft < numRead) ? numLeft : numRead;
			updateLevels( sampleBuffer, 0, numWrite );
			recorderProcessingChain.write( sampleBuffer, 0, numWrite );
			return false;
		}
		else
		{
			return true;
		}
	}

	public void stopRecordTask()
	{
		positionTracker.stop();

		if( inStream != null )
		{
			inStream.flush();
			inStream.stop();
			recorderProcessingChain.flush();

			// The close() may take a while so copy inStream to eliminate race.
			AudioInputStream tempStream = inStream;
			inStream = null;
			tempStream.close(); // may hang on old Sun JVM!
		}
	}

	/** Can we record anything? */
	public boolean isRecordable()
	{
		return (recording != null) && !recording.isLocked() && !isProtected();
	}

	public double getPositionInSeconds()
	{
		if( getState() == Recorder.RECORDING )
		{
			return recording.sampleIndexToTime( writeIndex );
		}
		else
		{
			return super.getPositionInSeconds();
		}
	}

	public double getMaxRecordableTime()
	{
		if( recording == null )
		{
			return 0.0;
		}
		else
		{
			return recording.getMaxRecordableTime();
		}
	}

	public double getMaxTime()
	{
		if( recording == null )
		{
			return 0.0;
		}
		else if( getState() == Recorder.RECORDING )
		{
			return recording.getMaxRecordableTime();
		}
		else
		{
			return recording.getMaxPlayableTime();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.javasonics.recplay.Recorder#recordAudio()
	 */
	public void recordAudio()
	{
		sendCommandToAudioThread( BUTTON_CMD_RECORD );
	}

	/** Called from end of signal processing chain. */
	public void record( short[] data, int offset, int numSamples )
	{
		recording.insert( writeIndex, data, offset, numSamples );
		writeIndex += numSamples;
	}
	
	public void flush()
	{
		recording.flush();
	}

	/**
	 * @return Returns the useTestSignal.
	 */
	public boolean isTestSignalEnabled()
	{
		return testSignalEnabled;
	}

	/**
	 * @param useTestSignal
	 *            The useTestSignal to set.
	 */
	public void setTestSignalEnabled( boolean useTestSignal )
	{
		this.testSignalEnabled = useTestSignal;
	}

	public void setTestSignalSpec( String testSignalSpec )
	{
		this.testSignalSpec = testSignalSpec;
	}

	public String stateToText( int state )
	{
		String text = null;
		switch( state )
		{
		case RECORDING:
			text = "recording";
			break;
		default:
			text = super.stateToText( state );
			break;
		}
		return text;
	}


}
