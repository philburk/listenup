package com.softsynth.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Buffer that you can fill up using a stream, then read out using a stream.
 * 
 * @author Phil Burk (C) 2004
 */
public abstract class DynamicBuffer
{
	/**
	 * Get a stream for writing to.
	 * Only creates one outputStream and returns same one each time.
	 */
	public abstract OutputStream getOutputStream() throws IOException;
	public abstract InputStream getInputStream() throws IOException;

	/** Delete any dynamically allocated files or arrays used to buffer the data. */
	public abstract void clear();
	
	/** Remove any data that would go into an InputStream. */
	public abstract void reset();

	/**
	 * @return number of bytes written to buffer
	 */
	public abstract int length();

	public static void writeStreamToStream( InputStream inStream,
			OutputStream outStream ) throws IOException
	{
		byte[] buffer = new byte[1024];
		while( true )
		{
			int numRead = inStream.read( buffer );
			if( numRead < 0 )
			{
				break; // EOF
			}
			outStream.write( buffer, 0, numRead );
		}
	}

}
