package com.softsynth.javasonics.util;
import java.io.*;

/**
 * Sample that can load itself from a standard WAV format file.
 *
 * @see RIFFParser
 * @see AudioSampleAIFF
 * @author (C) 1997 Phil Burk, SoftSynth.com, All Rights Reserved
 */
public class AudioSampleWAV extends AudioSampleLoader
{
	static final short WAVE_FORMAT_PCM = 1;
	static final short WAVE_FORMAT_ULAW = 7;
	static final short WAVE_FORMAT_IMA_ADPCM = 0x0011;

	static final int WAVE_ID = ('W' << 24) | ('A' << 16) | ('V' << 8) | 'E';
	static final int FMT_ID = ('f' << 24) | ('m' << 16) | ('t' << 8) | ' ';
	static final int DATA_ID = ('d' << 24) | ('a' << 16) | ('t' << 8) | 'a';
	static final int CUE_ID = ('c' << 24) | ('u' << 16) | ('e' << 8) | ' ';
	static final int FACT_ID = ('f' << 24) | ('a' << 16) | ('c' << 8) | 't';
	static final int SMPL_ID = ('s' << 24) | ('m' << 16) | ('p' << 8) | 'l';

	int samplesPerBlock = 0;
	int bytesPerBlock = 0;
	private int numFactSamples = 0;

	/** Create a WAV parser.
	 * @param stream A stream that may come from a file or in memory byte array.
	 * @exception IOException If parsing fails, or IO error occurs.
	 */
	public AudioSampleWAV(InputStream stream) throws IOException
	{
		load(stream);
	}

	public AudioSampleWAV()
	{
		super();
	}

	IFFParser makeParser(InputStream stream)
	{
		return new RIFFParser(stream); // RIFF not IFF
	}

	/* Set Sustain Loop by assuming first two markers are loop points. */
	void setLoopPoints()
	{
		if (cuePoints != null)
		{
			if (cuePoints.size() >= 2)
			{
				setSustainBegin( ((CuePoint) cuePoints.elementAt(0)).getPosition() );
				setSustainEnd( ((CuePoint) cuePoints.elementAt(1)).getPosition() );
			}
		}
	}

	/* Parse various chunks encountered in WAV file. */
	void parseCueChunk(IFFParser parser, int ckSize) throws IOException
	{
		int numCuePoints = parser.readIntLittle();
		if ((ckSize - 4) != (6 * 4 * numCuePoints))
			throw new EOFException("Cue chunk too short!");
		for (int i = 0; i < numCuePoints; i++)
		{
			int uniqueID = parser.readIntLittle(); /* dwName */
			int position = parser.readIntLittle();
			//System.out.println("parseCueChunk: " + i + " : " + position + ", " + uniqueID );
			parser.skip(4 * 4); /* dwName */
			insertSortedCue(new CuePoint(position, uniqueID));
		}
		setLoopPoints();
	}

	void parseFmtChunk(IFFParser parser, int ckSize) throws IOException
	{
		int format = parser.readShortLittle();
		samplesPerFrame = parser.readShortLittle();
		setSampleRate((double) parser.readIntLittle());
		parser.readIntLittle(); /* skip dwAvgBytesPerSec */
		bytesPerBlock = parser.readShortLittle();

		bitsPerSample = parser.readShortLittle();
		bytesPerSample = bytesPerFrame / samplesPerFrame;

		switch (format)
		{
			case WAVE_FORMAT_PCM :
				samplesPerBlock = (8 * bytesPerBlock) / bitsPerSample;
				switch (bitsPerSample)
				{
					case 8 :
						sampleDecoder = new SampleDecoder_Unsigned8();
						break;
					case 16 :
						sampleDecoder = new SampleDecoder_Little16();
						break;
					case 24 :
						sampleDecoder = new SampleDecoder_Little24();
						break;
					default :
						throw new IOException("Only 8 or 16 bit PCM samples supported.");
				}
				break;

			case WAVE_FORMAT_IMA_ADPCM:
				int extendedSize = parser.readShortLittle();
				/* read size of ADPCM extension */
				int framesPerBlock = parser.readShortLittle();
				samplesPerBlock = framesPerBlock * samplesPerFrame;
				sampleDecoder =
					new SampleCodec_IMA_ADPCM_WAV(
						bytesPerBlock,
						samplesPerFrame);
				break;
			
			case WAVE_FORMAT_ULAW:
				samplesPerBlock = (8 * bytesPerBlock) / bitsPerSample;
				sampleDecoder = new com.softsynth.javasonics.util.SampleDecoder_ULaw();
				break;
				
			default :
				throw new IOException(
					"Only WAVE_FORMAT_PCM and WAV_FORMAT_IMA_ADPCM supported, not "
						+ format);
		}
	}

