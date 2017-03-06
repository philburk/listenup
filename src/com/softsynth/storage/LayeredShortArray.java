package com.softsynth.storage;

/**
 * @author Phil Burk (C) 2004
 */
public class LayeredShortArray extends FileBasedShortArray implements
		EditableShortArray
{

	/**
	 * @return
	 */
	public VirtualByteArray createByteArray()
	{
		return new LayeredByteArray();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.storage.EditableShortArray#delete(int, int)
	 */
	public void delete( int index, int count )
	{
		((EditableByteArray) byteArray).delete( index << 1, count << 1 );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.storage.EditableShortArray#write(int, short[], int,
	 *      int)
	 */
	public void insert( int writeIndex, short[] array, int firstIndex,
			int numShorts )
	{
		int numShortsLeft = numShorts;
		int shortsInBuffer = buffer.length >> 1;
		int shortIndex = firstIndex;
		int byteWriteIndex = writeIndex << 1;
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
			((EditableByteArray) byteArray).write( byteWriteIndex, buffer, 0, byteIndex );
			byteWriteIndex += byteIndex;
		}
	}

	/**
	 * Simplify any temporary layering.
	 */
	public void flatten()
	{
		byteArray.flatten();
	}

}