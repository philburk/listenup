package com.softsynth.javasonics.footpedal;

import java.awt.Event;

/**
 * @author Phil Burk (C) 2004
 */
public class FootPedalEvent extends Event
{	
	// These must match values in JNI 'C' code.
	public final static int PLAY = 0;
	public final static int FORWARD = 1;
	public final static int REWIND = 2;
	protected static final int VALID = (1 << 3);

	/**
	 * @param target FootPedalDriver
	 * @param id FootPedal.PLAY, FORWARD, REWIND
	 * @param arg
	 */
	public FootPedalEvent(Object target, int id, Object arg)
	{
		super(target, id, arg);
	}

}
