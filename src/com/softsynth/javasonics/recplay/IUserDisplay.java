package com.softsynth.javasonics.recplay;

public interface IUserDisplay
{

	void displayMessage( String string );

	void reportExceptionAfterStopAudio( Throwable exc );

}
