package com.softsynth.javasonics;

/**
 * Exception thrown when an error occurs in an audio device.
 * @author Phil Burk (C) 2001 SoftSynth.com
 * @version 0.1
 */

public class AudioDeviceException extends RuntimeException
{
    public AudioDeviceException( String msg )
    {
        super( msg );
    }
}
