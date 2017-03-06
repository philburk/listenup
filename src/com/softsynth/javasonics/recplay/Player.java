package com.softsynth.javasonics.recplay;

import com.softsynth.javasonics.DeviceUnavailableException;

/**
 * Something that acts like a tape player.
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */
public interface Player
{
    public static final int STOPPED = 0;
    public static final int PAUSED = 1;
    public static final int PLAYING = 2;
    public static final int ABORTED = 3;
	public static final int RECORDING = 4;

    /** Start player threads or other resources. Don't play yet. */
    public void start();

    /** Stop anything from start(). */
    public void stop();

    /** Stop playing or recording. */
    public void stopAudio();
    
    /** Wait until player has stopped if not already stopped.
     ** @return true if timed out
     * @throws InterruptedException
     */
    public boolean waitUntilStopped( int msec ) throws InterruptedException;

    /** Begin playing. */
    public void playAudioNoRewind();

    /** Pause playing or recording. */
    public void pauseAudio();

    /** Can we play anything? */
    public boolean isPlayable();

    /** Is it stopped or paused? */
    public boolean isStopped();
    /** Is it playing? */
    public boolean isPlaying();
    /** Is it recording? */
    public boolean isRecording();

    /** Is it waiting for more samples to be downloaded? */
    public boolean isWaitingForData();

    /** Are we STOPPED or PLAYING or ...? */
    public int getState();

    public void setRecording(Recording recording) throws DeviceUnavailableException;

    public Recording getRecording();

    public int getSamplesPerFrame();

    public void setFrameRate(double frameRate) throws DeviceUnavailableException;

    public double getFrameRate();
    

    /**
     * Set size of buffers for streams.
     */
    public void setLatencyInFrames(int latencyInFrames);

    /* Get level or amplitude as a fraction of maximum in range of 0.0 to 1.0 */
    public float getLeftLevel();

    public float getRightLevel();

    /* Current playback or recording position in seconds. */
    public double getPositionInSeconds();

    public void setPositionInSeconds(double time);

	public int getStartIndex();
	public int getStopIndex();
	
    /** Time to start playing or recording if not the same as stopTime. */
    public double getStartTime();
    public void setStartTime( double time );
    /** Time to start playing or recording if not the same as startTime. */
    public double getStopTime();
    public void setStopTime( double time );
    
    /** Time in seconds to rewind when audio is paused. */
    public double getAutoBackStep();
    public void setAutoBackStep( double seconds );

    public double getMaxTime();
    public double getMaxPlayableTime();

    public void addPlayerListener(PlayerListener listener);

    public void removePlayerListener(PlayerListener listener);
    
    /** Notify PlayerListeners that something has changed. */
    public void notifyState();

    public void setPlaybackSpeed(float ratio);

    public float getPlaybackSpeed();

    public boolean isSkipping();

    public void setSkipping(boolean skipping);

    /**
     * @return duration in seconds of skip between buffers, used when
     *         isSkipping() == true
     */
    public double getSkipDuration();

    /**
     * Set duration in seconds of skip between playback buffers, used when
     * isSkipping() == true
     */
    public void setSkipDuration(double secondsToSkip);

    public void setReverse(boolean reverse);

    public boolean isReverse();

    public double getNormalizedPosition();

    /**
     *  
     */
    public void playFastForward();

    /**
     *  
     */
    public void playSlowForward();

    /**
     *  
     */
    public void playRewind();

    /**
     *  
     */
    public void playNormalSpeed();

    public void setSlowForwardSpeed(float speed);

    /** Delete any previously recorded material. */
    public void erase();
    
    /** Delete selected material. */
    public void eraseSelected();

    /**
     * 
     */
    public void toBegin();

    /**
     * 
     */
    public void toEnd();
    
    /** return@ true if state is Playing and playing is modified by ff or rew */
    public boolean isPlayModified();

	public String stateToText( int state );

	/** Enable or disable the option to protect a recording after
	 * it is made and before it is uploaded. */
	public void setRecordingProtection( boolean onOrOff );
	public boolean isProtected();
	public void setProtected( boolean onOrOff );
	
	/** Set time between timer display update intervals. */
	public void setTimeChangeInterval( int msec );

	/** Set time to rewind and play when pausing a recording. */
	public void setAutoPreview( double autoPreview );
}

