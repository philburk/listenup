package com.softsynth.javasonics.recplay;

/** Convert a Speex file to a Wav file.
 * This is intended for use on the server.
 */
import java.io.*;

import com.softsynth.javasonics.util.WAVWriter;

public class SpeexToWAV
{
	public SpeexToWAV()
	{
		super();
	}

	/**
	 * Read a Speex stream, decode it and write to a WAV file image stream.
	 * @param inStream Speex input stream
	 * @param outStream WAV output stream
	 * @param format WAVWriter.FORMAT_S16, FORMAT_U8, FORMAT_IMA_ADPCM or FORMAT_ULAW
	 * @throws IOException
	 */
	public void convertSpeexToWAV( InputStream inStream, OutputStream outStream, int format ) throws IOException
	{
		DynamicRecording recording = new DynamicRecording( Integer.MAX_VALUE, false, false );
		JSpeexDecoder decoder = new JSpeexDecoder();
		decoder.decode( inStream, recording );
		System.out.println("Decoding complete.");

        WAVWriter writer = new WAVWriter(outStream, format );
         // Write data in small chunks to avoid allocating huge array.
        int numTotal = recording.getMaxSamplesPlayable();
        int numLeft = numTotal;
        short[] buffer = new short[64];
        int SAMPLES_PER_FRAME = 1;
        writer.writeBeginning( numLeft, SAMPLES_PER_FRAME, (int) recording.getFrameRate() );
        int index = 0;
        while( numLeft > 0 )
        {
        	int numToMove = (numLeft < buffer.length ) ? numLeft : buffer.length;
        	recording.read( index, buffer, 0, numToMove );
        	writer.writeMiddle( buffer, 0, numToMove );
        	numLeft -= numToMove;
        	index += numToMove;
        }
        writer.writeEnd();
		System.out.println("WAV writing complete.");
	}
	
	/**
	 * Convert a Speex file to a Wav file.
	 * @param inFile Speex input file, must end with ".spx"
	 * @param outFile WAV output file, must end with ".wav"
	 * @param format WAVWriter.FORMAT_S16, FORMAT_U8, FORMAT_IMA_ADPCM or FORMAT_ULAW
	 * @throws IOException
	 */
	public void convertSpeexToWAV( File inFile, File outFile, int format ) throws IOException
	{
		if( !inFile.getName().toLowerCase().endsWith(".spx") )
		{
			throw new IOException("SpeexToWav inFile must end with .spx");
		}
		if( !outFile.getName().toLowerCase().endsWith(".wav") )
		{
			throw new IOException("SpeexToWav outFile must end with .wav");
		}

		FileInputStream rawInputStream = new FileInputStream( inFile );
		BufferedInputStream inStream = new BufferedInputStream( rawInputStream );

		FileOutputStream rawOutputStream = new FileOutputStream( outFile );
		BufferedOutputStream outStream = new BufferedOutputStream( rawOutputStream );

		convertSpeexToWAV( inStream, outStream, format );
		
        inStream.close();
        outStream.close();
	}

	static void usage()
	{
		System.out.println("SpeexToWav {infile.spx} {infile.wav} {-a}");
		System.out.println("  -a for ADPCM compression, default is 16 bit");
		System.out.println("  -u for ULaw compression");
	}
	/**
	 * @param args
	 */
	public static void main( String[] args )
	{
		String speexFileName = null;
		String wavFileName = null;
		int format = WAVWriter.FORMAT_S16;
		try
		{
			for( int i=0; i<args.length; i++ )
			{
				String s = args[i];
				if (s.charAt(0) == '-')
				{
					switch( s.charAt(1) )
					{
					case 'a':
						format = WAVWriter.FORMAT_IMA_ADPCM;
						break;
					case 'u':
						format = WAVWriter.FORMAT_ULAW;
						break;
						
					case 'h':
					case '?':
						usage();
						System.exit(0);
						break;
						
					default:
						System.err.println("Unrecognized option " + s );
						usage();
						System.exit(3);
					}
				}
				else if( speexFileName == null )
				{
					speexFileName = s;
				}
				else if( wavFileName == null )
				{
					wavFileName = s;
				}
				else
				{
					System.err.println( "Too many arguments " + s );
					usage();
					System.exit(4);
				}
			}
			
			if( (speexFileName == null) || (wavFileName == null ))
			{
				System.err.println( "Files not specified." );
				usage();
				System.exit(5);
			}
			File inFile = new File( speexFileName );
			File outFile = new File( wavFileName );
			SpeexToWAV converter = new SpeexToWAV();
			converter.convertSpeexToWAV( inFile, outFile, format );
			System.exit(0);
		} catch( FileNotFoundException e )
		{
			e.printStackTrace();
			System.exit(1);
		} catch( IOException e )
		{
			e.printStackTrace();
			System.exit(2);
		}
	}
}
