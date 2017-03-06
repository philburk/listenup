package com.softsynth.storage.test;

import com.softsynth.storage.EditableByteArrayDeletion;
import com.softsynth.storage.FileBasedByteArray;

/**
 * @author Phil Burk (C) 2004
 */
public class TestDeletion
{
	public void test()
	{
		byte[] bar = new byte[10];
		FileBasedByteArray byteArray = createFilledByteArray( bar.length );
		
		for( int i=0; i<bar.length; i++ ) bar[i] = -1;
		byteArray.read( 0, bar, 0, 10 );
		verify( bar, 0, 0 );
		verify( bar, 3, 3 );
		verify( bar, 9, 9 );
		
		for( int i=0; i<bar.length; i++ ) bar[i] = -1;
		byteArray.read( 8, bar, 0, 2 );
		verify( bar, 0, 8 );
		verify( bar, 1, 9 );
		
		EditableByteArrayDeletion deletion = new EditableByteArrayDeletion(byteArray, 5, 3 );

		System.out.println("Read before deletion");
		for( int i=0; i<bar.length; i++ ) bar[i] = -1;
		deletion.read( 0, bar, 0, 4 );
		verify( bar, 0, 0 );
		verify( bar, 3, 3 );
		verify( bar, 4, -1 );

		System.out.println("Read across deletion");
		for( int i=0; i<bar.length; i++ ) bar[i] = -1;
		deletion.read( 3, bar, 0, 4 );
		verify( bar, 0, 3 );
		verify( bar, 1, 4 );
		verify( bar, 2, 8 );
		verify( bar, 3, 9 );
		
		System.out.println("Read after deletion");
		for( int i=0; i<bar.length; i++ ) bar[i] = -1;
		deletion.read( 5, bar, 0, 2 );
		verify( bar, 0, 8 );
		verify( bar, 1, 9 );

		byteArray.clear();
	}

	FileBasedByteArray createFilledByteArray( int size )
	{
		FileBasedByteArray byteArray = new FileBasedByteArray();
		byte[] bar = new byte[size];
		for( int i=0; i<size; i++ ) bar[i] = (byte) i;
		byteArray.write( bar, 0, bar.length );
		return byteArray;
	}
	
	/**
	 * @param index
	 * @param expected
	 */
	protected void verify( byte[] bar, int index, int expected )
	{
		int val = bar[index];
		if( val != expected )
		{
			System.out.println("ERROR: at " + index + " got " + val + ", expected " + expected );
		}
		else
		{
			System.out.println("SUCCESS: at " + index + " got " + val );
		}
	}

	public static void main( String[] args )
	{
		TestDeletion app = new TestDeletion();
		app.test();
		System.exit(0);
	}
}
