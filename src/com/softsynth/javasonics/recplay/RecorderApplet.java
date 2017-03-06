package com.softsynth.javasonics.recplay;

import java.awt.FlowLayout;
import java.awt.Panel;

import com.softsynth.dsp.*;
import com.softsynth.javasonics.DeviceUnavailableException;
import com.softsynth.javasonics.util.Logger;

/**
 * Applet for Recording and Playing back sound using the JavaSonics Recorder.
 * This class provides a graphical front end for the non-graphical Recorder
 * Class.
 * <p>
 * These applet parameters are supported:
 * <p>
 * frameRate = desired frame rate, or "sample rate", for audio recording and
 * playback. If the desired frame rate is not supported then the nearest
 * available rate will be chosen.
 * <p>
 * maxRecordTime = the maximum number of seconds that can be recorded. If not
 * specified then very, very long recordings can be made.
 * <p>
 * volumeLabelLow = optional label for low end of VU meter <br>
 * volumeLabelMid = optional label for middle of VU meter <br>
 * volumeLabelHigh = optional label for high end of VU meter
 * <p>
 * Advanced version of Applet in com.softsynth.javasonics.recorder
 *
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */

public class RecorderApplet extends PlayerApplet
{
	private static final int MAX_FREE_RECORD_TIME = 60;
	private boolean recordingStatusShown = false;
	private LevelDisplay levelDisplay;

	// User settable parameters.
	private double chosenFrameRate = 22050.0;
	private boolean trimEnabled = false;
	// zero threshold means use default
	private float trimThreshold = 0.0f;
	// This is no longer considered necessary because any signed Java Applet can
	// record and upload audio.
	// TODO Remove this and the record alert code.
	private boolean showRecordingAlert = false;

	private boolean compressorEnabled = false;
	private float compressorAttackTime = DynamicGainControl.DEFAULT_ATTACK_TIME;
	private float compressorDecayTime = DynamicGainControl.DEFAULT_DECAY_TIME;
	private float compressorThreshold = DynamicGainControl.DEFAULT_THRESHOLD;
	private float compressorNoiseThreshold = DynamicGainControl.DEFAULT_NOISE_THRESHOLD;
	private float compressorCurvature = DynamicGainControl.DEFAULT_CURVATURE;

	private double maxRecordTime = 300.0; // in seconds
	protected String arrangement = "wide";
	private String volumeLabelLow;
	private String volumeLabelMid;
	private String volumeLabelHigh;

	protected boolean testSignalEnabled = false;
	protected String testSignalSpec = "sines,200,0.2,700,0.3,1345,0.1";
	private boolean recordByJavaScript = false;

	private boolean protectRecording = false;

	public RecorderApplet()
	{
		showTimeText = true;
	}

	protected void setChosenFrameRate( double rate )
	{
		chosenFrameRate = rate;
	}

	protected double getChosenFrameRate()
	{
		return chosenFrameRate;
	}

	@Override
	protected void checkAppletParameters()
	{
		super.checkAppletParameters();

		double rate = getDoubleParameter( "frameRate", getChosenFrameRate() );
		setChosenFrameRate( rate );

		if( isFreeMode() )
		{
			maxRecordTime = MAX_FREE_RECORD_TIME;
		}
		double maxRecordTimeTemp = getDoubleParameter( "maxRecordTime",
				maxRecordTime );
		setMaxRecordTime( maxRecordTimeTemp );

		trimEnabled = getBooleanParameter( "trimEnable", trimEnabled );
		trimThreshold = getFloatParameter( "trimThreshold", trimThreshold );

		testSignalEnabled = getBooleanParameter( "testSignal",
				testSignalEnabled );
		String temp = getParameter( "testSignalSpec" );
		if( temp != null )
		{
			testSignalSpec = temp;
		}

		compressorEnabled = getBooleanParameter( "compressorEnable",
				compressorEnabled );
		compressorAttackTime = getFloatParameter( "compressorAttackTime",
				compressorAttackTime );
		compressorDecayTime = getFloatParameter( "compressorDecayTime",
				compressorDecayTime );
		compressorThreshold = getFloatParameter( "compressorThreshold",
				compressorThreshold );
		compressorNoiseThreshold = getFloatParameter(
				"compressorNoiseThreshold", compressorNoiseThreshold );
		compressorCurvature = getFloatParameter( "compressorCurvature",
				compressorCurvature );

		// Specify labels for audio level (VU) meter
		volumeLabelLow = getParameter( "volumeLabelLow" );
		volumeLabelMid = getParameter( "volumeLabelMid" );
		volumeLabelHigh = getParameter( "volumeLabelHigh" );

		temp = getParameter( "arrangement" );
		if( temp != null )
		{
			arrangement = temp;
		}

		protectRecording = getBooleanParameter( "protectRecording",
				protectRecording );
	}

