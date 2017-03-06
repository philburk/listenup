package com.softsynth.javasonics.util;

import java.util.Vector;

import com.softsynth.javasonics.error.ErrorReporter;

/**
 * Background thread that processes command objects sent to its queue.
 * 
 * @author Phil Burk (C) 2004
 */
public class BackgroundCommandProcessor implements Runnable
{

	private Vector queue;
	private SafeThread thread;
	private volatile boolean abortRequested = false;
	private final static Object quitCommand = new Object();
	private int numBeingProcessed = 0;

	/**
	 * 
	 */
	public BackgroundCommandProcessor()
	{
		super();
		queue = new Vector();
	}

	/**
	 * Handle next object in queue.
	 * 
	 * @param item
	 * @return true if thread should exit
	 */
	public boolean processCommand( Object command )
	{
		Runnable runnable = (Runnable) command;
		runnable.run();
		return false;
	}

	public int getQueueDepth()
	{
		// numBeingProcessed is also synchronized with the queue
		synchronized( queue )
		{
			return queue.size() + numBeingProcessed;
		}
	}
	
	public void sendCommand( Object cmd )
	{
		synchronized( queue )
		{
			queue.addElement( cmd );
			queue.notifyAll();
		}
	}

	/**
	 * Abort current command in progress and clear the queue. But do not stop
	 * the thread.
	 */
	public void abortCommands()
	{
		abortRequested = true;
		SafeThread th = thread;
		if( th != null )
		{
			try
			{
				synchronized( th )
				{
					// Interrupt the thread no matter what it is doing.
					th.safeInterrupt();
				}
				waitForAbortToFinish();
			} catch( InterruptedException e )
			{
				Logger
						.println( "BackgroundCommandProcessor abort interrupted." );
			}
			
		}
	}

	private synchronized void waitForAbortToFinish() throws InterruptedException
	{
		// Background thread should wake up and clear abortRequested
		// flag.
		// Bail out after awhile so we don't hang.
		int timeout = 5;
		while( abortRequested && (timeout > 0) )
		{
			Logger
					.println( 2,
							"BackgroundCommandProcessor: abortCommands() waiting" );
			synchronized( queue )
			{
				queue.wait( 1000 );
			}
			timeout -= 1;
		}
		if( timeout <= 0 )
		{
			Logger.println( 0,
					"BackgroundCommandProcessor abort timed out." );
			abortRequested = false;
		}
	}

	public synchronized void start()
	{
		if( thread == null )
		{
			thread = new SafeThread( this, "BackgroundCommandProcessor" );
			thread.start();
		}
	}

	public synchronized void stop()
	{
		if( thread != null )
		{
			Thread temp = thread;
			thread = null;
			if( temp.isAlive() )
			{
				sendCommand( quitCommand );
			}
		}
	}

	public void run()
	{
		numBeingProcessed = 0;
		setupBackground();
		try
		{
			while( SafeThread.getGo() )
			{
				if( waitForNextItem() )
				{
					break;
				}
			}
		} catch( Exception e )
		{
			ErrorReporter.show( "Error processing a command.", e );
		} finally
		{
			Logger.println( 1, "BackgroundCommandProcessor.run() finished." );
			numBeingProcessed = 0;
		}
	}

	/**
	 * 
	 */
	public void setupBackground()
	{
	}

	private boolean waitForNextItem()
	{
		Object currentCommand = null;
		boolean result = false;
		try
		{
			synchronized( queue )
			{
				numBeingProcessed = 0;
				if( queue.size() == 0 )
				{
					queue.wait();
					Logger
							.println( 2,
									"BackgroundCommandProcessor.waitForNextItem() returned from wait()" );
				}
				if( queue.size() > 0 )
				{
					currentCommand = (Object) queue.elementAt( 0 );
					numBeingProcessed = 1;
					queue.removeElementAt( 0 );
				}
			}
			if( !abortRequested )
			{
				// Did we receive a command to stop from the foreground?
				if( currentCommand == quitCommand )
				{
					result = true;
				}
				else if( currentCommand != null )
				{
					result = processCommand( currentCommand );
				}
			}

		} catch( InterruptedException e )
		{
			Logger
					.println( 2,
							"BackgroundCommandProcessor waitForNextItem() interrupted." );
		} finally
		{
			numBeingProcessed = 0;

			if( abortRequested )
			{
				abortRequested = false;
				queue.removeAllElements();
			}

			// Let folks know the queue might be updated.
			synchronized( queue )
			{
				Logger
				.println( 1,
						"BackgroundCommandProcessor waitForNextItem() calling notifyAll()." );
				queue.notifyAll();
			}
		}

		return result;
	}

	/**
	 * @return Returns the abortRequested.
	 */
	public boolean isAbortRequested()
	{
		return abortRequested;
	}

	public boolean isAlive()
	{
		Thread temp = thread;
		if( temp != null )
		{
			return temp.isAlive();
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Used by unit tests to wait until commands have been completed.
	 * 
	 * @param timeOutMSec
	 * @return
	 * @throws InterruptedException
	 */
	public boolean waitUntilComplete( int timeOutMSec )
			throws InterruptedException
	{
		long giveUpAt = timeOutMSec + System.currentTimeMillis();
		synchronized( queue )
		{
			while( (numBeingProcessed > 0) || queue.size() > 0 )
			{
				long timeout = giveUpAt - System.currentTimeMillis();
				if( timeout <= 0 )
				{
					return true;
				}
				queue.wait( timeout );
			}
		}
		return false;
	}
}