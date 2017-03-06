package com.softsynth.storage;

/**
 * @author Phil Burk (C) 2004
 */
public class EditableByteArrayInsertion implements VirtualByteArray
{
	// Underlying array
	private VirtualByteArray byteArray;
	// Array containing inserted data.
	private VirtualByteArray insertedArray;
	private int insertionStart;

	/**
	 *  
	 */
	public EditableByteArrayInsertion(VirtualByteArray byteArray,
			VirtualByteArray insertedArray, int insertionStart)
	{
		this.byteArray = byteArray;
		this.insertedArray = insertedArray;
		this.insertionStart = insertionStart;
	}

	/**
	 * @return Returns the insertionIndex.
	 */
	public int getInsertionIndex()
	{
		return insertionStart + insertedArray.length();
	}

	/*
	 * Write new data at insertion point.
	 * 
	 * @see com.softsynth.storage.VirtualByteArray#write(byte[], int, int)
	 */
	public void insert( byte[] array, int firstIndex, int count )
	{
		insertedArray.write( array, firstIndex, count );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.storage.VirtualByteArray#write(byte[], int, int)
	 */
	public void write( byte[] array, int firstIndex, int count )
	{
		byteArray.write( array, firstIndex, count );
	}

	/** Read bytes from underlying array or from inserted array to make
	 * it look like there is just one contiguous array.
	 * 
	 * @see com.softsynth.storage.VirtualByteArray#read(int, byte[], int, int)
	 */
	public synchronized void read( int readIndex, byte[] array, int firstIndex, int count )
	{
		int numLeft = count;
		int targetIndex = firstIndex;

		while( numLeft > 0 )
		{
			int numToRead = numLeft;
			if( readIndex < insertionStart )
			{
				// Read before insertion block
				if( (readIndex + numToRead) > insertionStart )
				{
					numToRead = insertionStart - readIndex;
				}
				byteArray.read( readIndex, array, targetIndex, numToRead );
			}
			else if( readIndex < getInsertionIndex() )
			{
				// Read from insertion block
				if( (readIndex + numToRead) > getInsertionIndex() )
				{
					numToRead = getInsertionIndex() - readIndex;
				}
				insertedArray.read( readIndex - insertionStart, array,
						targetIndex, numToRead );
			}
			else
			{
				// Read after insertion block
				byteArray.read( readIndex - insertedArray.length(), array, targetIndex, numToRead );
			}

			numLeft -= numToRead;
			targetIndex += numToRead;
			readIndex += numToRead;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.storage.VirtualByteArray#length()
	 */
	public synchronized int length()
	{
		return byteArray.length() + insertedArray.length();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.storage.VirtualByteArray#clear()
	 */
	public synchronized void clear()
	{
		byteArray.clear();
		insertedArray.clear();
		insertionStart = 0;
	}

	public void flatten()
	{
		// Nothing to do here.
	}
}