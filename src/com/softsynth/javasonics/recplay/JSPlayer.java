package com.softsynth.javasonics.recplay;

import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import com.softsynth.dsp.BufferedSignalProcessor;
import com.softsynth.dsp.FloatToShortProcessor;
import com.softsynth.dsp.SampleRateConverter;
import com.softsynth.dsp.ShortToFloatProcessor;
import com.softsynth.javasonics.AudioDevice;
import com.softsynth.javasonics.AudioOutputStream;
import com.softsynth.javasonics.DeviceUnavailableException;
import com.softsynth.javasonics.core.SonicSystem;
import com.softsynth.javasonics.error.UserRuntimeException;
import com.softsynth.javasonics.util.Logger;
import com.softsynth.javasonics.util.MessageQueue;

/**
 * Record and Playback sound using JavaSonics. Has no GUI. Requires external
 * controller.
 * 
 * Advanced version of Recorder in com.softsynth.javasonics.recorder
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */
public class JSPlayer implements Player
{
	// commands sent to player
	private final static String BUTTON_CMD_QUIT = "quit";
	private final static String BUTTON_CMD_PLAY_NO_REWIND = "play";
	private final static String BUTTON_CMD_PLAY_NORMAL = "playnormal";
	protected final static String BUTTON_CMD_RECORD = "record";
	private final static String BUTTON_CMD_STOP = "stop";
	private final static String BUTTON_CMD_PAUSE = "pause";
	private final static String BUTTON_CMD_FASTFWD = "fastfwd";
	private final static String BUTTON_CMD_REWIND = "rewind";
	private final static String BUTTON_CMD_SLOWFWD = "slowfwd";
	private final static String BUTTON_CMD_ERASE = "erase";
	private final static String BUTTON_CMD_ERASE_SELECTED = "eraseSelected";
	private final static String BUTTON_CMD_TOBEGIN = "tobegin";
	private final static String BUTTON_CMD_TOEND = "toend";

	private MessageQueue commandQueue;
	protected Thread audioThread;
	private StateManager stateManager;

	protected static AudioDevice device = null;
	protected Recording recording;
	protected double frameRate = 44100.0;
	protected int numChannels;
	protected int state;
	protected float leftLevel;
	protected float rightLevel;
	// determines rate of exponential decay for volume level
	protected short[] sampleBuffer;
	private int latencyInFrames = SonicSystem.NOT_SPECIFIED;
	protected PositionTracker positionTracker;
	// Index of next frame to read. Zero when rewound.
	protected int readIndex;

	private PlayerProcessingChain playerProcessingChain;

	private final static int REQUIRED_VERSION = 5;
	private AudioOutputStream outStream;
	private float decayRate = 0.9995f;
	private Vector playListeners;
	private boolean isWaitingForData = false;
	private double startTime = 0.0;
	private double stopTime = 0.0;

	// MOD NICK
	// FF and REW on my DVD player skips 6 seconds at a time
	private double secondsToSkip = 2.0;
	private boolean skipping = false;
	private boolean reverse = false;
	private VariableSpeedControl variableSpeedControl;
	private boolean SHOW_DEBUG_SPEEDS = false;
	private float slowForwardSpeed = 1f;
	private boolean respondToSpeedControl;

	private boolean isRewFF = false;

	private boolean recordingProtectionOption = false;
	private boolean recordingProtected = false;

	private double framesWrittenToOutputStream; // post signal processing

	SignalGenerator signalGenerator;
	private double autoBackStep = 0.0;
	private double autoPreview = 0.0;
	private float playbackSpeed = 1.0f;

	public JSPlayer(boolean preferNative, double frameRate, int numChannels)
			throws DeviceUnavailableException
	{
		this.numChannels = numChannels;
		playListeners = new Vector();
		commandQueue = new MessageQueue();
		stateManager = new StateManager();

		// Make a frame position tracker so we can update GUI as record or
		// playback progresses.
		positionTracker = new PositionTracker();
		setTimeChangeInterval( 50 );
		positionTracker.addObserver( new Observer()
		{
			public void update( Observable observable, Object obj )
			{
				notifyTime();
			}
		} );

		// Get an audio device supported on this system.
		if( device == null )
		{
			device = SonicSystem.getDevice( preferNative, REQUIRED_VERSION );
		}

		int samplesPerLoop = 1 * 512; // small to locate better
		// Non-powers of two seem to hang line.close()
		sampleBuffer = new short[samplesPerLoop];

		playerProcessingChain = new PlayerProcessingChain();
		this.variableSpeedControl = new VariableSpeedControl( this );

		setFrameRate( frameRate );
	}

	class PlayerProcessingChain
	{
		// Player always has a signal processing chain that contains
		// shortToFloat, rate convertor and floatToShort.
		// The single sample rate converter is used
		// to handle both conversion from recording to hardware rate
		// and the variable speed playback.
		private ShortToFloatProcessor shortToFloatProcessor;
		private SampleRateConverter sampleRateConverter;
		private FloatToShortProcessor floatToShortProcessor;

