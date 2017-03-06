package com.softsynth.storage;


/**
 * Uses memory to store data. Moves data when inserting or deleting.
 * 
 * @author Phil Burk (C) 2004
 */
public class EditableSegmentedShortArray extends SegmentedShortArray implements
		EditableShortArray
{

	public synchronized void delete( int index, int numShorts )
	{
		ShortSegment currentSegment = null;
		int currentSegmentPosition = 0;
		int segmentIndex = 0;

		// Scan for segment containing first element to be deleted
		int nextSegmentPosition = 0;
		while( segmentIndex < segments.size() )
		{
			ShortSegment seg = (ShortSegment) segments
					.elementAt( segmentIndex++ );
			int position = nextSegmentPosition;
			nextSegmentPosition += seg.count;
			if( nextSegmentPosition > index )
			{
				currentSegment = seg;
				currentSegmentPosition = position;
				break;
			}
		}

		// Is deletion within one segment?
		if( nextSegmentPosition > (index + numShorts) )
		{
			int fromIndex = index + numShorts - currentSegmentPosition;
			int toIndex = index - currentSegmentPosition;
			int numToMove = currentSegment.count - fromIndex;
			if( numToMove > 0 )
			{
				System.arraycopy( currentSegment.data, fromIndex,
						currentSegment.data, toIndex, numToMove );
			}
			currentSegment.count -= numShorts;
			if( currentSegment.count == 0 )
			{
				segments.removeElement( currentSegment );
			}
		}
		else
		{
			int numLeft = numShorts;

			// Deletion extends past this segment so reduce its count.
			int numFromFirstSegment = nextSegmentPosition - index;
			currentSegment.count -= numFromFirstSegment;
			numLeft -= numFromFirstSegment;
			while( segmentIndex < segments.size() )
			{
				currentSegment = (ShortSegment) segments
						.elementAt( segmentIndex );
				// Is this segment entirely deleted.
				if( currentSegment.count <= numLeft )
				{
					segments.removeElement( currentSegment );
					numLeft -= currentSegment.count;
				}
				else
				{
					break;
				}
			}

			// Move data from last segment down to start of segment.
			if( numLeft > 0 )
			{
				int fromIndex = numLeft;
				int toIndex = 0;
				int numToMove = currentSegment.count - numLeft;
				if( numToMove > 0 )
				{
					System.arraycopy( currentSegment.data, fromIndex,
							currentSegment.data, toIndex, numToMove );
				}
				currentSegment.count -= numLeft;
				if( currentSegment.count == 0 )
				{
					segments.removeElement( currentSegment );
				}
			}

		}
		addToLength( 0 - numShorts );
	}

	/** Insert shorts in middle internal buffer. */
	public synchronized void insert( int writeIndex, short[] array, int firstIndex,
			int numShorts )
	{
		if( writeIndex == length() )
		{
			write( array, firstIndex, numShorts );
		}
		else
		{
			// Find matching segment.
			ShortSegment currentSegment = null;
			int currentSegmentPosition = 0;
			int segmentIndex = 0;

			// Scan for segment containing first element to be split
			int nextSegmentPosition = 0;
			while( segmentIndex < segments.size() )
			{
				ShortSegment seg = (ShortSegment) segments
						.elementAt( segmentIndex++ );
				int position = nextSegmentPosition;
				nextSegmentPosition += seg.count;
				if( nextSegmentPosition >= writeIndex )
				{
					currentSegment = seg;
					currentSegmentPosition = position;
					break;
				}
			}

			// Split segment if needed.
			if( writeIndex > currentSegmentPosition )
			{
				// Copy data past insertion point into new segment.
				int numAfter = nextSegmentPosition - writeIndex;
				ShortSegment afterSeg = new ShortSegment( numAfter );
				segments.insertElementAt( afterSeg, segmentIndex );
				int fromIndex = writeIndex - currentSegmentPosition;
				afterSeg.append( currentSegment.data, writeIndex
						- currentSegmentPosition, numAfter );
				currentSegment.count = fromIndex;
			}
			else
			{
				// Must be pointing at beginning of segment so insert empty segment at current point.
				segmentIndex -= 1;
				currentSegment = new ShortSegment();
				segments.insertElementAt( currentSegment, segmentIndex++ );
			}

			// Write some to first segment.
			int numLeft = numShorts;
			int fromIndex = firstIndex;
			
			// How much room is there in first segment.
			if( currentSegment.room() > 0 )
			{
				int numToWrite = (currentSegment.room() < numLeft) ? currentSegment
						.room()
						: numLeft;
				currentSegment.append( array, fromIndex, numToWrite );
				numLeft -= numToWrite;
				fromIndex += numToWrite;
			}

			// Insert complete segments and write
			// Is there room in last segment?
			while( numLeft > 0 )
			{
				ShortSegment seg = new ShortSegment();
				segments.insertElementAt( seg, segmentIndex++ );
				int numToWrite = (seg.room() < numLeft) ? seg.room() : numLeft;
				seg.append( array, fromIndex, numToWrite );
				numLeft -= numToWrite;
				fromIndex += numToWrite;
			}
			addToLength( numShorts );
		}
	}

}