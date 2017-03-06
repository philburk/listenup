package com.softsynth.storage;

/**
 * @author Phil Burk (C) 2004
 */
public interface VirtualShortArray
{
	/** Write shorts to end of internal buffer. */
	public void write( short[] array, int firstIndex,
			int numShorts );

	/** Read shorts from anywhere in buffer. */
	public void read( int readIndex, short[] array, int firstIndex,
			int numShorts );
	
	/** Number of valid data values in array. */
	public int length();
	
	public void clear();

	/**
	 * Simplify any temporary layering.
	 */
	public void flatten();
}