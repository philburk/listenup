package com.softsynth.javasonics.util;

import java.io.*;

/**
 * Parse Electronic Arts style IFF File.
 * 
 * IFF is a file format that allows "chunks" of data to
 * be placed in a hierarchical file. It was designed by Jerry Morrison at
 * Electronic Arts for the Amiga computer and is now used extensively by Apple
 * Computer and other companies. IFF is an open standard.
 * 
 * @see RIFFParser
 * @see AudioSampleAIFF
 * @author (C) 1997 Phil Burk, SoftSynth.com, All Rights Reserved
 */

public class IFFParser extends FilterInputStream
{
	private long numBytesRead = 0;
	private long totalSize = 0;
	private static boolean debug = false;

	public static final int LIST_ID = ('L' << 24) | ('I' << 16) | ('S' << 8)
			| 'T';
	public static final int FORM_ID = ('F' << 24) | ('O' << 16) | ('R' << 8)
			| 'M';

	public IFFParser(InputStream stream)
	{
		super( stream );
		numBytesRead = 0;
	}

	/**
	 * Size of file based on outermost chunk size plus 8. Can be used to report
	 * progress when loading samples.
	 * 
	 * @return Number of bytes in outer chunk plus header.
	 */
	public long getFileSize()
	{
		return totalSize;
	}

	/**
	 * Since IFF files use chunks with explicit size, it is important to keep
	 * track of how many bytes have been read from the file. Can be used to
	 * report progress when loading samples.
	 * 
	 * @return Number of bytes read from stream, or skipped.
	 */
	public long getOffset()
	{
		return numBytesRead;
	}

	/** @return Next byte from stream. Increment offset by 1. */
	public int read() throws IOException
	{
		numBytesRead++;
		return super.read();
	}

	/** @return Next byte array from stream. Increment offset by len. */
	public int read( byte[] bar, int off, int len ) throws IOException
	{
		// Reading from a URL can return before all the bytes are available.
		// So we keep reading until we get the whole thing.
		int cursor = off;
		int numLeft = len;
		// keep reading data until we get it all
		while( numLeft > 0 )
		{
			int numRead = super.read( bar, cursor, numLeft );
			if( numRead < 0 )
				return numRead;
			cursor += numRead;
			numBytesRead += numRead;
			numLeft -= numRead;
			// System.out.println("read " + numRead + ", cursor = " + cursor +
			// ", len = " + len);
		}
		return cursor - off;
	}

	/** @return Skip forward in stream and add numBytes to offset. */
	public long skip( long numBytes ) throws IOException
	{
		numBytesRead += numBytes;
		return super.skip( numBytes );
	}

	/** Read 32 bit signed integer assuming Big Endian byte order. */
	public int readIntBig() throws IOException
	{
		int result = read() & 0xFF;
		result = (result << 8) | (read() & 0xFF);
		result = (result << 8) | (read() & 0xFF);
		int data = read();
		if( data == -1 )
			throw new EOFException(
					"readIntBig() - EOF in middle of word at offset "
							+ numBytesRead );
		result = (result << 8) | (data & 0xFF);
		return result;
	}

	/** Read 32 bit signed integer assuming Little Endian byte order. */
	public int readIntLittle() throws IOException
	{
		int result = read() & 0xFF; // LSB
		result |= ((read() & 0xFF) << 8);
		result |= ((read() & 0xFF) << 16);
		int data = read();
		if( data == -1 )
			throw new EOFException(
					"readIntLittle() - EOF in middle of word at offset "
							+ numBytesRead );
		result |= (data << 24);
		return result;
	}

	/** Read 16 bit signed short assuming Big Endian byte order. */
	public short readShortBig() throws IOException
	{
		short result = (short) ((read() << 8)); // MSB
		int data = read();
		if( data == -1 )
			throw new EOFException(
					"readShortBig() - EOF in middle of word at offset "
							+ numBytesRead );
		result |= data & 0xFF;
		return result;
	}

	/** Read 16 bit signed short assuming Little Endian byte order. */
	public short readShortLittle() throws IOException
	{
		short result = (short) (read() & 0xFF); // LSB
		int data = read(); // MSB
		if( data == -1 )
			throw new EOFException(
					"readShortLittle() - EOF in middle of word at offset "
							+ numBytesRead );
		result |= data << 8;
		return result;
	}


