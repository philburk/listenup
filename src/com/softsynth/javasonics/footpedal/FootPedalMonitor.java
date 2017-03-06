package com.softsynth.javasonics.footpedal;

import java.awt.Frame;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import com.softsynth.javasonics.installer.Installer;
import com.softsynth.javasonics.transcriber.NetworkService;
import com.softsynth.javasonics.util.Logger;

/**
 * Monitor a foot pedal and send events to listeners if a button
 * changes.
 * 
 * @author Phil Burk (C) 2004
 */
public class FootPedalMonitor extends NetworkService
{
	private Vector listeners;
	private int previousCode = 0;
	
	private static final int DEFAULT_PORT = 17445;
	
	private static final String APP_NAME = "FootPedalServer";
	private static final String INFINITY_LIB_NAME = "hidwatch";

	/**
	 * 
	 */
	public FootPedalMonitor( Frame frame )
	{
		super(DEFAULT_PORT, frame);
		listeners = new Vector();
	}

	public void install() throws IOException
	{
		// The old code tried to put the DLL in c:\WIndows\System32 but Vista refused to cooperate.
		// Installer.getInstance().installNativeLibraryIfNeeded( getFrame(), INFINITY_LIB_NAME );
		// So now just put the library next to the company executables. 
		Installer.getInstance().installCompanyLibraryIfNeeded( getFrame(), INFINITY_LIB_NAME );
		super.install();
	}
	/**
	 * @param cmd
	 */
	public void handleCommandFromServer( String cmd )
	{
		if( (cmd.length() > 0) && (cmd.charAt(0) == '#'))
		{
			Logger.println( 2, "FootPedalMonitor.handleCommandFromServer() code = " + cmd );
			// Button states are encoded in 4 bit number added to 'A'.
			// This must match the way that the 'C' code works.
			int footPedalCode = cmd.charAt(1) - 'A';
			
			if( (footPedalCode & FootPedalEvent.VALID) != 0 )
			{
				handleCode( footPedalCode );
			}
		}
		else
		{
			Logger.println( 2, "FootPedalMonitor.handleCommandFromServer() message = " + cmd );
		}
	}

	/* (non-Javadoc)
	 * @see com.softsynth.javasonics.recplay.NetworkService#getApplicationName()
	 */
	public String getApplicationName()
	{
		return APP_NAME;
	}
	
	public void addFootPedalListener( FootPedalListener listener )
	{
		Logger.println( 2, "FootPedalMonitor.addFootPedalListener: " + listener );
		listeners.addElement( listener );
	}

	public void removeFootPedalListener( FootPedalListener listener )
	{
		Logger.println( 2, "FootPedalMonitor.removeFootPedalListener: " + listener );
		listeners.removeElement( listener );
	}

	private void fireButtonPressed( int button )
	{
		Enumeration lers = listeners.elements();
		FootPedalEvent event = new FootPedalEvent( this, button, this );
		while( lers.hasMoreElements() )
		{
			FootPedalListener listener = (FootPedalListener) lers.nextElement();
			Logger.println( 2, "FootPedalMonitor.fireButtonPressed() send to listener = " + listener );
			listener.buttonPressed( event );
		}
	}

	private void fireButtonReleased( int button )
	{
		Enumeration lers = listeners.elements();
		FootPedalEvent event = new FootPedalEvent( this, button, this );
		while( lers.hasMoreElements() )
		{
			FootPedalListener listener = (FootPedalListener) lers.nextElement();
			listener.buttonReleased( event );
		}
	}

	/** Check to see if bit changed and send message if so. */
	private void checkButtonChange( int code, int bit )
	{
		int mask = (1 << bit);
		// Is button on?
		int onMask = (code & mask);
		// Is it different than last time?
		if( (previousCode & mask) != onMask )
		{
			if( onMask != 0 )
				fireButtonPressed( bit );
			else
				fireButtonReleased( bit );
		}

	}

	private void handleCode( int code )
	{
		checkButtonChange( code, FootPedalEvent.PLAY );
		checkButtonChange( code, FootPedalEvent.FORWARD );
		checkButtonChange( code, FootPedalEvent.REWIND );
		previousCode = code;
	}

}