package com.softsynth.upload;

import java.io.InputStream;

/**
 * Listen for the progress and completion of an upload.
 * @author Phil Burk (C) 2009 Mobileer Inc
 */
public abstract class UploadListener
{	
	public InputStream filterInputStream( InputStream inStream )
	{
		return inStream;
	}
	public abstract void uploadComplete( );
}
