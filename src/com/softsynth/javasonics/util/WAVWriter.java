package com.softsynth.javasonics.util;
import java.io.*;

/**
 * Writes complete audio sample array to a stream in WAV format.
 * @author Phil Burk (C) 2001 SoftSynth.com
 * @version 0.6
 */
public class WAVWriter
{
	static final short WAVE_FORMAT_PCM = 1;
	static final short WAVE_FORMAT_ULAW = 7;
	static final short WAVE_FORMAT_IMA_ADPCM = 0x0011;

    public static final int FORMAT_S16 = 0;
    public static final int FORMAT_U8 = 1;
	public static final int FORMAT_IMA_ADPCM = 2;
	public static final int FORMAT_ULAW = 3;

    static final int EXT_FMT_SIZE = 4;
    static final int FACT_CHUNK_SIZE = 12;

	boolean debugFlag = false;
	private int   bytesPerSample;
    private int   bitsPerSample;
    private int   bytesPerBlock;
    private OutputStream  stream;
    private SampleEncoder encoder = null;
    private int format;

/** Open a writer that will write a WAV formatted file to the stream.
 *  The format can be either:<br>
 *  WAVWriter.FORMAT_S16 for signed 16 bit data,<br>
 *  WAVWriter.FORMAT_U8 for unsigned 8 bit data,<br>
 *  WAVWriter.FORMAT_ULAW for ULaw 8 bit data,<br>
 *  WAVWriter.FORMAT_IMA_ADPCM for IMA (DVI) ADPCM 4:1 compressed data.
 */
    public WAVWriter( OutputStream  stream, int format ) throws IOException
    {
        this.stream = stream;
        this.format = format;
    }

/** Write beginning WAV file containing the array of short samples.
 *  @param numSamples number of samples to be included
 *  @param samplesPerFrame one for mono, two for stereo
 *  @param frameRate frames per second in Hz, typically 44100 or 22050, AKA sample rate
 */
	public void writeBeginning( int numSamples,
        int samplesPerFrame, int frameRate ) throws IOException
	{
        if( encoder == null )
        {
            selectEncoder( samplesPerFrame );
        }
    // figure out sizes now so that we don't have to rewind the stream and update sizes
        int dataSize = encoder.calculateSize( numSamples );
		int fileSize = 12 + (8+16) + FACT_CHUNK_SIZE + (8+dataSize); // RIFF + fmt + fact + data
        if( format == FORMAT_IMA_ADPCM )
        {
            fileSize += EXT_FMT_SIZE ;
        }

    // write RIFF header
		writeHeader( fileSize );

		writeFormatChunk( samplesPerFrame, frameRate );
		writeFactChunk( numSamples );

		writeDataChunkBeginning( dataSize );
	}


	/**
	 * @param samples
	 * @param offset
	 * @param numSamples
	 * @throws IOException
	 */
	public void writeMiddle( short[] samples, int offset, int numSamples ) throws IOException
	{
		writeDataChunkMiddle( samples, offset, numSamples );
	}

	/**
	 * @throws IOException
	 * 
	 */
	public void writeEnd() throws IOException
	{
		writeDataChunkEnd();
	}
	
	/** Write a complete WAV file containing the array of short samples.
	 *  @param samples array of 16 bit PCM samples
	 *  @param offset index of first sample in array to be be included
	 *  @param numSamples number of samples to be included
	 *  @param samplesPerFrame one for mono, two for stereo
	 *  @param frameRate frames per second in Hz, typically 44100 or 22050, AKA sample rate
	 */
	public void write( short samples[], int offset, int numSamples,
        int samplesPerFrame, int frameRate ) throws IOException
	{
		writeBeginning( numSamples, samplesPerFrame, frameRate );
		writeMiddle( samples, offset, numSamples );
		writeEnd();
	}

	private void selectEncoder( int samplesPerFrame ) throws IOException
    {
        switch(format)
        {
        case FORMAT_S16:
            encoder = new SampleEncoder_Little16();
            bitsPerSample = 16;
            bytesPerSample = 2;
            bytesPerBlock = samplesPerFrame * 2;
            break;
        case FORMAT_U8:
            encoder = new SampleEncoder_Unsigned8();
            bitsPerSample = 8;
            bytesPerSample = 1;
            bytesPerBlock = samplesPerFrame;
            break;
        case FORMAT_IMA_ADPCM:
            bytesPerBlock = samplesPerFrame * 512;
            encoder = new SampleCodec_IMA_ADPCM_WAV( bytesPerBlock, samplesPerFrame );
            bitsPerSample = 4;
            break;
		case FORMAT_ULAW:
			encoder = new SampleEncoder_ULaw();
			bytesPerBlock = 1;
			bitsPerSample = 8;
			bytesPerSample = 1;
			break;
        default:
            throw new IOException("Only 8 or 16 bit PCM, or ADPCM samples supported.");
        }
    }