	private int convertByteToFrame( int byteOffset ) throws IOException
	{
		if (bytesPerBlock == 0)
		{
			throw new IOException("WAV file has bytesPerBlock = zero");
		}
		if (samplesPerFrame == 0)
		{
			throw new IOException("WAV file has samplesPerFrame = zero");
		}
		int nFrames =
				(samplesPerBlock * byteOffset)
					/ (samplesPerFrame * bytesPerBlock);
		return nFrames;
	}
	
	private int calculateNumFrames(int numBytes) throws IOException
	{
		int nFrames;
		if (numFactSamples > 0)
		{
			nFrames = numFactSamples / samplesPerFrame;
		} else
		{
			nFrames = convertByteToFrame( numBytes );
		}
		return nFrames;
	}

	// Read fraction in range of 0 to 0xFFFFFFFF and
	// convert to 0.0 to 1.0 range.
	private double readFraction(IFFParser parser) throws IOException
	{
		// Put L at end or we get -1.
		long maxFraction = 0x0FFFFFFFFL;
		// Get unsigned fraction. Have to fit in long.
		long fraction = ((long) parser.readIntLittle()) & maxFraction;
		return (double) fraction / (double) maxFraction;
	}
	
	void parseSmplChunk(IFFParser parser, int ckSize) throws IOException
	{
		
		parser.readIntLittle(); // Manufacturer
		parser.readIntLittle(); // Product
		parser.readIntLittle(); // Sample Period
		int unityNote = parser.readIntLittle();
		double pitchFraction = readFraction( parser );
		setPitch( unityNote + pitchFraction);
		
		parser.readIntLittle(); // SMPTE Format
		parser.readIntLittle(); // SMPTE Offset
		int numLoops = parser.readIntLittle();
		parser.readIntLittle(); // Sampler Data
		
		int lastCueID = Integer.MAX_VALUE;
		for( int i=0; i<numLoops; i++ )
		{
			int cueID = parser.readIntLittle();
			int type = parser.readIntLittle();
			int startByte = parser.readIntLittle();
			int endByte = parser.readIntLittle();
			double endFraction = readFraction( parser );
			int playCount = parser.readIntLittle();

			double startFrame = startByte;
			double endFrame = endByte;

			// According to the WAV standard, we should be treating the file start,end
			// as "byte offsets". But SoundForge and the Mac LoopEditor seem to treat them
			// as actual sample offsets.
			if( false )
			{
				startFrame = convertByteToFrame( startByte );
				endFrame = convertByteToFrame( endByte );
			}
			
			// We add one to make it match what SoundForge shows in the editor versus what is in the file.
			double finalEndFrame = endFrame + endFraction + 1;

			System.out.println( "startByte = " + startByte + ", startFrame = " + startFrame );
			System.out.println( "endByte = " + endByte + ", endFrame = " + endFrame+ ", finalEndFrame = " + finalEndFrame );

			// Use lowest numbered cue.
			if( cueID < lastCueID )
			{
				setSustainBegin( startFrame );
				setSustainEnd( finalEndFrame );
			}
		}
	}
	
	void parseFactChunk(IFFParser parser, int ckSize) throws IOException
	{
		numFactSamples = parser.readIntLittle();
	}

	void parseDataChunk(IFFParser parser, int ckSize) throws IOException
	{
		long numRead;
		dataPosition = parser.getOffset();
		if (ifLoadData)
		{
			byteData = new byte[ckSize];
			numRead = parser.read(byteData);
		} else
		{
			numRead = parser.skip(ckSize);
		}
		if (numRead != ckSize)
		{	throw new EOFException(
				"WAV data chunk too short! Read "
					+ numRead
					+ " instead of "
					+ ckSize);
		}
		numFrames = calculateNumFrames(ckSize);
	}

	public void handleForm(IFFParser parser, int ckID, int ckSize, int type)
		throws IOException
	{
		if ((ckID == RIFFParser.RIFF_ID) && (type != WAVE_ID))
			throw new IOException(
				"Bad WAV form type = " + IFFParser.IDToString(type));
	}

	/** Called by parse() method to handle chunks in a WAV specific manner.
	 * @param ckID four byte chunk ID such as 'data'
	 * @param ckSize size of chunk in bytes
	 * @return number of bytes left in chunk
	*/
	public void handleChunk(IFFParser parser, int ckID, int ckSize)
		throws IOException
	{
		switch (ckID)
		{
			case FMT_ID :
				parseFmtChunk(parser, ckSize);
				break;
			case DATA_ID :
				parseDataChunk(parser, ckSize);
				break;
			case CUE_ID :
				parseCueChunk(parser, ckSize);
				break;
			case FACT_ID :
				parseFactChunk(parser, ckSize);
				break;
			case SMPL_ID :
				parseSmplChunk(parser, ckSize);
				break;
			default :
				break;
		}
	}
	/* (non-Javadoc)
	 * @see com.softsynth.javasonics.util.AudioSampleLoader#isLittleEndian()
	 */
	boolean isLittleEndian()
	{
		return true;
	}
}
