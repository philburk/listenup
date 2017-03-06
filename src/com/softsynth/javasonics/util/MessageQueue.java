package com.softsynth.javasonics.util;

import java.util.Vector;

/**
 * Queue for message objects. This can be written to by multiple threads and
 * should be read by a single thread.
 * 
 * @author Phil Burk (C) 2004
 */
public class MessageQueue
{
	private Vector queue = new Vector();

	/**
	 * Put a message in the queue. May be called by multiple threads.
	 * 
	 * @param msg
	 */
	public synchronized void send( Object msg )
	{
		queue.addElement( msg );
		notifyAll();
	}

	/** Check for message but leave it in queue to avoid a race with waitUntilEmpty().
	 * @return
	 */
	public synchronized Object peekMessage()
	{
		if( !isMessageAvailable() )
		{
			return null;
		}
		else
		{
			return queue.firstElement();
		}
	}
	
	public  synchronized Object popMessage()
	{
		if( !isMessageAvailable() )
		{
			return null;
		}
		else
		{
			Object msg = queue.firstElement();
			queue.removeElementAt( 0 );
			notifyAll();
			return msg;
		}
	}

	/**
	 * Wait until the message queue is empty or times out.
	 * 
	 * @return true if timed out
	 * @throws InterruptedException
	 */
	public synchronized boolean waitUntilEmpty( int timeOutMSec )
			throws InterruptedException
	{
		long timeToGiveUp = System.currentTimeMillis() + timeOutMSec;

		while( isMessageAvailable() && (timeOutMSec > 0) )
		{
			wait( timeOutMSec );
			timeOutMSec = (int) (timeToGiveUp - System.currentTimeMillis());
		}
		return isMessageAvailable();
	}

	/**
	 * Warning, if another thread is reading the queue then the result of this
	 * may not be true by the time you use it.
	 */
	public boolean isMessageAvailable()
	{
		return (queue.size() > 0);
	}

}