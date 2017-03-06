package com.softsynth.javasonics.util;

import java.io.*;

/**
 * AudioSample that can load itself from a standard AIFF format file stream.
 * 
 * @see IFFParser
 * @see AudioSampleWAV
 * @author (C) 1997 Phil Burk, SoftSynth.com, All Rights Reserved
 */

public class AudioSampleAIFF extends AudioSampleLoader
{
	static final int AIFF_ID = ('A' << 24) | ('I' << 16) | ('F' << 8) | 'F';
	static final int AIFC_ID = ('A' << 24) | ('I' << 16) | ('F' << 8) | 'C';
	static final int COMM_ID = ('C' << 24) | ('O' << 16) | ('M' << 8) | 'M';
	static final int SSND_ID = ('S' << 24) | ('S' << 16) | ('N' << 8) | 'D';
	static final int MARK_ID = ('M' << 24) | ('A' << 16) | ('R' << 8) | 'K';
	static final int INST_ID = ('I' << 24) | ('N' << 16) | ('S' << 8) | 'T';
	static final int NONE_ID = ('N' << 24) | ('O' << 16) | ('N' << 8) | 'E';

	int sustainBeginID = -1;
	int sustainEndID = -1;
	int releaseBeginID = -1;
	int releaseEndID = -1;

	boolean gotINST = false;
	boolean gotMARK = false;

	/**
	 * Create a sample by loading it from an AIFF file..
	 * 
	 * @param stream
	 *            A stream that may come from a file or in memory byte array.
	 * @exception IOException
	 *                If parsing fails, or IO error occurs.
	 */
	public AudioSampleAIFF(InputStream stream) throws IOException
	{
		load( stream );
	}

	public AudioSampleAIFF()
	{
		super();
	}

	IFFParser makeParser( InputStream stream )
	{
		return new IFFParser( stream );
	}

	double read80BitFloat() throws IOException
	{
		/*
		 * This is not a full decoding of the 80 bit number but it should
		 * suffice for the range we expect.
		 */
		byte[] bytes = new byte[10];
		parser.read( bytes );
		int exp = ((bytes[0] & 0x3F) << 8) | (bytes[1] & 0xFF);
		int mant = ((bytes[2] & 0xFF) << 16) | ((bytes[3] & 0xFF) << 8)
				| (bytes[4] & 0xFF);
		// System.out.println( "exp = " + exp + ", mant = " + mant );
		return mant / (double) (1 << (22 - exp));
	}

	void parseCOMMChunk( IFFParser parser, int ckSize ) throws IOException
	{
		samplesPerFrame = parser.readShortBig();
		numFrames = parser.readIntBig();

		bitsPerSample = parser.readShortBig();
		switch( bitsPerSample )
		{
		case 8:
			sampleDecoder = new SampleDecoder_Signed8();
			break;
		case 16:
			sampleDecoder = new SampleDecoder_Big16();
			break;
		case 24:
			sampleDecoder = new SampleDecoder_Big24();
			break;
		default:
			throw new IOException(
					"Only 8, 16 or 24 bit PCM samples supported." );
		}

		bytesPerSample = (bitsPerSample + 7) / 8;
		bytesPerFrame = bytesPerSample * samplesPerFrame;

		setSampleRate( read80BitFloat() );
		if( ckSize > 18 )
		{
			int format = parser.readIntBig();
			if( format != NONE_ID )
			{
				throw new IOException( "Compression not supported, format "
						+ IFFParser.IDToString( format ) );
			}
		}
	}

	/* parse tuning and multi-sample info */
	void parseINSTChunk( IFFParser parser, int ckSize ) throws IOException
	{
		int baseNote = parser.readByte();
		int detune = parser.readByte();
		double pitch = baseNote + (0.01 * detune);
		setPitch( pitch );
		
		int lowNote = parser.readByte();
		int highNote = parser.readByte();
		
		parser.skip( 2 ); /* lo,hi velocity */
		int gain = parser.readShortBig();

		int playMode = parser.readShortBig(); /* sustain */
		sustainBeginID = parser.readShortBig();
		sustainEndID = parser.readShortBig();

		playMode = parser.readShortBig(); /* release */
		releaseBeginID = parser.readShortBig();
		releaseEndID = parser.readShortBig();

		gotINST = true;
		setLoops();
	}

