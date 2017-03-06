package com.softsynth.javasonics.recplay;

import com.softsynth.javasonics.error.WebDeveloperRuntimeException;

public class InvalidAudioRecordingException extends
		WebDeveloperRuntimeException
{
	private static final long serialVersionUID = 468889915455319543L;

	public InvalidAudioRecordingException(String msg)
	{
		super( msg );
	}

}
