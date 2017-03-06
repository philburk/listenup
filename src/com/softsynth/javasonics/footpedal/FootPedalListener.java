package com.softsynth.javasonics.footpedal;

/**
 * @author Phil Burk (C) 2004
 */
public interface FootPedalListener
{
	void buttonPressed( FootPedalEvent event );
	void buttonReleased( FootPedalEvent event );
}
