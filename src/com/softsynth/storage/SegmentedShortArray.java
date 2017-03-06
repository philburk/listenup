package com.softsynth.storage;

import java.util.Enumeration;
import java.util.Vector;

/**
 * @author Phil Burk (C) 2004
 */
public class SegmentedShortArray implements VirtualShortArray
{
	private final static int DEFAULT_SEGMENT_SIZE = (16 * 1024);
	int segmentSize = DEFAULT_SEGMENT_SIZE;
	Vector segments;
	int length = 0;

	class ShortSegment
	{
		short[] data;
		int count; // how many have been written.

		ShortSegment()
		{
			this( DEFAULT_SEGMENT_SIZE );
		}
		
		ShortSegment( int size )
		{
			data = new short[ size ];
			count = 0;
		}

		ShortSegment( short[] data )
		{
			this.data = data;
			count = data.length;
		}
		
		int length()
		{
			return data.length;
		}

		int room()
		{
			return data.length - count;
		}

		void append( short[] array, int firstIndex, int numShorts )
		{
			for( int i=0; i<numShorts; i++ )
			{
				data[count++] = array[ i + firstIndex ];
			}
		}

		void read( int readIndex, short[] array, int firstIndex, int numShorts )
		{
			for( int i=0; i<numShorts; i++ )
			{
				array[i + firstIndex ] = data[i + readIndex];
			}
		}
	}

	public SegmentedShortArray()
	{
		segments = new Vector();
	}

	/**
	 * @param length2
	 */
	protected void addToLength( int delta )
	{
		length += delta;
		
	}

	private int roomAtEnd()
	{
		if( segments.size() == 0 )
		{
			return 0;
		}
		else
		{
			ShortSegment seg = (ShortSegment) segments.lastElement();
			return seg.room();
		}
	}

	/** Write shorts to end of internal buffer. */
	public synchronized void write( short[] array, int firstIndex, int numShorts )
	{
		int nextIndex = firstIndex;
		int numLeft = numShorts;
		// Is there room in last segment?
		while( numLeft > 0 )
		{
			if( roomAtEnd() == 0 )
			{
				segments.addElement( new ShortSegment() );
			}
			ShortSegment seg = (ShortSegment) segments.lastElement();
			int numToWrite = (seg.room() < numLeft) ? seg.room() : numLeft;
			seg.append( array, nextIndex, numToWrite );
			numLeft -= numToWrite;
			nextIndex += numToWrite;
		}
		addToLength( numShorts );
	}

	/** Read shorts from internal buffer. */
	public synchronized void read( int readIndex, short[] array, int firstIndex,
			int numShorts )
	{
		int nextReadIndex = readIndex;
		int nextWriteIndex = firstIndex;
		int numLeft = numShorts;
		Enumeration segs = segments.elements();
		int position = 0;
		while( (numLeft > 0) && segs.hasMoreElements() )
		{
			ShortSegment seg = (ShortSegment) segs.nextElement();
			int numAfterReadIndex = seg.count - (nextReadIndex - position);
			// is some of the data in this segment?
			if( numAfterReadIndex > 0 )
			{
				int numToRead = (numAfterReadIndex < numLeft) ? numAfterReadIndex
						: numLeft;
				seg.read( nextReadIndex - position, array, nextWriteIndex,
						numToRead );
				numLeft -= numToRead;
				nextReadIndex += numToRead;
				nextWriteIndex += numToRead;
			}
			position += seg.count;
		}
		if( numLeft > 0 )
		{
			throw new RuntimeException("SegmentedShortArray ran out of segments while reading. numLeft = " + numLeft);
		}
	}

	/*
	 * How many valid samples are in the virtual array?
	 * 
	 * @see com.softsynth.javasonics.recplay.VirtualShortArray#length()
	 */
	public int length()
	{
		return length;
	}

	/*
	 * Remove all samples and 
	 * 
	 * @see com.softsynth.javasonics.recplay.VirtualShortArray#clear()
	 */
	public synchronized void clear()
	{
		segments.removeAllElements();
		length = 0;
	}

	public void flatten()
	{
		// Nothing to do here.
	}
	/**
	 * @return Returns the segmentSize.
	 */
	public int getSegmentSize()
	{
		return segmentSize;
	}
	/**
	 * @param segmentSize The segmentSize to set.
	 */
	public void setSegmentSize( int segmentSize )
	{
		this.segmentSize = segmentSize;
	}
}