package com.softsynth.upload;

/**
 * Listen to the progress of an upload so it can be reported to the user.
 * @author (C) Phil Burk, httpL//www.softsynth.com
*/
public interface ProgressListener
{
    boolean progressMade( int numSoFar, int numTotal );
}
