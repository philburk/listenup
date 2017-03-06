package com.softsynth.javasonics.error;

/** Exception that is meant to be handled by the web developer.
 * 
 * @author Phil Burk (C) 2010 Mobileer Inc
 */
public class WebDeveloperRuntimeException extends RuntimeException
{
	public WebDeveloperRuntimeException(String msg, Throwable ex)
	{
		super( msg, ex );
	}

	public WebDeveloperRuntimeException(String msg)
	{
		super( msg );
	}
}
