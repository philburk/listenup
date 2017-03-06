package com.softsynth.javasonics.util;

public class HeadTailLog
{
	private String[] head;
	private String[] tail;
	private int headCursor;
	private int tailCursor;
	private int wraps;
	
	public HeadTailLog(int headSize, int tailSize)
	{
		head = new String[headSize];
		tail = new String[tailSize];
	}

	public synchronized void add( String line )
	{
		if( headCursor < head.length )
		{
			head[headCursor++] = line;
		}
		else
		{
			tail[tailCursor++] = line;
			if( tailCursor >= tail.length)
			{
				tailCursor = 0;
				wraps += 1;
			}
		}
		
	}

	public int getHeadCount()
	{
		return headCursor;
	}
	public String getHeadLine(int n)
	{
		return head[n];
	}

	public int getTailCount()
	{
		if( wraps == 0 )
		{
			return tailCursor;
		}
		else
		{
			return tail.length;
		}
	}
	
	public synchronized String[] getTail()
	{
		String[] result;
		if( wraps == 0 )
		{
			result = new String[tailCursor];
			System.arraycopy( tail, 0, result, 0, tailCursor );
		}
		else
		{
			result = new String[tail.length];
			System.arraycopy( tail, tailCursor, result, 0, tail.length - tailCursor );
			System.arraycopy( tail, 0, result, tail.length - tailCursor, tailCursor );
		}
		return result;
	}

}
