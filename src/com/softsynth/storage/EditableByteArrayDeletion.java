package com.softsynth.storage;

/**
 * A layer on top of another VirtualByteArray that makes it looks like
 * a chunk has been deleted.
 * @author Phil Burk (C) 2004
 */
public class EditableByteArrayDeletion implements VirtualByteArray
{
	private VirtualByteArray byteArray;
	private int deletionStart;
	private int deletionCount;

	public EditableByteArrayDeletion(VirtualByteArray byteArray,
			int deletionStart, int deletionCount)
	{
		this.byteArray = byteArray;
		this.deletionStart = deletionStart;
		this.deletionCount = deletionCount;
	}

	/** Write bytes to end. */
	public void write( byte[] array, int firstIndex, int count )
	{
		byteArray.write( array, firstIndex, count );
	}

	/** Read bytes from underlying array but skip deleted portion. */
	public synchronized void read( int readIndex, byte[] array, int firstIndex,
			int count )
	{
		if( readIndex < deletionStart )
		{
			if( (readIndex + count) < deletionStart )
			{
				// Entire block before deletion point.
				byteArray.read( readIndex, array, firstIndex, count );
			}
			else
			{
				int size1 = deletionStart - readIndex;
				byteArray.read( readIndex, array, firstIndex, size1 );

				int startIndex = deletionStart + deletionCount;
				int size2 = count - size1;
				byteArray.read( startIndex, array, firstIndex + size1, size2 );
			}
		}
		else
		{
			byteArray
					.read( readIndex + deletionCount, array, firstIndex, count );
		}
	}

	public synchronized int length()
	{
		int blen = byteArray.length();
		if( blen < 0 )
			return blen;
		else
			return blen - deletionCount;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.javasonics.recplay.VirtualByteArray#clear()
	 */
	public synchronized void clear()
	{
		byteArray.clear();
		deletionStart = 0;
		deletionCount = 0;
	}


	public void flatten()
	{
		// Nothing to do here.
	}
}