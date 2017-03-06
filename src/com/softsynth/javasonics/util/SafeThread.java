package com.softsynth.javasonics.util;

/**
 * Thread that has a safe way to stop execution. One should never call stop() on
 * a thread because it can cause internal errors in Java. Instead you should
 * call the requestStop() method which gently requests that the thread should
 * stop.
 * 
 * @author Phil Burk (C) 2001 SoftSynth.com
 * @version 0.6
 */

public class SafeThread extends Thread
{
	private boolean go = true;

	public SafeThread(Runnable obj)
	{
		super( obj );
	}

	public SafeThread(Runnable obj, String name)
	{
		super( obj, name );
	}

	/**
	 * Check a variable specific to the calling thread that is used to enable
	 * thread execution. This method should only be called when executing under
	 * this thread. When this method returns false then you should return from
	 * the run() method.
	 * 
	 * @throws ClassCastException
	 *             if called from a thread that is not a subclass of SafeThread.
	 */
	public static boolean getGo()
	{
		return ((SafeThread) Thread.currentThread()).go;
	}

	/**
	 * Set the go flag to false so that the thread will die.
	 */
	public void clearGo()
	{
		go = false;
	}

	/**
	 * Interrupt()s the thread safely.
	 */
	public void safeInterrupt()
	{
		// Netscape sometimes throws a SecurityException when interrupt() is
		// called!
		try
		{
			interrupt();
		} catch( SecurityException exc )
		{
			System.out.println( "SafeThread: " + exc );
		}
	}
	
	/**
	 * Set the flag returned by getGo() to false. Interrupt()s the thread.
	 */
	public void requestStop()
	{
		go = false; // tell thread to exit
		safeInterrupt();
	}

	/**
	 * Set the flag returned by getGo() to false. Interrupt()s the thread. Then
	 * waits up to timeoutMsec milliseconds for the thread to die.
	 */
	public void requestStop( int timeoutMsec ) throws InterruptedException
	{
		requestStop();
		join( timeoutMsec );
	}
}
