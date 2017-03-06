package com.softsynth.javasonics.transcriber;

import java.awt.Frame;
import java.io.IOException;
import java.util.StringTokenizer;

import com.softsynth.javasonics.recplay.*;
import com.softsynth.javasonics.util.Logger;

/**
 * July 2008 - Added support for DIRECT mode for Classic devices. And support
 * for reading a footswitch without using HID. July 2009 - Added "press"
 * behavior and sendOnEOL.
 * 
 * @author Phil Burk (C) 2004-2008
 */

// TODO Fix double display in SpeechMikeServer when disconnected. (?)
public class SpeechMikeController extends NetworkPlayerController
{
	private static final String BEHAVIOR_NAME_ON_OFF = "onoff";
	private static final String BEHAVIOR_NAME_PRESS = "press";
	private static final String BEHAVIOR_NAME_TOGGLE = "toggle";
	private boolean recordingEnabled = true;
	private boolean recordMode = false;
	private boolean proMode = true;
	// So that rewind and forward will select to end.
	private boolean overwriteMode = true;

	// Singletons for inner class.
	private MikeBehavior behaviorToggle = new MikeBehaviorToggle();
	private MikeBehavior behaviorPress = new MikeBehaviorPressToRecord();
	private MikeBehavior behaviorOnOff = new MikeBehaviorOnOff();
	private MikeBehavior behavior = behaviorToggle;

	private boolean sendOnEOL = false;
	private SendOnEOLThread sendOnEOLThread;
	private Sendable sendable;

	private static final int SEND_ON_EOL_DELAY = 2000;

	// These commands are sent from the ListenUp client to the SpeechMikeServer.
    private static final String CMD_LED_ALLOFF = "off";
    private static final String CMD_LED_RECORD = "rec";
    private static final String CMD_LED_RECORD_BLINK = "recbl";
    private static final String CMD_LED_INSERT = "ins";
    private static final String CMD_LED_INSERT_BLINK = "insbl";
    private static final String CMD_LED_CMD = "cmd";
    private static final String CMD_LED_CMD_BLINK = "cmdbl";

	// These commands are sent from the SpeechMikeServer to the ListenUp client.
	private static final String CMD_TOGGLE = "PSTOGGLE_P";
	private static final String CMD_RECORD_ON = "RECORD_P";
	private static final String CMD_RECORD_OFF = "RECORD_R";
	private static final String CMD_FAST_FORWARD_ON = "FFWD_P";
	private static final String CMD_FAST_FORWARD_OFF = "FFWD_R";
	private static final String CMD_FAST_REWIND_ON = "FRWD_P";
	private static final String CMD_FAST_REWIND_OFF = "FRWD_R";
	private static final String CMD_EOL_ON = "LETTEREND_P";
	private static final String CMD_EOL_OFF = "LETTEREND_R";
	private static final String CMD_PLAY_ON = "PLAY_P";
	private static final String CMD_PLAY_OFF = "PLAY_R";
	private static final String CMD_STOP = "STOP_P";
	private static final String CMD_INSERT = "INSERT_P";
	private static final String CMD_DIRECT_PLAY_PRESSED = "CMD_DIRECT_PLAY_PRESSED";
	private static final String CMD_DIRECT_PLAY_RELEASED = "CMD_DIRECT_PLAY_RELEASED";
	private static final String CMD_DIRECT_RECORD_PRESSED = "CMD_DIRECT_RECORD_PRESSED";
	private static final String CMD_DIRECT_RECORD_RELEASED = "CMD_DIRECT_RECORD_RELEASED";
	private static final String CMD_DIRECT_STOP_PRESSED = "CMD_DIRECT_STOP_PRESSED";
	// Added with SpeechMike version 1.2
	private static final String CMD_VERSION = "VERSION";

	private HotKeyMap[] hotKeyMaps = { new HotKeyMap( "f1" ),
			new HotKeyMap( "f2" ), new HotKeyMap( "f3" ),
			new HotKeyMap( "f4" ), new HotKeyMap( "eol" ) };

	private boolean requestSelectToEnd;
	private double serverVersion;

