package com.softsynth.storage.test;

import com.softsynth.storage.LayeredByteArray;

/**
 * @author Phil Burk (C) 2004
 */
public class TestLayeredByteArray extends TestDeletion
{
	public void test()
	{
		byte[] bar = new byte[10];
		LayeredByteArray byteArray = new LayeredByteArray();
		for( int i = 0; i < bar.length; i++ )
			bar[i] = (byte) i;
		byteArray.write( bar, 0, bar.length );

		byteArray.delete( 4, 3 );
		for( int i = 0; i < bar.length; i++ )
			bar[i] = -1;
		byteArray.read( 3, bar, 0, 4 );
		int j = 0;
		verify( bar, j++, 3 );
		verify( bar, j++, 7 );
		verify( bar, j++, 8 );

		System.out.println("Insert into middle of array.");
		for( int i = 0; i < bar.length; i++ )
			bar[i] = (byte) (100 + i);
		byteArray.write( 4, bar, 0, 3 );

		for( int i = 0; i < bar.length; i++ )
			bar[i] = -1;
		byteArray.read( 3, bar, 0, 6 );
		j = 0;
		verify( bar, j++, 3 );
		verify( bar, j++, 100 );
		verify( bar, j++, 101 );
		verify( bar, j++, 102 );
		verify( bar, j++, 7 );
		verify( bar, j++, 8 );
		verify( bar, j++, -1 );
		
		System.out.println("Write to previous insertion point.");
		for( int i = 0; i < bar.length; i++ )
			bar[i] = (byte) (50 + i);
		byteArray.write( 7, bar, 0, 3 );

		for( int i = 0; i < bar.length; i++ )
			bar[i] = -1;
		byteArray.read( 3, bar, 0, 9 );
		j = 0;
		verify( bar, j++, 3 );
		verify( bar, j++, 100 );
		verify( bar, j++, 101 );
		verify( bar, j++, 102 );
		verify( bar, j++, 50 );
		verify( bar, j++, 51 );
		verify( bar, j++, 52 );
		verify( bar, j++, 7 );
		verify( bar, j++, 8 );
		verify( bar, j++, -1 );

		System.out.println("Delete across insertion.");
		byteArray.delete( 3, 3 );
		for( int i = 0; i < bar.length; i++ )
			bar[i] = -1;
		byteArray.read( 2, bar, 0, 6 );
		j = 0;
		verify( bar, j++, 2 );
		verify( bar, j++, 102 );
		verify( bar, j++, 50 );
		verify( bar, j++, 51 );
		verify( bar, j++, 52 );
		verify( bar, j++, 7 );
		verify( bar, j++, -1 );

		System.out.println("Insert into middle of array.");
		for( int i = 0; i < bar.length; i++ )
			bar[i] = (byte) (20 + i);
		byteArray.write( 5, bar, 3, 3 );

		for( int i = 0; i < bar.length; i++ )
			bar[i] = -1;
		byteArray.read( 2, bar, 0, 7 );
		j = 0;
		verify( bar, j++, 2 );
		verify( bar, j++, 102 );
		verify( bar, j++, 50 );
		verify( bar, j++, 23 );
		verify( bar, j++, 24 );
		verify( bar, j++, 25 );
		verify( bar, j++, 51 );
		verify( bar, j++, -1 );
		
		byteArray.clear();
	}

	public static void main( String[] args )
	{
		TestLayeredByteArray app = new TestLayeredByteArray();
		app.test();
		System.exit( 0 );
	}
}