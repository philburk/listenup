package com.softsynth.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.softsynth.javasonics.error.ErrorReporter;
import com.softsynth.javasonics.util.Logger;

/**
 * DynamicBuffer that uses a file for temporary storage of data.
 * 
 * @author Phil Burk (C) 2004
 */
public class DynamicFileBuffer extends DynamicBuffer
{
	private File tempFile;
	private BufferedOutputStream bufferedOutputStream;

	protected DynamicFileBuffer()
	{
		super();
		tempFile = createTempFile();
	}

	File createTempFile()
	{
		File file = StorageTools.createTempFile( "temp", ".raw" );
		// file.deleteOnExit(); // Requires Java 1.2 and later.
		return file;
	}

	/**
	 * Get a stream for writing to.
	 * Only creates one outputStream and returns same one each time.
	 * 
	 * @see com.softsynth.storage.DynamicBuffer#getOutputStream()
	 */
	public OutputStream getOutputStream() throws IOException
	{
		if( bufferedOutputStream == null )
		{
			try
			{
				FileOutputStream outStream = new FileOutputStream( tempFile );
				bufferedOutputStream = new BufferedOutputStream( outStream );
			} catch( SecurityException e )
			{
				ErrorReporter.show(
						"Could not open file. Needs signed Applet using Sun Java.",
						e );
			}
		}
		return bufferedOutputStream;
	}

	/**
	 * Get a stream for reading data from.
	 * 
	 * @see com.softsynth.storage.DynamicBuffer#getInputStream()
	 */
	public InputStream getInputStream() throws IOException
	{
		bufferedOutputStream.close();
		FileInputStream outStream = new FileInputStream( tempFile );
		BufferedInputStream bufferedInputStream = new BufferedInputStream(
				outStream );
		return bufferedInputStream;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.storage.DynamicBuffer#getLength()
	 */
	public int length()
	{
		return (int) tempFile.length();
	}

	public void clear()
	{
		if( bufferedOutputStream != null )
		{
			Logger.println( 2, "DynamicFileBuffer: deleting bufferedOutputStream" );
			try
			{
				bufferedOutputStream.close();
			} catch( IOException e )
			{
			}
			bufferedOutputStream = null;
		}
		if( tempFile != null )
		{
			if( tempFile.exists() )
			{
				Logger.println( 1, "DynamicFileBuffer: deleting temp file " + tempFile );
				tempFile.delete();
			}
			tempFile = null;
		}
	}

	public void reset()
	{
		throw new RuntimeException("reset() not implemented for file cache"); // TODO
	}

}