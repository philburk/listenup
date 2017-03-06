package com.softsynth.storage.test;

import com.softsynth.storage.EditableShortArray;


/**
 * Uses memory to store data. Moves data when inserting or deleting.
 * @author Phil Burk (C) 2004
 */
public class SimpleEditableShortArray extends DynamicShortArray implements
		EditableShortArray
{
	public void delete( int index, int numShorts )
	{
		int indexAbove = index + numShorts;
		int numToMove = length() - indexAbove;
		if( numToMove > 0 )
		{
			move( indexAbove, index, numToMove );
		}
		addToLength( 0 - numShorts );
	}

	/** Insert shorts to internal buffer. */
	public void insert( int writeIndex, short[] array, int firstIndex,
			int numShorts )
	{
    	checkRoom( numShorts );
		// If there are any samples above us then move them up.
		int numToMove = length() - writeIndex;
		if( numToMove > 0 )
		{
			move( writeIndex, writeIndex + numShorts, numToMove );
		}
		System.arraycopy(array, firstIndex, getData(), writeIndex, numShorts);
		addToLength( numShorts );
	}
}