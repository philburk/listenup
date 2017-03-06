package com.softsynth.storage.test;

import com.softsynth.storage.EditableByteArrayInsertion;
import com.softsynth.storage.FileBasedByteArray;

/**
 * @author Phil Burk (C) 2004
 */
public class TestInsertion extends TestDeletion
{
	public void test()
	{
		byte[] bar = new byte[10];
		FileBasedByteArray byteArray = createFilledByteArray( bar.length );
		FileBasedByteArray insertArray = new FileBasedByteArray();
		
		EditableByteArrayInsertion insertion = new EditableByteArrayInsertion( byteArray, insertArray, 5 );
		for( int i=0; i<bar.length; i++ ) bar[i] = (byte) (100 + i);
		insertion.insert( bar, 0, 3 );
		
		for( int i=0; i<bar.length; i++ ) bar[i] = -1;
		insertion.read( 3, bar, 0, 7 );
		int j=0;
		verify( bar, j++, 3 );
		verify( bar, j++, 4 );
		verify( bar, j++, 100 );
		verify( bar, j++, 101 );
		verify( bar, j++, 102 );
		verify( bar, j++, 5 );
		verify( bar, j++, 6 );
		
		insertion.clear();
	}


	public static void main( String[] args )
	{
		TestInsertion app = new TestInsertion();
		app.test();
		System.exit(0);
	}
}
