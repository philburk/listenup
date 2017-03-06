package com.softsynth.storage.test;

import com.softsynth.storage.FixedShortArray;
import com.softsynth.storage.SegmentedShortArray;
import com.softsynth.storage.VirtualShortArray;

/**
 * @author Phil Burk (C) 2004
 */
public class TestVirtualShortArray
{
	VirtualShortArray goodArray;
	VirtualShortArray testArray;
	short value = 0;
	int count = 0;

	public TestVirtualShortArray()
	{
		setup();
	}

	public void setup()
	{
		goodArray = new FixedShortArray( 32 * 1024 );
		SegmentedShortArray segArray = new SegmentedShortArray();
		segArray.setSegmentSize( 16 );
		testArray = segArray;
	}

	public void test()
	{
		writeBoth( 5, 30 );
		writeBoth( 2, 40 );
	}

	/**
	 * @param i
	 * @param j
	 */
	protected void writeBoth( int offset, int numShorts )
	{
		System.out.println( "writeBoth( " + offset + ", " + numShorts + ")" );
		short[] data = new short[numShorts + offset];
		for( int i = 0; i < numShorts; i++ )
		{
			data[i + offset] = value++;
		}
		goodArray.write( data, offset, numShorts );
		testArray.write( data, offset, numShorts );
		check();
	}

	/**
	 * @param index
	 * @param expected
	 */
	protected void check()
	{
		int numShorts = goodArray.length();
		short[] goodData = new short[numShorts];
		goodArray.read( 0, goodData, 0, numShorts );
		short[] testData = new short[numShorts];
		testArray.read( 0, testData, 0, numShorts );
		boolean pass = true;
		for( int i = 0; i < numShorts; i++ )
		{
			if( goodData[i] != testData[i] )
			{
				throw new RuntimeException( "ERROR: at " + i + " got "
						+ testData[i] + ", expected " + goodData[i] );
			}
		}
		System.out.println( "PASS: " + count++ + ", numShorts " + numShorts );
	}

	public static void main( String[] args )
	{
		TestVirtualShortArray app = new TestVirtualShortArray();
		app.test();
		System.exit( 0 );
	}
}
