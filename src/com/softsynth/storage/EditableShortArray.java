package com.softsynth.storage;


/**
 * @author Phil Burk (C) 2004
 */
public interface EditableShortArray extends VirtualShortArray
{
	public void delete( int index, int count );
	
	/** Write shorts to internal buffer. If before end then insert data.
	 */
	public void insert( int writeIndex, short[] array, int firstIndex,
			int numShorts );

}