	/**
	 * 
	 */
	public SpeechMikeController(Player player, Frame frame, int speechMikePort)
	{
		super( player, speechMikePort, frame );

		// Add listener that will change the LEDs on the speechmike.
		player.addPlayerListener( new PlayerListener()
		{
			public void playerLevelChanged( Player player )
			{
			}

			public void playerStateChanged( Player player, int state,
					Throwable thr )
			{
				behavior.setLightsByState( state );
				// We have to do this here so to avoid jumps after the audio
				// stops.
				if( requestSelectToEnd && player.isStopped() )
				{
					requestSelectToEnd = false;
					selectToEnd();
				}
			}

			public void playerTimeChanged( Player player, double time )
			{
			}
		} );

		turnLightsOff();
	}

	public void parseParameters( ParameterHolder parameters )
	{
		recordingEnabled = parameters.getBooleanParameter(
				"enableSpeechMikeRecording", recordingEnabled );
		String temp = parameters.getParameter( "speechMikeBehavior" );
		if( temp != null )
		{
			setBehavior( temp );
		}

		// Parse HotKey maps.
		for( int i = 0; i < hotKeyMaps.length; i++ )
		{
			HotKeyMap map = hotKeyMaps[i];
			map.hotKeySpec = parameters.getParameter( "speechMikeHotKey"
					+ map.mappedKey );
		}
	}

	public void setBehavior( String behaviorName )
	{
		if( behaviorName == null )
		{
			behavior = behaviorToggle;
		}
		else
		{
			behaviorName = behaviorName.toLowerCase();
			if( (behaviorName == null) || behaviorName.equals( BEHAVIOR_NAME_TOGGLE ) )
			{
				behavior = behaviorToggle;
			}
			else if( behaviorName.equals( BEHAVIOR_NAME_PRESS ) )
			{
				behavior = behaviorPress;
			}
			else if( behaviorName.equals( BEHAVIOR_NAME_ON_OFF ) )
			{
				behavior = behaviorOnOff;
			}
			else
			{
				throw new IllegalArgumentException(
						"Unrecognized behavior for SpeechMike = "
								+ behaviorName );
			}
		}
	}

	protected void sendMessageToServerSafely( String msg )
	{
		try
		{
			Logger.println( 1, "sendMessageToServerSafely: " + msg );
			sendMessageToServer( msg );
		} catch( IOException e )
		{
			e.printStackTrace();
		}
	}

	private void turnLightInsert()
	{
		sendMessageToServerSafely( CMD_LED_INSERT );
	}

	private void turnLightRecording()
	{
		sendMessageToServerSafely( CMD_LED_RECORD );
	}

	// Blink red if there is a danger of overwriting existing material.
	private void turnLightBlinking()
	{
		if( isRegionSelected() )
		{
			turnLightRecordingBlink();
		}
		else
		{
			turnLightInsertBlink();
		}
	}

	private boolean isRegionSelected()
	{
		return getPlayer().getStopIndex() > getPlayer().getStartIndex();
	}

	private void turnLightInsertBlink()
	{
		sendMessageToServerSafely( CMD_LED_INSERT_BLINK );
	}

	private void turnLightRecordingBlink()
	{
		sendMessageToServerSafely( CMD_LED_RECORD_BLINK );
	}

	private void turnLightsOff()
	{
		sendMessageToServerSafely( CMD_LED_ALLOFF );
	}

	private void pauseAudio()
	{
		getPlayer().pauseAudio();
	}

	private void playNormalSpeed()
	{
		getPlayer().playNormalSpeed();
	}

	/** Play but do not auto rewind if at end of recording. */
	private void playNoRewind()
	{
		getPlayer().playAudioNoRewind();
	}

	private void recordAudio()
	{
		if( recordingEnabled && (getPlayer() instanceof Recorder) )
		{
			((Recorder) getPlayer()).recordAudio();
		}
	}

	/**
	 * @param cmd
	 */
	public void handleCommandFromServer( String cmd )
	{
		Logger.println( 1, "SpeechMike: command = " + cmd );
		behavior.handleCommand( cmd );
	}

	/**
	 * Isolate alternative behaviors in subclasses of MikeBehavior. For example
	 * some customers want to record while holding down the record button.
	 * 
	 * @author Phil Burk (C) 2009 Mobileer Inc
	 */
	abstract class MikeBehavior
	{
		// Abstract methods that distinguish the differing behaviors.
		protected abstract void setLightsByState( int state );

