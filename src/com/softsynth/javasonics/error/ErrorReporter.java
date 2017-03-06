package com.softsynth.javasonics.error;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.softsynth.javasonics.util.MD5;
//import com.softsynth.javasonics.recplay.PlayerApplet;
import com.softsynth.upload.DiagnosticStatusUploader;

/**
 * Show Exceptions
 * 
 * @author (C) 2001 Phil Burk, SoftSynth.com, All Rights Reserved
 */

public class ErrorReporter extends JDialog implements BugReportable
{
	private static final String TEXT_PLEASE_WAIT = "Please wait a few seconds while the report is uploaded to the web server.";
	private static final String TEXT_GATHERED_INTO_A_REPORT = "A report was created but has not yet been sent.";
	private static final String TEXT_WILL_BE_SENT = "This information will be sent to us to help us debug the problem.";
	private static final String TEXT_THANK_YOU = "Sending report...";
	private static final String TEXT_REPORT_PROBLEM = "Report Problem...";

	private static final String UPLOAD_COREDUMP_URL = "http://www.javasonics.com/diagnostics/handle_upload_coredump.php";
	private static final String TIME_SALT = "bTw9ZakN72";

	private JButton okButton;
	private JButton detailButton;
	private JTextArea textArea;
	private Throwable aThrowable;
	private static String extraMessage;
	private static boolean showAlerts = true;
	private BugReportable bugReportable = this;
	private JTextField nameField;
	private JTextField emailField;
	private JTextArea infoArea;
	private JPanel userInfoPanel;
	private JPanel previousPanel;
	private JPanel sendingPanel;
	private CoreDump coreDump;
	private long timestamp = System.currentTimeMillis();

	public ErrorReporter(String title)
	{
		this( title, false );
	}

	/**
	 * Create a dialog that will display the message in a JTextArea.
	 */
	public ErrorReporter(String title, boolean modal)
	{
		super( new JFrame(), title, modal );
		setLayout( new BorderLayout() );

		switchToPanel( createInitialPanel() );

		// Close the window if user clicks close box.
		this.addWindowListener( new WindowAdapter()
		{
			public void windowClosing( WindowEvent e )
			{
				dispose();
			}
		} );
	}

