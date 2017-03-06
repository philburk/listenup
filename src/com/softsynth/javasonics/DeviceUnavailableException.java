package com.softsynth.javasonics;

import com.softsynth.javasonics.error.UserException;

/**
 * Exception thrown when a hardware device is busy, or otherwise unavailable.
 * 
 * @author Phil Burk (C) 2001 SoftSynth.com
 * @version 0.1
 */

public class DeviceUnavailableException extends UserException
{
	private final static String ERROR_HELP = 
			"\nThe Java Security settings in the browser may be blocking access to the microphone.\n"
			+ "Please try giving the Java plugin more permissions for this website.\n"
			+ "Or this may be caused by another application using the audio device.\n"
			+ "Please close other audio programs and TRY AGAIN TO PLAY OR RECORD.\n"
			+ "Please make sure you have speakers or headphones plugged in.\n"
			+ "If recording, please make sure you have a microphone plugged in.\n"
			+ "You may need to restart the browser after fixing the audio problem.\n";

	public DeviceUnavailableException(String msg, Throwable ex)
	{
		super( msg + ERROR_HELP, ex );
	}

	public DeviceUnavailableException(String msg)
	{
		super( msg + ERROR_HELP );
	}
}
