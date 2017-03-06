package com.softsynth.javasonics.recplay;

import java.io.IOException;

import org.xiph.speex.SpeexEncoder;

import com.softsynth.storage.DynamicBuffer;
import com.softsynth.storage.DynamicBufferFactory;
import com.softsynth.upload.ProgressListener;

/**
 * Recording that encodes Speex audio. Can encode on the fly to save time if not
 * editing.
 * 
 * @author Phil Burk (C) 2004
 */

class DynamicSpeexRecording extends DynamicRecording
{
	public interface StreamChunkListener
	{
		public void gotChunk(DynamicSpeexRecording dynamicSpeexRecording);
	}

	private JSpeexEncoder encoder;
	private double speexQuality = 3.0;
	private int speexComplexity = JSpeexEncoder.DEFAULT_COMPLEXITY;
	public String speexVersion;
	private StreamChunkListener chunkListener;
	private DynamicBuffer dynoBuffer;

	/**
	 * @param i
	 */
	public DynamicSpeexRecording(int maxSamples, boolean editable,
			boolean useFileCache)
	{
		super( maxSamples, editable, useFileCache );
		// Try to grab something from Speex here so we know ASAP if the JAR is
		// missing.
		speexVersion = SpeexEncoder.VERSION;
	}

	private void setupEncoder() throws IOException
	{
		encoder = new JSpeexEncoder();
		encoder.setQualityVBR( speexQuality );
		encoder.setQuality( (int) Math.round( speexQuality ) );
		encoder.setComplexity( speexComplexity );
		encoder.setSampleRate( (int) Math.round( getFrameRate() ) );
		dynoBuffer = DynamicBufferFactory.createDynamicBuffer();
		encoder.setOutputStream( dynoBuffer.getOutputStream() );
	}

	public synchronized void erase()
	{
		super.erase();
		deleteEncoder();
	}

	/**
	 * Destroy any existing encoder, probably because we edited message.
	 */
	protected void deleteEncoder()
	{
		super.deleteEncoder();

		if( encoder != null )
		{
			try
			{
				encoder.close();
				if( dynoBuffer != null )
				{
					// Delete file caches.
					dynoBuffer.clear();
					dynoBuffer = null;
				}
			} catch( IOException e )
			{
				e.printStackTrace();
			}
			encoder = null;
		}
	}
	
	/**
	 * @Overwrite
	 */
	public void flush()
	{
		super.flush();
		if( encoder != null )
		{
			try
			{
				encoder.flush();
			} catch( IOException e )
			{
				throw new RuntimeException( e );
			}
		}
	}

	/** Write to both the recording and the Speex compressor. */
	public void write( short[] samples, int firstIndex, int numSamples )
	{
		boolean atBeginning = (getMaxSamplesPlayable() == 0);
		
		super.write( samples, firstIndex, numSamples );

		try
		{
			if( atBeginning && (encoder == null) )
			{
				setupEncoder();
			}

			if( encoder != null )
			{
				encoder.encode( samples, firstIndex, numSamples );

				if( chunkListener != null )
				{
					chunkListener.gotChunk( this );
				}
			}
		} catch( IOException exc )
		{
			throw new RuntimeException( exc.toString() );
		}
	}

	public DynamicBuffer getDynamicBuffer()
	{
		return dynoBuffer;
	}
	
	// Return the sample data as a Speex formatted byte array.
	public DynamicBuffer getCompressedImage( int waveFormat,
			ProgressListener progressListener ) throws IOException
	{
		if( waveFormat == FORMAT_SPEEX )
		{
			// This can happen if we edited a recording.
			if( encoder == null )
			{
				setupEncoder();

				// Write data in small chunks to avoid allocating huge array.
				int numTotal = getMaxSamplesPlayable();
				int numLeft = numTotal;
				short[] buffer = new short[64];
				int index = 0;
				while( numLeft > 0 )
				{
					int numToMove = (numLeft < buffer.length) ? numLeft
							: buffer.length;
					read( index, buffer, 0, numToMove );
					encoder.encode( buffer, 0, numToMove );
					numLeft -= numToMove;
					index += numToMove;
					if( !progressListener.progressMade( index, numTotal ) )
					{
						break;
					}
				}
			}

			close();
			return dynoBuffer;
		}
		else
		{
			return super.getCompressedImage( waveFormat, progressListener );
		}
	}

	/**
	 * @throws IOException
	 * @overwrite
	 */
	public void close() throws IOException
	{
		encoder.flush();
		encoder.close();
		encoder = null;
	}

	/**
	 * @return Returns the speexQuality.
	 */
	public double getSpeexQuality()
	{
		return speexQuality;
	}

	/**
	 * @param speexQuality
	 *            The speexQuality to set.
	 */
	public void setSpeexQuality( double speexQuality )
	{
		this.speexQuality = speexQuality;
	}

	/**
	 * @return the speexComplexity
	 */
	public int getSpeexComplexity()
	{
		return speexComplexity;
	}

	/**
	 * @param speexComplexity
	 *            the speexComplexity to set
	 */
	public void setSpeexComplexity( int speexComplexity )
	{
		this.speexComplexity = speexComplexity;
	}

	public void setStreamChunkListener( StreamChunkListener chunkListener )
	{
		this.chunkListener = chunkListener;
	}
	
	public StreamChunkListener getStreamChunkListener()
	{
		return chunkListener;
	}
}