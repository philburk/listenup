package com.softsynth.javasonics.util;

/**Thread that waits for a timeout then aborts unless cancelled.
 * @author Phil Burk (C) 2004
 */

public abstract class WatchDogTimer extends Thread
{
	boolean cancel = false;
	int msecDelay = 4000;

	public void schedule( int msecDelay )
	{
		this.msecDelay = msecDelay;
		start();
	}
	public void run()
	{
		long startTime = System.currentTimeMillis();
		try
		{
			while( !cancel &&
					((System.currentTimeMillis() - startTime) < msecDelay) )
			{
				sleep( 200 );
			}
			if( !cancel )
			{
				handleTimeout();
			}
		} catch( InterruptedException ex )
		{
		}
	}

	abstract public void handleTimeout();
	
	public void cancel()
	{
		cancel = true;
		// The interrupt method can cause modifyThread exceptions when Applet
		// rapidly started and stopped
		// disable cuz of crash // interrupt();
	}
}