		protected abstract void handleProPlayOff();

		protected abstract void handleProPlayOn();

		protected abstract void handleProRecordOff();

		protected abstract void handleProRecordOn();

		protected abstract void handleFastRewindOn();
		
		protected abstract void handleFastRewindOff();

		protected abstract void handleFastForwardOff();

		public void handleCommand( String cmd )
		{
			boolean handled = handleProMode( cmd );
			if( !handled )
			{
				handled = handleDirectMode( cmd );
			}
			if( !handled )
			{
				handleCommonMode( cmd );
			}
		}

		/** Handler for Speech Mike Pro */
		protected boolean handleProMode( String cmd )
		{
			boolean result = true;
			if( cmd.equals( CMD_TOGGLE ) )
			{
				proMode = true;
				if( getPlayer().isStopped() )
				{
					if( recordMode )
					{
						recordAudio();
					}
					else
					{
						handleProPlayOn();
					}
				}
				else
				{
					if( recordMode )
					{
						pauseAudio();
					}
					else
					{
						handleProPlayOff();
					}
				}
			}
			else if( cmd.equals( CMD_PLAY_ON ) )
			{
				proMode = true;
				if( recordMode )
				{
					recordAudio();
				}
				else
				{
					handleProPlayOn();
				}
			}
			else if( cmd.equals( CMD_RECORD_ON ) )
			{
				proMode = true;
				handleProRecordOn();
			}
			else if( cmd.equals( CMD_RECORD_OFF ) )
			{
				proMode = true;
				handleProRecordOff();
			}
			else if( cmd.equals( CMD_STOP ) || cmd.equals( CMD_PLAY_OFF ) )
			{
				handleProPlayOff();
				recordMode = false;
			}
			else
			{
				result = false;
			}
			return result;
		}

		/**
		 * @param cmd
		 */
		private boolean handleDirectMode( String cmd )
		{
			boolean result = true;
			if( cmd.equals( CMD_DIRECT_PLAY_PRESSED ) )
			{
				proMode = false;
				playNoRewind();
			}
			else if( cmd.equals( CMD_DIRECT_RECORD_PRESSED ) )
			{
				proMode = false;
				recordAudio();
			}
			else if( cmd.equals( CMD_DIRECT_RECORD_RELEASED ) )
			{
				// Recording will have cleared any selected area.
				pauseAudio();
			}
			else if( cmd.equals( CMD_DIRECT_STOP_PRESSED )
					|| cmd.equals( CMD_DIRECT_PLAY_RELEASED ) )
			{
				pauseAudio();
				recordMode = false;
			}
			else
			{
				result = false;
			}
			return result;
		}

		private void handleCommonMode( String cmd )
		{
			if( cmd.equals( CMD_FAST_REWIND_ON ) )
			{
				recordMode = false;
				handleFastRewindOn();
			}
			else if( cmd.equals( CMD_FAST_REWIND_OFF ) )
			{
				recordMode = false;
				handleFastRewindOff();
			}
			else if( cmd.equals( CMD_FAST_FORWARD_ON ) )
			{
				recordMode = false;
				getPlayer().playFastForward();
			}
			else if( cmd.equals( CMD_FAST_FORWARD_OFF ) )
			{
				recordMode = false;
				handleFastForwardOff();
			}
			else if( cmd.equals( CMD_EOL_ON ) )
			{
				recordMode = false;
				goToEnd();
				if( sendOnEOL )
				{
					startSendOnEOL();
				}
			}
			else if( cmd.equals( CMD_EOL_OFF ) )
			{
				if( sendOnEOL )
				{
					cancelSendOnEOL();
				}
			}
			else if( cmd.equals( CMD_INSERT ) )
			{
				if( getPlayer().isStopped() )
				{
					if( isRegionSelected() )
					{
						clearSelection();
						overwriteMode = false;
					}
					else
					{
						selectToEnd();
						overwriteMode = true;
					}
				}
			}
			else if( cmd.startsWith( CMD_VERSION ) )
			{
				// Parse comma delimited string
				StringTokenizer parser = new StringTokenizer( cmd, " " );
				if( parser.hasMoreElements() )
				{
					parser.nextElement();
					String versionText = (String) parser.nextElement();
					try
					{
						serverVersion = Double.parseDouble( versionText );
						// We added support for the SpeechMike generating hot
						// keys in V1.2.
						if( serverVersion >= 1.2 )
						{
							sendHotKeyRequests();
						}
					} catch( NumberFormatException nfe )
					{
						nfe.printStackTrace();
					}
				}
			}

			setLightsByState();
		}