	/**
	 * @return
	 */
	public boolean isCompressorEnabled()
	{
		return compressorEnabled;
	}

	/**
	 * @return
	 */
	public boolean isTrimEnabled()
	{
		return trimEnabled;
	}

	/**
	 * @param b
	 */
	public void setCompressorEnabled( boolean b )
	{
		compressorEnabled = b;
	}

	/**
	 * @param b
	 */
	public void setTrimEnabled( boolean b )
	{
		trimEnabled = b;
	}

	/**
	 * @return Maximum time in seconds to record sound.
	 */
	public double getMaxRecordTime()
	{
		return maxRecordTime;
	}

	/**
	 * @param maxTime
	 *            Maximum time in seconds to record sound. Must be less than
	 *            value set by Applet parameter.
	 */
	public void setMaxRecordTime( double maxTime )
	{
		if( isFreeMode() )
		{
			if( maxTime > MAX_FREE_RECORD_TIME )
			{
				SimpleDialog
						.alert( "Maximum record time for free version is "
								+ MAX_FREE_RECORD_TIME
								+ " seconds.\n"
								+ "Set \"maxRecordTime\" parameter to less than or equal to "
								+ MAX_FREE_RECORD_TIME );
				maxTime = MAX_FREE_RECORD_TIME;
			}
		}
		maxRecordTime = maxTime;
	}

	/** Override this if you want something before the position bar. */
	protected Panel createLevelDisplay()
	{
		if( !showVUMeter )
		{
			return new Panel();
		}
		// Add volume meters to show loudness.
		levelDisplay = new LevelDisplay();
		if( (volumeLabelLow != null) || (volumeLabelMid != null)
				|| (volumeLabelHigh != null) )
		{
			levelDisplay.addLabels( volumeLabelLow, volumeLabelMid,
					volumeLabelHigh );
		}
		return levelDisplay;
	}

	/**
	 * Set the non-graphical player object.
	 *
	 * @throws DeviceUnavailableException
	 */
	@Override
	protected void setPlayer( Player pPlayer )
			throws DeviceUnavailableException
	{
		super.setPlayer( pPlayer );
		if( levelDisplay != null )
		{
			levelDisplay.setPlayer( pPlayer );
		}
	}

	protected void addUploadGUI()
	{
	}

	protected void addTextFieldGUI()
	{
	}

	private void addTallAudioGUI()
	{
		addNorthRack( createMainPanel() );

		boolean showTimeHere = showTimeText && !putTimeOnTop;
		if( showVUMeter && showTimeHere )
		{
			FlowLayout layout;
			Panel panelC = new Panel( layout = new FlowLayout() );
			layout.setHgap( 0 );
			layout.setVgap( 0 );
			panelC.setLayout( layout );
			panelC.add( createLevelDisplay() );
			panelC.add( createTimeTextDisplay() );
			addNorthRack( panelC );
		}
		else if( showVUMeter )
		{
			addNorthRack( createLevelDisplay() );
		}
		else if( showTimeHere )
		{
			addNorthRack( createTimeTextDisplay() );
		}

		if( showPositionDisplay )
		{
			addCenterRack( createPositionDisplay() );
		}
		// South racks
		addUploadGUI();
		addTextFieldGUI();
	}

	private void addWideAudioGUI()
	{
		Panel panelA = new Panel();
		panelA.setLayout( new FlowLayout() );
		panelA.add( createMainPanel() );
		panelA.add( createLevelDisplay() );
		addNorthRack( panelA );

		addCenterRack( createWideMiddleDisplay() );
		// South racks
		addUploadGUI();
		addTextFieldGUI();
	}

	@Override
	protected void addAudioGUI()
	{
		if( arrangement.equals( "wide" ) )
		{
			addWideAudioGUI();
		}
		else if( arrangement.equals( "tall" ) )
		{
			addTallAudioGUI();
		}
		else
		{
			throw new RuntimeException( "The arrangement = \"" + arrangement
					+ "\". Must be \"tall\" or \"wide\"." );
		}
		setWaveMenuEnabled( true );
	}

	@Override
	protected void setupAudio() throws Exception
	{
		setupPlayer( getChosenFrameRate() );
		setRecording( createRecording() );
	}

