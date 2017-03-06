package com.softsynth.javasonics.recplay;

import java.awt.Button;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panel with play/stop image buttons for controlling recording. This class
 * provides a graphical front end for the non-graphical Player Class.
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */

public class TextPlayerControl extends PlayerControlHandler
{

    /**
     * Construct a GUI for recording and playing back audio with standard tape
     * transport controls.
     */
    public TextPlayerControl()
	{
		// Call a function so we can add buttons before or after original buttons.
		addButtons();
	}

	protected void addButtons()
	{
        add(stopButton = new Button("Stop"));
        ((Button) stopButton).addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                handleStop();
            }
        });

        add(pauseButton = new Button("Pause"));
        ((Button) pauseButton).addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                handlePause();
            }
        });

        add(playButton = new Button("Play"));
        ((Button) playButton).addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                handlePlay();
            }
        });

        updateButtons();
    }
}
