package com.softsynth.javasonics.recplay;

/**
 * Used to abstract the ability to upload a recording using the Send button.
 * This gets called by the Transcriber.
 * @author Phil Burk (C) 2009 Mobileer Inc
 */
public interface Sendable
{
	/** Upload the recorded message to the server. */
	int sendRecordedMessage();

	/** Display message in status area.
	 */
	void displayMessage( String string );
}
