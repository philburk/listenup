package com.softsynth.javasonics.recplay;

/**
 * Used to abstract getParameter() of the Applet class.
 * @author Phil Burk (C) 2009 Mobileer Inc
 */
public interface ParameterHolder
{
	public String getParameter( String name );
	
	public boolean getBooleanParameter( String paramName,
			boolean defaultValue );

	public float getFloatParameter( String paramName, float defaultValue );
}
