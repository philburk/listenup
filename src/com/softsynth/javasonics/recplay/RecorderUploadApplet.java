package com.softsynth.javasonics.recplay;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Panel;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.softsynth.javasonics.DeviceUnavailableException;
import com.softsynth.javasonics.error.WebDeveloperRuntimeException;
import com.softsynth.javasonics.util.Logger;
import com.softsynth.javasonics.util.WAVWriter;
import com.softsynth.storage.DynamicBuffer;
import com.softsynth.upload.FileUploader;
import com.softsynth.upload.ProgressListener;
import com.softsynth.upload.UploadListener;

/**
 * Extend the RecorderApplet to allow uploading of recorded messages. The name
 * of the uploaded file must be provided one of three ways:
 * <ul>
 * <li>Get name from server script whose URL is specified using "getNameURL"
 * parameter.
 * <li>Pass filename directly using "uploadFileName" parameter.
 * <li>Pass from JavaScript using "uploadFileName" parameter.
 * </ul>
 * <p>
 * Additional Applet parameters for uploading a file:
 * <p>
 * uploadFileName = name for the uploaded file once it arrives on the server.
 * [optional]
 * <p>
 * getNameURL = URL of CGI script to get a unique file name for the uploaded
 * file. [optional]
 * <p>
 * uploadURL = URL of CGI script to receive the multi-part MIMI form. [required]
 * <p>
 * refreshURL = URL of a web page to be displayed when upload is finished.
 * [optional]
 * <p>
 * refreshTarget = Name of a frame on refreshURL. [optional]
 * <p>
 * fieldName_1 = name of first field variable to be sent to the server, may be
 * empty string to continue scan.<br>
 * fieldLabel_1 = text used to label the field.<br>
 * fieldDefault_1 = initial text value.<br>
 * fieldRows_1 = number of rows of text, default is one, zero means hide the
 * field.<br>
 * <p>
 *
 * @author (C) 2002 Phil Burk, http://www.softsynth.com
 */
