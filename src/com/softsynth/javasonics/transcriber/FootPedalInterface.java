package com.softsynth.javasonics.transcriber;

import java.awt.Frame;

import com.softsynth.javasonics.error.ErrorReporter;
import com.softsynth.javasonics.footpedal.FootPedalEvent;
import com.softsynth.javasonics.footpedal.FootPedalListener;
import com.softsynth.javasonics.footpedal.FootPedalMonitor;
import com.softsynth.javasonics.recplay.PlayerControlHandler;
import com.softsynth.javasonics.util.Logger;

/**
 * @author Phil Burk (C) 2004
 */
public class FootPedalInterface
{
	FootPedalMonitor footPedalMonitor = null;
	PlayerControlHandler playerControl;
	Frame frame;
	FootPedalListener listener;

	/**
	 *  
	 */
	public FootPedalInterface(Frame frame, PlayerControlHandler playerControl)
	{
		this.frame = frame;
		this.playerControl = playerControl;
	}

	public synchronized void stop()
	{
		if( footPedalMonitor != null )
		{
			if( listener != null )
			{
				footPedalMonitor.removeFootPedalListener( listener );
			}
			listener = null;
			Logger.println( 2, "FootPedalInterface.stop() Foot pedal monitor stopping." );
			footPedalMonitor.stop();
			footPedalMonitor = null;
		}
	}

	synchronized void startOnce( Frame frame, FootPedalListener listener )
	{
		Logger.println( 1, "FootPedalInterface.startOnce() monitor = " + footPedalMonitor);
		if( footPedalMonitor == null )
		{
			try
			{
				footPedalMonitor = new FootPedalMonitor( frame );
				footPedalMonitor.start();
				Logger.println( 2, "FootPedalInterface.startOnce() started." );
			} catch( UnsatisfiedLinkError exc )
			{
				ErrorReporter.show( "FootPedal library not installed.", exc );
			} catch( Throwable exc )
			{
				ErrorReporter.show( "FootPedal error.", exc );
			}
		}
		// Add a listener that will control the player.
		if( footPedalMonitor != null )
		{
			footPedalMonitor.addFootPedalListener( listener );
		}
	}

	/** Start monitor that generates events when the foot pedal is pressed. */
	public void start()
	{
		listener = new FootPedalListener()
		{
			public void buttonPressed( FootPedalEvent event )
			{
				switch( event.id )
				{
				case FootPedalEvent.PLAY:
					playerControl.handlePlay();
					break;
				case FootPedalEvent.REWIND:
					playerControl.handleRewind();
					break;
				case FootPedalEvent.FORWARD:
					playerControl.handleFastForward();
					break;
				default:
					break;
				}
			}

			public void buttonReleased( FootPedalEvent event )
			{
				switch( event.id )
				{
				case FootPedalEvent.PLAY:
				case FootPedalEvent.REWIND:
				case FootPedalEvent.FORWARD:
					playerControl.handlePause();
					break;
				default:
					break;
				}
			}
		};

		startOnce( frame, listener );
	}

	/** Is the foot pedal supported on this platform? */
	public static boolean isSupported()
	{
		String osName = System.getProperty( "os.name" );
		// Currently only supported on Windows.
		return( osName.toLowerCase().indexOf( "windows" ) >= 0 );
	}
}