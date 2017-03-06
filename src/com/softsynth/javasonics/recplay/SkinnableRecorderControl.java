package com.softsynth.javasonics.recplay;

import java.awt.event.*;

import com.softsynth.awt.*;

/**
 * Add "Record" button to player controlling recording. This class provides a
 * graphical front end for the non-graphical Recorder Class.
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */

public class SkinnableRecorderControl extends SkinnablePlayerControl
{
	Recorder recorder;
	ImageButton recordButton;

	/**
	 * Construct a GUI for recording and playing back audio with standard tape
	 * transport controls.
	 */
	public SkinnableRecorderControl(VisualTheme theme)
	{
		super( theme );
	}

	public void setPlayer( Player pPlayer )
	{
		recorder = (Recorder) pPlayer;
		super.setPlayer( pPlayer );
	}

	private void handleRecord()
	{
		// Disable now so we don't get two hits when asking mic permission.
		recordButton.setEnabled( false );
		recorder.recordAudio();
	}

	protected void addButtons( VisualTheme theme )
	{
		recordButton = theme.createRecordButton();
		add( recordButton );

		recordButton.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				handleRecord();
			}
		} );

		super.addButtons( theme );
	}

	protected void setRecordEnabled( boolean flag )
	{
		if( recordButton != null )
		{
			recordButton.setEnabled( flag );
		}
	}

	/**
	 * Enable or disable buttons based on current mode of operation.
	 */
	public void updateButtons()
	{
		super.updateButtons();
		if( recordButton == null )
			return;

		if( recorder == null )
		{
			setRecordEnabled( false );
		}
		else
		{
			switch( recorder.getState() )
			{
			case Recorder.STOPPED:
			case Recorder.PAUSED:
				setRecordEnabled( recorder.isRecordable() && isEnabled() );
				break;

			case Recorder.RECORDING:
			case Recorder.PLAYING:
				setRecordEnabled( false );
				break;
			}
		}
	}
}
