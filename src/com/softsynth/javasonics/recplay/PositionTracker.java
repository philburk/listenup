package com.softsynth.javasonics.recplay;

import java.util.Observable;

import com.softsynth.javasonics.util.SafeThread;

/**
 * Provide position information based on time stamped positions.
 * 
 * A startable and stoppable timer that periodically updates observers. This is
 * similar to the Timer class in JDK 1.3 except it will work in JDK 1.1.
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */
public class PositionTracker extends Observable implements Runnable
{
	private SafeThread thread;
	private long startTimeMSec;
	private int intervalMSec = 100;
	private TimeStampedAudioBufferList timeStampedAudioBufferList;
	private double positionWhenStopped = 0.0;

	public PositionTracker()
	{
		timeStampedAudioBufferList = new TimeStampedAudioBufferList();
	}

	/** @return frame position based on interpolating time stamps */
	public void setPosition( double pos )
	{
		positionWhenStopped = pos;
	}

	/** @return frame position based on interpolating time stamps */
	public synchronized double getCurrentPosition()
	{
		if( thread == null )
		{
			return positionWhenStopped;
		}
		else
		{
			long currTime = System.currentTimeMillis();
			double time = (currTime - startTimeMSec) * 0.001;
			// System.out.println("PositionTracker.getCurrentPosition(), time="
			// + time);
			double interpolatedFrameLocation = timeStampedAudioBufferList
					.timeToFrame( time );
			return interpolatedFrameLocation;
		}
	}

	/**
	 * Set time between notifications. Default is 100 msec.
	 */
	public void setInterval( int msec )
	{
		intervalMSec = msec;
	}

	public int getInterval()
	{
		return intervalMSec;
	}

	public synchronized void start()
	{
		// System.out.println("PositionTracker start!!!");
		startTimeMSec = System.currentTimeMillis();
		if( thread != null )
		{
			stop();
		}
		timeStampedAudioBufferList.clear();
		thread = new SafeThread( this, "PositionTracker" );
		thread.start();
	}

	/**
	 * Stop thread safely using SUN approved techniques.
	 */
	public synchronized void stop()
	{
		if( thread != null )
		{
			try
			{
				thread.requestStop( intervalMSec * 2 ); // wait for it to quit
			} catch( InterruptedException e )
			{
			}
			thread = null;
		}
	}

	public void run()
	{
		while( SafeThread.getGo() )
		{
			setChanged();
			notifyObservers();
			try
			{
				Thread.sleep( intervalMSec );
			} catch( InterruptedException e )
			{
			}
		}
	}

	/**
	 * @param i
	 * @param numSamples
	 * @param timeStamp
	 */
	protected void addTimeStamp( int position, double timeStamp )
	{
		timeStampedAudioBufferList.add( new AudioBufferTimeStamp( position,
				timeStamp ) );
	}

}