package com.softsynth.storage;


/**
 * @author Phil Burk (C) 2004
 */
public class FixedShortArray implements VirtualShortArray
{
	short[] data;
	int count;

	/**
	 *  
	 */
	public FixedShortArray( int size )
	{
		data = new short[size];
		count = 0;
	}
	
	/**
	 *  Create an array with preset data.
	 */
	public FixedShortArray( short[] data )
	{
		this.data = data;
		count = data.length;
	}

	public void addToLength( int numShorts )
    {
		count += numShorts;
    }
    
    /** Write shorts to end of internal buffer. */
    public void write( short[] array, int firstIndex, int numShorts)
    {
    	System.arraycopy(array, firstIndex, data, count, numShorts);
        count += numShorts;
    }

    /** Read shorts from internal buffer. */
    public void read(int readIndex, short[] array, int firstIndex, int numShorts)
    {
    	System.arraycopy(data, readIndex, array, firstIndex, numShorts);
    }

	/* (non-Javadoc)
	 * @see com.softsynth.javasonics.recplay.VirtualShortArray#length()
	 */
	public int length()
	{
		return count;
	}

	/* (non-Javadoc)
	 * @see com.softsynth.javasonics.recplay.VirtualShortArray#clear()
	 */
	public void clear()
	{
		count = 0;
	}

	public void flatten()
	{
		// Nothing to do here.
	}

	public void move( int fromIndex, int toIndex, int numToMove )
	{
		System.arraycopy( data, fromIndex, data, toIndex, numToMove );
	}

	public short[] getData()
	{
		return data;
	}
	/**
	 * @param newBuffer
	 */
	public void setData( short[] newBuffer )
	{
		data = newBuffer;
	}
}