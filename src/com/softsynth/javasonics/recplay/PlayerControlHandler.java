package com.softsynth.javasonics.recplay;

/**
 * Non-graphic Transport control with handleX() methods for responding to STOP,
 * PLAY, PAUSE, FAST FORWARD, REWIND, and SLOW FORWARD
 * 
 * gui buttons or foot pedal may call these methods ( This is a logical layer
 * that resides between the basic TransportControl and the graphical
 * SkinnableplayerControl )
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */

public class PlayerControlHandler extends TransportControl
{

	public void handleStop()
	{
		player.stopAudio();
	}

	public void handlePause()
	{
		player.pauseAudio();
	}

	public void handlePlay()
	{
		player.playNormalSpeed();
	}

	public void handleFastForward()
	{
		player.playFastForward();
	}

	public void handleSlowForward()
	{
		player.playSlowForward();
	}

	public void handleRewind()
	{
		player.playRewind();
	}

	public void handleToBegin()
	{
		player.toBegin();
	}

	public void handleToEnd()
	{
		player.toEnd();
	}

}