		/**
		 * Always have a ShortPlayGateway followed by a SampleRateConverter
		 */
		public PlayerProcessingChain()
		{
			shortToFloatProcessor = new ShortToFloatProcessor();
			sampleRateConverter = new SampleRateConverter( (float) 1.0 );
			shortToFloatProcessor.setNext( sampleRateConverter );
			floatToShortProcessor = createFloatToShortProcessor();

			sampleRateConverter.setNext( floatToShortProcessor );
		}

		protected FloatToShortProcessor createFloatToShortProcessor()
		{
			return new FloatToShortProcessor()
			{
				public void write( short[] data, int offset, int numSamples )
				{
					JSPlayer.this.play( data, offset, numSamples );
				}

				public void flush()
				{
				}
			};
		}

		public void flush()
		{
			shortToFloatProcessor.flush();
		}

		public void write( short[] data, int offset, int numSamples )
		{
			shortToFloatProcessor.write( data, offset, numSamples );
		}

		public void setRatio( float srcRatio )
		{
			sampleRateConverter.setRatio( srcRatio );
		}

		public void insertAfterRateConverter( BufferedSignalProcessor first,
				BufferedSignalProcessor last )
		{
			sampleRateConverter.setNext( first );
			last.setNext( floatToShortProcessor );
		}
	}

	public void play( short[] data, int offset, int numSamples )
	{
		// accumulate double # frames written to out stream
		// calc timestamp by dividing by hardware sample rate
		// getFrameRate()
		double timeStamp = framesWrittenToOutputStream / getFrameRate();
		positionTracker.addTimeStamp( readIndex, timeStamp );
		framesWrittenToOutputStream += numSamples;
		outStream.write( data, offset, numSamples );
	}

	protected void setState( int mode )
	{
		setState( mode, null );
	}

	protected void setState( int mode, Throwable thr )
	{
		this.state = mode;
		notifyState( thr );
	}

	/** Calculate volume levels from a region of the array of shorts. */
	protected void updateLevels( short[] samples, int offset, int numShorts )
	{
		int idx = offset;
		for( int i = 0; i < numShorts; )
		{
			float value = samples[idx++];
			if( value > leftLevel )
				leftLevel = value;
			else
				leftLevel *= decayRate;
			i++;

			if( numChannels > 1 )
			{
				value = samples[idx++];
				if( value > rightLevel )
					rightLevel = value;
				else
					rightLevel *= decayRate;
				i++;
			}
		}
		notifyLevel();
	}

	protected void startPlayTask() throws DeviceUnavailableException
	{
		// init stuff for timestamped audio buffers
		framesWrittenToOutputStream = 0;

		leftLevel = 0.0f;
		rightLevel = 0.0f;

		outStream = device.startSmartOutputStream( getRecording()
				.getFrameRate(), numChannels, getLatencyInFrames() );

		Logger.println( 2, "JSPlayer.startPlayTask(): Latency in frames = "
				+ outStream.getBufferSizeInFrames() );
		Logger.println( 1, "JSPlayer.startPlayTask(): stream sample rate = "
				+ outStream.getSampleRate() );

		setFrameRate( outStream.getSampleRate() );

		positionTracker.start();
	}

	protected int getBestStopIndex()
	{
		if( recording == null )
		{
			return 0;
		}
		int max = recording.getMaxSamplesPlayable();
		int idx = (getStartTime() == getStopTime()) ? max : recording
				.timeToSampleIndex( getStopTime() );
		if( idx > max )
		{
			idx = max;
		}
		return idx;
	}

	protected double getBestStartTime()
	{
		return (getStartTime() == getStopTime()) ? 0.0 : getStartTime();
	}

	protected double getBestStopTime()
	{
		return (getStartTime() == getStopTime()) ? getMaxPlayableTime()
				: getStopTime();
	}

	/** @return true if done playing. */
	private boolean doPlayTask()
	{
		boolean done = true;
		Recording reco = getRecording();
		if( reco == null )
		{
			return true;
		}

		int samplesPerLoop = sampleBuffer.length;

		// Recalculate this in case we are playing while loading.
		int numLeft = getBestStopIndex() - readIndex;
		if( isReverse() )
		{
			// Do not subtract 1 because points to next index to read.
			// Equals numSamples when at end of recording.
			numLeft = readIndex;
		}

		if( numLeft <= 0 )
		{
			if( reco.isDownloading() )
			{
				return false;
			}
		}
		else
		{
			int maxWrite = (numLeft < samplesPerLoop) ? numLeft
					: samplesPerLoop;

			if( isReverse() )
			{
				// Subtract first because we read from far end of block.
				// This avoids an EOF when read from end of fileCache.
				readIndex -= maxWrite;
				reco.readBackwards( readIndex, sampleBuffer, 0, maxWrite );
			}
			else
			{
				reco.read( readIndex, sampleBuffer, 0, maxWrite );
				readIndex += maxWrite;
			}

			playerProcessingChain.write( sampleBuffer, 0, maxWrite );

			updateLevels( sampleBuffer, 0, maxWrite );

			// skip buffers to cue quickly through recording
			if( isSkipping() )
			{
				int framesToSkip = (int) (secondsToSkip * reco.getFrameRate());
				if( isReverse() )
				{
					framesToSkip *= -1;
				}
				readIndex += framesToSkip;
			}
			done = false;
		}
		return done;
	}

