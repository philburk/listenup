package com.softsynth.upload;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.softsynth.javasonics.error.WebDeveloperRuntimeException;
import com.softsynth.javasonics.util.JavaScriptInterpreter;
import com.softsynth.javasonics.util.Logger;

class UploadField extends Panel
{
	String name;
	private String defaultText;
	private TextField textField;
	private TextArea textArea;
	private int minWidth = 1;

	public UploadField(String name, String label, String defaultText,
			int numRows)
	{
		Label textLabel;
		setLayout( new BorderLayout() );
		this.name = name;
		this.defaultText = defaultText;
		if( numRows >= 1 )
		{
			textLabel = new Label( label, Label.RIGHT )
			{
				public Dimension getPreferredSize()
				{
					Dimension original = super.getPreferredSize();
					if( original.width < minWidth )
						return new Dimension( minWidth, 1 );
					else
						return original;
				}

				public Dimension getMinimumSize()
				{
					Dimension original = super.getMinimumSize();
					if( original.width < minWidth )
						return new Dimension( minWidth, 1 );
					else
						return original;
				}
			};
			add( "West", textLabel );
			if( numRows == 1 )
			{
				add( "Center", textField = new TextField( defaultText ) );
			}
			else
			{
				add( "Center", textArea = new TextArea( defaultText, numRows,
						0, TextArea.SCROLLBARS_VERTICAL_ONLY ) );
			}
		}
	}

	String getText()
	{
		if( textField != null )
		{
			return textField.getText();
		}
		else if( textArea != null )
		{
			return textArea.getText();
		}
		else
			return defaultText;
	}

	/**
	 * @return
	 */
	public int getMinWidth()
	{
		return minWidth;
	}

	/**
	 * @param w
	 *            minimum width of label
	 */
	public void setMinWidth( int w )
	{
		minWidth = w;
	}

	public void setText( String value )
	{
		textField.setText( value );
	}

}

/**
 * Present a GUI to the user. Upload a file to the server.
 * 
 * @author (C) Phil Burk, http://www.softsynth.com/
 */
public abstract class FileUploader
{
	private Panel lastPanel;
	private Label statusLabel;
	private Button uploadButton;
	private URL uploadURL;
	private boolean go = true;
	private Vector uploadFields;
	private Hashtable nameValuePairs;
	private Hashtable uploadHeaderPairs;
	private Vector uploadListeners;
	private String sendText;
	private final static String CANCEL_TEXT = "Cancel";
	private int minLabelWidth;
	private JavaScriptInterpreter javaScriptInterpreter;

	private String userName = null;
	private String password = null;
	private boolean useURLConnection = false;

	/**
	 * Construct an uploader.
	 * 
	 * @param uploadURL
	 *            URL for a CGI script that will accept a file in
	 *            multipart/form-data.
	 */
	public FileUploader(URL uploadURL)
	{
		this.uploadURL = uploadURL;
		URLConnection.setDefaultAllowUserInteraction( true );
		uploadFields = new Vector();
		nameValuePairs = new Hashtable();
		uploadHeaderPairs = new Hashtable();
	}

	public void addRack( Component rack )
	{
		Panel panel = new Panel( new BorderLayout() );
		lastPanel.add( "Center", panel );
		panel.add( "North", rack );
		lastPanel = panel;
		panel.invalidate();
	}

	private void setSendText( String pSendText )
	{
		// Make Send button have extra space so the Cancel text will fit.
		while( pSendText.length() < 8 )
		{
			// Don't use StringBuffer because of change in thrown exceptions.
			pSendText = " " + pSendText + " ";
		}
		sendText = pSendText;
	}

