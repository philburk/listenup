package com.softsynth.javasonics.recplay;

import java.awt.Color;
import java.util.Observable;
import java.util.Observer;

/**
 * Display and control position in playback or recording.
 * 
 * MOD NICK use time for recording, timestamped buffer position for playback
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */

public class PositionBarControl implements PlayerListener
{
	private Color savedBackground;
	private Player player;
	private boolean wasWaitingForData = false;
	private PositionCanvas positionCanvas;

	public void setPlayer( Player pPlayer )
	{
		if( this.player != null )
			player.removePlayerListener( (PlayerListener) this );
		player = pPlayer;
		player.addPlayerListener( (PlayerListener) this );
	}

	public PositionBarControl(PositionCanvas pPosCanvas)
	{
		positionCanvas = pPosCanvas;
		positionCanvas.setEnabled( false );
		positionCanvas.addObserver( new Observer()
		{
			// Set player time when user drags scroll bar.
			public void update( Observable observable, Object obj )
			{
				if( player != null )
				{
					double time = positionCanvas.getValue()
							* player.getMaxPlayableTime();
					// 0-1 time
					player.setPositionInSeconds( time );
				}
			}
		} );
	}

	private void showTime( double time )
	{
		double dblPos = 0.0;
		double maxTime = positionCanvas.getMaxTime( player );
		if( time > maxTime )
		{
			time = maxTime;
		}
		// Prevent numeric underflow.
		if( maxTime > 0.00000001 )
		{
			dblPos = time / maxTime;
		}
		positionCanvas.setValue( dblPos );
	}

	public void playerTimeChanged( Player player, double time )
	{
		if( player.isWaitingForData() && !wasWaitingForData )
		{
			savedBackground = positionCanvas.getBackground();
			positionCanvas.setBackground( Color.yellow );
			wasWaitingForData = true;
		}
		else if( !player.isWaitingForData() && wasWaitingForData )
		{
			if( savedBackground != null )
			{
				positionCanvas.setBackground( savedBackground );
			}
			wasWaitingForData = false;
		}
		showTime( time );
	}

	public void playerLevelChanged( Player player )
	{
	}

	/**
	 * Update GUI based on current mode of operation.
	 */
	public void playerStateChanged( Player player, int state, Throwable thr )
	{
		switch( state )
		{
		case Player.STOPPED:
		case Player.PAUSED:
			// We need to enable the canvas so the popupMenu with "Load Most
			// Recent Recording" can be used.
			// positionCanvas.setEnabled(player.isPlayable()); // before 8/16/08
			positionCanvas.setEnabled( true );
			positionCanvas.setValue( player.getNormalizedPosition() ); // use
			// position
			// when
			// playing
			break;
		case Player.PLAYING:
		case Recorder.RECORDING:
			positionCanvas.setEnabled( false );
			wasWaitingForData = true;
			break;
		}
	}

	/**
	 * @return
	 */
	public PositionCanvas getPositionCanvas()
	{
		return positionCanvas;
	}

}