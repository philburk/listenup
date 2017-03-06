package com.softsynth.javasonics.recplay;

import java.awt.*;
import java.awt.event.*;

/**
 * Simple announcement Dialog.
 * 
 * @author (C) 2001 Phil Burk, SoftSynth.com, All Rights Reserved
 */

public class SimpleDialog extends Dialog
{
	private Button button0;
	private Font font;

	/**
	 * Create a dialog that will display the message in a textArea.
	 */
	public SimpleDialog(String title, String message)
	{
		super( new Frame(), title, true );
		setLayout( new BorderLayout() );
		// Close the window if user clicks close box.
		this.addWindowListener( new WindowAdapter(){
			public void windowClosing( WindowEvent e )
			{
				dispose();
			}
		} );

		font = new Font( "Dialog", Font.BOLD, 18 );
		setFont( font );

		add( "Center", new TextArea( message ) );

		Panel panel = new Panel();
		add( "South", panel );
		panel.add( button0 = new Button( "OK" ) );
		button0.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
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

	public static void alert( String message )
	{
		SimpleDialog dlg = new SimpleDialog( "JavaSonics Alert", message + "\n"
				+ PlayerApplet.getVersionText() );
		dlg.show();
	}
}