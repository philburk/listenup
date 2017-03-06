package com.softsynth.javasonics.installer;

import java.awt.Button;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Ask user whether it is OK to install the required code.
 * @author Phil Burk (C) 2004
 */
class AskToInstallDialog extends Dialog
{
	private Button installButton;
	private Button cancelButton;
	private Font font;
	private boolean response = false;
	
	/**
	 * Create a dialog that will display the message and ask Install or Cancel.
	 */
	public AskToInstallDialog(Frame frame, String libName, String dirName, String message )
	{
		super( frame, "Permission to Install", true );
		setLayout( new GridLayout( 0, 1 ) );

		font = new Font( "Dialog", Font.BOLD, 16 );
		setFont( font );
		
		String message1 = "The native code \"" + libName
		+ "\" is required for this application.";

		add( new Label( message1, Label.CENTER ) );
		add( new Label( message, Label.CENTER ) );

		Panel panel = new Panel();
		add( panel );
		panel.add( installButton = new Button( "Install" ) );
		installButton.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				response = true;
				dispose();
			}
		} );
		panel.add( cancelButton = new Button( "Cancel" ) );
		cancelButton.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				response = false;
				dispose();
			}
		} );

		pack();

		// center on screen
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int w = bounds().width;
		int h = bounds().height;
		int x = (screenSize.width - w) / 2;
		int y = (screenSize.height - h) / 2;
		setLocation( x, y );
	}

	public boolean ask()
	{
		show();
		return response;
	}
}