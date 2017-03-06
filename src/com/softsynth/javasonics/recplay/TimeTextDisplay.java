package com.softsynth.javasonics.recplay;

import java.awt.*;

/**
 * Display position in playback or recording as text.
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */

public class TimeTextDisplay extends Label implements PlayerListener
{
	private Player player;
	private static final String DEFAULT_TEXT = "00:00.0 / 00:00.0";

	public TimeTextDisplay()
	{
		this( 12, false );
	}

	public TimeTextDisplay(int fontSize, boolean useBold )
	{
		super( DEFAULT_TEXT, Label.CENTER );
		setFont( new Font( "Monospaced", (useBold ? Font.BOLD : Font.PLAIN),
				fontSize ) );
		showTime( 0.0 );
	}

	public void setPlayer( Player pPlayer )
	{
		if( this.player != null )
			player.removePlayerListener( (PlayerListener) this );
		player = pPlayer;
		player.addPlayerListener( (PlayerListener) this );
	}

	public static String timeToText( double time )
	{ 
		time += 0.05;
		int tenths = ((int) Math.floor( time * 10.0 )) % 10;
		int seconds = ((int) time) % 60;
		int minutes = ((int) time) / 60;
		StringBuffer buf = new StringBuffer();
		if( minutes < 10 )
		{
			buf.append( "0" );
		}
		buf.append( minutes );
		buf.append( ":" );
		if( seconds < 10 )
		{
			buf.append( "0" );
		}
		buf.append( seconds );
		buf.append( "." );
		buf.append( tenths );
		return buf.toString();
	}

	/* Update time labels. */
	private void showTime( double time )
	{
		if( player != null )
		{
			double maxTime = player.getMaxTime();
			if( time > maxTime )
				time = maxTime;
			setText( timeToText( time ) + " / " + timeToText( maxTime ) );
		}
		else
		{
			setText( DEFAULT_TEXT );
		}
	}

	/**
	 * Time changed so show it.
	 */
	public void playerTimeChanged( Player player, double time )
	{
		showTime( time );
	}

	public void playerLevelChanged( Player player )
	{
	}

	/**
	 * Show final time when stopped.
	 */
	public void playerStateChanged( Player player, int state, Throwable thr )
	{
		switch( state )
		{
		case Player.STOPPED:
		case Player.PAUSED:
			showTime( player.getPositionInSeconds() );
			break;
		case Player.PLAYING:
		case Recorder.RECORDING:
			break;
		}
	}

}