	/**
	 * Return a Panel containing buttons to upload a file, and TextFields for
	 * the optional parameters.
	 * 
	 * @param buttonBackground
	 * @param sendButtonText
	 * @param showSendButton
	 */
	public Panel setupGUI( boolean showSendButton, String pSendText,
			Color buttonBackground )
	{
		setSendText( pSendText );

		Panel panel = lastPanel = new Panel();
		lastPanel.setLayout( new BorderLayout() );

		// Status display
		Panel statusPanel = new Panel();
		addRack( statusPanel );
		statusPanel.setLayout( new BorderLayout() );

		statusLabel = new Label( "    ", Label.LEFT );
		statusPanel.add( "Center", statusLabel );

		// Buttons to send and cancel
		Panel eastPanel = new Panel();
		statusPanel.add( "East", eastPanel );

		if( showSendButton )
		{
			uploadButton = new Button( sendText );
			uploadButton.setBackground( buttonBackground );
			eastPanel.add( uploadButton );

			uploadButton.addActionListener( new ActionListener()
			{
				public void actionPerformed( ActionEvent e )
				{
					if( uploadButton.getLabel().equals( sendText ) )
					{
						sendButtonPressed();
					}
					else
					{
						cancel();
					}
				}
			} );
		}

		return panel;
	}

	public abstract void sendButtonPressed();

	public boolean isGoing()
	{
		return go;
	}

	public void cancel()
	{
		go = false;
	}

	/** Add a text entry field to the GUI that will get uploaded with the file. */
	public void addField( String name, String label, String defaultText,
			int numRows )
	{
		UploadField temp = new UploadField( name, label, defaultText, numRows );
		temp.setMinWidth( minLabelWidth );
		uploadFields.addElement( temp );
		addRack( temp );
	}

	public void setFieldValue( String name, String value )
	{
		Enumeration fields = uploadFields.elements();
		while( fields.hasMoreElements() )
		{
			UploadField uf = (UploadField) fields.nextElement();
			if( uf.name.equals( name ) )
			{
				uf.setText( value );
				break;
			}
		}
	}

	/** Display message in Label. */
	public void displayMessage( String msg )
	{
		statusLabel.setText( msg );
	}

	class CustomProgressListener implements ProgressListener
	{
		String msg;

		CustomProgressListener(String msg)
		{
			this.msg = msg;
		}

		/**
		 * Called by uploader to report progress. Required to implement
		 * UploadProgressListener.
		 * 
		 * @return true if upload should continue, false if it should abort.
		 */
		public boolean progressMade( int numSoFar, int numTotal )
		{
			// Cast intermediate value to long to avoid overflow.
			int percent = (int) ((100 * (long) numSoFar) / numTotal);
			String text = msg + percent + "% complete.";
			displayMessage( text );
			return go;
		}
	}

	public ProgressListener createProgressListener( String msg )
	{
		return new CustomProgressListener( msg );
	}

	/**
	 * Invoke a CGI script on the server to get a unique filename for the
	 * uploaded file. CGI script must return two lines of text.
	 * <P>
	 * 
	 * <pre>
	 *  Line 1:  SUCCESS or an error message.
	 *  Line 2:  {unique filename}
	 * </pre>
	 * 
	 * @param getNameURL
	 *            URL for a CGI script that will return a unique name for the
	 *            uploaded file.
	 */
	public String getFileNameFromServer( URL getNameURL ) throws IOException
	{
		String fileName = null;

		// Make the connection using HTTP GET and read the text response.
		URLConnection connection = getNameURL.openConnection();
		connection.setUseCaches( false );
		InputStream stream = connection.getInputStream();
		BufferedReader reader = new BufferedReader( new InputStreamReader(
				stream ) );

		// This simple protocol is specific to this PHP script.
		// Check to see if the first line is SUCCESS which means the server
		// script got an ID.
		String line = reader.readLine();
		if( line.startsWith( "SUCCESS" ) )
		{
			// Next line is the fileName.
			fileName = reader.readLine();
			displayMessage( "Got unique fileName = " + fileName );
		}
		else
		{
			displayMessage( "Error = " + line );
		}
		// Echo remainder of text from script for debugging.
		while( (line = reader.readLine()) != null )
		{
			Logger.println( 0, line );
		}

		stream.close();
		return fileName;
	}

