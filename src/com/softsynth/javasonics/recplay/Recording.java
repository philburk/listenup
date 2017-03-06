package com.softsynth.javasonics.recplay;

import java.io.IOException;
import java.io.OutputStream;

import com.softsynth.dsp.FloatToShortProcessor;
import com.softsynth.javasonics.util.WAVWriter;
import com.softsynth.storage.DynamicBuffer;
import com.softsynth.storage.DynamicBufferFactory;
import com.softsynth.upload.ProgressListener;

/**
 * Something that acts like a tape recording.
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */
public abstract class Recording extends FloatToShortProcessor
{
	/** Compression formats. */
	public static final int FORMAT_UNKNOWN = -1;
	public static final int FORMAT_S16 = WAVWriter.FORMAT_S16;
	public static final int FORMAT_U8 = WAVWriter.FORMAT_U8;
	public static final int FORMAT_IMA_ADPCM = WAVWriter.FORMAT_IMA_ADPCM;
	public static final int FORMAT_ULAW = WAVWriter.FORMAT_ULAW;
	public static final int FORMAT_SPEEX = 20;
	private int samplesPerFrame;
	protected int maxSamplesRecordable;
	protected double frameRate;
	private boolean locked = false;
	private boolean downloading = false;
	private boolean editable = true;

	private PeakAnalyser peakCache = null;

	public Recording()
	{
		samplesPerFrame = 1;
	}

	/** Delete any previously recorded material. */
	public abstract void erase();

	public abstract void read( int readIndex, short[] samples, int firstIndex,
			int numSamples );

	public abstract void readBackwards( int readIndex, short[] samples,
			int firstIndex, int numSamples );

	public abstract int getMaxSamplesPlayable();

	public void delete( int index, int count )
	{
	}

	public abstract void insert( int writeIndex, short[] samples,
			int firstIndex, int numSamples );

	public void insertPeaks( int writeIndex, int numSamples )
	{
		if( peakCache != null )
		{
			peakCache.insert( writeIndex, numSamples );
		}
	}

	public double sampleIndexToTime( int index )
	{
		return index / (getFrameRate() * getSamplesPerFrame());
	}

	public int timeToSampleIndex( double time )
	{
		return (int) (time * getFrameRate() * getSamplesPerFrame());
	}

	int findPreviousZeroCrossing( int startAt, int maxSearch )
	{
		int pos = startAt;
		int numToRead = (startAt < maxSearch) ? startAt : maxSearch;
		if( numToRead == 0 )
			return startAt;
		short[] buf = new short[numToRead];
		read( startAt - numToRead, buf, 0, numToRead );
		int idx = numToRead - 1;
		int after = buf[idx--];
		int before;
		while( idx >= 0 )
		{
			before = buf[idx];
			// System.out.println("idx = " + idx + ", before = " + before + ",
			// after = " + after );
			if( (before <= 0) && (after >= 0) )
			{
				pos = (startAt - numToRead) + idx + 1;
				break;
			}
			after = before;
			idx--;
		}
		return pos;
	}

	public double getMaxPlayableTime()
	{
		return sampleIndexToTime( getMaxSamplesPlayable() );
	}

	public double getMaxRecordableTime()
	{
		return sampleIndexToTime( getMaxSamplesRecordable() );
	}

	public void setFrameRate( double frameRate )
	{
		this.frameRate = frameRate;
	}

	public double getFrameRate()
	{
		return frameRate;
	}

	public void setSamplesPerFrame( int samplesPerFrame )
	{
		this.samplesPerFrame = samplesPerFrame;
	}

	public int getSamplesPerFrame()
	{
		return samplesPerFrame;
	}

	public int getMaxSamplesRecordable()
	{
		return maxSamplesRecordable;
	}

	/**
	 * Measure peak values of a recording based on the number of bins.
	 * 
	 * @return samples measured
	 */
	public synchronized PeakMeasurements measurePeaks( int width )
	{
		if( peakCache == null )
		{
			peakCache = new PeakCache( this );
		}
		return peakCache.measurePeaks( width );
	}