		protected void setLightsByState()
		{
			setLightsByState( getPlayer().getState() );
		}
	}

	/** Toggle record mode. Toggle record/play using play button. */
	class MikeBehaviorToggle extends MikeBehavior
	{

		protected void setLightsByState( int state )
		{
			if( (state == Player.STOPPED) || (state == Player.PAUSED) )
			{
				if( proMode )
				{
					if( recordMode )
					{
						turnLightBlinking();
					}
					else
					{
						turnLightsOff();
					}
				}
				else
				{
					turnLightBlinking();
				}
			}
			else if( state == Player.RECORDING )
			{
				turnLightRecording();
			}
			else
			{
				turnLightsOff();
			}
		}

		protected void handleProRecordOff()
		{
		}

		protected void handleProRecordOn()
		{
			recordMode = !recordMode; // toggle record mode.
			setLightsByState();
		}

		protected void handleFastRewindOn()
		{
			getPlayer().playRewind();
		}
		
		protected void handleFastRewindOff()
		{
		}

		protected void handleFastForwardOff()
		{
		}

		protected void handleProPlayOff()
		{
			pauseAudio();
		}

		protected void handleProPlayOn()
		{
			playNormalSpeed();
		}


	}

	/** Record while the Record button is pressed. */
	class MikeBehaviorPressToRecord extends MikeBehavior
	{
		protected void setLightsByState( int state )
		{
			if( (state == Player.STOPPED) || (state == Player.PAUSED) )
			{
				turnLightBlinking();
			}
			else if( state == Player.RECORDING )
			{
				turnLightRecording();
			}
			else
			{
				turnLightsOff();
			}
		}

		protected void handleProRecordOn()
		{
			recordMode = false;
			recordAudio();
		}

		protected void handleProRecordOff()
		{
			recordMode = false;
			overwriteMode = true;
			pauseAudio();
		}

		protected void handleFastRewindOn()
		{
			// Clear selection range so that we can set it even if we hit beginning and stop with finger on button.
			if( overwriteMode )
			{
				getPlayer().setStopTime( getPlayer().getStartTime() );
			}
			getPlayer().playRewind();
		}
		
		protected void handleFastRewindOff()
		{
			requestSelectToEnd = overwriteMode;
			pauseAudio();
			playNormalSpeed();
		}

		protected void handleFastForwardOff()
		{
			requestSelectToEnd = overwriteMode;
			pauseAudio();
		}

		protected void handleProPlayOff()
		{
			requestSelectToEnd = overwriteMode;
			pauseAudio();
		}

		protected void handleProPlayOn()
		{
			playNoRewind();
		}
	}

	/** Toggle recording on/off when Record is pressed. */
	class MikeBehaviorOnOff extends MikeBehaviorPressToRecord
	{

		/** Handler for Speech Mike Pro */
		protected boolean handleProMode( String cmd )
		{
			if( cmd.equals( CMD_TOGGLE ) )
			{
				proMode = true;
				if( getPlayer().isStopped() )
				{
					handleProPlayOn();
				}
				else
				{
					handleProPlayOff();
				}
				return true;
			}
			else
			{
				return super.handleProMode( cmd );
			}
		}
		
		protected void handleProRecordOn()
		{
			recordMode = !recordMode;
			System.out.println("recordMode set to " + recordMode);
			if( recordMode )
			{
				recordAudio();
			}
			else
			{
				overwriteMode = true;
				pauseAudio();
			}
		}

		protected void handleProRecordOff()
		{
		}

		protected void handleProPlayOn()
		{
			recordMode = false;
			super.handleProPlayOn();
		}
		
