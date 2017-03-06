package com.softsynth.javasonics.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Log output messages with a timestamp.
 * 
 * @author Phil Burk (C) 2003
 */
public class Logger
{
	private static long startTime;
	private static int threshold = 0;
	private static PrintStream out = System.out;
	private static String lineBuffer = "";
	private static HeadTailLog headTailLog;

	static
	{
		reset();
	}

	private static String digits3( int n )
	{
		StringBuffer buf = new StringBuffer();

		for( int i = 0; i < 3; i++ )
		{
			int temp = n / 10;
			int digit = (n - (temp * 10));
			buf.insert( 0, digit );
			n = temp;
		}
		return buf.toString();
	}

	public static String getTimeText()
	{
		long now = System.currentTimeMillis();
		int elapsedMSec = (int) (now - startTime);
		int seconds = elapsedMSec / 1000;
		int rem = elapsedMSec - (seconds * 1000);

		return seconds + "." + digits3( rem );
	}

	private static void outPrint( String msg )
	{
		lineBuffer += msg;
	}

	private static void outPrint( char b )
	{
		lineBuffer += b;
	}

	private static void outPrintln( int level, String msg )
	{
		outPrint( msg );
		outPrintln( level );
	}

	private static void outPrintln( int level )
	{
		String logLine = getTimeText() + ", " + lineBuffer;
		if( level <= threshold )
		{
			out.println( "ListenUp: " + logLine );
		}
		// Put all log data in HeadTailLog regardless of level.
		headTailLog.add( logLine );
		lineBuffer = "";
	}

	public static void println( String msg )
	{
		println( 2, msg );
	}

	private static void print( String msg )
	{
		outPrint( msg );
	}

	public static void println( int level, String msg )
	{
		print( msg );
		outPrintln( level );
	}

	public static void printMemory( int level, String msg )
	{
		print( msg );
		Runtime rt = Runtime.getRuntime();
		rt.gc();
		rt.gc();
		long freeMem = rt.freeMemory();
		long totalMem = rt.totalMemory();
		print( ", freeMem = " + freeMem );
		print( ", totalMem = " + totalMem );
		outPrintln( level );
	}

	/**
	 * Set level of debug output. Messages at or below this level will be
	 * printed.
	 * 
	 * @return
	 */
	public static int getLevel()
	{
		return threshold;
	}

	/**
	 * @param i
	 */
	public static void setLevel( int i )
	{
		threshold = i;
	}

	public static HeadTailLog getHeadTailLog()
	{
		return headTailLog;
	}

	/**
	 * Read characters from networked stream in multiple reads if necessary.
	 * 
	 * @return number of bytes read.
	 */
	public static int readNetworkStream( InputStream input, byte[] bar,
			int off, int len ) throws IOException
	{
		// Reading from a URL can return before all the bytes are available.
		// So we keep reading until we get the whole thing.
		int cursor = off;
		int numLeft = len;
		// keep reading data until we get it all
		while( numLeft > 0 )
		{
			int numRead = input.read( bar, cursor, numLeft );
			if( numRead < 0 )
			{
				int totalRead = cursor - off;
				if( totalRead == 0 )
				{
					return numRead;
				}
				else
				{
					return totalRead;
				}
			}
			cursor += numRead;
			numLeft -= numRead;
		}
		return cursor - off;
	}

	public static void dumpBytes( URL url, int maxBytes ) throws IOException
	{
		URLConnection conn = url.openConnection();
		InputStream input = conn.getInputStream();
		dumpBytes( input, maxBytes );
		input.close();
	}

	public static String toHexString( int value, int numChars )
	{
		String text = Integer.toHexString( value );
		if( text.length() < numChars )
		{
			StringBuffer buf = new StringBuffer();
			for( int i = 0; i < (numChars - text.length()); i++ )
			{
				buf.append( '0' );
			}
			buf.append( text );
			text = buf.toString();
		}
		return text;
	}

	public static void dumpBytes( InputStream input, int maxBytes )
			throws IOException
	{
		byte[] lineBuffer = new byte[16];
		int numToPrint = maxBytes;
		int bytesPrinted = 0;
		boolean hitEOF = false;
		while( (numToPrint > 0) && !hitEOF )
		{
			outPrint( "   " + toHexString( bytesPrinted, 4 ) + ":  " );
			int numToRead = (numToPrint < lineBuffer.length) ? numToPrint
					: lineBuffer.length;
			int numRead = readNetworkStream( input, lineBuffer, 0, numToRead );
			if( numRead < 0 )
			{
				hitEOF = true;
			}
			else
			{
				for( int i = 0; i < numRead; i++ )
				{
					int b = lineBuffer[i] & 0x00FF;
					outPrint( toHexString( b, 2 ) + " " );
					if( (i & 3) == 3 )
					{
						outPrint( " " );
					}
				}
				for( int i = numRead; i < lineBuffer.length; i++ )
				{
					outPrint( "   " );
				}
				outPrint( " " );

				for( int i = 0; i < numRead; i++ )
				{
					int b = lineBuffer[i] & 0x00FF;
					if( (b >= 0x20) && (b < 0x7f) )
					{
						outPrint( (char) b );
					}
					else
					{
						outPrint( '.' );
					}
				}

				numToPrint -= numToRead;
				bytesPrinted += numRead;
				outPrintln( 0 );
			}
		}
		if( hitEOF )
		{
			outPrintln( 0, "  EOF" );
		}
		else
		{
			outPrintln( 0, "    ...more bytes remaining in file..." );
		}
	}

	public static void reset()
	{
		startTime = System.currentTimeMillis();
		headTailLog = new HeadTailLog( 100, 100 );
	}

}
