package com.softsynth.xml;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Write to a file with indentation at the beginning of a line.
 * One advantage of using a PrintWriter is that it automatically handles
 * line terminators properly on different hosts.
 *
 * @author Phil Burk, (C) 2000 SoftSynth.com All Rights Reserved, PROPRIETARY and CONFIDENTIAL
 */

public class IndentingWriter extends PrintWriter
{
	int spacesPerIndentation = 4;
	int indentation = 0;
	int position = 0;

	public IndentingWriter( OutputStream stream )
	{
		super( stream, true );
	}
	
	public IndentingWriter( Writer writer )
	{
		super( writer, true );
	}

	public void setIndentation( int level )
	{
		indentation = level;
	}
	public int getIndentation()
	{
		return indentation;
	}

/** Increase level of indentation by one.
 */
	public void indent()
	{
		indentation++;
	}
/** Decrease level of indentation by one.
 * Don't let level go below zero.
 */
	public void undent()
	{
		indentation--;
		if( indentation < 0 ) indentation = 0;
	}

/** Print string. If at left margin, add spaces for current level of indentation.
 */
	public void print( String s )
	{
		if( position == 0 )
		{
			int numSpaces = indentation * spacesPerIndentation;
			for( int i=0; i<numSpaces; i++ ) print( ' ' );
			position += numSpaces;
		}
		super.print( s );
		// System.out.print(s);
		position += s.length();
	}

	public void rawPrintln(String s)
	{
		super.println(s);
		position = 0;
	}
	
	public void println()
	{
		super.println();
		position = 0;
	}

	public void println( String s )
	{
		print(s);
		println();
	}
}

