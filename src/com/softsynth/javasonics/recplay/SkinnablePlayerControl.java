package com.softsynth.javasonics.recplay;

import com.softsynth.awt.*;

import java.awt.event.*;

/**
 * Panel with play/stop image buttons for controlling recording. This class
 * provides a graphical front end for the non-graphical Player Class.
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */

public class SkinnablePlayerControl extends PlayerControlHandler
{

	/**
	 * Construct a GUI for recording and playing back audio with standard tape
	 * transport controls.
	 */
	public SkinnablePlayerControl(VisualTheme theme)
	{
		// Call a function so we can add buttons before or after original buttons.
		addButtons( theme );
	}

	protected void addButtons( VisualTheme theme )
	{
		add( stopButton = theme.createStopButton() );
		((ImageButton) stopButton).addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				handleStop();
			}
		} );

		add( pauseButton = theme.createPauseButton() );
		((ImageButton) pauseButton).addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				handlePause();
			}
		} );

		add( playButton = theme.createPlayButton() );
		((ImageButton) playButton).addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				handlePlay();
			}
		} );

		updateButtons();
	}
}