	private void stopPlayTask( boolean stopQuick )
	{
		// Logger.println(0,"Enter stopPlayTask");
		playerProcessingChain.flush();

		AudioOutputStream tempOutStream = outStream;
		if( outStream != null )
		{
			if( !stopQuick )
			{
				// This is necessary or the sound will stop way too soon at end
				// of recording.
				outStream.drain();
				if( true )
				{
					// This HACK is needed because drain() returns a little too
					// early on Windows JavaSound.
					// The very end of a recording was often not played.
					try
					{
						Thread.sleep( 200 );
					} catch( InterruptedException e )
					{
						e.printStackTrace();
					}
				}
			}

			outStream.stop();
			outStream = null;
		}

		leftLevel = 0.0f;
		rightLevel = 0.0f;

		positionTracker.stop();
		tempOutStream.close();
	}

	/**
	 * Stubs overwritten by recorder.
	 */
	protected void startRecordTask() throws DeviceUnavailableException
	{
		throw new RuntimeException( "startRecordTask() in JSplayer called!" );
	}

	protected boolean doRecordTask() throws DeviceUnavailableException,
			InterruptedException
	{
		throw new RuntimeException( "doRecordTask() in JSplayer called!" );
	}

	protected void stopRecordTask()
	{
		throw new RuntimeException( "stopRecordTask() in JSplayer called!" );
	}

	private void slowForwardSetup()
	{
		resetVariableSpeedControl();
		setPlaybackSpeed( getSlowForwardSpeed() );
		setReverse( false );
	}

	private void handlePlayRewind() throws DeviceUnavailableException
	{
		isRewFF = true;
		setRespondToSpeedControl( false );
		float speed = nextRewindSpeed();
		if( SHOW_DEBUG_SPEEDS )
			System.out.println( "JSPlayer.playRewind(), Rewind SPEED " + speed );
		setPlaybackSpeed( speed );
		setReverse( true );
		handlePlayAudio( false );
	}

	/** Erase recording now. */
	private void eraseRecording()
	{
		if( recording != null )
		{
			recording.erase();
		}
		setStartTime( 0.0 );
		setStopTime( 0.0 );
		setPositionInSeconds( 0.0 );
	}

	protected void handleErase()
	{
		if( !recordingProtected )
		{
			// Stop here so we do not flush more data into recording after
			// clearing
			// it.
			handleStopAudio();

			eraseRecording();
			// Stop again here so we send stop message to listeners and
			// give them a chance to update button states.
			handleStopAudio();
		}
	}

	public void deleteSelectedRange()
	{
		if( recording != null && recording.isEditable() )
		{
			int s1 = getStartIndex();
			int s2 = getStopIndex();
			int ds = s2 - s1;
			if( ds > 0 )
			{
				// Update readIndex in case it is affected.
				// This fixed a bug that caused the new recording to go to the
				// wrong place
				// if the cursor was past the startIndex.
				if( readIndex > s1 )
				{
					int tempIndex = readIndex - ds;
					if( tempIndex < s1 )
					{
						tempIndex = s1;
					}
					readIndex = tempIndex;
				}
				// Delete the actual data.
				recording.delete( s1, ds );
				// Eliminate selection range.
				setStopTime( getStartTime() );
			}
			notifyTime();
		}
	}

	protected void handleEraseSelected()
	{
		if( !recordingProtected )
		{
			// Stop here so we do not flush more data into recording after
			// clearing
			// it.
			handleStopAudio();

			deleteSelectedRange();

			// Stop again here so we send stop message to listeners and
			// give them a chance to update button states.
			handleStopAudio();
		}
	}

	private void notifyState( Throwable thr )
	{
		Enumeration listeners = playListeners.elements();
		int state = getState();
		while( listeners.hasMoreElements() )
		{
			PlayerListener listener = (PlayerListener) listeners.nextElement();
			listener.playerStateChanged( this, state, thr );
		}
	}

	protected void notifyTime()
	{
		double time = getPositionInSeconds();
		Enumeration listeners = playListeners.elements();
		while( listeners.hasMoreElements() )
		{
			PlayerListener listener = (PlayerListener) listeners.nextElement();
			listener.playerTimeChanged( this, time );
		}
	}

	private void notifyLevel()
	{
		Enumeration listeners = playListeners.elements();
		while( listeners.hasMoreElements() )
		{
			PlayerListener listener = (PlayerListener) listeners.nextElement();
			listener.playerLevelChanged( this );
		}
	}

	/**
	 * @see com.softsynth.javasonics.recplay.Player#nextRewindSpeed()
	 */
	private float nextRewindSpeed()
	{
		return variableSpeedControl.nextRewindSpeed();
	}

	/**
	 * 
	 */
	private float nextFastForwardSpeed()
	{
		return variableSpeedControl.nextFastForwardSpeed();
	}

	private void handleToEnd()
	{
		handleStopAudio();
		double endTime = getMaxPlayableTime();
		setStartTime( endTime );
		setStopTime( endTime );
		setPositionInSeconds( endTime );
	}

