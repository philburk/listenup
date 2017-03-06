package com.softsynth.javasonics.recplay;

/**
 * @author Phil Burk (C) 2004
 */
public class HotKeyOptions
{
	public static final int PLAY_INDEX = 0;
	public static final int STOP_INDEX = 1;
	public static final int PAUSE_INDEX = 2;
	public static final int RECORD_INDEX = 3;
	public static final int FORWARD_INDEX = 4;
	public static final int REWIND_INDEX = 5;
	public static final int TO_END_INDEX = 6;
	public static final int TO_BEGIN_INDEX = 7;
	public String[] options = new String[8];

	public boolean useHotKeys()
	{
		for( int i=0; i<options.length; i++ )
		{
			if( options[i] != null ) return true;
		}
		return false;
	}
}