	@Override
	public Player createPlayer( double frameRate ) throws Exception
	{
		JSRecorder recorder = new JSRecorder( preferNative, frameRate,
				getNumChannels() );
		recorder.setTestSignalEnabled( testSignalEnabled );
		recorder.setTestSignalSpec( testSignalSpec );

		recorder.setRecordingProtection( protectRecording );
		recorder.setTimeChangeInterval( timeChangeInterval );
		return recorder;
	}

	@Override
	protected void updateSignalProcessors()
	{
		super.updateSignalProcessors();
		if( (getRecording() != null) && (getPlayer() != null) )
		{
			BufferedSignalProcessor first;
			BufferedSignalProcessor last;
			first = last = new OffsetBlocker();

			// VOX voice activated switch
			if( isTrimEnabled() )
			{
				Logger.println( "silence trimmer enabled." );
				// Use player frame rate because before SR converter.
				boolean trusted = true;
				SilenceTrimmer trimmer = new SilenceTrimmer( getRecording()
						.getFrameRate(), trusted );
				if( trimThreshold > 0.0f )
					trimmer.setThreshold( trimThreshold );
				last.setNext( trimmer );
				last = trimmer;
			}

			if( compressorEnabled )
			{
				Logger.println( "Dynamic Range Compression enabled." );
				DynamicGainControl compressor = new DynamicGainControl(
						(float) getRecording().getFrameRate() );
				compressor.setAttackTime( compressorAttackTime );
				compressor.setDecayTime( compressorDecayTime );
				compressor.setThreshold( compressorThreshold );
				compressor.setNoiseThreshold( compressorNoiseThreshold );
				compressor.setCurvature( compressorCurvature );
				last.setNext( compressor );
				last = compressor;

			}
			((JSRecorder) getPlayer()).setRecordChain( first, last );
		}
	}

	@Override
	public Recording createRecording( int maxSamples )
	{
		Logger.println( 1, "createRecording() with maxSamples = " +  maxSamples + ", rate = " + getChosenFrameRate() );
		Recording reco = new DynamicRecording( maxSamples, isEditable(),
				useFileCache );
		reco.setFrameRate( getChosenFrameRate() );
		return reco;
	}

	@Override
	public Recording createRecording( short[] data )
	{
		Recording reco = createRecording();
		reco.insert( 0, data, 0, data.length );
		return reco;
	}

	@Override
	public Recording createRecording()
	{
		Logger.println( 1, "createRecording() with a maxRecordTime = " +  maxRecordTime + ", rate = " + getChosenFrameRate() );
		return createRecording( (int) (maxRecordTime * getChosenFrameRate() * getNumChannels()) );
	}

	/** Return the non-graphical recorder object. */
	protected final Recorder getRecorder()
	{
		return (Recorder) getPlayer();
	}

	@Override
	protected Panel createMainPanel()
	{
		canRecord = true;
		return super.createMainPanel();
	}

	/**
	 * Called by recorder when starting or stopping recording.
	 */
	@Override
	public synchronized void playerStateChanged( Player player, int state,
			Throwable exc )
	{
		// Hide alert here in case recorder hits maxRecordTime
		// TODO wrap all this in isApplet()
		if( state != Recorder.RECORDING )
		{
			RecordingAlert.hideAlert();
			if( recordingStatusShown )
			{
				showStatus( "Recording stopped." );
				recordingStatusShown = false;
			}
		}
		else
		{
			if( showRecordingAlert && recordByJavaScript )
			{
				// We are being called by JavaScript so alert user about
				// recording!
				RecordingAlert.showAlert();
			}

			if( isApplet() )
			{
				showStatus( "Now Recording!" );
				recordingStatusShown = true;
			}
		}
		super.playerStateChanged( player, state, exc );
	}

	@Override
	public void setupTest()
	{
	}

	/* Can be run as either an application or as an applet. */
	public static void main( String args[] )
	{
		final RecorderApplet applet = new RecorderApplet();
		applet.runApplication(args);
	}

	// JavaScript Interface =========================================
	/**
	 * Begin recording audio input. Use message passing to background thread to
	 * allow JavaScript to trigger recording. Continue until stopAudio() is
	 * called or maxSeconds is reached.
	 */
	public void record()
	{
		if( getRecorder() != null )
		{
			abortDownloading();
			recordByJavaScript = true;
			getRecorder().recordAudio();
		}
	}

	/**
	 * Is audio recorder currently recording?
	 */
	@Override
	public boolean isRecording()
	{
		return getRecorder().getState() == Recorder.RECORDING;
	}

	/**
	 * @return
	 */
	public float getTrimThreshold()
	{
		return trimThreshold;
	}

	/**
	 * @param f
	 */
	public void setTrimThreshold( float f )
	{
		trimThreshold = f;
	}

}
