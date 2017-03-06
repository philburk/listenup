
package com.softsynth.xml;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Pretty print an XML file.
 * Indent each nested element.
 *
 * @author (C) 1997 Phil Burk, All Rights Reserved, PROPRIETARY and CONFIDENTIAL
 * @see XMLReader
 * @see XMLListener
 */

/*********************************************************************************
 */
public class XMLPrinter extends IndentingWriter implements XMLListener
{

	public XMLPrinter()
	{
		this( System.out );
	}

	public XMLPrinter( OutputStream stream )
	{
		super( stream );
	}

/** Print a file passed as a command line argument.
 */
    public static void main(String args[])
	{
		String             fileName;

		fileName = ( args.length > 0 ) ? args[0] : "xmlpatch.txt";
		try
		{
			InputStream     stream = (InputStream) (new FileInputStream(fileName));
			XMLReader xmlr = new XMLReader( stream );
			xmlr.setXMLListener( new XMLPrinter() );
			xmlr.parse();
			xmlr.close();
			stream.close();
		} catch( IOException e ) {
			System.out.println( "Error = " + e );
		} catch( SecurityException e ) {
			System.out.println( "Error = " + e );
		}
	}

	public void beginElement( String tag, Hashtable attributes, boolean ifEmpty )
	{
		print("<" + tag );
		indent();
		Enumeration e = attributes.keys();
		if( e.hasMoreElements() ) println();
		while( e.hasMoreElements() )
		{
			String key = (String) e.nextElement();
			String value = (String) attributes.get(key);
			println( key + "=\"" + value + "\"" );
		}
		if( ifEmpty )
		{
			undent();
			println("/>");
		}
		else
		{
			println(">");
		}
	}

	public void foundContent( String content )
	{
		if( content != null ) println(content);
	}

	public void endElement( String tag )
	{
		undent();
		println("</" + tag + ">");
	}
}

