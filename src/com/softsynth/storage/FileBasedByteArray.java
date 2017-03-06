package com.softsynth.storage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.softsynth.javasonics.error.ErrorReporter;
import com.softsynth.javasonics.util.Logger;

/**
 * @author Phil Burk (C) 2004
 */
public class FileBasedByteArray implements VirtualByteArray
{
	// Store audio in a random access file.
	private RandomAccessFile ranFile;
	private File file;

	private void makeCacheFile() throws IOException
	{
		try
		{
			file = StorageTools.createTempFile( "cache", ".raw" );
			Logger.println(0,"Create cache file " + file );
			ranFile = new RandomAccessFile( file, "rw" );
		} catch( SecurityException e )
		{
			ErrorReporter.show(
					"Could not open file. Needs signed Applet using Sun Java.",
					e );
		}
	}

    /** Write bytes to end of file. */
    public synchronized void write( byte[] array, int firstIndex, int count)
    {
		try
		{
			if( ranFile == null )
			{
				makeCacheFile();
			}

			ranFile.seek( ranFile.length() );
			ranFile.write( array, firstIndex, count );

		} catch( IOException e )
		{
			ErrorReporter.show( "File cache write() failure.", e );
		}
	}

	/** Read shorts from file. */
	public synchronized void read( int readIndex, byte[] array,
			int firstIndex, int count )
	{
		try
		{
			if( ranFile == null )
			{
				makeCacheFile();
			}

			ranFile.seek( (long) readIndex );
			ranFile.readFully( array, firstIndex, count );
		} catch( IOException e )
		{
			ErrorReporter.show( "File cache read() failure.", e );
		}
	}

	public synchronized int length()
	{
		if( ranFile == null )
			return 0;

		try
		{
			// Shift by one to convert numBytes to numShorts.
			return (int) ranFile.length();
		} catch( IOException e )
		{
			ErrorReporter.show( "File cache length() failure.", e );
		}
		return -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.javasonics.recplay.VirtualByteArray#clear()
	 */
	public synchronized void clear()
	{
		try
		{
			if( ranFile != null )
			{
				ranFile.close();
				ranFile = null;
				Logger.println(0,"Delete cache file " + file );
				file.delete();
				file = null;
			}
		} catch( IOException e )
		{
			ErrorReporter.show( "File cache clear() failure.", e );
		}
	}

	public void flatten()
	{
		// Nothing to do here.
	}
}