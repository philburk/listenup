package com.softsynth.javasonics.recplay;

/**
 * Listen to a Player object so we know when it stopped or started.
 * These are typically used to update GUI components.
 *
 * @author (C) 2003 Phil Burk, All Rights Reserved
 */

public interface PlayerListener
{
/** Receive notification of a state change. */
   	public void playerStateChanged( Player player, int state, Throwable thr );
   	
/** Receive notification of a time change. */
   	public void playerTimeChanged( Player player, double time );
   	
/** Receive notification of a level change. */
   	public void playerLevelChanged( Player player );
}

