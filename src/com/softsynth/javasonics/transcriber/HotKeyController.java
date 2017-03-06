package com.softsynth.javasonics.transcriber;

import java.awt.Frame;
import java.io.IOException;

import com.softsynth.javasonics.error.ErrorReporter;
import com.softsynth.javasonics.recplay.HotKeyOptions;
import com.softsynth.javasonics.recplay.Player;
import com.softsynth.javasonics.recplay.Recorder;
import com.softsynth.javasonics.util.Logger;

/**
 * @author Phil Burk (C) 2004
 */
public class HotKeyController extends NetworkPlayerController
{
	private static final int DEFAULT_PORT = 17295;
	private static final int HOT_KEY_OFFSET = 100;
	private static final String APP_NAME = "HotKeyServer";
	private HotKeyOptions hotKeyOptions;

	/**
	 * @param hotKeyOptions  
	 *  
	 */
	public HotKeyController(Player player, Frame frame, HotKeyOptions hotKeyOptions )
	{
		super( player, DEFAULT_PORT, frame );
		this.hotKeyOptions = hotKeyOptions;
	}

	/**
	 * @param playCommand2
	 * @param request
	 */
	private void registerHotKey( int index, String request )
	{
		try
		{
			String msg = "a " + index + "=" + request;
			Logger.println( 2, "registerHotKey: " + msg.trim() );
			sendMessageToServer( msg );
		} catch( IOException e )
		{
			ErrorReporter.show( "Error registering HotKey", e );
		}
	}

	/** Overrides default from NetworkServices. */
	public void loadOptions()
	{
		for( int i = 0; i < hotKeyOptions.options.length; i++ )
		{
			if( hotKeyOptions.options[i] != null )
			{
				registerHotKey( HOT_KEY_OFFSET + i, hotKeyOptions.options[i] );
			}
		}
	}

	/**
	 * @param playCommand2
	 * @param request
	 */
	protected void requestEcho( String text )
	{
		try
		{
			String msg = "e " + text;
			Logger.println( 0, "requestEcho: " + msg );
			sendMessageToServer( msg );
		} catch( IOException e )
		{
			ErrorReporter.show( "Error synchronizing with HotKeyServer", e );
		}
	}

	/**
	 * @param cmd
	 */
	public void handleCommandFromServer( String cmd )
	{
		Logger.println( 0, "HotKey: command = " + cmd );
		char c = cmd.charAt( 0 );
		if( c == '#' )
		{
			String indexText = cmd.substring( 1 );
			handleHotKeyIndex( indexText );
		}
		else if( cmd.startsWith( "ERROR" ) )
		{
			throw new RuntimeException( "HotKeyController " + cmd );
		}
	}

	/**
	 * @param cmd
	 */
	private void handleHotKeyIndex( String indexText )
	{
		int command = Integer.parseInt( indexText ) - HOT_KEY_OFFSET;
		switch( command )
		{
		case HotKeyOptions.PLAY_INDEX:
			getPlayer().playNormalSpeed();
			break;
		case HotKeyOptions.RECORD_INDEX:
			if( getPlayer() instanceof Recorder )
			{
				((Recorder) getPlayer()).recordAudio();
			}
			break;
		case HotKeyOptions.PAUSE_INDEX:
			getPlayer().pauseAudio();
			break;
		case HotKeyOptions.STOP_INDEX:
			getPlayer().stopAudio();
			break;
		case HotKeyOptions.REWIND_INDEX:
			getPlayer().playRewind();
			break;
		case HotKeyOptions.FORWARD_INDEX:
			getPlayer().playFastForward();
			break;
		case HotKeyOptions.TO_END_INDEX:
			getPlayer().toEnd();
			break;
		case HotKeyOptions.TO_BEGIN_INDEX:
			getPlayer().toBegin();
			break;
		default:
			throw new RuntimeException(
					"HotKeyController got unrecognized hot key ID." );
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.javasonics.recplay.NetworkService#getApplicationName()
	 */
	public String getApplicationName()
	{
		return APP_NAME;
	}

	/** Is the speech mike supported on this platform? */
	public static boolean isSupported()
	{
		String osName = System.getProperty( "os.name" );
		// Currently only supported on Windows.
		return( osName.toLowerCase().indexOf( "windows" ) >= 0 );
	}
}