public final class RecorderUploadApplet extends RecorderApplet implements
		Sendable
{
	/**
	 *
	 */
	private static final long serialVersionUID = 1113231869510232577L;
	// The following variable are settable using Applet parameters.
	private boolean showSendButton = true;
	private boolean showUploader = true;
	private boolean stashBeforeSend = false;
	private double speexQuality = JSpeexEncoder.DEFAULT_QUALITY;
	private int speexComplexity = JSpeexEncoder.DEFAULT_COMPLEXITY;
	/** PHP script that gets unique file name. */
	private String getNameUrlName;
	private String uploadFileName;
	/** PHP script that enters uploaded file into the database. */
	private String uploadUrlName;
	private URL refreshUrl;
	private String refreshTarget;
	private String uploadCompletionScript = null;
	private String uploadFailureScript = null;

	// MOD 20060410 nd, option to use URLConnection vs. socket
	private boolean useURLConnection = false;
	private String sendButtonText = "Send";

	/** If true the periodically POST uploads to the server as a kind of stream. */
	private boolean streamUploads = false;
	private boolean queueUploads = false;

	private Hashtable nameValuePairs;

	private Hashtable uploadHeaderPairs;

	private Vector uploadListeners;

	// Set indirectly based on Applet parameters.
	private URL getNameURL;
	private URL uploadURL;


	private Vector uploadFields;
	private int minFieldLabelWidth = 80;
	private FileUploader fileUploader;
	private QueueUploaderThread uploadThread;

	private int previousState = Recorder.STOPPED;

	private File userHomeDir = null;
	private boolean userHomeDirFailed = false;

	private boolean uploadInProgress;

	private boolean sendOnEOL;

	private String uniqueId;

	/** This will get incremented each time a recording is uploaded. */
	private int messageIndex;
	private boolean eraseAfterSend = true;

	public String getUniqueId()
	{
		return uniqueId;
	}

	public void setUniqueId( String uniqueId )
	{
		this.uniqueId = uniqueId;
	}

	@Override
	public final boolean init1() throws Exception
	{
		uniqueId = generateUniqueId();

		try
		{
			userHomeDir = new File( System.getProperty( "user.home" ) );
		} catch( Exception e )
		{
			// Only print message once.
			if( !userHomeDirFailed )
			{
				Logger.println( 0, "Attempt to get user.home failed. " + e );
				userHomeDirFailed = true;
			}
		}
		if( getFormat() == Recording.FORMAT_UNKNOWN )
		{
			setFormat( Recording.FORMAT_IMA_ADPCM );
		}

		uploadListeners = new Vector();

		return super.init1();
	}

	private String generateUniqueId()
	{
		StringBuffer buffer = new StringBuffer();
		for( int i=0; i<10; i++ )
		{
			char c = (char) ('a' + (Math.random() * ('z' - 'a')));
			buffer.append(c);
		}
		return buffer.toString();
	}


	@Override
	public final void init2()
	{
		super.init2();

		uploadFields = new Vector();
		try
		{
			getUploadParameters();
			getFieldParameters();
		} catch( NullPointerException e )
		{
			// Ignore - probably just because running as an application.
		}

		getNameURL = makeAbsoluteURL( getNameUrlName );

		if( uploadUrlName == null )
		{
			throw new WebDeveloperRuntimeException(
					"uploadURL was not specified" );
		}
		uploadURL = makeAbsoluteURL( uploadUrlName );
		if( uploadURL == null )
		{
			throw new WebDeveloperRuntimeException(
					"uploadURL param generated null URL: " + uploadUrlName );
		}
		// Logger.println( "call checkDomain()" );
		// FIXME getLicenseManager().checkDomain( uploadURL );

		Logger.println( "refreshURL = " + refreshUrl );
		Logger.println( "refreshTarget = " + refreshTarget );
		Logger.println( "uploadURL = " + uploadURL );
		Logger.println( "uploadURL port = " + uploadURL.getPort() );
		Logger.println( "getNameUrl = " + getNameURL );
	}

	/** Make a GUI for uploading files. */
	@Override
	protected void addUploadGUI()
	{
		// Prevent GUI setup if cgiUploadUrl not licensed.
		// FIXME getLicenseManager().checkDomain( uploadURL );
		setupUploader();
		Panel uploadPanel = fileUploader.setupGUI( showSendButton,
				sendButtonText, getButtonBackground() );
		if( showUploader )
		{
			addSouthRack( uploadPanel );
		}
		String msg = "Status: ____";
/* FIXME
 		if( getLicenseManager().checkPermission( "test" )

				|| EXPIRE_AFTER_TEST_PERIOD )
		{
			msg = "TEST: V" + VERSION_NUMBER + " (" + BUILD_NUMBER + ")";
		}

		// Show licensed domains when on LAN to shame people against stealing
		// WAN licenses.
		else if( isLicenseValid() && getLicenseManager().isOnLAN() )
		{
			msg = "For: " + getLicenseManager().getDomains();
		}
*/
		displayMessage( msg );
	}


	@Override
	protected void addTextFieldGUI()
	{
		Enumeration fields = uploadFields.elements();
		while( fields.hasMoreElements() )
		{
			UploadFieldDescriptor ufd = (UploadFieldDescriptor) fields
					.nextElement();
			fileUploader.addField( ufd.name, ufd.label, ufd.defaultText,
					ufd.numRows );
		}
	}

	/** Get sequential fields from \<param\> tags. */

	void getFieldParameters()
	{
		minFieldLabelWidth = getIntegerParameter( "minFieldLabelWidth",
				minFieldLabelWidth );

		int i = 1;
		while( true )
		{
			String nameParam = getParameter( "fieldName_" + i );
			if( nameParam == null )
				break;
			if( nameParam.length() > 0 )
			{
				UploadFieldDescriptor ufd = new UploadFieldDescriptor(
						nameParam );
				String temp = getParameter( "fieldLabel_" + i );
				if( temp != null )
					ufd.label = temp;
				temp = getParameter( "fieldDefault_" + i );
				if( temp != null )
					ufd.defaultText = temp;
				try
				{
					temp = getParameter( "fieldRows_" + i );
					if( temp != null )
					{
						ufd.numRows = Integer.parseInt( temp );
					}
					Logger.println( "Adding " + ufd );
					uploadFields.addElement( ufd );
				} catch( NumberFormatException e )
				{
					System.err.println( "fieldRows_" + i
							+ " Applet parameter illegal.\n" + e );
				}
			}
			i++;
		}
	}

	/**
	 * Get Applet parameters that define a web page to be refreshed when the
	 * file is uploaded.
	 */
	void getUploadParameters()
	{
		// Optional parameters.
		String urlParam = getParameter( "refreshURL" );
		if( urlParam != null )
		{
			refreshUrl = makeAbsoluteURL( urlParam );
		}

		refreshTarget = getParameter( "refreshTarget" );

		uploadFileName = getParameter( "uploadFileName" );

		String temp = getParameter( "getNameURL" );
		if( temp != null )
			getNameUrlName = temp;

		speexQuality = getDoubleParameter( "speexQuality", speexQuality );
		speexComplexity = getIntegerParameter( "speexComplexity",
				speexComplexity );

		// Specify whether to show the Status Messages.
		showUploader = getBooleanParameter( "showUploader", showUploader );

		// Specify whether to show the Send button.
		showSendButton = getBooleanParameter( "showSendButton", showSendButton );
		// Get text to use in the String button.
		temp = getParameter( "sendButtonText" );
		if( temp != null )
		{
			sendButtonText = temp;
		}

		temp = getParameter( "uniqueId" );
		if( temp != null )
		{
			uniqueId = temp;
		}

		stashBeforeSend = getBooleanParameter( "stashBeforeSend",
				stashBeforeSend );
		int stashMaxFiles = getIntegerParameter( "stashMaxFiles",
				StashedRecordingManager.getMaxFiles() );
		StashedRecordingManager.setMaxFiles( stashMaxFiles );

		// MOD 20060410 nd
		useURLConnection = getBooleanParameter( "useURLConnection",
				useURLConnection );

		queueUploads = getBooleanParameter( "queueUploads", queueUploads );
		streamUploads = getBooleanParameter( "streamUploads", streamUploads );
		if( streamUploads && isEditable() )
		{
			throw new WebDeveloperRuntimeException(
					"Cannot set both \"streamUploads\" and \"editable\" Applet parameters." );
		}
		if( streamUploads && (getFormat() != Recording.FORMAT_SPEEX) )
		{
			throw new WebDeveloperRuntimeException(
					"Must use \"speex\" format with \"streamUploads\"." );
		}

		sendOnEOL = getBooleanParameter( "sendOnEOL", sendOnEOL );

		eraseAfterSend = getBooleanParameter( "eraseAfterSend", eraseAfterSend );

		// Required parameters
		temp = getParameter( "uploadURL" );
		if( temp == null )
		{
			throw new WebDeveloperRuntimeException(
					"Required parameter uploadURL not specified." );
		}
		uploadUrlName = temp;

	}

	@Override
	public void displayMessage( String msg )
	{
		if( fileUploader != null )
		{
			fileUploader.displayMessage( msg );
		}
	}

	public void updateButtons( Player player )
	{
		if( transportControl != null )
		{
			transportControl.updateButtons();
		}

		boolean sendable = player.isStopped() && player.isPlayable();

		if( fileUploader != null )
		{
			fileUploader.setEnabled( sendable );
		}
	}

	/**
	 * Called by recorder when starting or stopping recording.
	 */
	@Override
	public synchronized void playerStateChanged( Player player, int state,
			Throwable thr )
	{
		updateButtons( player );
		if( state == Recorder.RECORDING )
		{
			displayMessage( "Recording" );
			try
			{
				showStatus( "Now RECORDING!" );
			} catch( NullPointerException e )
			{
			}
			uploadDiagnosticStatus( "record_started", false, null );
		}
		else if( previousState == Recorder.RECORDING )
		{
			displayMessage( "" );
			try
			{
				showStatus( "" );
			} catch( NullPointerException e )
			{
			}
			uploadDiagnosticStatus( "record_done", false, null );
		}
		previousState = state;
		super.playerStateChanged( player, state, thr );
	}

	@Override
	protected void setupAudio() throws Exception
	{
		super.setupAudio();
		if( sendOnEOL )
		{
			getLuFactory().setSendable( this );
		}
	}

	/** Setup URLs for scripts used to upload files. */

	void setupUploader()
	{
		try
		{
			nameValuePairs = new Hashtable();
			uploadHeaderPairs = new Hashtable();

			fileUploader = new FileUploader( uploadURL )
			{
				@Override
				public void sendButtonPressed()
				{
					sendRecordedMessage();
				}
			};
			fileUploader.setMinLabelWidth( minFieldLabelWidth );
			fileUploader.setUserName( userName );
			fileUploader.setPassword( password );
			fileUploader.setJavaScriptInterpreter( this );
			fileUploader.setUseURLConnection( useURLConnection );
			uploadThread = new QueueUploaderThread( this, fileUploader );
			uploadThread.start();
		} catch( SecurityException e )
		{
			System.err.println( e );
		}
	}

	@Override
	public void stop()
	{
		Logger.println( 1, "Begin RecorderUploadApplet.stop()" );
		if( fileUploader != null )
		{
			fileUploader.cancel(); // In case it is running.
		}
		if( uploadThread != null )
		{
			uploadThread.stop();
		}
		// Stop JavaScript from being called during shutdown and crashing
		// FireFox.
		uploadCompletionScript = null;
		uploadFailureScript = null;
		super.stop();
		Logger.println( 1, "Finished RecorderUploadApplet.stop()" );
	}

	/*
	 * Called when file upload fails.
	 */
	protected void onUploadFailure()
	{
		if( stashBeforeSend )
		{
			SimpleDialog.alert( getUserProperty( "how.to.load.stashed" ) );
		}
	}

	/*
	 * Called when file has been uploaded. Refreshes a specified web frame with
	 * a given URL.
	 */
	protected void onUploadCompletion()
	{
		if( refreshUrl != null )
		{
			if( refreshTarget != null )
			{
				getAppletContext().showDocument( refreshUrl, refreshTarget );
			}
			else
			{
				getAppletContext().showDocument( refreshUrl );
			}
		}
		else
		{
			repaint();
		}
	}

	// This listens for data coming out of the encoder.
	// It then grabs the available data and uploads it to the server.
	class UploaderStreamChunkListener implements
			DynamicSpeexRecording.StreamChunkListener
	{
		int sequenceNumber;

		private void sendAnyData( DynamicSpeexRecording dynamicSpeexRecording, int minimum )
		{
			DynamicBuffer dynoBuffer;
			try
			{
				dynoBuffer = dynamicSpeexRecording.getDynamicBuffer();
				int len = dynoBuffer.length();
				if( len > minimum )
				{
					Logger.println( 1, "Stream chunk of encoded data, length = "
									+ len
									+ ", dur = " + dynamicSpeexRecording.getMaxPlayableTime() );

					UploadCommand command = new AppendingUploadCommand( uploadFileName );

					// Make copy of the data available in an stream.
					InputStream inStream = dynoBuffer.getInputStream();
					command.setCompressedImageStream( inStream );

					command.setDuration( dynamicSpeexRecording.getMaxPlayableTime() );
					command.setChunkIndex( sequenceNumber++ );

					uploadThread.sendCommand( command );
					dynoBuffer.reset();
				}
			} catch( IOException e )
			{
				throw new RuntimeException( "Unexpected error. ", e );
			}
		}

		@Override
		public void gotChunk( DynamicSpeexRecording dynamicSpeexRecording )
		{
			if( isRecording() )
			{
				sendAnyData( dynamicSpeexRecording, 1000 );
			}
		}

		public void finish( DynamicSpeexRecording dynamicSpeexRecording )
		{
			dynamicSpeexRecording.flush();
			try
			{
				dynamicSpeexRecording.close();
			} catch( IOException e )
			{
				throw new RuntimeException( "Unexpected error. ", e );
			}

			Logger.println(  1, "Finish stream, isRecording = " + isRecording()
					+ ", len = " + dynamicSpeexRecording.getDynamicBuffer().length());
			// Set threshold to zero so we send even a small amount.
			sendAnyData( dynamicSpeexRecording, 0 );

			// Tell the server to save the uploaded file.
			UploadCommand command = new FinishingUploadCommand( uploadFileName );
			command.setChunkIndex( sequenceNumber++ );
			uploadThread.sendCommand( command );
		}
	}

	@Override
	public Recording createRecording( int maxSamples )
	{
		Recording reco;
		switch( getFormat() )
		{
		case Recording.FORMAT_SPEEX:
			DynamicSpeexRecording dsr = new DynamicSpeexRecording( maxSamples,
					isEditable(), useFileCache );
			dsr.setSpeexQuality( speexQuality );
			dsr.setSpeexComplexity( speexComplexity );
			if( streamUploads )
			{
				UploaderStreamChunkListener chunkListener = new UploaderStreamChunkListener();
				dsr.setStreamChunkListener( chunkListener );
			}
			reco = dsr;
			break;
		default:
			reco = new DynamicRecording( maxSamples, isEditable(), useFileCache );
			break;
		}
		reco.setFrameRate( getChosenFrameRate() );
		return reco;
	}

	/** Save message to a file for testing. */
	private void stashRecordedMessage( String savedFileName,
			InputStream compressedImage )
	{
		String msg = stashedRecordingManager.saveRecordedMessage(
				savedFileName, compressedImage );
		displayMessage( msg );
	}

	private class AppendingUploadCommand extends UploadCommand
	{

		public AppendingUploadCommand(String fileName)
		{
			super( fileName );
			setUniqueId( RecorderUploadApplet.this.getUniqueId() );
			setMessageIndex( messageIndex );
			// Pass a clone so we can add more pairs as part of the upload process
			// and not collide with others in the queue.
			setNameValuePairs( (Hashtable) nameValuePairs.clone() );
			setUploadHeaderPairs( uploadHeaderPairs );
		}

		@Override
		public String getAction()
		{
			return UploadCommand.SERVER_ACTION_APPEND;
		}

		@Override
		public InputStream setupCompressedImageStream() throws IOException
		{
			return getCompressedImageStream();
		}
	}

	private abstract class CommonUploadCommand extends UploadCommand
	{
		DynamicBuffer compressedImage = null;


		public CommonUploadCommand(String fileName)
		{
			super( fileName );
			setRecording( RecorderUploadApplet.this.getRecording() );
			setDuration( RecorderUploadApplet.this.getRecording().getMaxPlayableTime() );
			setUniqueId(  RecorderUploadApplet.this.getUniqueId() );
			setMessageIndex( messageIndex++ );
			setCompletionScript( uploadCompletionScript );
			setFailureScript( uploadFailureScript );

			setNameValuePairs( nameValuePairs );
			nameValuePairs = new Hashtable();

			setUploadHeaderPairs( uploadHeaderPairs );
			uploadHeaderPairs = new Hashtable();

			setUploadListeners( (Vector) uploadListeners.clone() );
		}

		@Override
		public boolean isRunInBackground()
		{
			return queueUploads;
		}

		@Override
		public void handleUploadFinished( boolean uploaded )
		{
			if( compressedImage != null )
			{
				compressedImage.clear();
			}

			if( uploaded )
			{
				// After uploading, we can remove the protection because
				// it is safely on the server.
				getRecorder().setProtected( false );
				getRecorder().setPositionInSeconds( 0.0 );
			}

			if( transportControl != null )
			{
				transportControl.setEnabled( true );
			}
			updateButtons( getRecorder() );

			// Do this last because it might cause the Applet to stop() and
			// go to another page.
			if( uploaded )
			{
				onUploadCompletion();
			}
			else
			{
				onUploadFailure();
			}
			repaintWaveDisplay();

			uploadInProgress = false;
		}
	}

	private class SendingUploadCommand extends CommonUploadCommand
	{
		public SendingUploadCommand(String fileName)
		{
			super( fileName );
		}

		@Override
		public String getAction()
		{
			return UploadCommand.SERVER_ACTION_UPLOAD;
		}

		@Override
		public boolean isSendingRecording()
		{
			return true;
		}

		@Override
		public InputStream setupCompressedImageStream() throws IOException
		{
			verifyUploadFileName( this );

			Recording recording = getRecording();
			if( recording != null )
			{
				compressedImage = createCompressedImage( getRecording() );

				// Save local copy in case upload fails.
				if( stashBeforeSend && (compressedImage != null) )
				{
					InputStream compressedStream = compressedImage
							.getInputStream();
					displayMessage( "Stashing copy on hard drive." );
					stashRecordedMessage( getFileName(),
							compressedStream );
					compressedStream.close();
				}

				return compressedImage.getInputStream();
			}
			return null;
		}

		private DynamicBuffer createCompressedImage( Recording recording )
				throws IOException
		{
			DynamicBuffer compressedImage = null;
			if( fileUploader.isGoing() )
			{
				Logger.printMemory( 2, "start upload" );
				ProgressListener progressListener = fileUploader
						.createProgressListener( "Compressing Audio: " );
				displayMessage( "Consolidating Edits ..." );
				recording.finalizeEdits();
				displayMessage( "Compressing Audio..." );
				compressedImage = recording.getCompressedImage( getFormat(),
						progressListener );
				Logger.printMemory( 2, "got compressed image" );
				displayMessage( "Compression Complete." );
			}
			return compressedImage;
		}

	}

	private class FinishingUploadCommand extends CommonUploadCommand
	{
		public FinishingUploadCommand(String fileName)
		{
			super( fileName );
		}

		@Override
		public String getAction()
		{
			return UploadCommand.SERVER_ACTION_FINISH;
		}

		@Override
		public boolean isSendingRecording()
		{
			return false;
		}

		@Override
		public InputStream setupCompressedImageStream() throws IOException
		{
			return null;
		}
	}

	protected void verifyUploadFileName( UploadCommand command )
	{
		if( getNameURL == null )
		{
			if( command.getFileName() == null )
			{
				throw new WebDeveloperRuntimeException( "Name of uploaded file not specified!" );
			}
		}
		else
		{
			// This technique is deprecated.
			// There really is no point because the server can rename
			// any uploaded files.
			// Get the file name from the server.
			try
			{
				String upFileName = fileUploader.getFileNameFromServer( getNameURL );
				if( upFileName == null )
				{
					throw new WebDeveloperRuntimeException( "Server did not provide unique name for file!" );
				}
				command.setFileName( upFileName );
			} catch( IOException e )
			{
				throw new WebDeveloperRuntimeException( "Could not connect to fileName generator!", e );
			}
		}
	}

	@Override
	public void setupTest()
	{
		super.setupTest();

		// Make recording persist when leaving page and returning.
		setTestParameter( "eraseOnStop", "no" );

		// forceListen = true;
		setTestParameter( "autoPlay", "no" );

		setTestParameter( "queueUploads", "false" );

		setTestParameter( "debugLevel", "2" );
		setTestParameter( "useURLConnection", "yes" );
		setTestParameter( "arrangement", "wide" );
		setTestParameter( "compressorEnabled", "yes" );
		setTestParameter( "eraseAfterSend", "yes" );
		setTestParameter( "stashMaxFiles", "53" );

		// setTestParameter( "autoPreview", "3.0" );
		setTestParameter( "autoBackStep", "0.0" );

		// setTestParameter( "useTextButtons", "yes" );
		setTestParameter( "buttonBackground", "FF0000" );
		setTestParameter( "buttonBackground", "FF0000" );
		//setTestParameter( "forceError", "test ErrorReporter" );

		if( true )
		{
			setTestParameter( "fieldName_1", "code" );
			setTestParameter( "fieldLabel_1", "Code:" );
			setTestParameter( "fieldDefault_1", "XYZ123" );
		}

		// Test Transcription.
		if( false )

		{
			// Test case where readyScript is not executed for rpowell5064
			// http://www.javasonics.com/forum/viewtopic.php?f=17&t=577&p=1023#p1023
			setTestParameter( "showLogo", "no" );
			setTestParameter( "showSendButton", "no" );
			setTestParameter( "showPauseButton", "no" );
			setTestParameter( "showPositionDisplay", "no" );
			setTestParameter( "protectRecording", "no" );
			setTestParameter( "showTimeText", "no" );
			setTestParameter( "showWaveForm", "no" );
			setTestParameter( "showPauseButton", "no" );
		}
		else if( true )
		{
			setTestParameter( "transcription", "yes" );
			setTestParameter( "useSpeechMike", "yes" );
			// "press" or "toggle"
			setTestParameter( "speechMikeBehavior", "onoff" );
			setTestParameter( "sendOnEOL", "yes" );
			setTestParameter( "useFootPedal", "no" );
			setTestParameter( "showSpeedControl", "no" );
			setTestParameter( "speechMikePort", "17669" );
			setTestParameter( "autoRewind", "2.5" );

			if( false )
			{
				// Ask Speech Mike to generate HotKey presses.
				setTestParameter( "speechMikeHotKeyF1", "space+alt" );
				setTestParameter( "speechMikeHotKeyF4", "z+shift+alt" );
				setTestParameter( "speechMikeHotKeyEOL", "f4+alt" );
				setTestParameter( "useFootPedal", "no" );
			}

			if( false )
			{
				// Ask ListenUp to respond to Hot Keys
				setTestParameter( "playHotKey", "p+alt+shift" );
				setTestParameter( "recordHotKey", "r+alt+shift" );

				setTestParameter( "pauseHotKey", "a+alt+shift" );
				setTestParameter( "stopHotKey", "s+alt+shift" );
				setTestParameter( "rewindHotKey", "w+alt+shift" );
				setTestParameter( "forwardHotKey", "f+alt+shift" );
				setTestParameter( "toEndHotKey", "e+alt+shift" );
				setTestParameter( "toBeginHotKey", "b+alt+shift" );
			}
		}

		if( false ) // repeatedly download and upload a recording
		{
			setTestParameter( "uploadFileName", "message_12345.wav" );
			setTestParameter( "sampleURL", TEST_URL
					+ "/listenup/uploads/message_12345.wav" );
			setTestParameter( "uploadUrl", TEST_URL
					+ "/test/qa/handle_upload_simple.php" );
			setTestParameter( "format", "ulaw" );
		}
		else if( false ) // Test STREAMING
		{
			setTestParameter( "editable", "no" );
			setTestParameter( "streamUploads", "yes" );
			setTestParameter( "uploadFileName", "streamed_message.spx" );
			setTestParameter( "uniqueId", "fake_unique_id" );
//			setTestParameter( "sampleURL", TEST_URL
//					+ "/test/listenup/uploads/streamed_message.spx" );
			setTestParameter( "uploadUrl", TEST_URL
					+ "/test/qa/handle_upload_stream.php" );
			setTestParameter( "format", "speex" );
			setTestParameter( "frameRate", "16000" );
		}
		else if( false ) // Test upload to unlicensed server.
		{
			setTestParameter( "uploadFileName", "message_12345.spx" );
			setTestParameter( "sampleURL", TEST_URL
					+ "/test/listenup/uploads/message_12345.spx" );
			setTestParameter( "uploadUrl", TEST_URL
					+ "/test/qa/handle_upload_simple.php" );
		}
		else if( false ) // Test playing 11 KHz file messing up maxRecordTime
		{
			setTestParameter( "maxRecordTime", "60.0" );
			setTestParameter( "uploadFileName", "message_12345.wav" );
			setTestParameter( "sampleURL", TEST_URL
					+ "/listenup/uploads/qa_r11025_adpcm.wav" );
			setTestParameter( "uploadUrl", TEST_URL
					+ "/test/qa/handle_upload_simple.php" );
		}
		else if( true ) // Test upload
		{
			setTestParameter( "uploadFileName", "message_12345.spx" );
			setTestParameter( "uploadUrl", TEST_URL
					+ "/test/qa/handle_upload_simpleZ.php" );
			setTestParameter( "format", "s16" );
			setTestParameter( "frameRate", "22050" );
			setTestParameter( "streamUploads", "no" );
			setTestParameter( "editable", "yes" );

			// setTestParameter( "testSignal", "yes" );
			// setTestParameter( "testSignalSpec", "sines,300.0,0.3,987.0,0.2"
			// );
		}
		else if( true ) // Test setting headers
		{
			setTestParameter( "uploadFileName", "../message_xyz.spx" );
			setTestParameter( "uploadUrl", TEST_URL
					+ "/listenup/php_test/dump_header.php" );
			setTestParameter( "frameRate", "16000.0" );
		}
		System.out.println( "Java version is "
				+ System.getProperty( "java.version" ) );
	}

	/** Add a listener for testing. */

	public void addUploadListener( UploadListener uploadListener )
	{
		uploadListeners.addElement( uploadListener );
	}

	/**
	 * @param timeOutMSec
	 * @return
	 * @throws InterruptedException
	 * @return true if timed out
	 */
	public boolean waitUntilUploaded( int timeOutMSec )
			throws InterruptedException
	{
		if( uploadThread != null )
		{
			return uploadThread.waitUntilComplete( timeOutMSec );
		}
		return false;
	}

	/** Test Applet locally. */
	static public void main( String[] args )
	{
		RecorderUploadApplet applet = new RecorderUploadApplet();
		applet.runApplication(args);
	}

	class UploadFieldDescriptor extends Panel
	{
		private static final long serialVersionUID = -4220815139520992024L;
		String name;
		String label;
		String defaultText;
		int numRows;

		public UploadFieldDescriptor(String name)
		{
			this.name = name;
			label = name;
			defaultText = "";
			numRows = 1;
		}

		@Override
		public String toString()
		{
			return "UploadFieldDescriptor: name = " + name + ", label = "
					+ label + ", default = " + defaultText + ", numRows = "
					+ numRows;
		}
	}

	protected static File browseForFileSave( Frame frame, File defaultFile,
			String message )
	{
		return browseForFile( frame, defaultFile, message, FileDialog.SAVE );
	}

	private void appendTwoDigits( StringBuffer buffer, int n )
	{
		if( n < 10 )
		{
			buffer.append( "0" );
		}
		buffer.append( n );
	}

	private String getDateTimeText()
	{
		Date date = new Date();
		StringBuffer buffer = new StringBuffer();
		buffer.append( date.getYear() + 1900 );
		appendTwoDigits( buffer, date.getMonth() + 1 );
		appendTwoDigits( buffer, date.getDate() );
		buffer.append( '_' );
		appendTwoDigits( buffer, date.getHours() );
		appendTwoDigits( buffer, date.getMinutes() );
		appendTwoDigits( buffer, date.getSeconds() );
		return buffer.toString();
	}

	/** Upload entire message. */
	private int sendEntireMessage( String upFileName )
	{
		Logger.println( 1, "sendRecordedMessage(" + upFileName + ")" );
		if( (getRecorder() == null) || !((Player) getRecorder()).isPlayable() )
		{
			Logger.println( 0,
					"   Recording is empty and could not be uploaded!" );
			return -1;
		}

		synchronized( fileUploader )
		{
			if( uploadInProgress )
			{
				Logger.println( 0,
						"sendRecordedMessage: Previous upload not yet complete!" );
			}
			else
			{
				if( upFileName == null )
				{
					upFileName = (getFormat() == Recording.FORMAT_SPEEX) ? "untitled.spx"
							: "untitled.wav";
				}

				// Make sure we are stopped. Added to prevent bug 0030.
				guaranteeStopped();

				UploadCommand command = new SendingUploadCommand( upFileName );
				if( queueUploads )
				{
					// Create a new empty recording to replace the one put in
					// the queue.
					try
					{
						setRecording( createRecording() );
					} catch( DeviceUnavailableException e )
					{
						reportExceptionAfterStopAudio( e );
					}
				}
				else
				{
					uploadInProgress = true;
					// Turn off buttons until upload completes.
					if( transportControl != null )
					{
						transportControl.setEnabled( false );
						transportControl.updateButtons();
					}
				}

				uploadThread.sendCommand( command );
			}
		}
		return 0;
	}

	/**
	 * Save the current recording in a background thread if stopped. This should
	 * be called by a background thread to avoid hanging the GUI.
	 */
	private void saveRecordedMessageJob()
	{
		Logger.println( 1, "saveRecordedMessage()" );
		if( (getRecorder() == null) || !((Player) getRecorder()).isPlayable() )
		{
			Logger.println( 0, "   Recording is empty and could not be saved!" );
			return;
		}

		if( !isRecording() && isPlayable() && (userHomeDir != null) )
		{
			// Make sure we are stopped. Added to prevent bug 0030.
			guaranteeStopped();

			File defaultFile = new File( userHomeDir, "recording_"
					+ getDateTimeText() + ".wav" );
			Frame frame = new Frame();
			File saveFile = browseForFileSave( frame, defaultFile,
					"Save ListenUp recording" );
			if( saveFile != null )
			{
				DynamicBuffer compressedImage;
				try
				{
					displayMessage( "Consolidating Edits ..." );
					getRecording().finalizeEdits();
					displayMessage( "Compressing Audio..." );
					ProgressListener progressListener = fileUploader
							.createProgressListener( "Compressing Audio: " );
					compressedImage = getRecording().getCompressedImage(
							WAVWriter.FORMAT_S16, progressListener );
					SaveLocalCommand command = new SaveLocalCommand( this,
							saveFile, compressedImage );
					uploadThread.sendCommand( command );

				} catch( IOException e )
				{
					reportExceptionAfterStopAudio( e );
				}
			}
			// Try to keep the frame around for a while to avoid
			// "java.lang.NullPointerException: null pData"
			/*
			 * try { Thread.sleep( 1000 ); } catch( InterruptedException e ) { }
			 */
			// We used to call getExtendedState but that is Java 1.4.
			Logger.println( 2, "Frame count = " + frame.getComponentCount() );
		}

	}


	// --------------------- Start JavaScript API
	// -----------------------------------
	/**
	 * Specify JavaScript command to be executed when upload has completed. For
	 * example:
	 *
	 * <pre>
	 * document.myApplet.setUploadCompletionScript( &quot;uploadComplete();&quot; );
	 * </pre>
	 */
	public void setUploadCompletionScript( String scriptText )
	{
		Logger.println( 1, "setUploadCompletionScript('" + scriptText + "')" );
		uploadCompletionScript = scriptText;
	}

	/**
	 * Specify JavaScript command to be executed if upload fails. For example:
	 *
	 * <pre>
	 * document.myApplet.setUploadFailureScript( &quot;uploadFailed();&quot; );
	 * </pre>
	 */
	public void setUploadFailureScript( String scriptText )
	{
		Logger.println( 1, "setUploadFailureScript('" + scriptText + "')" );
		uploadFailureScript = scriptText;
	}

	/**
	 * This is mostly used for testing and debugging.
	 *
	 * @return how many recording have been successfully uploaded
	 */
	public int getUploadCompletionCount()
	{
		if( uploadThread != null )
		{
			return uploadThread.getUploadCompletionCount();
		}
		else
		{
			return 0;
		}
	}

	/**
	 * This is mostly used for testing and debugging.
	 *
	 * @return how many recording have been successfully uploaded
	 */
	public int getUploadFailureCount()
	{
		if( uploadThread != null )
		{
			return uploadThread.getUploadFailureCount();
		}
		else
		{
			return 0;
		}
	}

	/**
	 * This is mostly used for testing and debugging.
	 *
	 * @return true if uploaded thread is still alive
	 */
	public boolean isUploaderAlive()
	{
		if( uploadThread != null )
		{
			return uploadThread.isAlive();
		}
		else
		{
			return false;
		}
	}

	/**
	 * Add to list of name/value pairs to be uploaded with next sound file. List
	 * will be cleared internally after calling sendRecordedMessage().
	 */

	public void addNameValuePair( String name, String value )
	{
		nameValuePairs.put( name, value );
	}


	public void addUploadHeaderPair( String name, String value )
	{
		uploadHeaderPairs.put( name, value );
	}

	/** Upload a recorded sound file to the server. */
	@Override
	public int sendRecordedMessage()
	{
		return sendRecordedMessage( uploadFileName );
	}

	/**
	 * @deprecated use addNameValuePair() instead.
	 */
	@Deprecated
	public int sendRecordedMessage( String idFieldName, String idFieldValue )
	{
		return sendRecordedMessage( uploadFileName, idFieldName, idFieldValue );
	}

	/** @deprecated use addNameValuePair() instead. */
	@Deprecated
	public int sendRecordedMessage( String upFileName, String idFieldName,
			String idFieldValue )
	{
		addNameValuePair( idFieldName, idFieldValue );
		return sendRecordedMessage( upFileName );
	}

	public int finishStreamingUpload()
	{
		guaranteeStopped();
		Recording recording = getRecording();
		if( recording != null )
		{
			if( recording instanceof DynamicSpeexRecording )
			{
				DynamicSpeexRecording speexRecording = (DynamicSpeexRecording) recording;
				UploaderStreamChunkListener uscl = (UploaderStreamChunkListener) speexRecording.getStreamChunkListener();
				uscl.finish( speexRecording );
			}
		}
		return 0;
	}

	/** Do upload in a background thread so we don't hang GUI. */
	public int sendRecordedMessage( String upFileName )
	{
		if( streamUploads )
		{
			return finishStreamingUpload();
		}
		else
		{
			return sendEntireMessage( upFileName );
		}
	}


	/**
	 * How many recordings are in the queue to be uploaded, including any
	 * currently being uploaded?
	 */
	public int getUploadQueueDepth()
	{
		if( uploadThread != null )
		{
			return uploadThread.getQueueDepth();
		}
		else
		{
			return 0;
		}
	}

	/** Save the current recording in a background thread if stopped. */
	public void saveRecordedMessage()
	{
		new Thread()
		{
			@Override
			public void run()
			{
				saveRecordedMessageJob();
			}
		}.start();
	}


	/**
	 * @return the number of recordings that have been uploaded.
	 */
	public int getUploadCount()
	{
		if( uploadThread != null )
		{
			return uploadThread.getUploadCount();
		}
		else
		{
			return 0;
		}
	}

	public boolean getEraseAfterSend()
	{
		return eraseAfterSend;
	}

	public void setEraseAfterSend( boolean eraseAfterSend )
	{
		this.eraseAfterSend = eraseAfterSend;
	}

	/** Set the text value for a user entry field in the file uploader. */
	public void setFieldValue( String name, String value )
	{
		if( fileUploader != null )
		{
			fileUploader.setFieldValue( name, value );
		}
	}

}
