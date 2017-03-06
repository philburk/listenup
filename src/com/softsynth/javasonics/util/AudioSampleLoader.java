package com.softsynth.javasonics.util;
import java.io.*;

/**
 * The AudioSampleLoader class is used to load AudioSamples from a stream.
 *
 * @author (C) 2001 Phil Burk, SoftSynth.com, All Rights Reserved
 */

abstract public class AudioSampleLoader extends AudioSample implements ChunkHandler
{
	protected byte[]  byteData;
	boolean        ifLoadData; /* If true, load sound data into memory. */
	long           dataPosition;  /* Number of bytes from beginning of file where sound data resides. */
	IFFParser      parser;
	protected int  bitsPerSample;
	protected int  bytesPerFrame; // in the file
	protected int  bytesPerSample; // in the file
    SampleDecoder  sampleDecoder = null;

	public static final float FLOAT_CONVERSION_LIMIT = 32768.0f;
	
	public  AudioSampleLoader()
	{
	}

/** Create an AudioSampleLoader object that can load a WAV or AIFF file.
 *  The file is not loaded by this method.
 *  FileName must have a common suffix like ".wav" or ".aiff".
 *  See AudioSample.getFileType(fileName) for the valid suffices.
 */
    public static AudioSampleLoader createSample( String fileName ) throws IOException
    {
		switch( AudioSample.getFileType( fileName ) )
		{
		case AudioSample.AIFF:
			return new AudioSampleAIFF();
		case AudioSample.WAV:
			return new AudioSampleWAV();
		default:
			throw new IOException("Unrecognized sample file suffix on " + fileName );
		}
    }


/** @return Number of bytes from beginning of stream where sound data resides. */
	public long getDataPosition()
	{
		return dataPosition;
	}

	/** Assume stream is positioned within the sample data area.
	 *  You can use:
	 *  <br>
	 *  stream.skip( sample.getDataPosition() );
	 *  <br>
	 *  on a freshly opened stream to seek to the beginning of data.
	 */
	public int read( InputStream stream, short[] shorts, int offset, int numShorts ) throws IOException
	{
        if( sampleDecoder == null ) throw new IOException( "No decoder for this sample format.");
        return sampleDecoder.read( stream, shorts, offset, numShorts );
	}

	/** Assume stream is positioned within the sample data area.
	 *  You can use:
	 *  <br>
	 *  stream.skip( sample.getDataPosition() );
	 *  <br>
	 *  on a freshly opened stream to seek to the beginning of data.
	 */
	public int read( InputStream stream, float[] floats, int offset, int numShorts ) throws IOException
	{
        if( sampleDecoder == null ) throw new IOException( "No decoder for this sample format.");
        return sampleDecoder.read( stream, floats, offset, numShorts );
	}
	
	public void load( InputStream stream  ) throws IOException
	{
		load( stream, true );
	}

	abstract boolean isLittleEndian();
	
/** Load the sample from the given stream.
 *
 * @param stream May be any stream from a file, a URL, a byte array, etc.
 * @param ifLoadData If true, sample data will be loaded into memory.
 */
	public void load( InputStream stream, boolean ifLoadData ) throws IOException
	{
		shortData = loadShorts( stream, ifLoadData );
	}

/** Load the sample data from the given stream and return it in an array of shorts.
 *
 * @param stream May be any stream from a file, a URL, a byte array, etc.
 * @param ifLoadData If true, sample data will be loaded into memory.
 * @exception IOException If parsing fails, or IO error occurs.
 */
	public short[] loadShorts( InputStream stream, boolean ifLoadData ) throws IOException
	{
		byte[] returnData = loadBytes( stream, ifLoadData );
		short[] shortData = null;
		if( ifLoadData && (numFrames > 0) )
		{
			int numShorts = getNumSamples();
			shortData = new short[ numShorts ];
			read( new ByteArrayInputStream( returnData), shortData, 0, numShorts );
		}
		return shortData;
	}

	/** Load the sample data from the given stream and return it in an array of shorts.
	 *
	 * @param stream May be any stream from a file, a URL, a byte array, etc.
	 * @param ifLoadData If true, sample data will be loaded into memory.
	 * @exception IOException If parsing fails, or IO error occurs.
	 */
		public float[] loadFloats( InputStream stream ) throws IOException
		{
			byte[] returnData = loadBytes( stream, true );
			float[] floatData = null;
			if( ifLoadData && (numFrames > 0) )
			{
				int numFloats = getNumSamples();
				floatData = new float[ numFloats ];
				read( new ByteArrayInputStream( returnData), floatData, 0, numFloats );
			}
			return floatData;
		}


/** Load the sample data from the given stream and return it in an array of raw bytes.
 *
 * @param stream May be any stream from a file, a URL, a byte array, etc.
 * @param ifLoadData If true, sample data will be loaded into memory.
 * @exception IOException If parsing fails, or IO error occurs.
 */
	public byte[] loadBytes( InputStream stream, boolean ifLoadData ) throws IOException
	{
		this.ifLoadData = ifLoadData;
		parser = makeParser( stream );
		parser.parse( this );  // parser will call back to handleChunk() and handleForm()
		byte[] returnData = byteData;
		byteData = null;
		parser = null;
		return returnData;
	}
		
//	abstract void convertBytesToShorts( byte[] bytes, short[] shorts, int numShorts );
	abstract IFFParser makeParser( InputStream stream );

/* Convert unsigned bytes to signed shorts.
	void convertUnsignedBytesToShorts( byte[] byteData, short[] shortData, int numShorts )
	{
		for( int i=0; i<numShorts; i++ )
		{
			shortData[i] = (short)((byteData[i] + 0x80) << 8);
		}
	}
*/
/* Convert signed bytes to signed shorts. */
	void convertSignedBytesToShorts( byte[] byteData, short[] shortData, int numShorts )
	{
		for( int i=0; i<numShorts; i++ )
		{
			shortData[i] = (short) (byteData[i] << 8);
		}
	}

/* Convert little endian byte pairs to shorts.
	void convertLittleBytesToShorts( byte[] byteData, short[] shortData, int numShorts )
	{
		for( int i=0; i<numShorts; i++ )
		{
			shortData[i] = (short) ((byteData[i*2] & 0xFF) | (byteData[i*2 + 1] << 8));
		}
	}
*/
/* Convert big endian byte pairs to shorts. */
	void convertBigBytesToShorts( byte[] byteData, short[] shortData, int numShorts )
	{
		for( int i=0; i<numShorts; i++ )
		{
			shortData[i] = (short) ((byteData[i*2 + 1] & 0xFF) | (byteData[i*2] << 8));
		}
	}

/** This can be read by another thread when load()ing a sample to determine
 * how many bytes have been read so far.
 */
	public synchronized long getNumBytesRead()
	{
		IFFParser p = parser; // prevent race
		if( p != null ) return p.getOffset();
		else return 0;
	}

/** This can be read by another thread when load()ing a sample to determine
 * how many bytes need to be read.
 */
	public synchronized long getFileSize()
	{
		IFFParser p = parser; // prevent race
		if( p != null ) return p.getFileSize();
		else return 0;
	}
}