	private void sendFields( MultipartFormDataUploader uploader )
			throws IOException
	{
		Enumeration fields = uploadFields.elements();
		while( fields.hasMoreElements() )
		{
			UploadField uf = (UploadField) fields.nextElement();
			uploader.addFormData( uf.name, uf.getText() );
		}
	}

	/** Set list of name/value pairs to be uploaded with next sound file. */
	public void setNameValuePairs( Hashtable nameValuePairs )
	{
		this.nameValuePairs = nameValuePairs;
	}

	public void setUploadHeaderPairs( Hashtable uploadHeaderPairs )
	{
		this.uploadHeaderPairs = uploadHeaderPairs;
	}

	/**
	 * Set button to "Cancel" if not queued.
	 * 
	 * @param queued
	 */
	public void beginTransaction( boolean runInBackground )
	{
		go = true;
		if( !runInBackground && (uploadButton != null) )
		{
			uploadButton.setLabel( CANCEL_TEXT );
		}
	}

	public void finishTransaction()
	{
		go = false;
		if( uploadButton != null )
		{
			uploadButton.setLabel( sendText );
		}
	}

	/** @return true if uploaded successfully */
	public boolean uploadFileImage( String fileName, String MIMEType,
			InputStream inStream ) throws IOException
	{
		boolean success = false;
		String errorMessage = null;
		String completionMessage = "No result code from upload script.";

		Logger.println( "uploadFileImage: ----------- " + fileName );

		// Connect to URL and send first part of MIME data.
		MultipartFormDataUploader uploader = MultipartFormDataUploader
				.createUploader( uploadURL, useURLConnection );

		uploader.setUserName( userName );
		uploader.setPassword( password );

		uploader.open();

		// Send contents of custom fields.
		sendFields( uploader );

		// Send name value pairs.
		if( nameValuePairs != null )
		{
			Enumeration keys = nameValuePairs.keys();
			while( keys.hasMoreElements() )
			{
				String name = (String) keys.nextElement();
				String value = (String) nameValuePairs.get( name );
				uploader.addFormData( name, value );
			}
			nameValuePairs = null;
		}

		uploader.setUploadHeaderPairs( uploadHeaderPairs );

		// Send file if present.
		if( inStream != null )
		{
			uploader.addFile( "userfile", fileName, MIMEType, inStream );
		}

		if( go )
		{
			displayMessage( "Wait to finish uploading." );
		}
		else
		{
			displayMessage( "Upload cancelled." );
		}

		// finish building upload image
		uploader.close();
		// transmit the formatted image to the server
		uploader.upload( 1024, createProgressListener( "Uploading: " ) );

		if( go )
		{
			displayMessage( "Waiting for response code from server." );
			completionMessage = "Uploaded but no response code.";

			// Allow uploadListeners to tee off the response from the server.
			// This is mostly used for unit testing.
			InputStream responseStream = uploader.getInputStream();
			Enumeration listeners = uploadListeners.elements();
			while( listeners.hasMoreElements() )
			{
				UploadListener listener = (UploadListener) listeners
						.nextElement();
				responseStream = listener.filterInputStream( responseStream );
			}

			BufferedReader in = uploader
					.setupReaderAndParseHeader( responseStream );

			// Print the servers response for debugging.
			int responseCode = uploader.getResponseCode();
			int responseType = responseCode / 100;
			String responseText = responseCode + " "
					+ uploader.getResponseMessage();

			if( responseCode == 0 )
			{
				completionMessage = errorMessage = "HTTP response was empty.";
			}
			else if( responseType == 3 )
			{
				completionMessage = errorMessage = "HTTP redirection not supported: "
						+ responseText
						+ "\n"
						+ "Try useURLConnection parameter.";
			}
			// FIXME - should we trap this?
			// else if( responseType == 1 )
			// {
			// completionMessage = errorMessage =
			// "HTTP CONTINUE not supported: " + responseText;
			// }
			else if( responseCode == 404 )
			{
				completionMessage = errorMessage = "HTTP error: "
						+ responseText
						+ "\n    The uploadURL is probably incorrect = "
						+ uploadURL;
			}
			else if( responseCode == 405 )
			{
				completionMessage = errorMessage = "HTTP error: "
						+ responseText + "\n    POST method not allowed to "
						+ uploadURL;
			}
			else if( responseType >= 4 )
			{
				completionMessage = errorMessage = "HTTP error: "
						+ responseText;
			}
			else
			// Must be 2xx which is good. May also be 1xx which is puzzling.
			{
				completionMessage = "Response code " + responseCode + " but no SUCCESS or ERROR!";
				
				String inputLine;
				// Process lines from server.
				while( (inputLine = in.readLine()) != null )
				{
					// Print output for debugging.
					Logger.println( 0, inputLine );

					// Make comparison case insensitive.
					String upperLine = inputLine.toUpperCase();
					// Handle commands and response codes sent from server.
					if( upperLine.startsWith( "SUCCESS" )
							|| upperLine.startsWith( "WARNING" ) )
					{
						completionMessage = inputLine;
						success = true;
					}
					else if( upperLine.startsWith( "ERROR" ) )
					{
						completionMessage = errorMessage = inputLine;
					}
					else if( upperLine.startsWith( "CALLJS " ) )
					{
						if( javaScriptInterpreter != null )
						{
							String command = inputLine.substring( 7 );
							javaScriptInterpreter
									.executeJavaScriptCommand( command );
						}
					}

				}
			}
			in.close();

			listeners = uploadListeners.elements();
			while( listeners.hasMoreElements() )
			{
				UploadListener listener = (UploadListener) listeners
						.nextElement();
				listener.uploadComplete();
			}
		}
		else
		{
			completionMessage = "Upload cancelled.";
		}

		displayMessage( completionMessage );

		Logger.println( "uploadFileImage: ----------- " + completionMessage );

		if( errorMessage != null )
		{
			throw new WebDeveloperRuntimeException( errorMessage );
		}

		return success;
	}

