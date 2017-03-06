package com.softsynth.javasonics.util;
/**
 * CuePoint for an AudioSample.
 * Position is a frame index.
 * ID is a unique identifier that is often generated
 * automatically by the file generator.
 *
 * @author (C) 1997 Phil Burk, SoftSynth.com, All Rights Reserved
 */

public class CuePoint
{
	int position;
	int uniqueID;
	static int nextID = 0;

/** Create a CuePoint at the given position.
 * @param position index of associated frame
 * @param uniqueID unique identifier that is often generated automatically by the file generator.
 */
 	public CuePoint( int position, int uniqueID )
	{
		this.uniqueID = uniqueID;
		this.position = position;
	}
	public CuePoint( int position )
	{
		this( position, nextID++ );
	}

	public void setPosition( int position )
	{
		this.position = position;
	}
	public int getPosition()
	{
		return position;
	}
	public int getID()
	{
		return uniqueID;
	}
}