	private void handleToBegin()
	{
		handleStopAudio();
		setStartTime( 0.0 );
		setStopTime( 0.0 );
		setPositionInSeconds( 0 );
	}

	private void rewindIfAtEnd()
	{
		// If we are at the end, or extremely close, then rewind before playing.
		double currentTime = getPositionInSeconds();
		// System.out.println("Play currentTime = " + currentTime );
		// System.out.println("Play getStopTime = " + getStopTime() );
		// System.out.println("Play getBestStopTime = " + getBestStopTime() );
		double overTime = currentTime - getBestStopTime();
		if( overTime > -0.01 ) // very close to end or past end
		{
			double bestTime = getBestStartTime();
			// System.out.println("Play position forced to " + bestTime );
			setPositionInSeconds( bestTime );
		}
	}

	/**
	 * 
	 */
	private void handlePlayAudio( boolean rewindIfAtEnd )
			throws DeviceUnavailableException
	{
		switch( state )
		{
		case STOPPED:
		case PAUSED:
			// Only rewind if we are playing forward.
			if( !isReverse() && rewindIfAtEnd )
			{
				rewindIfAtEnd();
			}
			startPlayTask();
			setState( Player.PLAYING );
			break;

		case PLAYING:
			// this could have been called by FF or Rew so notifyState listeners
			// to trigger gui update
			notifyState(); // MOD ND
			break;
		}
	}

	private void handleStopAudio()
	{
		setRespondToSpeedControl( false );
		double bestTime = getBestStartTime();
		switch( state )
		{
		case STOPPED:
			break;

		case PLAYING:
			stopPlayTask( true );
			break;

		case Recorder.RECORDING:
			// Stay at end of recording so we can easily append to end.
			bestTime = getPositionInSeconds();
			stopRecordTask();
			break;

		case PAUSED:
			break;
		}
		setPositionInSeconds( bestTime );
		setState( Player.STOPPED );
		Logger.println( 1, "Recording contains " + recording.getMaxSamplesPlayable() + " samples." );
	
	}

	/**
	 * Background thread response to pause command.
	 */
	private void handlePauseAudio()
	{
		setRespondToSpeedControl( false );
		// This was added because FFW on footpedal ran faster each time it was
		// pressed.
		resetVariableSpeedControl();
		switch( state )
		{
		case STOPPED:
			break;

		case Recorder.RECORDING:
			double pos = getPositionInSeconds();
			stopRecordTask();
			// Only autopPreview if very near end.
			boolean doPreview = (autoPreview > 0.0)
					&& ((getMaxPlayableTime() - pos) < 0.1);
			if( doPreview )
			{
				pos -= autoPreview;
				if( pos < 0.0 )
				{
					pos = 0.0;
				}
			}
			// So we can start at currently heard position.
			setPositionInSeconds( pos );
			// Asynchronously queue a play request.
			// Delay it to avoid device conflict reported by Nirav Merchant.
			if( doPreview )
			{
				new Thread()
				{
					public void run()
					{
						try
						{
							sleep( 50 );
						} catch( InterruptedException e )
						{
						}
						playNormalSpeed();
					}
				}.start();
			}
			break;

		case PLAYING:
			pos = getPositionInSeconds();
			// System.out.println("Pause at " + pos );
			stopPlayTask( true );
			if( !isRewFF && (autoBackStep > 0.0) )
			{
				pos -= autoBackStep;
				if( pos < 0.0 )
				{
					pos = 0.0;
				}
			}
			// So we can start at currently heard position.
			setPositionInSeconds( pos );
			break;

		case PAUSED:
			break;
		}
		setState( Player.PAUSED );
		Logger.println( 1, "Recording contains " + recording.getMaxSamplesPlayable() + " samples." );
	}

	private void handlePlayFastForward() throws DeviceUnavailableException
	{
		isRewFF = true;
		setRespondToSpeedControl( false );
		float speed = nextFastForwardSpeed();
		if( SHOW_DEBUG_SPEEDS )
			System.out
					.println( "JSPlayer.playFastForward(), FF SPEED " + speed );
		setPlaybackSpeed( speed );
		setReverse( false );
		handlePlayAudio( false );
	}

	private void handlePlaySlowForward( boolean rewindIfAtEnd )
			throws DeviceUnavailableException
	{
		isRewFF = false;
		setRespondToSpeedControl( true );
		slowForwardSetup();
		handlePlayAudio( rewindIfAtEnd );
	}

	/** Run audio state machine for one time slice. */
	private void runAudioTask() throws InterruptedException,
			DeviceUnavailableException
	{
		switch( state )
		{
		case PLAYING:
			if( doPlayTask() ) // If it returns true then stop.
			{
				// Stop at end when playing stops by itself so we can record
				// more.
				double position = isReverse() ? getBestStartTime()
						: getBestStopTime();
				// double position = getBestStartTime(); // This was the
				// behavior before 8/1/08
				stopPlayTask( false );
				setPositionInSeconds( position );
				setState( STOPPED );
			}
			break;

		case Recorder.RECORDING:
			if( doRecordTask() ) // If it returns true then stop.
			{
				handleStopAudio();
			}
			break;

		default:
			Thread.sleep( 100 );
			break;
		}
	}

