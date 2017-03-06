package com.softsynth.javasonics.installer;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Show running status of bytes downloaded.
 * @author Phil Burk (C) 2004
 */
class InstallProgressDialog extends Dialog
{
	private Button cancelButton;
	private Font font;
	private boolean response = true;
	private Label progressLabel;
	private Button continueButton;

	/**
	 * Create a dialog that will display the message in a textArea.
	 */
	public InstallProgressDialog(Frame frame)
	{
		super( frame, "Installation Progress", true );
		setLayout( new GridLayout( 0, 1 ) );

		font = new Font( "Dialog", Font.BOLD, 16 );
		setFont( font );

		progressLabel = new Label(
				"--------- out of --------- bytes downloaded.", Label.CENTER );
		add( progressLabel );

		Panel panel = new Panel();
		add( panel );

		panel.add( cancelButton = new Button( "Cancel" ) );
		cancelButton.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				response = false;
				dispose();
			}
		} );

		panel.add( continueButton = new Button( "Continue" ) );
		continueButton.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				response = true;
				dispose();
			}
		} );
		continueButton.setEnabled( false );
		pack();

		// center on screen
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int w = bounds().width;
		int h = bounds().height;
		int x = (screenSize.width - w) / 2;
		int y = (screenSize.height - h) / 2;
		setLocation( x, y );
	}

	/**
	 * @return false if cancelled.
	 */
	public boolean ask()
	{
		show();
		return response;
	}

	/**
	 * @param bytesDownloaded
	 * @param totalBytes
	 */
	public void showProgress( int bytesDownloaded, int totalBytes )
	{
		String msg;
		if( bytesDownloaded >= totalBytes )
		{
			cancelButton.setEnabled( false );
			continueButton.setEnabled( true );
			msg = "Installation successful.";
		}
		else
		{
			int kdb = bytesDownloaded >> 10;
			int ktb = totalBytes >> 10;
			msg = kdb + " KB out of " + ktb + " KB downloaded.";
		}
		progressLabel.setText( msg );

	}
}