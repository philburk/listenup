
package com.softsynth.xml;
import java.util.*;
import java.io.*;

/**
 * Parse an XML stream using a simple State Machine
 *
 * @author (C) 1997 Phil Burk, All Rights Reserved, PROPRIETARY and CONFIDENTIAL

 * @see XMLListener
 * @see XMLPrinter
 */

public class XMLReader extends PushbackInputStream
{
	XMLListener        listener;
	final static int   IDLE = 0;
	final static int   INTAG = 0;
	static int         depth = 0;

	final static int STATE_TOP = 0;
	final static int STATE_TAG_NAME = 1;
	final static int STATE_TAG_FIND_ANGLE = 2;
	final static int STATE_TAG_ATTR_NAME = 3;
	final static int STATE_TAG_FIND_EQUAL = 4;
	final static int STATE_TAG_FIND_QUOTE = 5;
	final static int STATE_TAG_ATTR_VALUE = 6;
	final static int STATE_CONTENT = 7;
	final static int STATE_CHECK_END = 8;
	final static int STATE_TAG_SKIP = 9;

	public void setXMLListener( XMLListener listener )
	{
		this.listener = listener;
	}

	public XMLReader( InputStream stream )
	{
		super( stream );
	}

/*****************************************************************
 */
	public void parse() throws IOException
	{
		int    i;
		char   c;

		while(true)
		{
			i = read();
			if( i < 0 ) break; // got end of file
			c = (char) i;

            if( c == '<' )
            {
                parseElement();
            }
            else if( !Character.isWhitespace( c ) )
            {
                throw new StreamCorruptedException( "Unexpected character. This doesn't look like an XML file!" );
            }
		}
	}

	String charToString( char c )
	{
		String s;
		switch( c )
		{
		case '\r': s = "\\r"; break;
		case '\n': s = "\\n"; break;
		case '\t': s = "\\t"; break;
		default:
			if( Character.isWhitespace(c) ) s = " ";
			else s = "" + c;
			break;
		}
		return s;
	}

	boolean isStringWhite( String s )
	{
		if( s == null ) return true;
		int len = s.length();
		for( int i=0; i<len; i++ )
		{
			if( !Character.isWhitespace( s.charAt( i ) ) )
			{
				return false;
			}
		}
		return true;
	}

/*****************************************************************
 */
	void parseElement() throws IOException
	{
		int    state = STATE_TAG_NAME;
		int    i;
		char   c;
		String tagName = "";
		String name = null, value = null;
		boolean ifEmpty = false;
		boolean endTag = false;
		boolean done = false;
		boolean skipWhiteSpace = true;
		char endQuote = '"';  // may also be single quote
		String content = null;
		Hashtable attributes = new Hashtable();

		// System.out.println("\nparseElement() ---------- " + depth++);
		while(!done)
		{
			do
			{
				i = read();
				if( i < 0 ) throw new EOFException("EOF inside element!");
				c = (char) i;
			} while( skipWhiteSpace && Character.isWhitespace( c ) );
			skipWhiteSpace = false;

			// System.out.print("(" + charToString(c) + "," + state + ")" );

			switch( state )
			{

			case STATE_TAG_NAME:
				if( Character.isWhitespace( c ) )
				{
					skipWhiteSpace = true;
					state = STATE_TAG_FIND_ANGLE;
				}
				else if( c == '/' ) // this tag has no matching end tag
				{
					ifEmpty = true;
					state = STATE_TAG_FIND_ANGLE;
				}
				else if( c == '>' ) // end of tag
				{
					if( endTag )
					{
						listener.endElement( tagName );
						done = true;
					}
					else
					{
						listener.beginElement( tagName, attributes, ifEmpty );
						state = STATE_CONTENT;
					}
				}
				else if(c == '?')
				{
					state = STATE_TAG_SKIP; // got version stuff so skip to end
				}
				else if(c == '!') // FIXME - parse for "--"
				{
					state = STATE_TAG_SKIP; // got comment
				}
				else
				{
					tagName += c;
				}
				break;

			case STATE_TAG_SKIP:
				if( c == '>' )
				{
					done = true;
				}
				break;

			case STATE_TAG_FIND_ANGLE:
				if( c == '/' ) // this tag has no matching end tag
				{
					ifEmpty = true;
				}
				else if( c == '>' )
				{
					if( endTag )
					{
						listener.endElement( tagName );
						done = true;
					}
					else
					{
						listener.beginElement( tagName, attributes, ifEmpty );
						state = STATE_CONTENT;
						done = ifEmpty;
					}
				}
				else
				{
					state = STATE_TAG_ATTR_NAME;
					name = "" + c;
				}
				break;

			case STATE_TAG_ATTR_NAME:
				if( Character.isWhitespace( c ) )
				{
					skipWhiteSpace = true;
					state = STATE_TAG_FIND_EQUAL;
				}
				else if( c == '=' )
				{
					skipWhiteSpace = true;
					state = STATE_TAG_FIND_QUOTE;
				}
				else
				{
					name += c;
				}
				break;

			case STATE_TAG_FIND_EQUAL:
				if( c == '=' )
				{
					skipWhiteSpace = true;
					state = STATE_TAG_FIND_QUOTE;
				}
				else
				{
					throw new StreamCorruptedException("Found " + charToString(c) + ", expected =.");
				}
				break;

			case STATE_TAG_FIND_QUOTE:
				if( c == '"' )
				{
					state = STATE_TAG_ATTR_VALUE;
					value = "";
					endQuote = '"';
				}
				else if( c == '\'' )
				{
					state = STATE_TAG_ATTR_VALUE;
					value = "";
					endQuote = '\'';
				}
				else
				{
					throw new StreamCorruptedException("Found " + charToString(c) + ", expected '\"'.");
				}
				break;

			case STATE_TAG_ATTR_VALUE:
				if( c == endQuote )
				{
					attributes.put( name, value );
					// System.out.println("\ngot " + name + " = " + value );
					skipWhiteSpace = true;
					state = STATE_TAG_FIND_ANGLE;
				}
				else
				{
					value += c;
				}
				break;

			case STATE_CONTENT:
				if( c == '<' )
				{
					state = STATE_CHECK_END;
					if( !isStringWhite( content ) )
					{
						String unescaped = XMLTools.unescapeText( content );
						listener.foundContent( unescaped );
					}
					content = null;
				}
				else
				{
					if( content == null ) content = "";
					content += c;
				}
				break;

			case STATE_CHECK_END:
				if( c == '/' )
				{
					endTag = true;
					state = STATE_TAG_NAME;
					tagName = "";
				}
				else
				{
					unread( c );
					parseElement();
					state = STATE_CONTENT;
				}
				break;
			}
		}
		// System.out.println("\nparseElement: returns, " + --depth );
	}

/** Get a single attribute from the Hashtable. Use the default if not found.
 */
	public static int getAttribute( Hashtable attributes, String key, int defaultValue )
	{
		String s = (String) attributes.get( key );
		return (s == null) ? defaultValue : Integer.parseInt( s );
	}

/** Get a single attribute from the Hashtable. Use the default if not found.
 */
	public static double getAttribute( Hashtable attributes, String key, double defaultValue )
	{
		String s = (String) attributes.get( key );
		return (s == null) ? defaultValue : Double.valueOf( s ).doubleValue();
	}
}
