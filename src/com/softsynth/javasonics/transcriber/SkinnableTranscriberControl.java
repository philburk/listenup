package com.softsynth.javasonics.transcriber;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

import com.softsynth.awt.ImageButton;
import com.softsynth.javasonics.recplay.*;

/**
 * Panel with ToBegin/Rewind/Forward/ToEnd image buttons for controlling playback (no recording).
 * This class provides a graphical front end for Transcriber.
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */

public class SkinnableTranscriberControl extends SkinnablePlayerControl
{
	PlaybackSpeedControlPanel playbackSpeedControlPanel;

	/**
	 * Construct a GUI for recording and playing back audio with standard tape
	 * transport controls.
	 * 
	 * add original theme to arg, use both
	 */
	public SkinnableTranscriberControl(VisualTheme theme,
			TranscriberVisualTheme transcriberTheme)
	{
		super( theme );

		setToBeginButton( transcriberTheme.createToBeginButton() );
		add( getToBeginButton() );
		((ImageButton) getToBeginButton())
				.addActionListener( new ActionListener()
				{
					public void actionPerformed( ActionEvent e )
					{
						handleToBegin();
					}
				} );

		setRewindButton( transcriberTheme.createRewindButton() );
		add( getRewindButton() );
		((ImageButton) getRewindButton())
				.addActionListener( new ActionListener()
				{
					public void actionPerformed( ActionEvent e )
					{
						handleRewind();
					}
				} );

		setFastForwardButton( transcriberTheme.createFastForwardButton() );
		add( getFastForwardButton() );
		((ImageButton) getFastForwardButton())
				.addActionListener( new ActionListener()
				{
					public void actionPerformed( ActionEvent e )
					{
						handleFastForward();
					}
				} );

		setToEndButton( transcriberTheme.createToEndButton() );
		add( getToEndButton() );
		((ImageButton) getToEndButton())
				.addActionListener( new ActionListener()
				{
					public void actionPerformed( ActionEvent e )
					{
						handleToEnd();
					}
				} );

		playbackSpeedControlPanel = new PlaybackSpeedControlPanel();
		//        ((PlaybackSpeedControlPanel)
		// playbackSpeedControlPanel).setPlayer(player);
		playbackSpeedControlPanel.setBevelled( true );
		add( playbackSpeedControlPanel );

		updateButtons();
	}

	public void setSpeedScrollbarEnabled( boolean enabled )
	{
		if( playbackSpeedControlPanel != null )
			playbackSpeedControlPanel.setEnabled( enabled );
	}

	public void setPlayer( Player pPlayer )
	{
		super.setPlayer( pPlayer );
		if( playbackSpeedControlPanel != null )
		{
			playbackSpeedControlPanel.addObserver( new Observer()
			{
				public void update( Observable arg0, Object arg1 )
				{
					if( getPlayer() != null )
					{
						getPlayer().setSlowForwardSpeed(
								playbackSpeedControlPanel.getSpeed() );
					}
					else
					{
						System.out
								.println( "PlaybackSpeedControlPanel player null" );
					}
				}
			} );
		}
	}
}