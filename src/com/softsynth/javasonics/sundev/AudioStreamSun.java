package com.softsynth.javasonics.sundev;

import com.softsynth.javasonics.*;
import com.softsynth.javasonics.util.WatchDogTimer;

import javax.sound.sampled.*;

import java.awt.*;
import java.awt.event.*;

/**
 * Implement AudioOutputStream using SUN's JavaSound
 * 
 * @author Phil Burk (C) 2001 SoftSynth.com
 * @version 0.1
 */

abstract class AudioStreamSun extends AudioStreamBase
{
	static final int FRAMES_PER_BUFFER = 256; // original size
	// static final int FRAMES_PER_BUFFER = 4096; // more stable

	static final int AVAILABILITY_GAP = 1024 * 2; // always leave this much
	byte bytes[];
	DataLine line;
	int bitsPerSample;
	protected int bytesPerSample;
	protected int bytesPerFrame;
	protected int sampleRate;
	AudioFormat format;
	// Set avoidBlockingIO true as a workaround for this Sun bug:
	// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6261423
	// It caused short audible dropouts in the sound.
	protected static boolean avoidBlockingIO = true;

	static
	{
		// Turn off workaround for non-Sun vendors
		String vendor = System.getProperty( "java.vendor" );
		// System.out.println( "Stream Vendor = " + vendor );
		avoidBlockingIO &= (vendor.indexOf( "Sun" ) == 0);
		// System.out.println( "avoidBlockingIO = " + avoidBlockingIO );
	}

	protected AudioStreamSun(DataLine line, AudioFormat format,
			int samplesPerFrame, int bitsPerSample)
	{
		super( samplesPerFrame );
		this.format = format;
		sampleRate = (int) format.getFrameRate();
		bytes = new byte[FRAMES_PER_BUFFER * samplesPerFrame
				* ((bitsPerSample + 7) / 8)];
		// System.out.println("JavaSound record buffer is " + bytes.length +
		// " bytes.");
		// allocate enough bytes for buffer
		this.line = line;
		this.bitsPerSample = bitsPerSample;
		bytesPerSample = (bitsPerSample + 7) / 8;
		bytesPerFrame = bytesPerSample * samplesPerFrame;

		/*
		 * // print controls supported by this line
		 * System.out.println("----------- line = " + line ); Control ctrls[] =
		 * line.getControls(); for( int i=0; i<ctrls.length; i++ )
		 * System.out.println("Control = " + ctrls[i] ); /*
		 */
	}

	public float getSampleRate()
	{
		return sampleRate;
	}
	
	/** Get size of audio FIFO buffer in frames. */
	public int getBufferSizeInFrames()
	{
		return line.getBufferSize() / (getSamplesPerFrame() * bytesPerSample);
	}

	public abstract void open( int bufferSizeInFrames )
			throws DeviceUnavailableException;

	public void start()
	{
		line.start();
	}

	public void stop()
	{
		line.stop();
	}

	public void drain()
	{
		line.drain();
	}

	public void flush()
	{
		line.flush();
	}

	/**
	 * JavaSound in JDK 1.4.1 on WinXP has a bug that can cause close() to hang!
	 * Report this to the user so they can fix the problem.
	 */
	private void reportCloseHang()
	{
		final Frame frame = new Frame( "Message from JavaSonics" );
		frame.setSize( 400, 250 );
		frame.setLocation( 50, 50 );
		frame.addWindowListener( new WindowAdapter()
		{
			public void windowClosing( WindowEvent e )
			{
				frame.hide();
			}
		} );
		TextArea textArea = new TextArea();
		frame.setLayout( new BorderLayout() );
		frame.add( "Center", textArea );
		textArea.append( "Attempt to close audio stream failed.\n" );
		textArea.append( "This probably due to JavaSound bug #4635534.\n" );
		textArea.append( "The close() method may hang on WinXP or Win2K\n" );
		textArea.append( "Please upgrade your Sun Java plugin.\n" );
		textArea.append( "For more information, please visit:\n\n" );
		textArea.append( "   http://www.javasonics.com/support/hang.html\n\n" );
		textArea
				.append( "You can copy and paste that link into your browser.\n" );
		frame.show();
	}

	public void close()
	{
		if( line.isOpen() )
		{
			/*
			 * Attempt to prevent hanging bug in JavaSound.
			 * http://developer.java
			 * .sun.com/developer/bugParade/bugs/4383457.html
			 */
			if( line.isActive() )
			{
				line.flush();
				line.stop();
			}

			try
			{
				// TODO - Maybe remove this some day. Letting Sun javaSound line
				// settle.
				Thread.sleep( 100 );
			} catch( Exception e )
			{
			}

			// Thanks to Knute Johnson for this timeout trick.
			// Schedule bug alert. Cancel if close does not hang.
			WatchDogTimer watchdog = new WatchDogTimer()
			{
				public void handleTimeout()
				{
					reportCloseHang();
				}
			};
			// System.out.println("Setting watchdog in AudioStreamSun.");
			watchdog.schedule( 4000 );

			if( false )
			{
				try
				{
					System.out.println( "SIMULATE HANG!!!" );
					Thread.sleep( 10000 );
				} catch( Exception e )
				{
				}
			}

			line.close(); // may hang!

			watchdog.cancel();
		}
	}

	public int availableSamples()
	{
		if( !isOpen() )
			return 0;
		return line.available() / bytesPerSample;
	}

	protected void sleepUntilAvailable( int numBytesNeeded )
	{
		int timeoutMsec = 2000;
		int bytesAvailable = line.available() - AVAILABILITY_GAP;
		// System.out.println("bytesAvailable = " + bytesAvailable +
		// ", numBytesNeeded = " + numBytesNeeded);
		while( bytesAvailable < numBytesNeeded )
		{
			if( timeoutMsec <= 0 )
			{
				throw new DeviceTimeoutException(
						"Timeout waiting for audio data! Mic unplugged?" );
			}
			int bytesToWaitFor = numBytesNeeded - bytesAvailable;
			int samplesToWaitFor = bytesToWaitFor / bytesPerFrame;
			int msecToWait = samplesToWaitFor * 1000 / sampleRate;
			if( msecToWait < 1 )
			{
				msecToWait = 1;
			}
			
			// sleep a little over to avoid short sleeps thrashing the scheduler
			msecToWait += (msecToWait >> 2);
			
			try
			{
				// System.out.println("Sleeping for " + msecToWait );
				Thread.sleep( msecToWait );
				timeoutMsec -= msecToWait;
			} catch( InterruptedException e )
			{
				throw new RuntimeException( "Sleep interrupted." );
			}
			bytesAvailable = line.available() - AVAILABILITY_GAP;
		}
	}

	public int getFramePosition()
	{
		return line.getFramePosition();
	}

	/**
	 * @return
	 */
	public AudioFormat getFormat()
	{
		return format;
	}

}