package com.softsynth.javasonics;

import com.softsynth.javasonics.error.UserRuntimeException;

/**
 * Exception thrown when a hardware device stops reading.
 * 
 * @author Phil Burk (C) 2010 SoftSynth.com
 * @version 0.1
 */

public class DeviceTimeoutException extends UserRuntimeException
{
	private final static String ERROR_HELP = "\nAudio input or output suddenly failed.\n"
			+ "This may be caused by unplugging a USB microphone.\n";

	public DeviceTimeoutException(String msg, Throwable ex)
	{
		super( msg + ERROR_HELP, ex );
	}

	public DeviceTimeoutException(String msg)
	{
		super( msg + ERROR_HELP );
	}
}