	private JPanel createInitialPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BorderLayout() );

		textArea = new JTextArea( 20, 80 );
		JScrollPane sp = new JScrollPane( textArea );
		panel.add( "Center", sp );

		JPanel buttonBar = new JPanel();
		panel.add( "South", buttonBar );

		buttonBar.add( okButton = new JButton( "OK" ) );
		okButton.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				dispose();
			}
		} );
		
		buttonBar.add( detailButton = new JButton( TEXT_REPORT_PROBLEM ) );
		detailButton.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				if( userInfoPanel == null )
				{
						userInfoPanel = createUserInfoPanel();
				}
				switchToPanel( userInfoPanel );
				detailButton.setEnabled( false );
			}
		} );
		
		return panel;
	}


	private void switchToPanel( JPanel panel )
	{
		if( previousPanel != null )
		{
			remove( previousPanel );
		}
		add( panel, BorderLayout.CENTER );
		updateSize();
		previousPanel = panel;
	}

	private JPanel createUserInfoPanel()
	{
		JPanel userPanel = new JPanel();
		userPanel.setLayout( new BoxLayout( userPanel, BoxLayout.Y_AXIS ) );

		userPanel.add( createLeftLabel( TEXT_GATHERED_INTO_A_REPORT ) );
		userPanel.add( createLeftLabel( "Your name: (OPTIONAL)" ) );
		userPanel.add( nameField = new JTextField() );
		userPanel.add( createLeftLabel( "Your email address: (OPTIONAL, Enter if you want a reply.)" ) );
		userPanel.add( emailField = new JTextField() );
		userPanel
				.add( createLeftLabel( "Other information that might help us solve the problem: (OPTIONAL)" ) );
		infoArea = new JTextArea( 20, 60 );
		JScrollPane scrollPane = new JScrollPane( infoArea );
		userPanel.add( scrollPane );

		JPanel buttonBar = new JPanel();

		JButton viewButton;
		buttonBar.add( viewButton = new JButton( "View Report..." ) );
		viewButton.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				try
				{
					coreDump = generateCoreDump();
					showViewingPanel();
				} catch( IOException e1 )
				{
					println( "Error generating report: " + errToString(e1) );
				}
			}

		} );

		buttonBar.add( createSendButton() );
		buttonBar.add( createCancelButton() );
		userPanel.add( buttonBar );

		return userPanel;
	}

	private Component createLeftLabel( String string )
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BorderLayout() );
		JLabel label = new JLabel( string );
		label.setAlignmentX( Component.LEFT_ALIGNMENT );
		panel.add( label );
		return panel;
	}

	private JButton createSendButton()
	{
		JButton sendButton = new JButton( "Send Report" );
		sendButton.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				try
				{
					coreDump = generateCoreDump();
					showSendingPanel();
				} catch( IOException e1 )
				{
					println( "Error sending report: " + errToString(e1) );
				}
			}

		} );
		return sendButton;
	}

	private JButton createCancelButton()
	{
		JButton cancelButton = new JButton( "Cancel" );
		cancelButton.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				dispose();
			}
		} );
		return cancelButton;
	}

	public void dispose()
	{
		textArea = null;
		super.dispose();
	}

	protected void showViewingPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BorderLayout() );

		panel.add( new JLabel( TEXT_WILL_BE_SENT ), BorderLayout.NORTH );

		JTextArea textArea = new JTextArea( 30, 80 );
		JScrollPane sp = new JScrollPane( textArea );
		panel.add( sp, BorderLayout.CENTER );

		textArea.append( coreDump.toString() );

		JPanel buttonBar = new JPanel();
		panel.add( buttonBar, BorderLayout.SOUTH );

		JButton backButton;
		buttonBar.add( backButton = new JButton( "<<<Back" ) );
		backButton.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				switchToPanel( userInfoPanel );
			}
		} );
		buttonBar.add( createSendButton() );
		buttonBar.add( createCancelButton() );

		switchToPanel( panel );
	}

	protected void showSendingPanel() throws IOException
	{
		if( sendingPanel == null )
		{
			sendingPanel = createSendingPanel();
		}
		switchToPanel( sendingPanel );
		sendCoreDump( coreDump );
	}
	
	private String errToString( Throwable thr )
	{
		OutputStream bytes = new ByteArrayOutputStream();
		PrintStream errOut = new PrintStream( bytes );
		thr.printStackTrace( errOut );
		errOut.close();
		return bytes.toString();
	}

	private JPanel createSendingPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BorderLayout() );

		panel.add( new JLabel( TEXT_THANK_YOU ), BorderLayout.NORTH );

		textArea = new JTextArea( 30, 80 );
		JScrollPane sp = new JScrollPane( textArea );
		panel.add( sp, BorderLayout.CENTER );

		JPanel buttonBar = new JPanel();
		panel.add( buttonBar, BorderLayout.SOUTH );

		buttonBar.add( okButton );
		return panel;
	}

	private void sendCoreDump( CoreDump coreDump ) throws IOException
	{
		DiagnosticStatusUploader uploader = new DiagnosticStatusUploader(
				getBugReportURL() )
		{
			public void printResponseLine( String line )
			{
				ErrorReporter.this.println( line );
			}
		};

		uploader.addNameValuePair( "timestamp", "" + timestamp );
		uploader.addNameValuePair( "salted", MD5.toHash( timestamp + TIME_SALT )
				.toLowerCase() );

		uploader.addSystemInfo();

		uploader.addNameValuePair( "type", "coredump" );

		String userEmail = emailField.getText().trim();
		uploader.addNameValuePair( "useremail", userEmail );

		byte[] image = coreDump.getBytes();
		InputStream inStream = (InputStream) (new ByteArrayInputStream( image ));
		uploader.addFile( "coredump", "text/xml", inStream );

		println( TEXT_PLEASE_WAIT );
		uploader.dispatch();
	}

	private CoreDump generateCoreDump() throws IOException
	{
		CoreDump coreDump = bugReportable.createCoreDump( timestamp );
		if( aThrowable != null )
		{
			coreDump.addThrowable( aThrowable );
		}

		String userName = nameField.getText().trim();
		coreDump.writeTag( "username", userName );

		String userEmail = emailField.getText().trim();
		coreDump.writeTag( "useremail", userEmail );

		String userMessage = infoArea.getText().trim();
		coreDump.writeTag( "usermessage", userMessage );

		coreDump.close();

		return coreDump;
	}

	private void updateSize()
	{
		validate();
		pack();

		// center on screen
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int w = getWidth();
		int h = getHeight();
		int x = (screenSize.width - w) / 2;
		int y = (screenSize.height - h) / 2;
		setLocation( x, y );
	}

	public void println( String message )
	{
		JTextArea temp = textArea; // unsynchronized
		if( temp != null )
		{
			temp.append( message );
			println();
		}
		else
		{
			System.out.println( message );
		}
	}

	public void println()
	{
		JTextArea temp = textArea; // unsynchronized
		if( temp != null )
		{
			temp.append( "\n" );
			temp.setCaretPosition( textArea.getDocument().getLength() );
		}
		else
		{
			System.out.println();
		}
	}

	public URL getBugReportURL()
	{
		try
		{
			return new URL( UPLOAD_COREDUMP_URL );
		} catch( MalformedURLException e )
		{
			return null;
		}
	}

	public CoreDump createCoreDump( long timestamp )
	{
		CoreDump coreDump = new CoreDump( timestamp );
		coreDump.open();
//		coreDump.addApplication( "ListenUp", PlayerApplet.VERSION_NUMBER,
//				PlayerApplet.BUILD_NUMBER );
		coreDump.addCommon();
		return coreDump;
	}

	public void showThrowable( Throwable thr )
	{
		aThrowable = thr;
		if( extraMessage != null )
		{
			println( extraMessage );
		}
		if( isUserProblem() )
		{
			println( "Local Error: " + thr.getMessage() );
			System.err.println( "ListenUp error: " + aThrowable );
		}
		else
		{
			println( "Error: " + thr );
			thr.printStackTrace();
		}
		Throwable cause = aThrowable.getCause();
		if( cause != null )
		{
			println( "Cause: " + cause );
		}

		if( isUserProblem() )
		{
			println( "----------------------------------" );
			println( "We think this may be a problem that you can solve." );
			println( "If you cannot solve the problem then please report by clicking \""
					+ TEXT_REPORT_PROBLEM + "\" below" );
			println( "or ask for help from the technical support folks for this website." );
			println( "----------------------------------" );
		}
		else if( aThrowable instanceof WebDeveloperRuntimeException )
		{
			println( "----------------------------------" );
			println( "This is probably due to a problem on the website." );
			println( "Please email this entire message to the technical support folks for this website." );

			println();
			println( "----------- System Details ---------------------" );
			if( extraMessage != null )
			{
				println( extraMessage );
			}
			println( "java.version = " + System.getProperty( "java.version" ) );
			println( "java.vendor = " + System.getProperty( "java.vendor" ) );
			println( "os.arch = " + System.getProperty( "os.arch" ) );
			println( "os.name = " + System.getProperty( "os.name" ) );
			println( "os.version = " + System.getProperty( "os.version" ) );
			println( "------------------------------------------------" );
			
			detailButton.setEnabled( false );
		}

		updateSize();
		setVisible( true );
	}

	private boolean isUserProblem()
	{
		return (aThrowable instanceof UserException)
				|| (aThrowable instanceof UserRuntimeException);
	}

	public static void show( Throwable thr )
	{
		if( showAlerts )
		{
			ErrorReporter dlg = new ErrorReporter( "JavaSonics Alert" );
			dlg.showThrowable( thr );
		}
	}

	public static void show( String message, Throwable thr )
	{
		if( showAlerts )
		{
			ErrorReporter dlg = new ErrorReporter( "JavaSonics Alert" );
			dlg.println( message );
			dlg.showThrowable( thr );
		}
	}

	/**
	 * @return Returns the extraMessage.
	 */
	public static String getExtraMessage()
	{
		return extraMessage;
	}

	/**
	 * @param extraMessage
	 *            The extraMessage to set.
	 */
	public static void setExtraMessage( String pExtraMessage )
	{
		extraMessage = pExtraMessage;
	}

	public BugReportable getBugReportable()
	{
		return bugReportable;
	}

	public void setBugReportable( BugReportable bugReportable )
	{
		this.bugReportable = bugReportable;
		detailButton.setEnabled( bugReportable != null );
	}

	/**
	 * @return the showAlerts
	 */
	public static boolean isShowAlerts()
	{
		return showAlerts;
	}

	/**
	 * @param showAlerts
	 *            Set to false to disable the popup error messages.
	 */
	public static void setShowAlerts( boolean showAlerts )
	{
		ErrorReporter.showAlerts = showAlerts;
	}
}