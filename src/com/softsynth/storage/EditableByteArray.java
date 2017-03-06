package com.softsynth.storage;


/**
 * @author Phil Burk (C) 2004
 */
public interface EditableByteArray extends VirtualByteArray
{
	public void delete( int index, int count );
	
	/** Write shorts to internal buffer. If before end then insert data.
	 */
	public void write( int writeIndex, byte[] array, int firstIndex,
			int count );

}