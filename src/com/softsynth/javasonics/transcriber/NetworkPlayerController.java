package com.softsynth.javasonics.transcriber;

import java.awt.Frame;

import com.softsynth.javasonics.recplay.Player;

/**
 * Connection to a networked native app that can control the ListenUp player.
 * An example might be a HotKey controller or a Philips Speech Mike.
 * @author Phil Burk (C) 2004
 */
public abstract class NetworkPlayerController extends NetworkService
{
	private Player player;

	/**
	 *  
	 */
	public NetworkPlayerController(Player player, int port, Frame frame)
	{
		super(port, frame);
		this.player = player;
	}

	/**
	 * @return Returns the recorder.
	 */
	public Player getPlayer()
	{
		return player;
	}

	/**
	 * @param player
	 */
	public void setPlayer( Player player )
	{
		this.player = player;
	}
}