	/**
	 * @param sendable
	 */
	public void setEnabled( boolean sendable )
	{
		// System.out.println("FileUploader.setEnabled(), sendable = " +
		// sendable );
		if( uploadButton != null )
		{
			uploadButton.setEnabled( sendable );
		}
	}

	/**
	 * @return
	 */
	public int getMinLabelWidth()
	{
		return minLabelWidth;
	}

	/**
	 * Set this so that fields line up vertically.
	 * 
	 * @param w
	 *            minimum width of label
	 */
	public void setMinLabelWidth( int w )
	{
		minLabelWidth = w;
	}

	/**
	 * @return
	 */
	public String getPassword()
	{
		return password;
	}

	/**
	 * @return
	 */
	public String getUserName()
	{
		return userName;
	}

	/**
	 * @param string
	 */
	public void setPassword( String string )
	{
		password = string;
	}

	/**
	 * @param string
	 */
	public void setUserName( String string )
	{
		userName = string;
	}

	public void setUseURLConnection( boolean useURLConnection )
	{
		this.useURLConnection = useURLConnection;

	}

	/**
	 * @return Returns the javaScriptInterpreter.
	 */
	public JavaScriptInterpreter getJavaScriptInterpreter()
	{
		return javaScriptInterpreter;
	}

	/**
	 * @param javaScriptInterpreter
	 *            The javaScriptInterpreter to set.
	 */
	public void setJavaScriptInterpreter(
			JavaScriptInterpreter javaScriptInterpreter )
	{
		this.javaScriptInterpreter = javaScriptInterpreter;
	}

	public void setUploadListeners( Vector uploadListeners )
	{
		this.uploadListeners = uploadListeners;
	}

}
