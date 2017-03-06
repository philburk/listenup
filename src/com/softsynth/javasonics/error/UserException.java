package com.softsynth.javasonics.error;

/** Exception that is meant to be handled by the user.
 * 
 * @author Phil Burk (C) 2010 Mobileer Inc
 */
public class UserException extends Exception
{
	public UserException(String msg, Throwable ex)
	{
		super( msg, ex );
	}

	public UserException(String msg)
	{
		super( msg );
	}
}
