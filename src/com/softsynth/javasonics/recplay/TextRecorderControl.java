package com.softsynth.javasonics.recplay;

import java.awt.Button;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Add "Record" button to player controlling recording. This class provides a
 * graphical front end for the non-graphical Recorder Class.
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */

public class TextRecorderControl extends TextPlayerControl
{
	Recorder recorder;
	Button recordButton;

	public void setPlayer( Player pPlayer )
	{
		recorder = (Recorder) pPlayer;
		super.setPlayer( pPlayer );
	}

	public void setButtonBackground( Color color )
	{
		super.setButtonBackground( color );
		if( recordButton != null ) recordButton.setBackground(  color );
	}
	
	public void handleRecord()
	{
		// Disable now so we don't get two hits when asking mic permission.
		recordButton.setEnabled( false );
		recorder.recordAudio();
	}

	/**
	 * Construct a GUI for recording and playing back audio with standard tape
	 * transport controls.
	 */
	public TextRecorderControl()
	{
		super();
	}

	protected void addButtons()
	{
		recordButton = new Button( "Record" );
		add( recordButton );

		recordButton.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				handleRecord();
			}
		} );

		super.addButtons();
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
			recordButton.setEnabled( false );
		}
		else
		{
			switch( recorder.getState() )
			{
			case Recorder.STOPPED:
			case Recorder.PAUSED:
				recordButton
						.setEnabled( recorder.isRecordable() && isEnabled() );
				break;

			case Recorder.RECORDING:
			case Recorder.PLAYING:
				recordButton.setEnabled( false );
				break;
			}
		}
	}
}