	/** Return the sample data as an Input Stream.
	 * @param progressListener may be null
	 */
	public DynamicBuffer getCompressedImage( int waveFormat,
			ProgressListener progressListener ) throws IOException
	{
		// Map Recorder formats to JavaSonics WAV formats.
		int jsFormat = -1;
		switch( waveFormat )
		{
		case FORMAT_S16:
			jsFormat = WAVWriter.FORMAT_S16;
			break;
		case FORMAT_U8:
			jsFormat = WAVWriter.FORMAT_U8;
			break;
		case FORMAT_IMA_ADPCM:
			jsFormat = WAVWriter.FORMAT_IMA_ADPCM;
			break;
		case FORMAT_ULAW:
			jsFormat = WAVWriter.FORMAT_ULAW;
			break;
		default:
			throw new IOException( "Unrecognized WAV format = " + waveFormat );
		}
		DynamicBuffer dynoBuffer = DynamicBufferFactory.createDynamicBuffer();
		OutputStream stream = dynoBuffer.getOutputStream();
		WAVWriter writer = new WAVWriter( stream, jsFormat );

		// Write data in small chunks to avoid allocating huge array.
		int numTotal = getMaxSamplesPlayable();
		int numLeft = numTotal;
		short[] buffer = new short[64];
		writer.writeBeginning( numLeft, samplesPerFrame, (int) getFrameRate() );
		int index = 0;
		while( numLeft > 0 )
		{
			int numToMove = (numLeft < buffer.length) ? numLeft : buffer.length;
			read( index, buffer, 0, numToMove );
			writer.writeMiddle( buffer, 0, numToMove );
			numLeft -= numToMove;
			index += numToMove;
			if( progressListener != null )
			{
				if( !progressListener.progressMade( index, numTotal ) )
				{
					break;
				}
			}
		}
		writer.writeEnd();
		return dynoBuffer;
	}

	/**
	 * @return
	 */
	public boolean isLocked()
	{
		return locked;
	}

	/**
	 * @param lockit
	 */
	public void setLocked( boolean lockit )
	{
		locked = lockit;
	}

	/**
	 * @return
	 */
	public boolean isDownloading()
	{
		return downloading;
	}

	/**
	 * @param b
	 */
	public void setDownloading( boolean b )
	{
		downloading = b;
	}

	/**
	 * @return Returns the editable.
	 */
	public boolean isEditable()
	{
		return editable;
	}

	/**
	 * @param editable
	 *            The editable to set.
	 */
	public void setEditable( boolean editable )
	{
		this.editable = editable;

	}

	/** Clean up any temporary structures created during the editing process. */
	public void finalizeEdits()
	{
	}

	public void close() throws IOException
	{
	}

	public double correlateSine( double frequency, double startTime,
			double endTime )
	{
		double magnitude = 0.0;
		int startIndex = timeToSampleIndex( startTime );
		int endIndex = timeToSampleIndex( endTime );
		int numTotal = getMaxSamplesPlayable();
		if( (startIndex < 0) || (startIndex >= endIndex) )
		{
			throw new IllegalArgumentException("correlateSine: startTime out of range");
		}
		if( (endIndex < 0) || (endIndex >= numTotal) )
		{
			throw new IllegalArgumentException("correlateSine: endTime out of range");
		}
		int numLeft = endIndex - startIndex;
		short[] buffer = new short[64];
		int index = startIndex;
		double phase = 0.0;
		double phaseIncrement = 2.0 * Math.PI * frequency / getFrameRate();
		double sinAccumulator = 0.0;
		double cosAccumulator = 0.0;
		while( numLeft > 0 )
		{
			int numToMeasure = (numLeft < buffer.length) ? numLeft : buffer.length;
			read( index, buffer, 0, numToMeasure );
			for( int i=0; i<numToMeasure; i++ )
			{
				double sample = (double) buffer[i];
				sinAccumulator += sample * Math.sin( phase );
				cosAccumulator += sample * Math.cos( phase );
				phase += phaseIncrement;
			}
			numLeft -= numToMeasure;
			index += numToMeasure;
			phase = phase % (Math.PI * 2.0);
		}
		double divisor = 32768.0 * (endIndex - startIndex);
		sinAccumulator = sinAccumulator / divisor; 
		cosAccumulator = cosAccumulator / divisor;
		// TODO Why do I have to multiply by 2.0? Need it to make result come out right.
		magnitude = 2.0 * Math.sqrt( (sinAccumulator * sinAccumulator) + (cosAccumulator * cosAccumulator ));
		return magnitude;
	}
}