		protected void handleProPlayOff()
		{
			recordMode = false;
			super.handleProPlayOff();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.javasonics.recplay.NetworkService#getApplicationName()
	 */
	public String getApplicationName()
	{
		return  "SpeechMikeServer";
	}

	class HotKeyMap
	{
		String mappedKey;
		String hotKeySpec;

		public HotKeyMap(String mappedKey)
		{
			this.mappedKey = mappedKey;
		}
	}

	public void sendHotKeyRequests()
	{
		// Parse HotKey maps.
		for( int i = 0; i < hotKeyMaps.length; i++ )
		{
			HotKeyMap map = hotKeyMaps[i];
			if( map.hotKeySpec != null )
			{
				sendMessageToServerSafely( "sethk" + " " + map.mappedKey + "="
						+ map.hotKeySpec.trim() );
			}
		}
	}

	public void cancelSendOnEOL()
	{
		if( sendOnEOLThread != null )
		{
			sendOnEOLThread.cancel();
			sendOnEOLThread = null;
		}
	}

	/**
	 * Start counting down then upload the recording.
	 * 
	 */
	public void startSendOnEOL()
	{
		if( getPlayer().getMaxPlayableTime() > 0.0001 )
		{
			cancelSendOnEOL();
			sendOnEOLThread = new SendOnEOLThread();
			sendOnEOLThread.start();
		}
	}

	class SendOnEOLThread extends Thread
	{
		boolean go = true;
		boolean messageDisplayed = false;

		public void run()
		{
			if( sendable != null )
			{
				try
				{
					int countdown = SEND_ON_EOL_DELAY;
					int duration = 500;
					boolean lightsOn = false;
					turnLightsOff();
					while( go && (countdown > 0) )
					{
						sleep( duration );
						if( lightsOn )
						{
							turnLightsOff();
						}
						else
						{
							turnLightInsert();
						}
						lightsOn = !lightsOn;
						countdown -= duration;
						if( go )
						{
							sendable.displayMessage( "Send in " + countdown
									+ " msec." );
							messageDisplayed = true;
						}
						duration = 100;
					}
					if( go )
					{
						sendable.sendRecordedMessage();
						messageDisplayed = false;
					}
				} catch( InterruptedException e )
				{

				} finally
				{
					behavior.setLightsByState();
				}
			}
		}

		public void cancel()
		{
			go = false;
			interrupt();
			if( messageDisplayed )
			{
				sendable.displayMessage( "Send cancelled." );
			}
		}
	}

	/**
	 * @return Returns the recordingEnabled.
	 */
	public boolean isRecordingEnabled()
	{
		return recordingEnabled;
	}

	/**
	 * @param recordingEnabled
	 *            The recordingEnabled to set.
	 */
	public void setRecordingEnabled( boolean recordingEnabled )
	{
		this.recordingEnabled = recordingEnabled;
	}

	/** Is the speech mike supported on this platform? */
	public static boolean isSupported()
	{
		String osName = System.getProperty( "os.name" );
		// Currently only supported on Windows.
		return (osName.toLowerCase().indexOf( "windows" ) >= 0);
	}

	/**
	 * If non-null then pressing and holding EOL button will cause the recorded
	 * message to be sent/uploaded.
	 * 
	 * @param sendable
	 */
	public void setSendable( Sendable sendable )
	{
		this.sendable = sendable;
		sendOnEOL = (sendable != null);
	}

	private void goToEnd()
	{
		double maxTime = getPlayer().getMaxTime();
		// Turn off selected region.
		getPlayer().setStartTime( maxTime );
		getPlayer().setStopTime( maxTime );
		getPlayer().setPositionInSeconds( maxTime );
	}

	// Select from the current position to the end of the recording.
	private void selectToEnd()
	{
		// Prepare to re-record over from here to the end.
		double pos = getPlayer().getPositionInSeconds();
		getPlayer().setStartTime( pos );
		getPlayer().setStopTime( getPlayer().getMaxTime() );
		// Force display to update.
		getPlayer().setPositionInSeconds( pos );
	}

	private void clearSelection()
	{
		double pos = getPlayer().getPositionInSeconds();
		getPlayer().setStartTime( pos );
		getPlayer().setStopTime( pos );
		// Force redraw of display.
		getPlayer().setPositionInSeconds( pos );
	}
}