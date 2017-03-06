package com.softsynth.storage.test;

import java.io.ByteArrayOutputStream;

import com.softsynth.storage.SegmentedShortArray;
import com.softsynth.storage.VirtualShortArray;


/**
 * @author Phil Burk (C) 2004
 */
public class BenchmarkWriting
{
	private static final int BLOCK_SIZE = 160;
	static short[] shortData;
	static byte[] byteData;
	
	public BenchmarkWriting()
	{
		shortData = new short[BLOCK_SIZE];
		byteData = new byte[BLOCK_SIZE];
	}

	public void testVirtualShortArray( VirtualShortArray varray )
	{
		long startTime = System.currentTimeMillis();
		int count = 0;
		for( count = 0; count < (12 * 1024 * 1024); count += shortData.length )
		{
			varray.write( shortData, 0, shortData.length );
		}
		long endTime = System.currentTimeMillis();
		System.out.println( "Elapsed msec = " + (endTime - startTime) );
		System.out.println( "Num shorts = " + count );
	}

	public void test1()
	{
		System.out.println( "Start writing DynamicShortArray." );
		DynamicShortArray varray = new DynamicShortArray();
		testVirtualShortArray( varray );
	}
	public void test2()
	{
		System.out.println( "Start writing SegmentedShortArray." );
		SegmentedShortArray varray = new SegmentedShortArray();
		testVirtualShortArray( varray );
	}
	public void test3()
	{
		System.out.println( "Start writing ByteArrayOutputStream." );
		long startTime = System.currentTimeMillis();
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		int count = 0;
		for( count = 0; count < (12 * 1024 * 1024); count += byteData.length )
		{
			outStream.write( byteData, 0, byteData.length );
		}
		long endTime = System.currentTimeMillis();
		System.out.println( "Elapsed msec = " + (endTime - startTime) );
		System.out.println( "Num bytes = " + count );
	}

	public static void main( String[] args )
	{
		BenchmarkWriting app = new BenchmarkWriting();

		System.out
				.println( "Version = " + System.getProperty( "java.version" ) );
		app.test1();
		app.test2();
		System.exit( 0 );
	}
}