	private void setLoops()
	{
		// Wait until we have parsed both INST and MARK chunks so we have all
		// the info we need.
		if( gotINST && gotMARK )
		{
			int beginFrame = findCuePosition( sustainBeginID );
			setSustainBegin( beginFrame );
			int endFrame = findCuePosition( sustainEndID );
			setSustainEnd( endFrame );

			beginFrame = findCuePosition( releaseBeginID );
			setReleaseBegin( beginFrame );
			endFrame = findCuePosition( releaseEndID );
			setReleaseEnd( endFrame );
		}
	}

	void parseSSNDChunk( IFFParser parser, int ckSize ) throws IOException
	{
		long numRead;
		// System.out.println("parseSSNDChunk()");
		int offset = parser.readIntBig();
		parser.readIntBig(); /* blocksize */
		parser.skip( offset );
		dataPosition = parser.getOffset();
		int numBytes = ckSize - 8 - offset;
		if( ifLoadData )
		{
			byteData = new byte[numBytes];
			numRead = parser.read( byteData );
		}
		else
		{
			numRead = parser.skip( numBytes );
		}
		if( numRead != numBytes )
			throw new EOFException( "AIFF data chunk too short!" );
	}

	void parseMARKChunk( IFFParser parser, int ckSize ) throws IOException
	{
		long startOffset = parser.getOffset();
		int numCuePoints = parser.readShortBig();
		//System.out.println( "parseCueChunk: numCuePoints = " + numCuePoints );
		for( int i = 0; i < numCuePoints; i++ )
		{
			// Some AIF files have a bogus numCuePoints so check to see if we
			// are at end.
			long numInMark = parser.getOffset() - startOffset;
			if( numInMark >= (long) ckSize )
			{
				System.out
						.println( "Reached end of MARK chunk with bogus numCuePoints = "
								+ numCuePoints );
				break;
			}
			
			int uniqueID = parser.readShortBig();
			int position = parser.readIntBig();
			int len = parser.read();
			if( (len & 1) == 0 )
				len++; /* pad so len plus chars is even */
			// System.out.println( "parseCueChunk: " + i + " : " + position + ", " + uniqueID );
			parser.skip( len );
			insertSortedCue( new CuePoint( position, uniqueID ) );
		}

		gotMARK = true;
		setLoops();
	}

	/**
	 * Called by parse() method to handle FORM chunks in an AIFF specific
	 * manner.
	 * 
	 * @param ckID
	 *            four byte chunk ID such as 'data'
	 * @param ckSize
	 *            size of chunk in bytes
	 * @exception IOException
	 *                If parsing fails, or IO error occurs.
	 */
	public void handleForm( IFFParser parser, int ckID, int ckSize, int type )
			throws IOException
	{
		if( (ckID == IFFParser.FORM_ID) && (type != AIFF_ID)
				&& (type != AIFC_ID) )
			throw new IOException( "Bad AIFF form type = "
					+ IFFParser.IDToString( type ) );
	}

	/**
	 * Called by parse() method to handle chunks in an AIFF specific manner.
	 * 
	 * @param ckID
	 *            four byte chunk ID such as 'data'
	 * @param ckSize
	 *            size of chunk in bytes
	 * @exception IOException
	 *                If parsing fails, or IO error occurs.
	 */
	public void handleChunk( IFFParser parser, int ckID, int ckSize )
			throws IOException
	{
		switch( ckID )
		{
		case COMM_ID:
			parseCOMMChunk( parser, ckSize );
			break;
		case SSND_ID:
			parseSSNDChunk( parser, ckSize );
			break;
		case MARK_ID:
			parseMARKChunk( parser, ckSize );
			break;
		case INST_ID:
			parseINSTChunk( parser, ckSize );
			break;
		default:
			break;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.javasonics.util.AudioSampleLoader#isLittleEndian()
	 */
	boolean isLittleEndian()
	{
		return false;
	}

}