	/**
	 * Start recording.
	 */
	private void handleRecordAudio() throws DeviceUnavailableException
	{
		Logger.println( 2, "JSPlayer.handleRecordAudio()" );
		if( !recordingProtected )
		{
			setRespondToSpeedControl( false );
			switch( state )
			{
			case STOPPED:
				// If we can't edit the recording, then just erase it before we
				// start recording.
				if( !recording.isEditable() )
				{
					eraseRecording();
				}
			case PAUSED:
				startRecordTask();
				setState( Recorder.RECORDING );
				recordingProtected = recordingProtectionOption;
				break;

			case PLAYING:
				break;
			}
		}
	}

	class StateManager implements Runnable
	{

		/**
		 * Parse command from UI threads. This runs in the same thread as the
		 * audio playback and recording to avoid synchronization issues.
		 * 
		 * @param msg
		 * @throws Exception
		 */
		protected void processButtonMessage( String msg ) throws Exception
		{
			if( msg.equals( BUTTON_CMD_PLAY_NO_REWIND ) )
			{
				if( isPlayable() )
				{
					boolean rewindIfAtEnd = false;
					handlePlaySlowForward( rewindIfAtEnd );
				}
			}
			else if( msg.equals( BUTTON_CMD_PLAY_NORMAL ) )
			{
				if( isPlayable() )
				{
					boolean rewindIfAtEnd = true;
					handlePlaySlowForward( rewindIfAtEnd );
				}
			}
			else if( msg.equals( BUTTON_CMD_STOP ) )
			{
				handleStopAudio();
			}
			else if( msg.equals( BUTTON_CMD_RECORD ) )
			{
				handleRecordAudio();
			}
			else if( msg.equals( BUTTON_CMD_ERASE ) )
			{
				handleErase();
			}
			else if( msg.equals( BUTTON_CMD_ERASE_SELECTED ) )
			{
				handleEraseSelected();
			}
			else if( msg.equals( BUTTON_CMD_PAUSE ) )
			{
				handlePauseAudio();
			}
			else if( msg.equals( BUTTON_CMD_REWIND ) )
			{
				handlePlayRewind();
			}
			else if( msg.equals( BUTTON_CMD_FASTFWD ) )
			{
				handlePlayFastForward();
			}
			else if( msg.equals( BUTTON_CMD_SLOWFWD ) )
			{
				boolean rewindIfAtEnd = false;
				handlePlaySlowForward( rewindIfAtEnd );
			}
			else if( msg.equals( BUTTON_CMD_TOBEGIN ) )
			{
				// System.out
				// .println( "JSPlayer receives BUTTON_CMD_TOBEGIN from command
				// queue" );
				handleToBegin();
			}
			else if( msg.equals( BUTTON_CMD_TOEND ) )
			{
				// System.out
				// .println( "JSPlayer receives BUTTON_CMD_TOEND from command
				// queue"
				// );
				handleToEnd();
			}
		}

		/**
		 * Background thread reads commands and keeps audio alive in one thread.
		 */
		public void run()
		{
			int pri = Thread.currentThread().getPriority();
			Logger.println( 1, "JSPlayer audio priority started at " + pri );
			if( pri < Thread.MAX_PRIORITY )
			{
				pri += 1;
				Thread.currentThread().setPriority( pri );
				Logger.println( 1, "JSPlayer audio priority raised to " + pri );
			}
			try
			{
				try
				{
					while( true )
					{
						String msg = (String) commandQueue.peekMessage();

						try
						{
							// handle message if we got one
							if( msg != null )
							{
								if( msg.equals( BUTTON_CMD_QUIT ) )
								{
									Logger
											.println( 2,
													"JSPlayer received BUTTON_CMD_QUIT" );
									commandQueue.popMessage();
									break;
								}
								else
								{
									Logger.println( 2, "JSPlayer run() got: "
											+ msg );
									try
									{
										processButtonMessage( msg );
									} finally
									{
										commandQueue.popMessage();
									}
								}
							}
							// Run one time slice of the audio task.
							runAudioTask();
						} catch( DeviceUnavailableException dex )
						{
							if( !isStopped() )
							{
								handleStopAudio();
							}
							setState( state, dex );

						} catch( UserRuntimeException urx )
						{
							if( !isStopped() )
							{
								handleStopAudio();
							}
							setState( state, urx );
						}
					}
				} finally
				{
					if( state != STOPPED )
					{
						handleStopAudio();
					}
					Logger.println( 1, "Exiting audio thread." );
				}
			} catch( InterruptedException ex )
			{
				Logger
						.println( 1,
								"Audio Thread was interrupted, probably because the Applet was shut down." );
			} catch( SecurityException ex )
			{
				String msg = ex.getMessage()
						+ "\nThis is probably because you did not grant permission when the certificate appeared."
						+ "\nPlease close all browser windows, restart the browser and try again.";
				SecurityException eUser = new SecurityException( msg );
				setState( Recorder.ABORTED, eUser );
			} catch( Throwable thr )
			{
				setState( Recorder.ABORTED, thr );
			}
		}
	}