	public int readUShortLittle() throws IOException
	{
		return ((int)readShortLittle()) & 0x0000FFFF;
	}
	
	/** Read 8 bit signed byte. */
	public byte readByte() throws IOException
	{
		return (byte) read();
	}

	/** Read 32 bit signed int assuming IFF order. */
	public int readChunkSize() throws IOException
	{
		return readIntBig();
	}

	/** Convert a 4 character IFF ID to a String */
	public static String IDToString( int ID )
	{
		byte bar[] = new byte[4];
		bar[0] = (byte) (ID >> 24);
		bar[1] = (byte) (ID >> 16);
		bar[2] = (byte) (ID >> 8);
		bar[3] = (byte) ID;
		return new String( bar );
	}

	/** Parse the stream and pass the forms and chunks to the ChunkHandler */
	public void parse( ChunkHandler handler ) throws IOException
	{
		if( debug )
			System.out.println( "parse() ------- begin" );
		numBytesRead = 0;
		int ID = readIntBig();
		if( !isForm( ID ) )
		{
			throw new IOException("Not an audio file. ID is '" + IDToString(ID) + "'" );
		}
		int numBytes = readChunkSize();
		totalSize = numBytes + 8;
		parseChunk( handler, ID, numBytes );
		if( debug )
			System.out.println( "parse() ------- end" );
	}

	/**
	 * Parse the FORM and pass the chunks to the ChunkHandler The cursor should
	 * be positioned right after the type field.
	 */
	void parseForm( ChunkHandler handler, int ID, int numBytes, int type )
			throws IOException
	{
		while( numBytes > 8 )
		{
			int ckid = readIntBig();
			int size = readChunkSize();
			numBytes -= 8;
			if( size <= 0 )
			{
				throw new IOException( "Bad IFF FORM Size: " + IDToString( ckid )
						+ " = 0x" + Integer.toHexString( ckid )
						+ ", Size = " + size );
			}
			parseChunk( handler, ckid, size );
			if( (size & 1) == 1 )
				size++; // even-up
			numBytes -= size;
			if( debug )
			{
				System.out.println("parseForm: numBytes left in form = " + numBytes );
			}
		}

		if( numBytes > 0 )
		{
			System.out.println("IFF Parser detected " + numBytes + " bytes of garbage at end of FORM.");
			skip( numBytes );
		}
	}

	/**
	 * Does the following chunk ID correspond to a container type like FORM?
	 */
	public boolean isForm( int ckid ) throws IOException
	{
		switch( ckid )
		{
		case LIST_ID:
		case FORM_ID:
			return true;
		default:
			return false;
		}
	}

	/*
	 * Parse one chunk from IFF file. After calling handler, make sure stream is
	 * positioned at end of chunk.
	 */
	void parseChunk( ChunkHandler handler, int ckid, int numBytes )
			throws IOException
	{
		long startOffset, endOffset;
		int numRead;
		if( debug )
		{
			System.out.println( "parseChunk( " + IDToString( ckid ) + ", "
					+ numBytes + " )" );
		}
		startOffset = getOffset();
		if( isForm( ckid ) )
		{
			int type = readIntBig();
			if( debug )
				System.out.println( "parseChunk:    form = " + IDToString( ckid )
						+ ", " + numBytes + ", " + IDToString( type ) );
			handler.handleForm( this, ckid, numBytes - 4, type );
			endOffset = getOffset();
			numRead = (int) (endOffset - startOffset);
			if( numRead < numBytes )
				parseForm( handler, ckid, (numBytes - numRead), type );
		}
		else
		{
			handler.handleChunk( this, ckid, numBytes );
		}
		endOffset = getOffset();
		numRead = (int) (endOffset - startOffset);
		if( debug )
		{
			System.out.println( "parseChunk:    endOffset = " + endOffset );
			System.out.println( "parseChunk:    numRead = " + numRead );
		}
		if( (numBytes & 1) == 1 )
			numBytes++; // even-up
		if( numRead < numBytes )
			skip( numBytes - numRead );
	}

}
