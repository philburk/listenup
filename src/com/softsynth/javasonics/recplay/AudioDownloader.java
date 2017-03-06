/*
 * Created on Nov 15, 2003
 */
package com.softsynth.javasonics.recplay;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import com.softsynth.javasonics.DeviceUnavailableException;
import com.softsynth.javasonics.error.WebDeveloperRuntimeException;
import com.softsynth.javasonics.util.*;
import com.softsynth.upload.Base64Encoder;

// import com.softsynth.upload.test.SlowNetworkSimulator;

/**
 * Download files asynchronously and notify listener when done.
 * 
 * @author Phil Burk (C) 2003
 */
class AudioDownloader extends BackgroundCommandProcessor
{
	/** Minimum audio in buffer before play when spooling. */
	private final static double MIN_SECONDS_BUFFERED = 3.0;

	/** Used to simulate slow downloads. */
	private long startMSec;
	private int bytesDownloaded;
	// SlowNetworkSimulator slowNetSimulator;

	private String userName = null;
	private String password = null;
	private RecordingFactory recordingFactory;
	private boolean ignoreMissingSample = false;

	protected AudioDownloader(RecordingFactory recordingFactory)
	{
		this.recordingFactory = recordingFactory;
		// slowNetSimulator = new SlowNetworkSimulator();
	}

	/** Add an audio URL to the queue for background downloading. */
	protected void requestDownload( URL url, DownloadListener listener )
	{
		DownloadItem item = new DownloadItem();
		item.url = url;
		item.listener = listener;
		sendCommand( item );
	}

	/**
	 * Determine whether we have enough to start playing and still keep up with
	 * the network download.
	 * 
	 * @param recording
	 * @return true if ready to play
	 */
	private boolean isEnoughToPlay( Recording recording, double startPosition )
	{
		boolean enoughToPlay = false;
		boolean downloadingFastEnough = false;

		double frameRate = recording.getFrameRate();
		if( frameRate > 0.0 )
		{
			// How much have we downloaded so far.
			int numSamplesDownloaded = recording.getMaxSamplesPlayable();

			// Are we downloading faster than we can play it?
			long currentTimeMSec = System.currentTimeMillis();
			double audioTime = numSamplesDownloaded / recording.getFrameRate();
			// TODO fix for stereo
			double downloadTime = (currentTimeMSec - startMSec) * 0.001;
			downloadingFastEnough = audioTime > (2.0 * downloadTime);

			// Do we have enough to start playing?
			enoughToPlay = (audioTime > (startPosition+MIN_SECONDS_BUFFERED) );
			// System.out.print("audioSeconds = " + audioTime);
			// System.out.println(", downloadSeconds = " + downloadTime);
		}
		return (downloadingFastEnough && enoughToPlay);
	}

	private Recording loadRecordingFromIFFStream( int firstByte,
			InputStream pbStream ) throws IOException, SecurityException
	{
		AudioSampleLoader sampleLoader;
		// Is it a 'RIFF' WAV stream?
		if( firstByte == 'R' )
		{
			sampleLoader = new AudioSampleWAV();
		}
		// Or a 'FORM' AIFF stream?
		else if( firstByte == 'F' )
		{
			sampleLoader = new AudioSampleAIFF();
		}
		else
		{
			throw new IOException( "Unrecognized sample file format." );
		}

		// load the entire sample
		sampleLoader.load( pbStream );
		if( sampleLoader.getNumFrames() <= 0 )
		{
			throw new IOException( "Audio sample has zero frames." );
		}
		short[] data = sampleLoader.getShorts();
		if( data == null )
		{
			throw new IOException( "Audio sample data could not be loaded." );
		}
		Recording recording = recordingFactory.createRecording( data );
		recording.setFrameRate( sampleLoader.getSampleRate() );
		if( sampleLoader.getSamplesPerFrame() != 1 )
		{
			throw new IOException(
					"Attempting to play a stereo sample. Only mono samples supported." );
		}
		return recording;
	}

