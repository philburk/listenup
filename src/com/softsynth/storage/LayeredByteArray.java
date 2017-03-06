package com.softsynth.storage;

import com.softsynth.javasonics.util.Logger;

/**
 * @author Phil Burk (C) 2004
 */
public class LayeredByteArray implements EditableByteArray
{
	private VirtualByteArray top;
	private int numLayers = 0;

	/**
	 * Override this to use something other than the default FileBasedByteArray.
	 * 
	 * @return array for basic storage or insertion layer
	 */
	VirtualByteArray createByteArray()
	{
		return new FileBasedByteArray();
	}

	public void write( byte[] array, int firstIndex, int count )
	{
		if( top == null )
		{
			top = createByteArray();
		}
		top.write( array, firstIndex, count );
	}

	public synchronized void read( int readIndex, byte[] array, int firstIndex,
			int count )
	{
		top.read( readIndex, array, firstIndex, count );
	}

	public synchronized int length()
	{
		if( top == null )
			return 0;
		else
			return top.length();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.javasonics.recplay.VirtualByteArray#clear()
	 */
	public synchronized void clear()
	{
		if( top != null )
		{
			top.clear();
			top = null;
		}
	}

	/**
	 * Add a layer that makes it look like something has been deleted.
	 * 
	 * @see com.softsynth.storage.EditableByteArray#delete(int, int)
	 */
	public void delete( int index, int count )
	{
		Logger.println( 1, "LayeredByteArray.delete( " + index + ", " + count
				+ ")" );
		VirtualByteArray deletion = new EditableByteArrayDeletion( top, index,
				count );
		numLayers += 1;
		top = deletion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.storage.EditableByteArray#write(int, short[], int,
	 *      int)
	 */
	public synchronized void write( int writeIndex, byte[] array, int firstIndex, int count )
	{
		if( top == null )
		{
			top = createByteArray();
		}

		if( writeIndex == length() )
		{
			// Append to end of array.
			top.write( array, firstIndex, count );
		}
		else
		{
			// Inserting into middle of array.
			EditableByteArrayInsertion insertion = null;
			if( top instanceof EditableByteArrayInsertion )
			{
				// Maybe we can use existing insertion layer.
				insertion = (EditableByteArrayInsertion) top;
				// Require a new insertion layer if inserting at a different
				// point.
				if( writeIndex != insertion.getInsertionIndex() )
				{
					insertion = null;
				}
			}

			if( insertion == null )
			{
				Logger.println( 1,
						"LayeredByteArray.write: make new insertion at "
								+ writeIndex + ", " + count + ")" );
				insertion = new EditableByteArrayInsertion( top,
						createByteArray(), writeIndex );

				numLayers += 1;
				top = insertion;
			}
			insertion.insert( array, firstIndex, count );
		}
	}

	public synchronized void flatten()
	{
		if( numLayers > 0 )
		{
			VirtualByteArray flat = createByteArray();

			byte[] bar = new byte[1024];
			int numLeft = top.length();
			int readIndex = 0;
			while( numLeft > 0 )
			{
				int numToRead = (numLeft < bar.length ) ? numLeft : bar.length;
				top.read(readIndex, bar, 0, numToRead );
				flat.write( bar, 0, numToRead );
				numLeft -= numToRead;
				readIndex += numToRead;
			}
			numLayers = 0;
			top.clear();
			top = flat;
		}
	}
}