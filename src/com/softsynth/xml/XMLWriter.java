package com.softsynth.xml;

import java.io.*;
import java.util.Stack;

/**********************************************************************
 * Write XML formatted file.
 * 
 * @author (C) 2000 Phil Burk, SoftSynth.com, All Rights Reserved
 */

public class XMLWriter extends IndentingWriter
{
	Stack tagStack = new Stack();
	boolean hasContent = false;

	public XMLWriter(OutputStream stream)
	{
		super( stream );
	}

	public XMLWriter(Writer outputStreamWriter)
	{
		super(outputStreamWriter);
	}

	public void writeAttribute( String name, String value )
	{
		print( " " + name + "=\"" + XMLTools.escapeText( value ) + "\"" );
	}

	public void writeAttribute( String name, int value )
	{
		writeAttribute( name, Integer.toString( value ) );
	}

	public void writeAttribute( String name, long value )
	{
		writeAttribute( name, Long.toString( value ) );
	}

	public void writeAttribute( String name, double value )
	{
		writeAttribute( name, Double.toString( value ) );
	}

	public void writeAttribute( String name, boolean value )
	{
		writeAttribute( name, (value ? "1" : "0") );
	}

	public void writeHeader()
	{
		println( "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" );
	}

	public void startTag( String name )
	{
		beginTag( name );
	}

	public void beginTag( String name )
	{
		if( !hasContent && (tagStack.size() > 0) )
		{
			beginContent();
			println();
		}
		print( "<" + name );
		tagStack.push( name );
		hasContent = false;
		indent();
	}

	public void endTag()
	{
		undent();
		String name = (String) tagStack.pop();
		if( hasContent )
		{
			println( "</" + name + ">" );
		}
		else
		{
			println( " />" );
		}
		// If there are tags on the stack, then they obviously had content
		// because we are ending a nested tag.
		hasContent = !tagStack.isEmpty();
	}

	public void beginContent()
	{
		print( ">" );
		hasContent = true;
	}

	public void endContent()
	{
	}

	public void writeComment( String text ) throws IOException
	{
		if( !hasContent && (tagStack.size() > 0) )
		{
			beginContent();
			println();
		}
		println( "<!-- " +  XMLTools.escapeText( text ) + "-->" );
	}

	public void writeContent( String string )
	{
		beginContent();
		print( XMLTools.escapeText( string ) );
		endContent();
	}

	public void writeTag( String tag, String content )
	{
		beginTag(tag);
		writeContent(content);
		endTag();
	}

}
