
package com.softsynth.xml;
/**
 * Listener for parsing an XML stream.
 *
 * @author (C) 1997 Phil Burk, All Rights Reserved, PROPRIETARY and CONFIDENTIAL
 * @see XMLReader
 * @see XMLPrinter
 */

public interface XMLListener
{
/** Handles the start of an element. The flag ifEmpty if there is no content or endTag. */
	void beginElement( String tag, java.util.Hashtable attributes, boolean ifEmpty );
/** Handles the content of an element. */
	void foundContent( String content );
/** Handles the end of an element. */
	void endElement( String tag );
}
