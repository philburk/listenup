package com.softsynth.storage;

/**
 * @author Phil Burk (C) 2004
 */
public interface VirtualByteArray
{
	/** Write shorts to end of internal buffer. */
	public void write( byte[] array, int firstIndex,
			int count );

	/** Read bytes from anywhere in buffer. */
	public void read( int readIndex, byte[] array, int firstIndex,
			int count );
	
	public int length();
	
	public void clear();

	/**
	 * Simplify any temporary layering.
	 */
	public void flatten();
}