	/**
	 * @param string
	 */
	protected void sendCommandToAudioThread( String string )
	{
		Logger.println( 2, "sendCommandToAudioThread( " + string + " )" );
		commandQueue.send( string );
	}

	/** queue command to jump cursor to beginning */
	public void toBegin()
	{
		sendCommandToAudioThread( BUTTON_CMD_TOBEGIN );
	}

	/** queue command to jump cursor to end */
	public void toEnd()
	{
		sendCommandToAudioThread( BUTTON_CMD_TOEND );
	}

	/**
	 * Play back the recorded sound from the buffer. Do not rewind if at end.
	 */
	public void playAudioNoRewind()
	{
		sendCommandToAudioThread( BUTTON_CMD_PLAY_NO_REWIND );
	}

	public void playNormalSpeed()
	{
		sendCommandToAudioThread( BUTTON_CMD_PLAY_NORMAL );
	}

	public void playFastForward()
	{
		sendCommandToAudioThread( BUTTON_CMD_FASTFWD );
	}

	public void playSlowForward()
	{
		sendCommandToAudioThread( BUTTON_CMD_SLOWFWD );
	}

	public void playRewind()
	{
		sendCommandToAudioThread( BUTTON_CMD_REWIND );
	}

	/** Is stopped or paused. */
	public boolean isStopped()
	{
		return (state == STOPPED) || (state == PAUSED);
	}

	public boolean isPlaying()
	{
		return (state == PLAYING);
	}

	public boolean isRecording()
	{
		return (state == RECORDING);
	}

	/** Pause playing or recording. */
	public void pauseAudio()
	{
		sendCommandToAudioThread( BUTTON_CMD_PAUSE );
	}

	/**
	 * Stop playing or recording.
	 */
	public void stopAudio()
	{
		sendCommandToAudioThread( BUTTON_CMD_STOP );
	}

	/** Can we play anything? */
	public boolean isPlayable()
	{
		return (recording != null) && (recording.getMaxSamplesPlayable() > 0);
	}

	/** Can we record anything? */
	public boolean isRecordable()
	{
		return false;
	}

	public double getMaxTime()
	{
		return getMaxPlayableTime();
	}

	public double getMaxPlayableTime()
	{
		if( recording == null )
			return 0.0;
		else
			return recording.getMaxPlayableTime();
	}

	protected int clipReadIndex()
	{
		return readIndex = clipReadIndex( readIndex );
	}

	protected int clipReadIndex( int index )
	{
		if( index > recording.getMaxSamplesPlayable() )
		{
			index = recording.getMaxSamplesPlayable();
		}
		else if( index < 0 )
		{
			index = 0;
		}
		return index;
	}

	public synchronized void setPositionInSeconds( double time )
	{
		if( recording != null )
		{
			// Use temporary nextReadIndex so we don't set readIndex to bad
			// value.
			int nextReadIndex = recording.timeToSampleIndex( time );
			readIndex = clipReadIndex( nextReadIndex );
			double pos = (double) readIndex;
			positionTracker.setPosition( pos );
			//new RuntimeException( "Position set to " + pos + " at " + System.currentTimeMillis() ).printStackTrace();
		}
		notifyTime();
	}

	public double getPositionInSeconds()
	{
		double pos = positionTracker.getCurrentPosition();
		//Logger.println( 0, "getPositionInSeconds: currentPosition = " + pos );
		// This was added because sometimes the positionTracker can return a negative value
		// if it is not tracking.
		if( pos < 0 )
		{
			pos = readIndex;
		}
		double seconds = 0.0;
		if( recording != null )
		{
			seconds = recording.sampleIndexToTime( (int) pos );
			//Logger.println( 0, "getPositionInSeconds: seconds = " + seconds );
		}
		return seconds;
	}

	public void addPlayerListener( PlayerListener listener )
	{
		Logger.println( 3, "JSPlayer.addPlayerListener( " + listener + " )" );
		playListeners.addElement( listener );
		listener.playerStateChanged( this, getState(), null );
	}

	public void removePlayerListener( PlayerListener listener )
	{
		Logger.println( 3, "JSPlayer.removePlayerListener( " + listener + " )" );
		playListeners.removeElement( listener );
	}

	public void notifyState()
	{
		notifyState( null );
	}

	/**
	 */
	public void resetVariableSpeedControl()
	{
		variableSpeedControl.reset();
	}

	/**
	 * @return Returns the respondToSpeedControl.
	 */
	public boolean isRespondToSpeedControl()
	{
		return respondToSpeedControl;
	}

	/**
	 * @param respondToSpeedControl
	 *            The respondToSpeedControl to set.
	 */
	public void setRespondToSpeedControl( boolean respondToSpeedControl )
	{
		this.respondToSpeedControl = respondToSpeedControl;
	}

	/**
	 * @return Returns the startTime.
	 */
	public double getStartTime()
	{
		return startTime;
	}

	/**
	 * @param startTime
	 *            The startTime to set.
	 */
	public void setStartTime( double startTime )
	{
		if( startTime < 0.0 )
		{
			startTime = 0.0;
		}
		this.startTime = startTime;
	}

