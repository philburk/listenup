package com.softsynth.storage;


/**
 * @author Phil Burk (C) 2004
 */
public class FileBasedShortArray implements VirtualShortArray
{
	// Store audio in a random access file.
	protected VirtualByteArray byteArray;
	// Buffer for converting shorts to bytes
	protected byte[] buffer;
	protected static final int BUFFER_SIZE = 2048;

	/**
	 *  
	 */
	public FileBasedShortArray()
	{
		buffer = new byte[BUFFER_SIZE];
		byteArray = createByteArray();
	}
	
	/**
	 * @return
	 */
	public VirtualByteArray createByteArray()
	{
		return new FileBasedByteArray();
	}

	/** Write shorts to internal buffer. */
	public void write( short[] array, int firstIndex, int numShorts )
	{
		int numShortsLeft = numShorts;
		int shortsInBuffer = buffer.length >> 1;
		int shortIndex = firstIndex;
		while( numShortsLeft > 0 )
		{
			int numShortsToWrite = (numShortsLeft < shortsInBuffer) ? numShortsLeft
					: shortsInBuffer;

			int byteIndex = 0;
			for( int i = 0; i < numShortsToWrite; i++ )
			{
				short sample = array[shortIndex++];
				buffer[byteIndex++] = (byte) (sample);
				buffer[byteIndex++] = (byte) (sample >> 8);
			}
			numShortsLeft -= numShortsToWrite;

			// Shift by one to convert shorts to bytes.
			byteArray.write( buffer, 0, byteIndex );
		}
	}

	/** Read shorts from file. */
	public synchronized void read( int readIndex, short[] array,
			int firstIndex, int numShorts )
	{
		int numShortsLeft = numShorts;
		int shortsInBuffer = buffer.length >> 1;
		int shortIndex = firstIndex;
		int byteReadIndex = readIndex << 1;
		while( numShortsLeft > 0 )
		{
			int numShortsToRead = (numShortsLeft < shortsInBuffer) ? numShortsLeft
					: shortsInBuffer;

			// Shift by one to convert numShorts to numBytes.
			int numBytesToRead = (numShortsToRead << 1);
			byteArray.read( byteReadIndex, buffer, 0, numBytesToRead );
			byteReadIndex += numBytesToRead;

			int byteIndex = 0;
			for( int i = 0; i < numShortsToRead; i++ )
			{
				int loByte = buffer[byteIndex++];
				int hiByte = buffer[byteIndex++];

				int sample = (hiByte << 8) | (loByte & 0x00FF);
				array[shortIndex++] = (short) sample;
			}
			numShortsLeft -= numShortsToRead;
		}
	}

	public synchronized int length()
	{
		int blen = byteArray.length();
		if( blen < 0 )
			return blen;
		else
			return blen >> 1; // convert to num shorts
	}

	public synchronized void clear()
	{
		byteArray.clear();
	}

	public void flatten()
	{
		// Nothing to do here.
	}
}