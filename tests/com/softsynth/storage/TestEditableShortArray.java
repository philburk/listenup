package com.softsynth.storage.test;

import java.util.Random;

import com.softsynth.storage.EditableSegmentedShortArray;
import com.softsynth.storage.EditableShortArray;

/**
 * @author Phil Burk (C) 2004
 */
public class TestEditableShortArray extends TestVirtualShortArray
{
	final static int TEST_SIZE = 16;
	Random rand = new Random();
	
	public void setup()
	{
		goodArray = new SimpleEditableShortArray();
		EditableSegmentedShortArray segArray = new EditableSegmentedShortArray();
		segArray.setSegmentSize( TEST_SIZE );
		testArray = segArray;
	}

	public void test1()
	{
		writeBoth( 0, 60 );
		// Delete in middle of segment
		deleteBoth( 5, 3 );
	}
	public void test2()
	{
		writeBoth( 0, 60 );
		deleteBoth( 3, 37 );
		insertBoth( 0, 5, 14 );
	}
	
	public void testDelete( int index )
	{
		writeBoth( 0, 100 );
		deleteBoth( index, 30 );
	}
	public void testInsert( int index )
	{
		writeBoth( 0, 100 );
		insertBoth( index, 4, 30 );
	}

	int choose( int size )
	{
		return rand.nextInt( size );
	}
	/**
	 * 
	 */
	private void testRandom( int num )
	{
		writeBoth( 0, 100 );
		for( int i=0; i<num; i++ )
		{
			if( goodArray.length() > 200 )
			{
				int index = choose( goodArray.length() );
				int numShorts = choose( goodArray.length() - index );
				deleteBoth( index, numShorts );
			}
			else
			{
				int writeIndex = choose( goodArray.length() + 1 );
				int offset = choose( 10 );
				int numShorts = choose( 50 );
				insertBoth( writeIndex, offset, numShorts );
			}
		}
	}
	/**
	 * @param i
	 * @param j
	 */
	protected void insertBoth( int writeIndex, int offset, int numShorts )
	{
		System.out.println( "insertBoth( " + writeIndex + ", " + offset + ", "
				+ numShorts + ")" );
		short[] data = new short[numShorts + offset];
		for( int i = 0; i < numShorts; i++ )
		{
			data[i + offset] = value++;
		}
		((EditableShortArray) goodArray).insert( writeIndex, data, offset,
				numShorts );
		((EditableShortArray) testArray).insert( writeIndex, data, offset,
				numShorts );

		check();
	}

	protected void deleteBoth( int index, int numShorts )
	{
		System.out.println( "deleteBoth( " + index + ", " + numShorts + ")" );
		((EditableShortArray) goodArray).delete( index, numShorts );
		((EditableShortArray) testArray).delete( index, numShorts );

		check();
	}

	public static void main( String[] args )
	{
		
		new TestEditableShortArray().test1();
		new TestEditableShortArray().test2();
		
		new TestEditableShortArray().testDelete(14);
		new TestEditableShortArray().testDelete(15);
		new TestEditableShortArray().testDelete(16);
		new TestEditableShortArray().testDelete(17);
		
		new TestEditableShortArray().testInsert(0);
		new TestEditableShortArray().testInsert(14);
		new TestEditableShortArray().testInsert(15);
		new TestEditableShortArray().testInsert(16);
		new TestEditableShortArray().testInsert(17);
		
		new TestEditableShortArray().testRandom( 100000 );
		System.exit( 0 );
	}

}