    private void write( int b )  throws IOException
    {
        stream.write( b );
    }

/** Write a 32 bit integer to the stream in Little Endian format.
 */
	private void writeIntLittle( int n )  throws IOException
	{
		write( n & 0xFF );
		write( (n>>8) & 0xFF );
		write( (n>>16) & 0xFF );
		write( (n>>24) & 0xFF );
	}

/** Write a 16 bit integer to the stream in Little Endian format.
 */
	private void writeShortLittle( short n )  throws IOException
	{
		write( n & 0xFF );
		write( (n>>8) & 0xFF );
	}

/** Write a 'fact' chunk to the WAV file containing the sample count.
 */
	private void writeFactChunk( int numSamples )  throws IOException
	{
		write('f'); write('a'); write('c'); write('t');
        writeIntLittle( 4 ); // size of chunk
        writeIntLittle( numSamples );
	}

/** Write an 'fmt ' chunk to the WAV file containing the given information.
 */
	private void writeFormatChunk( int samplesPerFrame, int frameRate )  throws IOException
	{
        int framesPerBlock = 0;
        int bytesPerSecond;
		write('f'); write('m'); write('t'); write(' ');
        if( format == FORMAT_IMA_ADPCM )
        {
		    writeIntLittle( 20 ); // size of chunk
            framesPerBlock = ((SampleCodec_IMA_ADPCM_WAV) encoder).calculateFramesPerBlock();
            bytesPerSecond = (frameRate * bytesPerBlock) / framesPerBlock;
		    writeShortLittle( WAVE_FORMAT_IMA_ADPCM );
        }
		else if( format == FORMAT_ULAW )
		{
			writeIntLittle( 18 ); // TODO why is size of chunk 2 bigger than PCM? 
			bytesPerSecond = frameRate * samplesPerFrame * bytesPerSample;
			writeShortLittle( WAVE_FORMAT_ULAW );
		}
		else
		{
			writeIntLittle( 16 ); // size of chunk
			bytesPerSecond = frameRate * samplesPerFrame * bytesPerSample;
			writeShortLittle( WAVE_FORMAT_PCM );
		}
		
		writeShortLittle( (short) samplesPerFrame );
		writeIntLittle( frameRate );
		writeIntLittle( bytesPerSecond ); /* bytes/second */
		writeShortLittle( (short) bytesPerBlock ); /* nBlockAlign */
		writeShortLittle( (short) bitsPerSample ); /* bits per sample */
    // write FMT chunk extension
        if( format == FORMAT_IMA_ADPCM )
        {
            writeShortLittle( (short) 2 );  /* Size in bytes of following data. */
		    writeShortLittle( (short) framesPerBlock );
        }
		else if( format == FORMAT_ULAW )
		{
			writeShortLittle( (short) 0 ); // TODO - what is this?
		}
	}

	private void writeDataChunkBeginning( int dataSize )  throws IOException
	{
		write('d'); write('a'); write('t'); write('a');
		writeIntLittle( dataSize ); // size
	}
	private void writeDataChunkMiddle( short samples[], int offset, int numSamples )  throws IOException
	{
		encoder.write( stream, samples, offset, numSamples );
	}
	private void writeDataChunkEnd()  throws IOException
	{
		encoder.finish( stream );
	}
	

/** Write a 'RIFF' file header and a 'WAVE' ID to the WAV file.
 */
	private void writeHeader( int fileSize )  throws IOException
	{
		write('R'); write('I'); write('F'); write('F');
		writeIntLittle( fileSize - 8 );
		write('W'); write('A'); write('V'); write('E');
	}

/** Write a complete WAV file containing the array of short samples.
 */
	public void write( short samples[], int samplesPerFrame, int frameRate ) throws IOException
	{
        write( samples, 0, samples.length, samplesPerFrame, frameRate );
	}


/*
    public static void main(String args[])
	{
		try
		{
            File outFile = new File("E:\\new.wav");
            FileOutputStream outStream = new FileOutputStream(outFile);

		    //WAVWriter app = new WAVWriter( outStream, WAVWriter.FORMAT_U8 );
		    //WAVWriter app = new WAVWriter( outStream, WAVWriter.FORMAT_S16 );
		    WAVWriter app = new WAVWriter( outStream, WAVWriter.FORMAT_IMA_ADPCM );

            File inFile = new File("E:\\mono_sr22050.raw");
            app.test3(inFile);

            outStream.close();
            System.out.println("Test finished.");
		} catch( IOException e ) {
			System.err.println( e );
		}
		System.exit(0);
	}

	void testFile(File inFile) throws IOException
	{
		FileInputStream inStream = new FileInputStream(inFile);
		byte buf[] = new byte[(int)inFile.length()];
		short samples[] = new short[(int)(inFile.length()/2)];

		inStream.read(buf);
		byteToShort(buf, buf.length, 0, samples);

	    write(samples, 1, 22050);
	}

	public void byteToShort(byte[] byteData, int byteDataLength,
                            int startOffset, short[] shortData)
    {
        int j = startOffset;
        for (int i = 0; i < byteDataLength; i+= 2)
        {
            shortData[j++] = getShortVal(byteData[i], byteData[i+1]);
        }
    }

    short getShortVal(byte lowOrder, byte highOrder)
    {
        return ((short)((lowOrder & 0xff) | (highOrder << 8)));
    }
*/

}
