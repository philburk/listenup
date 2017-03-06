package com.softsynth.javasonics.util;

import java.io.IOException;

/**
 * Handle IFF Chunks as they are parsed from an IFF or RIFF file.
 * 
 * @see IFFParser
 * @see AudioSampleAIFF
 * @author (C) 1997 Phil Burk, SoftSynth.com, All Rights Reserved
 */
public interface ChunkHandler
{
	/**
	 * The parser will call this when it encounters a FORM or LIST chunk that
	 * contains other chunks. This handler can either read the form's chunks, or
	 * let the parser find them and call handleChunk().
	 * 
	 * @param ID
	 *            a 4 byte identifier such as FORM_ID that identifies the IFF
	 *            chunk type.
	 * @param numBytes
	 *            number of bytes contained in the FORM, not counting the FORM
	 *            type.
	 * @param type
	 *            a 4 byte identifier such as AIFF_ID that identifies the FORM
	 *            type.
	 */
	public void handleForm( IFFParser parser, int ID, int numBytes, int type )
			throws IOException;

	/**
	 * The parser will call this when it encounters a chunk that is not a FORM
	 * or LIST. This handler can either read the chunk's, or ignore it. The
	 * parser will skip over any unread data. Do NOT read past the end of the
	 * chunk!
	 * 
	 * @param ID
	 *            a 4 byte identifier such as SSND_ID that identifies the IFF
	 *            chunk type.
	 * @param numBytes
	 *            number of bytes contained in the chunk, not counting the ID
	 *            and size field.
	 */
	public void handleChunk( IFFParser parser, int ID, int numBytes )
			throws IOException;
}
