package com.softsynth.javasonics.error;

/** RuntimeException that is meant to be handled by the user.
 * 
 * @author Phil Burk (C) 2010 Mobileer Inc
 */
public class UserRuntimeException extends RuntimeException
{
	public UserRuntimeException(String msg, Throwable ex)
	{
		super( msg, ex );
	}

	public UserRuntimeException(String msg)
	{
		super( msg );
	}
}
