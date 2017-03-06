package com.softsynth.storage;

import java.io.*;

/**
 * 
 * Dynamic buffer that uses a byte array for temporary storage of data.
 * 
 * @author Phil Burk (C) 2004
 */
public class DynamicMemoryBuffer extends DynamicBuffer
{
	private ByteArrayOutputStream outStream;
	private int size = 0;

	protected DynamicMemoryBuffer()
	{
		super();
		outStream = new ByteArrayOutputStream();
	}

	public OutputStream getOutputStream() throws IOException
	{
		return outStream;
	}

	public InputStream getInputStream() throws IOException
	{
		byte[] bar = outStream.toByteArray();
		size = bar.length;
		return new ByteArrayInputStream( bar );
	}

	public void clear()
	{
		// Closing a ByteArrayOutputStream has no effect.
		outStream = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.storage.DynamicBuffer#getLength()
	 */
	public int length()
	{
		if( outStream == null )
			return size;
		else
			return outStream.size();
	}

	public void reset()
	{
		if( outStream != null )
		{
			outStream.reset();
		}		
	}
}