	class DownloadAbortException extends RuntimeException
	{
		DownloadAbortException(String msg)
		{
			super( msg );
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.softsynth.javasonics.recplay.BackgroundCommandProcessor#processItem
	 * (java.lang.Object)
	 */
	public boolean processCommand( Object obj )
	{
		boolean succeeded = false;
		final DownloadItem item = (DownloadItem) obj;

		Logger.println( 0, "start downloading " + item.url );
		// slowNetSimulator.start();
		startMSec = System.currentTimeMillis();
		URLConnection conn = null;
		boolean fileExists = false;
		try
		{
			final Recording reco;

			conn = item.url.openConnection();

			// Request authorization if needed for HTTPS.
			if( (password != null) && (userName != null) )
			{
				// Encode the bytes of the string
				String encoding = Base64Encoder
						.encode( (userName + ":" + password).getBytes() );
				conn.setRequestProperty( "Authorization", "Basic " + encoding );
			}

			// Explicitly make the HTTP request now.
			conn.connect();
			// Check the response code.
			if( conn instanceof HttpURLConnection )
			{
				HttpURLConnection hcon = (HttpURLConnection) conn;
				int responseCode = hcon.getResponseCode();
				Logger.println( 1, "AudioDownloader: responseCode = "
						+ responseCode );
				if( responseCode >= 0 )
				{
					if( responseCode == 404 )
					{
						if( ignoreMissingSample )
						{
							Logger.println( 0, "ignoreMissingSample of " + item.url );
							return false;
						}
						else
						{
							throw new WebDeveloperRuntimeException( "404 - Could not find "
													+ item.url );
						}
					}
					else if ((responseCode < 200) || (responseCode >= 400))
					{
						throw new WebDeveloperRuntimeException( "HTTP Response code = "
							+ responseCode );
					}
				}
			}

			int length = conn.getContentLength();
			Logger.println( 2, "AudioDownloader: contentLength = " + length );
			if( length == 0 )
			{
				throw new WebDeveloperRuntimeException( "Audio file has zero length!" );
			}

			// Get a stream to read from the URL.
			// If the file cannot be found then an exception will get thrown
			// here.
			InputStream stream1 = conn.getInputStream();
			fileExists = true;
			reco = loadRecordingFromInputStream( item,  stream1, item.url.toString() );
			Logger.println( 1, "finish downloading " + item.url );
			succeeded = true;
			item.listener.finished( reco );
		} catch( IOException e )
		{
			// WARNING - Java 1.4 threw a FileNotFoundException but Java 1.6
			// threw an IOException
			// So use this funky flag.
			if( !fileExists && ignoreMissingSample )
			{
				Logger.println( 0, "ignoreMissingSample of " + item.url );
			}
			else
			{
				String explanation = "File does not exist or is corrupted, server is unavailable, or network disconnected.";
				if( conn != null )
				{
					if( conn instanceof HttpURLConnection )
					{
						HttpURLConnection hcon = (HttpURLConnection) conn;
						try
						{
							explanation = "HTTP response = "
									+ hcon.getResponseMessage();
						} catch( IOException e1 )
						{
						}
					}
				}

				String errorMessage = "Cannot load audio from: \n\n    "
						+ item.url + "\n\n" + explanation;
				item.listener.caughtException( errorMessage, e );
				Logger.println( 0, "IO error downloading " + item.url );
				if( fileExists )
				{
					try
					{
						Logger.dumpBytes( item.url, 16 * 256 );
					} catch( IOException e1 )
					{
					}
				}
			}
		} catch( DownloadAbortException e )
		{
			Logger.println( 1, "abort downloading " + item.url );
		} catch( Throwable e )
		{
			item.listener.caughtException( "error downloading " + item.url, e );
			Logger.println( 0, "error downloading " + item.url + ", " + e );
		}
		finally
		{
			if( !succeeded )
			{
				item.listener.failed();
			}
		}
		// slowNetSimulator.stop();
		return false;
	}

	public Recording loadRecordingFromInputStream( final DownloadItem item,
			 InputStream stream1, final String sourceName )
			throws IOException, DeviceUnavailableException
	{
		final Recording reco;
		PushbackInputStream pbStream = new PushbackInputStream( stream1 );

		// Get first byte of stream to determine file type.
		int firstByte = pbStream.read();
		pbStream.unread( firstByte ); // so parsers can see it
		Logger.println( 2, "AudioDownloader: firstByte = " + firstByte );

		// Is it an Ogg Speex stream?
		if( firstByte == 'O' )
		{
			reco = recordingFactory.createRecording();
			// Define a decoder that checks for enough data periodically.
			JSpeexDecoder decoder = new JSpeexDecoder()
			{
				int checkAt = 0;

				public void gotPacket( int numBytes )
				{
					bytesDownloaded += numBytes;
					if( bytesDownloaded > checkAt )
					{
						// slowNetSimulator.simulateNetworkDelay(numBytes);
						checkAt = bytesDownloaded + 256;
						if( (item != null) && !item.reportedReady
								&& isEnoughToPlay( reco, item.listener.getStartPosition() ) )
						{
							try
							{
								item.listener.gotEnoughToPlay( reco );
							} catch( DeviceUnavailableException de )
							{
								item.listener.caughtException( "error downloading " + sourceName, de );
								Logger.println( 0, "error downloading " + sourceName + ", " + de );
							}
							item.reportedReady = true;
						}
					}
					if( isAbortRequested() )
					{
						throw new DownloadAbortException( "Download of "
								+ sourceName + " aborted." );
					}
				}
			};

			// Download and decode file. This could take awhile.
			bytesDownloaded = 0;
			try
			{
				reco.setLocked( true );
				decoder.decode( pbStream, (DynamicRecording) reco );
			} finally
			{
				reco.setLocked( false );
			}
			// Fully downloaded so report ready if not already reported.
			if( (item != null) && !item.reportedReady )
			{
				item.listener.gotEnoughToPlay( reco );
				item.reportedReady = true;
			}
		}
		else
		{
			reco = loadRecordingFromIFFStream( firstByte, pbStream );
			// Fully downloaded so that's obviously enough.
			if( item != null )
			{
				item.listener.gotEnoughToPlay( reco );
			}
		}
		Logger.println( 2, "close input stream" );
		pbStream.close();
		return reco;
	}

	class DownloadItem
	{
		URL url;
		Recording recording;
		DownloadListener listener;
		boolean reportedReady = false;
	}

	/**
	 * @return
	 */
	protected String getPassword()
	{
		return password;
	}

	/**
	 * @return
	 */
	protected String getUserName()
	{
		return userName;
	}

	/**
	 * @param string
	 */
	protected void setPassword( String string )
	{
		password = string;
	}

	/**
	 * @param string
	 */
	protected void setUserName( String string )
	{
		userName = string;
	}

	/**
	 * Lower background of audio downloader because it can spin free and eat up
	 * 100% of the CPU. Under Java 1.5.0_05 and later that causes JavaSound to
	 * glitch. Fixes bug 0035.
	 */
	public void setupBackground()
	{
		int pri = Thread.currentThread().getPriority();
		Logger.println( 1, "AudioDownloader priority started at " + pri );
		if( pri > Thread.MIN_PRIORITY )
		{
			pri -= 1;
			Thread.currentThread().setPriority( pri );
			Logger.println( 1, "AudioDownloader priority lowered to " + pri );
		}
	}

	/**
	 * @return the ignoreMissingSample
	 */
	public boolean isIgnoreMissingDownload()
	{
		return ignoreMissingSample;
	}

	/**
	 * @param ignoreMissingSample
	 *            the ignoreMissingSample to set
	 */
	public void setIgnoreMissingSample( boolean ignoreMissingSample )
	{
		this.ignoreMissingSample = ignoreMissingSample;
	}
}