	/**
	 * @return Returns the stopTime.
	 */
	public double getStopTime()
	{
		return stopTime;
	}

	/**
	 * @param stopTime
	 *            The stopTime to set.
	 */
	public void setStopTime( double stopTime )
	{
		this.stopTime = stopTime;
	}

	public int getStartIndex()
	{
		if( recording == null )
			return 0;
		int idx = recording.timeToSampleIndex( getStartTime() );
		if( idx > recording.getMaxSamplesPlayable() )
		{
			idx = recording.getMaxSamplesPlayable();
		}
		return idx;
	}

	public int getStopIndex()
	{
		if( recording == null )
		{
			return 0;
		}
		int idx = recording.timeToSampleIndex( getStopTime() );
		if( idx > recording.getMaxSamplesPlayable() )
		{
			idx = recording.getMaxSamplesPlayable();
		}
		return idx;
	}

	/*
	 * * @return state == PLAYING && modifiedPlay. True if FF or Rew, false if
	 * playing normal
	 * 
	 * @see com.softsynth.javasonics.recplay.Player#isPlayModified()
	 */
	public boolean isPlayModified()
	{
		return (state == PLAYING && isRewFF);
	}

	/**
	 * @return true if timed out.
	 * @throws InterruptedException
	 * @see com.softsynth.javasonics.recplay.Player#waitUntilStopped(int)
	 */
	public boolean waitUntilStopped( int timeOutMSec )
			throws InterruptedException
	{
		long timeToGiveUp = System.currentTimeMillis() + timeOutMSec;

		commandQueue.waitUntilEmpty( timeOutMSec );

		timeOutMSec = (int) (timeToGiveUp - System.currentTimeMillis());
		while( !isStopped() && (timeOutMSec > 0) )
		{
			// This was a synchronized wait() but it was
			// hanging the player in setPositionInSeconds()
			// and preventing the stop command from being processed.
			Thread.sleep( 100 );
			Logger.println( 1, "waitUntilStopped spinning, timeOutMSec = "
					+ timeOutMSec );
			timeOutMSec = (int) (timeToGiveUp - System.currentTimeMillis());
		}
		return !isStopped();
	}

	/** Delete any previously recorded material in the audio thread. */
	public void erase()
	{
		sendCommandToAudioThread( BUTTON_CMD_ERASE );
	}

	/** Delete selected material in the audio thread. */
	public void eraseSelected()
	{
		sendCommandToAudioThread( BUTTON_CMD_ERASE_SELECTED );
	}

	public void stop()
	{
		if( audioThread != null )
		{
			sendCommandToAudioThread( BUTTON_CMD_QUIT );
			try
			{
				audioThread.join( 1000 );
			} catch( InterruptedException e )
			{
				Logger.println( 1, "Caught " + e );
			}
		}
		audioThread = null;
	}

	public void start()
	{
		if( audioThread != null )
		{
			stop();
		}
		audioThread = new Thread( stateManager, "JSPlayer" );
		audioThread.start();
	}

	public void setRecording( Recording pRecording )
			throws DeviceUnavailableException
	{
		validateRecording( pRecording );
		recording = pRecording;
		if( pRecording != null )
		{
			Logger.println( 1, "JSPlayer: recording frame rate = "
					+ pRecording.getFrameRate() );
			setFrameRate( pRecording.getFrameRate() );
			setPlaybackSpeed( 1.0f );
		}
		setStartTime( 0.0 );
		setStopTime( 0.0 );
		setPositionInSeconds( 0.0 );
		notifyState();
	}

	private void validateRecording( Recording pRecording )
	{
		if( pRecording != null )
		{
			if( pRecording.getFrameRate() < 4000.0 )
			{
				throw new InvalidAudioRecordingException("Sample rate too low = " + pRecording.getFrameRate() );
			}
			if( pRecording.getSamplesPerFrame() != 1 )
			{
				throw new InvalidAudioRecordingException("Only mono samples supported. Not " + pRecording.getSamplesPerFrame() );
			}
		}
	}

	public Recording getRecording()
	{
		return recording;
	}

	/**
	 * Does the audio device support this sample rate for the current
	 * samplesPerFrame?
	 */
	public boolean isRateSupported( double frameRate )
	{
		return device.isOutputSupported( (float) frameRate, numChannels, 16 );
	}

	/**
	 * Set rate for recording and playback.
	 * 
	 * @throws DeviceUnavailableException
	 */
	public void setFrameRate( double frameRate )
			throws DeviceUnavailableException
	{
		this.frameRate = frameRate;
		updateSampleRateConverter();
	}

	public double getFrameRate()
	{
		return frameRate;
	}

	/**
	 * Set size of buffers for streams.
	 */
	public void setLatencyInFrames( int latencyInFrames )
	{
		this.latencyInFrames = latencyInFrames;
	}

	public int getLatencyInFrames()
	{
		return latencyInFrames;
	}

	/**
	 * Set number of samples (channels) in a frame. 1 for mono, 2 for stereo.
	 * This triggers a reallocation of the audio buffer.
	 */
	public void setSamplesPerFrame( int numChannels )
	{
		this.numChannels = numChannels;
	}

	public int getSamplesPerFrame()
	{
		return numChannels;
	}

