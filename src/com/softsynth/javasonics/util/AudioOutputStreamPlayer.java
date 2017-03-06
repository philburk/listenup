package com.softsynth.javasonics.util;
import com.softsynth.javasonics.*;

/**
 * Write to an AudioStream in a Thread.
 * Users of this class must write a play() method for the thread
 * that will be called by the run() method of this class.
 * The stream will be started and stopped by the run() method
 * so your play method does not need to.
 * @author Phil Burk (C) 2001 SoftSynth.com
 * @version 0.6
 */

public abstract class AudioOutputStreamPlayer implements Runnable
{
	private SafeThread     thread = null;
	private AudioOutputStream stream;

	public AudioOutputStreamPlayer( AudioOutputStream stream )
	{
		this.stream = stream;
	}

	public AudioOutputStream getStream()
	{
		return stream;
	}

	public void open()  throws DeviceUnavailableException
	{
		stream.open();
	}

/** Open stream associated with this player. */
	public void open( int bufferSizeInFrames )  throws DeviceUnavailableException
	{
		stream.open( bufferSizeInFrames );
	}

	public void close()
	{
		stream.close();
	}

/** Start a new thread that can write to the stream.
 *  Any currently running thread is stopped.
 */
	public void start()
	{
		stop( 1000 );
		thread = new SafeThread( this );
		thread.start();
	}

/** Calls stop(1000)
 */
	public void stop()
	{
        stop( 1000 );
	}
/** Set a flag that tells the background thread to stop.
 *  Then waits up to timeoutMsec until the thread stops.
 */
	public void stop( int timeoutMsec )
	{
        SafeThread myThread;
        synchronized( this )
        {
            myThread = thread;
            thread = null;
        }

		if( myThread != null )
		{
			try
			{
				myThread.requestStop(timeoutMsec);
			} catch( InterruptedException e ) {
			}
		}
	}

/** Start()s the stream and calls the play() method.
 *  Then drain()s and stop()s the stream.
 */
	public void run()
    {
        stream.start();

        play();

        if( SafeThread.getGo() ) stream.drain();

        stream.stop();
        stream.flush();
    }

/** This method is called by the run() method after the stream is started.
 * The stream will be stopped when it returns.
 */
    public abstract void play();

}
