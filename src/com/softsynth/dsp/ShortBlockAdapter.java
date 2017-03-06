package com.softsynth.dsp;

/** Adapt block size in a stream of data.
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */
public abstract class ShortBlockAdapter
{
	private short[] pending;

	private int numPending;

	public static void main(String args[])
	{
		System.out.println("Test block adapter.");
		ShortBlockAdapter adapter = new ShortBlockAdapter( 10 )
		{
			int idx = 0;

			public void processBlock( short[] data, int num )
			{
				for( int i=0; i<num; i++ )
				{
					if( data[ i ] != idx )
					{
						throw new RuntimeException( "Sequence error " +
							data[ i ] + " != " + idx );
					}
					System.out.println( "Verify " + idx );
					idx += 1;
				}
			}
		};
		
		short[] data = new short[20];
		short index = 0;
		for( int i=0; i<10; i++ )
		{
			int numData = (int) (data.length * Math.random());
			for( int j=0; j<numData; j++ )
			{
				data[j] = index++;
			}
			adapter.write( data, 0, numData );
		}
		System.out.println("Flush");
		System.out.flush();
		adapter.flush( (short) 0 );
	}

	/**
	 * @param blockSize
	 */
	public ShortBlockAdapter(int blockSize )
	{
		pending = new short[ blockSize ];
		numPending = 0;
	}
	
	/**
	 * Flush any data remaining in the buffer.
	 * @param value Number used to fill remainder of buffer.
	 */
	public void flush( int value )
	{
		// Fill remainder of block with value.
		if( numPending > 0 )
		{
			int numToAdd = pending.length - numPending;
			for( int i=0; i<numToAdd; i++ )
			{
				pending[ numPending + i ] = (short) value;
			}
			processBlock( pending, pending.length );
			numPending = 0;
		}
	}

	
	/**
	 * @param data
	 * @param i
	 * @param numData
	 */
	public void write(short[] data, int offset, int numData)
	{
		while( numData > 0 )
		{
			// Try to fill block;
			int numToAdd = pending.length - numPending;
			if( numToAdd > numData ) numToAdd = numData;
			for( int i=0; i<numToAdd; i++ )
			{
				pending[ numPending + i ] = data[offset + i ];
			}
			numData -= numToAdd;
			offset += numToAdd;
			numPending += numToAdd;
			
			// If block full then send it.
			if( numPending == pending.length )
			{
				processBlock( pending, numPending );
				numPending = 0;
			}
		}
	}

	/**
	 * @param pending
	 * @param num
	 */
	public abstract void processBlock( short[] pending, int num );

}