	/** Return current mode such as STOPPED, PLAYING or RECORDING */
	public int getState()
	{
		return state;
	}

	/* Level for left channel of stereo stream, or mono channel. */
	public float getLeftLevel()
	{
		return leftLevel * (1.0f / 32767.0f);
	}

	/* Level for right channel of stereo stream. */
	public float getRightLevel()
	{
		return rightLevel * (1.0f / 32767.0f);
	}

	/**
	 * @return Returns the last slowForwardSpeed.
	 */
	public float getSlowForwardSpeed()
	{
		return slowForwardSpeed;
	}

	/**
	 * Stores slow forward speed so we can return to it after FF and REW changed
	 * speed. Just sets the value if currently fast forwarding or rewinding,
	 * else sets playback speed to this value
	 * 
	 * @param speed
	 *            , float 0.5 .. 1.0
	 */
	public void setSlowForwardSpeed( float speed )
	{
		this.slowForwardSpeed = speed;
		if( isRespondToSpeedControl() )
		{
			slowForwardSetup();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.javasonics.recplay.Player#isWaitingForData()
	 */
	public boolean isWaitingForData()
	{
		return isWaitingForData;
	}

	/*
	 * ratio 1.0 plays back normal speed
	 * 
	 * @see com.softsynth.javasonics.recplay.Player#setPlaybackSpeed(float)
	 */
	public void setPlaybackSpeed( float ratio )
	{
		playbackSpeed = ratio;
		updateSampleRateConverter();
	}

	protected void updateSampleRateConverter()
	{
		if( recording != null )
		{
			float compoundRatio = (float) (playbackSpeed
					* recording.getFrameRate() / getFrameRate());
			Logger.println( 2, "JSPlayer: SRC ratio = " + compoundRatio + " = "
					+ playbackSpeed + " * " + recording.getFrameRate() + " / "
					+ getFrameRate() );
			playerProcessingChain.setRatio( compoundRatio );
		}
	}

	/*
	 * Get ratio of SampleRateConverter
	 * 
	 * @see com.softsynth.javasonics.recplay.Player#getPlaybackSpeed()
	 */
	public float getPlaybackSpeed()
	{
		return playbackSpeed;
	}

	/**
	 * @return Returns the secondsToSkip between buffers played back. Used when
	 *         in super fast forward or rewind
	 */
	public double getSkipDuration()
	{
		return secondsToSkip;
	}

	/**
	 * @param secondsToSkip
	 *            The secondsToSkip to set between buffers played back. Used
	 *            when in super fast forward or rewind
	 */
	public void setSkipDuration( double secondsToSkip )
	{
		this.secondsToSkip = secondsToSkip;
	}

	/**
	 * @return Returns flag for skipping. Used when in super fast forward or
	 *         rewind, when true, plays a buffer and skips
	 */
	public boolean isSkipping()
	{
		return skipping;
	}

	/**
	 * @param skipping
	 *            flag.
	 */
	public void setSkipping( boolean skipping )
	{
		this.skipping = skipping;
	}

	/**
	 * Set reverse playback flag
	 * 
	 * @see com.softsynth.javasonics.recplay.Player#setReverse(boolean)
	 */
	public void setReverse( boolean reverse )
	{
		this.reverse = reverse;
	}

	/**
	 * Reverse playback flag
	 * 
	 * @see com.softsynth.javasonics.recplay.Player#isReverse()
	 */
	public boolean isReverse()
	{
		return reverse;
	}

	/** @return scaled current position 0..1 */
	public double getNormalizedPosition()
	{
		if( positionTracker == null || recording == null )
		{
			return 0;
		}
		int playable = recording.getMaxSamplesPlayable();
		if( playable <= 0 )
		{
			return 0;
		}
		return positionTracker.getCurrentPosition() / playable;
	}

	public String stateToText( int state )
	{
		String text = "unknown";
		switch( state )
		{
		case STOPPED:
			text = "stopped";
			break;
		case PAUSED:
			text = "paused";
			break;
		case PLAYING:
			text = "playing";
			break;
		case ABORTED:
			text = "aborted";
			break;
		}
		return text;
	}

	/**
	 * Protected a non-empty recording from being overwritten.
	 * 
	 * @param recordingProtected
	 *            The recordingProtected to set.
	 */

	public void setRecordingProtection( boolean onOrOff )
	{
		recordingProtectionOption = onOrOff;
	}

	public boolean isProtected()
	{
		return recordingProtected;
	}

	public void setProtected( boolean onOrOff )
	{
		recordingProtected = onOrOff;
	}

	public void setTimeChangeInterval( int msec )
	{
		positionTracker.setInterval( msec );
	}

	public double getAutoBackStep()
	{
		return autoBackStep;
	}

	public void setAutoBackStep( double seconds )
	{
		autoBackStep = seconds;
	}

	/**
	 * @return the autoPreview
	 */
	public double getAutoPreview()
	{
		return autoPreview;
	}

	/**
	 * @param autoPreview
	 *            the autoPreview to set
	 */
	public void setAutoPreview( double autoPreview )
	{
		this.autoPreview = autoPreview;
	}
}