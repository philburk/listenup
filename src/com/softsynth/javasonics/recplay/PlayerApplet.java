package com.softsynth.javasonics.recplay;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.SwingConstants;

import com.softsynth.awt.BevelledPanel;
import com.softsynth.awt.ImageLabel;
import com.softsynth.javasonics.DeviceUnavailableException;
import com.softsynth.javasonics.core.SonicSystem;
import com.softsynth.javasonics.error.BugReportable;
import com.softsynth.javasonics.error.CoreDump;
import com.softsynth.javasonics.error.ErrorReporter;
import com.softsynth.javasonics.error.UserException;
import com.softsynth.javasonics.error.WebDeveloperRuntimeException;
import com.softsynth.javasonics.installer.Installer;
import com.softsynth.javasonics.util.BackgroundCommandProcessor;
import com.softsynth.javasonics.util.HeadTailLog;
import com.softsynth.javasonics.util.JavaScriptInterpreter;
import com.softsynth.javasonics.util.Logger;
import com.softsynth.ssl.SSLTools;
import com.softsynth.storage.DynamicBufferFactory;
import com.softsynth.upload.DiagnosticStatusUploader;
import com.softsynth.upload.Query;
import com.softsynth.xml.XMLWriter;

import netscape.javascript.JSObject;

/**
 * Applet for Playing back sound using JavaSonics. This class provides a
 * graphical front end for the non-graphical Player Class.
 *
 * @author (C) 2001-7 Phil Burk, All Rights Reserved
 */

public class PlayerApplet extends Applet implements PlayerListener,
		RecordingFactory, JavaScriptInterpreter, ParameterHolder, IUserDisplay,
		BugReportable
{
	// TODO Update version for feature releases.
	final static int MAJOR_VERSION = 1;
	final static int MINOR_VERSION = 103;
	final static int REVISION = 1;
	final static String VERSION_QUALIFIER = "";

	final static boolean THIS_WAS_NOT_BROKEN = false;

	public final static String VERSION_NUMBER = MAJOR_VERSION + "."
			+ MINOR_VERSION + "." + REVISION;
	// TODO Update index whenever we build a release.
	public final static int BUILD_NUMBER = 516;
	// TODO Set new date when code built for release.
	private final static long BUILD_TIME = new GregorianCalendar( 2017,
			GregorianCalendar.JANUARY, 28 ).getTime().getTime();

	// This is used to prevent an untested version from being used indefinitely
	// if it leaks out.
	// FIXME set back to false before official release
	protected final static boolean EXPIRE_AFTER_TEST_PERIOD = true;
	private final static long EXPIRE_NUM_DAYS = 200;

	// TODO Set this to BUILD_TIME or other date before which we want to require
	// a maintenance fee to upgrade.
	private final static long UPGRADE_CUTOFF_TIME = new GregorianCalendar(
			2008, GregorianCalendar.JULY, 16 ).getTime().getTime();
	private final static long ALLOW_UPGRADE_NUM_DAYS = 365 + 10;

	final static String VERSION_TEXT = "V" + VERSION_NUMBER + VERSION_QUALIFIER
			+ " (build " + BUILD_NUMBER + ", " + (new Date( BUILD_TIME )) + ")";

	// Licenses before this date can upgrade regardless of the
	// UPGRADE_CUTOFF_TIME. Do NOT change!
	private final static long GRANDFATHER_TIME = new GregorianCalendar( 2007,
			GregorianCalendar.JUNE, 1 ).getTime().getTime(); // DO NOT CHANGE

	private int debugLevel = 0;
	// protected final static String TEST_URL =
	// "file:/D:/releases/listenup_20080512";
	protected final static String TEST_URL = "http://www.javasonics.com";
	// protected final static String TEST_URL = "http://localhost";
	// protected final static String TEST_URL = "http://phil-macbook.local";
	// protected final static String TEST_URL = "http://www.philburk.org";

	protected String mTestCodebase = TEST_URL + "/listenup/codebase/";
	//protected final static String TEST_CODEBASE = TEST_URL + "/test/qa/codebase_good_branded/";
	//  "file:///C:/Users/phil/workspace/javasonics/website/listenup/codebase/listenup_license.txt";
	// protected final static String TEST_CODEBASE =
	// "http://dev.portfolio.no/listenup/codebase/";
	// protected final static String TEST_CODEBASE = TEST_URL +
	// "/test/qa/codebase_demo_1/";
	// protected final static String TEST_CODEBASE =
	// "https://kenlaji.clinicyou.com/codebase/";

	// These variables are set by Applet parameters at initialization.
	private boolean isTranscriptionPermitted = false;
	private boolean isTranscriptionEnabled = false;
	private int numChannels = 1;
	private static final Color DEFAULT_BACKGROUND = new Color( 0x00E6E3D0 );
	private Color background = DEFAULT_BACKGROUND;
	private Color foreground = Color.black;
	// These defaults will be overridden by the background & foreground set by
	// the user.
	private Color waveBackground = Color.white;
	private Color waveForeground = Color.black;
	private Color buttonBackground = DEFAULT_BACKGROUND;

	private String sampleURLname = null;
	private String diagnosticURLname = null;
	private String logoURLname = null;
	private String skinURLname = null;
	private String skinFFRewURLname = null;

	private URL diagnosticURL = null;
	private URL logoURL = null;
	private URL skinURL = null;
	private URL skinFFRewURL = null;

	private String sessionID = null;
	private boolean autoPlay = false;
	private boolean forceListen = false;
	private boolean useDialog = false;
	protected String userName = null;
	protected String password = null;
	private boolean editable = false;
	// Set true to prefer plugin, set false to prefer SUN JavaSound
	protected boolean preferNative = false;

	private String openDialogMessage = "Open Voice Window";
	private int latencyInFrames = SonicSystem.NOT_SPECIFIED;
	private boolean showTransport = true;
	private boolean showLogo = true;
	private boolean showWaveform = true;
	private boolean packButtons = false;
	private int bevelSize = 4;
	// Set during initialization.
	protected TransportControl transportControl;
	private PositionBarControl positionBarControl;
	private TimeTextDisplay timeTextDisplay;
	private boolean useOldSpeechMike = false; // Use old 'C' based
												// SpeechMikeServer.exe
	private boolean useSpeechMike = false;
	private boolean useFootPedal = false;
	private int speechMikePort = 17443;
	private int footPedalPort = 17445;
	protected HotKeyOptions hotKeyOptions = new HotKeyOptions();

	// Cache large temporary buffers on disk.
	protected boolean useFileCache = false;

	private Panel outerPanel;
	private Panel nextPanel;
	private Frame testFrame; // just for testing
	private Dialog optionalDialog;
	private Button showDialogButton;
	private Container guiContainer;
	// private VisualTheme visualTheme;
	private int format = Recording.FORMAT_UNKNOWN;

	// Use Text buttons for visually impaired users with screen readers.
	private boolean useTextButtons = false;
	// true if customer allowed to substitute JavaSonics logo
	private boolean isLogoEnabled = false;
	// true if customer allowed to remove JavaSonics logo
	private boolean isNoLogoEnabled = true; // Always true after V1.83

	private boolean isBranded = false;
	private AudioDownloader downloader;
	private Player player;

	private String transportListenerScript = null;
	private String appletReadyScript = null;
	private int previousPlayerState = Player.STOPPED;
	private boolean javaScriptExecutionAllowed = true;
	private BackgroundCommandProcessor backgroundCommandProcessor;
	private ReportToJavaScript progressToJavaScript;
	protected int timeChangeInterval = 50; // msec

	private Label waitForInitLabel = null;
	private boolean runningAsApplet = false;
	private boolean licenseeTrusted = false;
	private boolean initFailed = true;

	private ListenUpFactory luFactory;

	protected boolean canRecord = false;
	protected boolean showPauseButton = true;
	protected boolean showVUMeter = true;
	protected boolean showPositionDisplay = true;
	protected boolean showSpeedControl = true;
	protected boolean showTimeText = false;
	protected boolean putTimeOnTop = false;
	protected int timeTextSize = 12;
	protected boolean timeTextBold = false;

	protected boolean showErrorAlerts = true;
	private boolean validLicense = false;
	private boolean freeMode = true;
	private int savedRandomKey = 12345;
	private static boolean nagScreenAlreadyRespondedTo = false;
	protected StashedRecordingManager stashedRecordingManager;
	protected Properties userProperties;
	private Properties testParameters;
	protected double autoBackStep = 0.0;
	protected double autoPreview = 0.0;
	private double startPosition;
	private WaveDisplay waveDisplay;
	protected boolean ignoreMissingSample = false;
	private final Applet thisApplet = this;

	private String downloadCompletionScript = null;
	private String downloadFailureScript = null;

	// Used for calling JavaScript from Java.
	private JSObject javaScriptWindow = null;
	// Save parameters for coreDump
	private Hashtable savedParameters = new Hashtable();
	private String userAgent;
	private boolean mEnableStandAlone = true;

	public PlayerApplet()
	{
		// For storing test parameters for unit tests.
		testParameters = new Properties();
		// Set user visible strings for menus and help.
		userProperties = new Properties();
		// WARNING - setProperty() does not work in Java 1.1. So use put().
		userProperties.put( "load.most.recent",
				"Load Most Recently Stashed Recording" );
		userProperties.put( "delete.selection", "Delete Selection" );

		userProperties
				.put( "how.to.load.stashed",
						"There was an error when uploading this recording.\n"
								+ "But it was saved temporarily on disk.\n"
								+ "You can recover it later by right-clicking on the waveform display and using the menu.\n"
								+ "Note that attempting to upload a different recording will overwrite this saved copy.\n" );
	}

	/** Are we running in the context of a browser as a real Applet? */
	public boolean isApplet()
	{
		return runningAsApplet;
	}

	/** Do we trust this licensee? */
	public final boolean isTrusted()
	{
		return licenseeTrusted;
	}

	/** Is the ListenUp license valid? */
	public final boolean isLicenseValid()
	{
		return validLicense;
	}

	/** Is the ListenUp license in free mode? */
	public final boolean isFreeMode()
	{
		return freeMode;
	}

	// Override default method. Provides info to browser.
	@Override
	public String getAppletInfo()
	{
		return "ListenUp (C) 2003-8 by SoftSynth";
	}

	protected void uploadDiagnosticStatus( String status, boolean addSysInfo,
			String errorMsg )
	{
		if( diagnosticURL != null )
		{
			Logger.println( 1, "Start uploadDiagnosticStatus: " + status );
			try
			{
				DiagnosticStatusUploader uploader = new DiagnosticStatusUploader(
						diagnosticURL );
				uploader.addNameValuePair( "diagnosis", status );
				if( sessionID != null )
				{
					uploader.addNameValuePair( "sessionID", sessionID );
				}
				if( addSysInfo || (errorMsg != null) )
					uploader.addSystemInfo();
				if( errorMsg != null )
				{
					uploader.addNameValuePair( "errorMsg", errorMsg );
				}
				uploader.dispatch();
			} catch( IOException exc )
			{
				System.err.println( exc );
			}
			Logger.println( 1, "Finish uploadDiagnosticStatus: " + status );
		}
	}

	@Override
	public CoreDump createCoreDump( long timestamp )
	{
		CoreDump coreDump = new CoreDump(timestamp);
		coreDump.open();
		coreDump.addApplication( this.getClass().getName(), VERSION_NUMBER,
				BUILD_NUMBER );
		URL codeURL = getCodeURL();
		if( codeURL != null )
		{
			coreDump.writeTag( "codebase", codeURL.toExternalForm() );
		}

		try
		{
			URL docURL = super.getDocumentBase();
			coreDump.writeTag( "docbase", docURL.toExternalForm() );
		} catch( NullPointerException exc )
		{
			// Cuz run as application.
		}


		if( userAgent != null )
		{
			coreDump.writeTag( "useragent", userAgent );
		}

		coreDump.addCommon();
		writeParametersToCoreDump( coreDump );
		writeLogsToCoreDump( coreDump );

		return coreDump;
	}

	private void writeParametersToCoreDump( CoreDump coreDump )
	{
		XMLWriter xmlWriter = coreDump.getXMLWriter();
		xmlWriter.beginTag( "parameters" );

		Enumeration keys = savedParameters.keys();
		while( keys.hasMoreElements() )
		{
			String name = (String) keys.nextElement();
			String value = (String) savedParameters.get( name );
			xmlWriter.beginTag( "param" );
			xmlWriter.writeAttribute( "name", name );
			xmlWriter.writeAttribute( "value", value );
			xmlWriter.endTag();
		}

		xmlWriter.endContent();
		xmlWriter.endTag();
	}

	private void writeLogsToCoreDump( CoreDump coreDump )
	{
		HeadTailLog htl = Logger.getHeadTailLog();
		XMLWriter xmlWriter = coreDump.getXMLWriter();
		xmlWriter.beginTag( "log" );
		xmlWriter.beginContent();
		// head
		int headCount = htl.getHeadCount();
		for( int i = 0; i < headCount; i++ )
		{
			xmlWriter.writeTag( "line", htl.getHeadLine( i ) );
		}
		// tail
		if( htl.getTailCount() > 0 )
		{
			String[] tail = htl.getTail();
			xmlWriter.writeTag( "break", "lines deleted" );
			for( int i = 0; i < tail.length; i++ )
			{
				xmlWriter.writeTag( "line", tail[i] );
			}
		}
		xmlWriter.endContent();
		xmlWriter.endTag();
	}

	/**
	 * Security check to make sure we are running on a licensed server.
	 *
	 * @param codeURL
	 * @return true if the license is valid.
	 */
	private final boolean checkHostLicense( URL codeURL )
	{
		boolean valid = false;

		// In the original code, these used to be set based on the license file.
		licenseeTrusted = true;
		isTranscriptionPermitted = true;
		isBranded = false;
		isLogoEnabled = true;
		showLogo = false;

		return valid;
	}

	private final URL getCodeURL()
	{
		URL codeURL = null;
		try
		{
			// Call super method to prevent overwriting by subclasses.
			codeURL = super.getCodeBase();
			runningAsApplet = true;
		} catch( Exception exc )
		{

			if( Query.getJavaVersion() >= 1.4 )
			{
				SSLTools.disableCertificateValidation();
			}
			try
			{
				codeURL = new URL( mTestCodebase );
			} catch( MalformedURLException e2 )
			{
				e2.printStackTrace();
			}
		}
		return codeURL;
	}

	/* Test version will expire in case we have a license security hole. */
	private final void checkExpiration()
	{
		final long msecPerDay = 1000 * 60 * 60 * 24;
		long now = System.currentTimeMillis();
		if( EXPIRE_AFTER_TEST_PERIOD )
		{
			long expiresOn = BUILD_TIME + (msecPerDay * EXPIRE_NUM_DAYS);
			if( now > expiresOn )
			{
				System.out.println( "Time now is " + now );
				String msg = "ListenUp Beta Test Period expired. Get updated version of ListenUp from www.javasonics.com";
				SimpleDialog.alert( msg );
				throw new SecurityException( msg );
			}
			else
			{
				long hoursLeft = (expiresOn - now) / (1000 * 60 * 60);
				long daysLeft = hoursLeft / 24;
				hoursLeft = hoursLeft - (daysLeft * 24);
				System.out
						.println( "WARNING: ListenUp beta test version expires in "
								+ daysLeft
								+ " days and "
								+ hoursLeft
								+ " hours! " );
			}
		}
	}

	/**
	 * Check to see if this code was built more than one year after the license
	 * issue date.
	 *
	 * @param timeIssued
	 */
	private final void checkUpgrade( long timeIssued )
	{
		// Grandfather in old users.
		// Only check if license issued after grandfather date.
		// Folks with earlier license avoid the check.
		if( timeIssued > GRANDFATHER_TIME )
		{
			final long msecPerDay = 1000 * 60 * 60 * 24;
			long lastUpgradeTime = timeIssued
					+ (msecPerDay * ALLOW_UPGRADE_NUM_DAYS);
			// If the current software too new for the user's license?
			if( UPGRADE_CUTOFF_TIME > lastUpgradeTime )
			{
				throw new SecurityException(
						"This version of ListenUp compiled more than "
								+ (ALLOW_UPGRADE_NUM_DAYS - 1)
								+ " days past license date of "
								+ new Date( timeIssued )
								+ "\n"
								+ "Maintenance fee required to use this new version." );
			}
		}
	}

	/**
	 * Called internally by browser when an Applet is started. Do <b>not </b>
	 * call from JavaScript.
	 */
	@Override
	public final void init()
	{
		ErrorReporter.setExtraMessage( "ListenUp version " + VERSION_TEXT );
		Logger.reset();
		try
		{
			checkMicrosoftJava();
			Logger.println( 1, "Begin PlayerApplet.init()" );

			setupLiveConnect();

			// Break up initialization so higher level classes can order their
			// own inits.
			if( init1() )
			{
				initSecurity();
				Logger.println( 1, "Call init2()." );
				init2();

				uploadDiagnosticStatus( "init", true, null );
				initFailed = false;
			}
			Logger.println( 1, "Finished PlayerApplet.init()" );
		} catch( Throwable thr )
		{
			reportExceptionAndHalt( thr );
			throw new RuntimeException( thr.getMessage() );
		}
	}

	private void setupLiveConnect()
	{
		if( javaScriptWindow == null )
		{
			try
			{
				javaScriptWindow = JSObject.getWindow( thisApplet );
				userAgent = (String) javaScriptWindow.eval( "navigator.userAgent;" );
			}
			catch( Exception e )
			{
				Logger.println( 0, "Error creating LiveConnect object. " + e );
			}
		}
	}

	protected void checkMicrosoftJava() throws UserException
	{
		String vendor = System.getProperty( "java.vendor" );
		if( vendor.indexOf( "Microsoft" ) >= 0 )
		{
			throw new UserException(
					"The old Microsoft Java 1.1 is no longer supported by Microsoft or by ListenUp.\n"
							+ "Please upgrade to Sun Java." );
		}
	}

	public boolean init1() throws Exception
	{
		// App or Applet?
		// This will throw an exception if running as an application.
		try
		{
			// Call super method to prevent overwriting by subclasses.
			super.getCodeBase();
			runningAsApplet = true;
		} catch( Throwable thr )
		{
		}

		Logger.setLevel( debugLevel );
		Logger.println( 1, "Begin init()" );
		Logger.println( 0, "ListenUp version = " + getVersionText() );

		setBackground( background );
		setForeground( foreground );
		waitForInitLabel = new Label(
				"Please wait while Applet is initialized..." );
		add( waitForInitLabel );
		syncGUI();

		// Get parameters that may affect license validation.
		checkInitialAppletParameters();

		// Do we need to install the JavaSonics plugin?
		PluginChecker checker = new PluginChecker( this );
		boolean needPlugin = checker.installIfNeeded();
		// Only continue with the Applet if we don't need the plugin.
		// Otherwise bail quietly because we have gone off to get the
		// plugin.
		return !needPlugin;
	}

	public final void initSecurity()
	{
		checkExpiration();
	}

	protected void init2()
	{
		// Requires licenseManager
		stashedRecordingManager = new StashedRecordingManager( this );

		checkAppletParameters();

		ErrorReporter.setShowAlerts( showErrorAlerts );
		if( isApplet() )
		{
			progressToJavaScript = new ReportToJavaScript( this );
		}

		// Build URLs needed by Applet.
		try
		{
			diagnosticURL = makeAbsoluteURL( diagnosticURLname );
			logoURL = makeAbsoluteURL( logoURLname );
			skinURL = makeAbsoluteURL( skinURLname );
			skinFFRewURL = makeAbsoluteURL( skinFFRewURLname );
		} catch( Exception e1 )
		{
			throw new WebDeveloperRuntimeException( "Error in URL: "
					+ e1.getMessage(), e1 );
		}

		DynamicBufferFactory.setUseFiles( useFileCache );

		try
		{
			buildFactory();
		} catch( Exception e1 )
		{
			throw new RuntimeException( "Tried to build class factory.\n"
					+ "You may be using the wrong JAR file.\n"
					+ e1.getMessage() );
		}
	}

	/**
	 * Create an abstract class to handle the extensions offered by the
	 * transcription jar.
	 *
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	private void buildFactory() throws InstantiationException,
			IllegalAccessException, ClassNotFoundException
	{
		final String standardFactoryLicenseClassName = "com.softsynth.javasonics.recplay.ListenUpFactoryStandard";
		final String transcriberFactoryLicenseClassName = "com.softsynth.javasonics.transcriber.ListenUpFactoryTranscriber";

		if( isTranscriptionEnabled )
		{
			Logger.println( 1, "Making ListenUpFactoryTranscriber" );
			setLuFactory( (ListenUpFactory) JSClassLoader.load(
					transcriberFactoryLicenseClassName, isApplet() )
					.newInstance() );
			getLuFactory().setFFRewSkinURL( skinFFRewURL );
		}
		else
		{
			Logger.println( 1, "Making ListenUpFactoryStandard" );
			setLuFactory( (ListenUpFactory) JSClassLoader.load(
					standardFactoryLicenseClassName, isApplet() ).newInstance() );
		}
		getLuFactory().setSkinURL( skinURL );
	}

	protected void setLuFactory( ListenUpFactory luFactory )
	{
		this.luFactory = luFactory;
	}

	protected ListenUpFactory getLuFactory()
	{
		return luFactory;
	}

	public static String getVersionText()
	{
		return VERSION_TEXT;
	}

	/**
	 * @return
	 */
	protected String getSampleURL()
	{
		return sampleURLname;
	}

	URL makeAbsoluteURL( String urlName )
	{
		if( urlName == null )
			return null;

		URL result;
		try
		{
			// Is it already absolute?
			if( urlName.startsWith( "http" ) )
			{
				result = new URL( urlName );
			}
			else
			{
				try
				{
					result = new URL( getDocumentBase(), urlName );
				} catch( NullPointerException e )
				{
					// probably an application, not Applet, so allow any URL
					result = new URL( urlName );
				}
			}
		} catch( IOException exc )
		{
			reportExceptionAfterStopAudio( exc );
			return null;
		}
		return result;
	}

	/**
	 * Test version that loads from local properties if not an Applet.
	 *
	 * @param paramName
	 * @return value of parameter
	 */
	@Override
	public String getParameter( String paramName )
	{
		String result = null;
		boolean gotResult = false;
		try
		{
			if( runningAsApplet )
			{
				result = super.getParameter( paramName );
				gotResult = true;
			}
		} catch( NullPointerException e )
		{
			e.printStackTrace();
		}
		if( !gotResult )
		{
			result = (String) testParameters.get( paramName.toLowerCase() );
		}
		if( result != null )
		{
			savedParameters.put( paramName, result );
		}
		return result;
	}

	Color getColorParameter( String paramName, Color defaultColor )
	{
		Color color = defaultColor;
		String temp = getParameter( paramName );
		if( temp != null )
		{
			try
			{
				int value = Integer.parseInt( temp, 16 );
				color = new Color( value );
			} catch( NumberFormatException e )
			{
				throw new WebDeveloperRuntimeException( "Error in " + paramName
						+ " parameter. Invalid hex integer = " + temp, e );
			}
		}
		return color;
	}

	protected int getIntegerParameter( String paramName, int defaultValue )
	{
		int value = defaultValue;
		String temp = getParameter( paramName );
		if( temp != null )
		{
			try
			{
				value = Integer.parseInt( temp );
			} catch( NumberFormatException e )
			{
				throw new WebDeveloperRuntimeException( "Error in " + paramName
						+ " parameter. Invalid integer = " + temp, e );
			}
		}
		return value;
	}

	protected double getDoubleParameter( String paramName, double defaultValue )
	{
		double value = defaultValue;
		String temp = getParameter( paramName );
		if( temp != null )
		{
			try
			{
				value = Double.valueOf( temp ).doubleValue();
			} catch( NumberFormatException e )
			{
				throw new WebDeveloperRuntimeException( "Error in " + paramName
						+ " parameter. Invalid double = " + temp, e );
			}
		}
		return value;
	}

	@Override
	public float getFloatParameter( String paramName, float defaultValue )
	{
		float value = defaultValue;
		String temp = getParameter( paramName );
		if( temp != null )
		{
			try
			{
				value = Float.valueOf( temp ).floatValue();
			} catch( NumberFormatException e )
			{
				throw new WebDeveloperRuntimeException( "Error in " + paramName
						+ " parameter. Invalid float = " + temp, e );
			}
		}
		return value;
	}

	@Override
	public boolean getBooleanParameter( String paramName, boolean defaultValue )
	{
		boolean value = defaultValue;
		String temp = getParameter( paramName );
		if( temp != null )
		{
			// Added 5/26/09. Was rejecting "Yes".
			temp = temp.toLowerCase();
			if( temp.equals( "yes" ) || temp.equals( "true" ) )
			{
				value = true;
			}
			else if( temp.equals( "no" ) || temp.equals( "false" ) )
			{
				value = false;
			}
			else
			{
				throw new WebDeveloperRuntimeException( "Error for "
						+ paramName + " parameter. Invalid boolean = " + temp
						+ "\n"
						+ "Must be \"yes\" or \"no\" or \"true\" or \"false\"." );
			}
		}
		return value;
	}

	private void checkFormatParameter()
	{
		String temp = getParameter( "format" );
		Logger.println( 1, "Compression format = " + temp + " = " + format );
		if( temp != null )
		{
			if( temp.equals( "adpcm" ) )
			{
				setFormat( Recording.FORMAT_IMA_ADPCM );
			}
			else if( temp.equals( "speex" ) )
			{
				setFormat( Recording.FORMAT_SPEEX );
			}
			else if( temp.equals( "s16" ) )
			{
				setFormat( Recording.FORMAT_S16 );
			}
			else if( temp.equals( "u8" ) )
			{
				setFormat( Recording.FORMAT_U8 );
			}
			else if( temp.equals( "ulaw" ) )
			{
				setFormat( Recording.FORMAT_ULAW );
			}
			else
			{
				throw new WebDeveloperRuntimeException(
						"Unrecognized compression format = " + temp );
			}
		}
	}

	/* These parameters may be needed by the license validation code. */
	private void checkInitialAppletParameters()
	{
		// The webpage can pass in a userAgent to assist debugging.
		String temp = getParameter( "userAgent" );
		if( temp != null )
		{
			userAgent = temp;
		}

		// Get this first in case we want to print debug info about
		// AppletParameters.
		debugLevel = getIntegerParameter( "debugLevel", debugLevel );
		Logger.setLevel( debugLevel );

		sessionID = getParameter( "sessionID" );
		userName = getParameter( "userName" );
		password = getParameter( "password" );

		useTextButtons = getBooleanParameter( "useTextButtons", useTextButtons );
	}

	/* These parameters may use the license permissions. */
	protected void checkAppletParameters()
	{

		// Specify audio latency. Controls size of audio buffer and delay
		// between program and audio being heard.
		latencyInFrames = getIntegerParameter( "latencyInFrames",
				latencyInFrames );

		// Name sample to be played as the primary message.
		String temp = getParameter( "sampleURL" );
		if( temp != null )
		{
			sampleURLname = temp;
		}

		// Name script to receive diagnostic info.
		temp = getParameter( "diagnosticURL" );
		if( temp != null )
		{
			diagnosticURLname = temp;
		}

		if( isLogoEnabled )
		{
			// Customer logo
			temp = getParameter( "logoURL" );
			if( temp != null )
			{
				logoURLname = temp;
			}
		}

		// Disable or Enable logo display.
		if( isNoLogoEnabled )
		{
			showLogo = getBooleanParameter( "showLogo", showLogo );
		}

		// Custom skin for buttons.
		temp = getParameter( "skin" );
		if( temp != null )
		{
			skinURLname = temp;
		}

		// Custom skin for buttons.
		temp = getParameter( "skinFFRew" );
		if( temp != null )
		{
			skinFFRewURLname = temp;
		}

		showTransport = getBooleanParameter( "showTransport", showTransport );
		autoPlay = getBooleanParameter( "autoPlay", autoPlay );
		packButtons = getBooleanParameter( "packButtons", packButtons );

		setEditable( getBooleanParameter( "editable", isEditable() ) );
		preferNative = getBooleanParameter( "preferNative", preferNative );

		if( getBooleanParameter( "transcription", isTranscriptionEnabled ) )
		{
			if( isTranscriptionPermitted )
			{
				isTranscriptionEnabled = true;
			}
			else
			{
				throw new SecurityException(
						"The \"transcription\" parameter not allowed with this license. That option must be purchased." );
			}
		}

		showWaveform = getBooleanParameter( "showWaveform", showWaveform );
		useOldSpeechMike = getBooleanParameter( "useOldSpeechMike",
				useOldSpeechMike );
		useSpeechMike = getBooleanParameter( "useSpeechMike", useSpeechMike );
		useFootPedal = getBooleanParameter( "useFootPedal", useFootPedal );
		speechMikePort = getIntegerParameter( "speechMikePort", speechMikePort );
		footPedalPort = getIntegerParameter( "footPedalPort", footPedalPort );
		useFileCache = getBooleanParameter( "useFileCache", useFileCache );

		hotKeyOptions.options[HotKeyOptions.PLAY_INDEX] = getParameter( "playHotKey" );
		hotKeyOptions.options[HotKeyOptions.STOP_INDEX] = getParameter( "stopHotKey" );
		hotKeyOptions.options[HotKeyOptions.PAUSE_INDEX] = getParameter( "pauseHotKey" );
		hotKeyOptions.options[HotKeyOptions.RECORD_INDEX] = getParameter( "recordHotKey" );
		hotKeyOptions.options[HotKeyOptions.FORWARD_INDEX] = getParameter( "forwardHotKey" );
		hotKeyOptions.options[HotKeyOptions.REWIND_INDEX] = getParameter( "rewindHotKey" );
		hotKeyOptions.options[HotKeyOptions.TO_END_INDEX] = getParameter( "toEndHotKey" );
		hotKeyOptions.options[HotKeyOptions.TO_BEGIN_INDEX] = getParameter( "toBeginHotKey" );

		appletReadyScript = getParameter( "readyScript" );

		bevelSize = getIntegerParameter( "bevelSize", bevelSize );
		background = getColorParameter( "background", background );
		foreground = getColorParameter( "foreground", foreground );

		waveBackground = getColorParameter( "waveBackground",
				background.brighter() );
		// Pick up modified foreground.
		waveForeground = getColorParameter( "waveForeground", foreground );

		buttonBackground = getColorParameter( "buttonBackground",
				buttonBackground );

		useDialog = getBooleanParameter( "useDialog", useDialog );
		temp = getParameter( "openDialogMessage" );
		if( temp != null )
		{
			openDialogMessage = temp;
		}

		// MOD ND 20060410 new gui control flags
		showPauseButton = getBooleanParameter( "showPauseButton",
				showPauseButton );
		showVUMeter = getBooleanParameter( "showVUMeter", showVUMeter );
		showSpeedControl = getBooleanParameter( "showSpeedControl", showSpeedControl );
		showPositionDisplay = getBooleanParameter( "showPositionDisplay",
				showPositionDisplay );

		showTimeText = getBooleanParameter( "showTimeText", showTimeText );
		putTimeOnTop = getBooleanParameter( "putTimeOnTop", putTimeOnTop );
		timeTextBold = getBooleanParameter( "timeTextBold", timeTextBold );
		timeTextSize = getIntegerParameter( "timeTextSize", timeTextSize );

		// MOD PLB 20080310 Allow developer to turn off popup error alerts.
		showErrorAlerts = getBooleanParameter( "showErrorAlerts",
				showErrorAlerts );

		timeChangeInterval = getIntegerParameter( "timeChangeInterval",
				timeChangeInterval );

		autoBackStep = getDoubleParameter( "autoBackStep", autoBackStep );
		autoPreview = getDoubleParameter( "autoPreview", autoPreview );

		startPosition = getDoubleParameter( "startPosition", startPosition );

		ignoreMissingSample = getBooleanParameter( "ignoreMissingSample",
				ignoreMissingSample );

		checkFormatParameter();

		// Set library folder for loading binaries like the
		// "FootPedalServer.exe".
		temp = getParameter( "libFolder" );
		if( (temp == null) && EXPIRE_AFTER_TEST_PERIOD )
		{
			temp = "libsbeta";
		}
		if( temp != null )
		{
			try
			{
				Installer.getInstance().setLibFolder( temp );
			} catch( Exception e )
			{
				Logger.println( 1, "Could not set installer libFolder to " + temp + ": " + e );
			}
		}

	}

	/**
	 * Set compression format.
	 *
	 * @param format
	 *            format ID, for example Player.FORMAT_IMA_ADPCM
	 */
	public void setFormat( int format )
	{
		this.format = format;
	}

	public int getFormat()
	{
		return format;
	}

	class DownloadAdapter implements DownloadListener
	{
		private boolean autoStart = false;
		private double loadStartPosition = 0.0;
		private boolean ready = false;
		private Throwable exc = null;
		// static not allowed in inner classes
		public final int ERROR_TIMEOUT = -1;
		public final int ERROR_DOWNLOAD = -2;

		public DownloadAdapter(boolean auto, double loadStartPosition)
		{
			autoStart = auto;
			this.loadStartPosition = loadStartPosition;
		}

		@Override
		public void caughtException( String msg, Throwable e )
		{
			exc = e;

			if( e instanceof IOException )
			{
				reportException( msg, exc );
			}
			else
			{
				reportExceptionAndHalt( e );
			}
			synchronized( this )
			{
				notifyAll();
			}
		}

		@Override
		public void gotEnoughToPlay( Recording recording )
				throws DeviceUnavailableException
		{
			Logger.println( 1,
					"DownloadAdapter.gotEnoughToPlay -> setRecording" );
			Recording oldRecording = getRecording();
			if( recording != oldRecording )
			{
				setRecording( recording );
				if( oldRecording != null )
				{
					oldRecording.erase();
				}
			}
			numChannels = 1; // TODO - what about stereo?
			setAudioSelection( loadStartPosition, loadStartPosition );
			synchronized( this )
			{
				ready = true;
				notifyAll();
			}
			executeJavaScriptCommand( downloadCompletionScript );
			// Play loaded sample on startup.
			if( autoStart )
			{
				play();
			}
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.softsynth.javasonics.recplay.DownloadListener#progress(int,
		 * int)
		 */
		@Override
		public void progress( int bytesDownloaded, int bytesTotal )
		{
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * com.softsynth.javasonics.recplay.DownloadListener#finished(com.softsynth
		 * .javasonics.recplay.Recording)
		 */
		@Override
		public void finished( Recording recording )
		{
			getPlayer().notifyState();
		}

		/** Return result of download attempt. */
		private int getResult()
		{
			int result = (ready) ? 0 : ((exc != null) ? ERROR_DOWNLOAD
					: ERROR_TIMEOUT);
			return result;
		}

		public synchronized int waitForReady( int msec )
		{
			try
			{
				long startTime = System.currentTimeMillis();
				while( !ready
						&& ((System.currentTimeMillis() - startTime) < msec) )
				{
					wait( 100 );
				}
				if( !ready )
				{
					Logger.println( 0, "waitForReady timed out after " + msec
							+ " msec." );
				}
			} catch( InterruptedException e )
			{
			}
			return getResult();
		}

		@Override
		public void failed()
		{
			executeJavaScriptCommand( downloadFailureScript );
		}

		@Override
		public double getStartPosition()
		{
			return loadStartPosition;
		}
	}

	/**
	 * Load a recording in a background thread. This should be run by a
	 * background thread to avoid hanging the GUI and to allow loading a file in
	 * a signed Applet.
	 */
	class LoadFromDiskCommand implements Runnable
	{
		@Override
		public void run()
		{
			Logger.println( 1, "LoadFromDiskCommand.run()" );
			if( initFailed )
			{
				RuntimeException e0 = new RuntimeException(
						"ListenUp Applet not yet initialized. Use readyScript parameter." );
				reportException( "Method loadRecording() called too soon.",
						e0 );
				throw e0;
			}
			guaranteeStopped();
			abortDownloading();

			Frame frame = new Frame();
			File loadFile = browseForFileLoad( frame, null,
					"Load a monophonic recording from your hard drive." );
			if( loadFile != null )
			{
				FileInputStream fis;
				try
				{
					fis = new FileInputStream( loadFile );
					BufferedInputStream bis = new BufferedInputStream( fis );
					Recording recording = downloader
							.loadRecordingFromInputStream( null, bis,
									loadFile.getAbsolutePath() );
					setRecording( recording );

					executeJavaScriptCommand( downloadCompletionScript );

				} catch( Throwable e )
				{
					executeJavaScriptCommand( downloadFailureScript );
					reportExceptionAfterStopAudio( e );
				}
			}
		}
	}

	public Player createPlayer( double frameRate ) throws Exception
	{
		Logger.println( 2, "PlayerApplet.createPlayer( " + frameRate
				+ " ) JSPlayer" );
		Player plr = new JSPlayer( preferNative, frameRate, numChannels );
		plr.setTimeChangeInterval( timeChangeInterval );
		return plr;
	}

	protected void setupPlayer( double frameRate ) throws Exception
	{
		// Use a player to playback existing audio.
		Player plr = createPlayer( frameRate );
		if( latencyInFrames > 0 )
		{
			plr.setLatencyInFrames( latencyInFrames );
		}
		setPlayer( plr );
		plr.start();
	}

	protected void setupAudio() throws Exception
	{
		// Make a player that will get used for all recordings.
		setupPlayer( 44100.0 );
		Recording reco = createRecording( 0 );
		reco.setFrameRate( 44100.0 );
		setRecording( reco );
	}

	protected Recording getRecording()
	{
		Player player = getPlayer();
		if( player == null )
			return null;
		else
			return player.getRecording();
	}

	protected void setRecording( Recording recording )
			throws DeviceUnavailableException
	{
		Logger.println(
				2,
				" setRecording() maxPlayable = "
						+ recording.getMaxSamplesPlayable() );
		getPlayer().setRecording( recording );
		updateSignalProcessors();
	}

	/**
	 * Create an empty recording with a fixed size.
	 */
	@Override
	public Recording createRecording( int maxSamples )
	{
		return new FixedRecording( maxSamples );
	}

	/**
	 * Create a recording with preset data.
	 */
	@Override
	public Recording createRecording( short[] data )
	{
		return new FixedRecording( data );
	}

	@Override
	public Recording createRecording()
	{
		return new DynamicRecording( Integer.MAX_VALUE, false, useFileCache );
	}

	protected void syncGUI()
	{
		// Synchronize Java display so buttons show up.
		Component parent = getParent();
		if( parent != null )
		{
			parent.validate();
			Toolkit toolkit = getToolkit();
			if( toolkit != null )
			{
				toolkit.sync();
			}
		}
	}

	protected void finishGUI()
	{
		syncGUI();
		if( useDialog )
		{
			optionalDialog.pack();
			// center on screen
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			int w = optionalDialog.bounds().width;
			int h = optionalDialog.bounds().height;
			int x = (screenSize.width - w) / 2;
			int y = (screenSize.height - h) / 2;
			optionalDialog.setBounds( x, y, w, h );
			// Dialog will show when button pressed.
		}
	}

	/**
	 * Called internally by browser when an Applet is started. Do <b>not </b>
	 * call from JavaScript.
	 */
	@Override
	public final void start()
	{
		if( initFailed )
		{
			return;
		}

		Logger.println( 1, "Begin PlayerApplet.start()" );
		startApplication();
		Logger.println( 1, "Finished PlayerApplet.start()" );
	}

	protected void startApplication()
	{
		try
		{
			removeAll();
			setupGUI();

			setupAudio();

			downloader = new AudioDownloader( this );
			downloader.setUserName( userName );
			downloader.setPassword( password );
			downloader.setIgnoreMissingSample( ignoreMissingSample );
			// Start thread here so it inherits the permissions of the Applet
			// and can create file caches.
			downloader.start();

			finishGUI();

			if( sampleURLname != null )
			{
				loadRecording( sampleURLname, autoPlay, false, startPosition );
			}

			uploadDiagnosticStatus( "start_ok", false, null );

			if( appletReadyScript != null )
			{
				Logger.println( "Execute appletReadyScript." );
				executeJavaScriptCommand( appletReadyScript );
			}

			// Check for forced error for testing.
			String forcedErrorMessage = getParameter("forceError");
			{
				if( forcedErrorMessage != null )
				{
					throw new RuntimeException("forceError: " + forcedErrorMessage);
				}
			}

		} catch( NoSuchMethodError e )
		{
			reportStartError( e, "Error may be due to using old Java." );
		} catch( Throwable e )
		{
			reportStartError( e, "Error starting ListenUp Applet." );
		}

		validate();
		repaint();
	}

	public void reportStartError( Throwable e, String advice )
	{
		removeAll();
		setLayout( new BorderLayout() );
		TextArea textArea = new TextArea( 6, 80 );
		add( "Center", textArea );
		textArea.append( "Java version " + System.getProperty( "java.version" )
				+ "\n" );
		textArea.append( advice + "\n" );
		textArea.append( e.toString() );
		reportException( advice, e );
		syncGUI();
	}

	/**
	 *
	 */
	protected void launchStartupTasks()
	{
	}

	/** Return the non-graphical player object. */
	protected Player getPlayer()
	{
		return player;
	}

	/**
	 * Set the non-graphical player object.
	 *
	 * @throws DeviceUnavailableException
	 */
	protected void setPlayer( Player pPlayer )
			throws DeviceUnavailableException
	{
		if( player != null )
		{
			if( progressToJavaScript != null )
			{
				player.removePlayerListener( progressToJavaScript );
			}
			player.removePlayerListener( this );
			getLuFactory().stop();
		}

		player = pPlayer;

		player.setAutoBackStep( autoBackStep );
		player.setAutoPreview( autoPreview );

		player.addPlayerListener( this ); // tell us when player finishes
		if( progressToJavaScript != null )
		{
			player.addPlayerListener( progressToJavaScript );
		}
		// playbackSpeedControlPanel.setEnabled(false);

		player.setRecording( getRecording() );
		updateSignalProcessors();

		// TODO - these should be listening to a player change event
		if( transportControl != null )
		{
			transportControl.setPlayer( pPlayer );
		}

		if( useSpeechMike || useOldSpeechMike )
		{
			getLuFactory().startSpeechMike( player, useOldSpeechMike, speechMikePort );
		}

		if( useFootPedal )
		{
			getLuFactory().startFootPedal( player );
		}

		getLuFactory().startHotKeys( player, hotKeyOptions );

		// Pass Applet parameters to transcriber.
		getLuFactory().parseParameters( this );

		if( positionBarControl != null )
		{
			positionBarControl.setPlayer( pPlayer );
			if( showWaveform )
			{
				((WaveDisplay) positionBarControl.getPositionCanvas())
						.setPlayer( pPlayer );
			}
		}

		if( timeTextDisplay != null )
		{
			timeTextDisplay.setPlayer( pPlayer );
		}
	}

	protected void updateSignalProcessors()
	{
	}

	protected void setupVisualTheme() throws IOException
	{
		Logger.println( "start setupVisualTheme()" );
		getLuFactory().createVisualTheme( this );
		setBackground( background );
		setForeground( foreground );
		Logger.println( "finish setupVisualTheme()" );
	}

	private ImageLabel createCompanyLogo() throws IOException
	{
		InputStream inStream;
		// Get logo from URL or data class.
		if( isLogoEnabled && (logoURL != null) )
		{
			inStream = logoURL.openConnection().getInputStream();
		}
		else
		{
			inStream = getClass().getResourceAsStream(
					"/images/JavaSonicsLogo.gif" );
		}

		if( inStream == null )
		{
			throw new RuntimeException( "Could not load JavaSonics logo image." );
		}
		Image img = VisualTheme.loadImage( this, inStream );
		inStream.close();
		ImageLabel lbl = new ImageLabel( img );
		return lbl;
	}

	protected boolean isLogoShown()
	{
		return showLogo;
	}

	protected TimeTextDisplay createTimeTextDisplay()
	{
		timeTextDisplay = new TimeTextDisplay( timeTextSize, timeTextBold );
		return timeTextDisplay;
	}

	public void repaintWaveDisplay()
	{
		if( waveDisplay != null )
		{
			waveDisplay.repaint();
		}
	}

	protected Component createPositionDisplay()
	{
		PositionCanvas pos;
		if( showWaveform )
		{
			waveDisplay = new WaveDisplay();
			waveDisplay.setBackground( waveBackground );
			waveDisplay.setForeground( waveForeground );
			pos = waveDisplay;
			waveDisplay
					.setWavePopupMenuFactory( new WavePopupMenuFactory( this ) );
		}
		else
		{
			pos = new PositionScrollbar();
		}
		positionBarControl = new PositionBarControl( pos );
		return pos;
	}

	protected void setWaveMenuEnabled( boolean flag )
	{
		if( waveDisplay != null )
		{
			waveDisplay.setPopupMenuEnabled( flag );
		}
	}

	private class ButtonLink extends JButton
	{
		private static final long serialVersionUID = 8039457590740568548L;
		URI uri;
		ButtonLink(String text, String uriName, String helpText )
		{
			super(text);
			try
			{
				this.uri = new URI(uriName);
			} catch( Exception e1 )
			{
				e1.printStackTrace();
				throw new RuntimeException(e1);
			}
	        setHorizontalAlignment(SwingConstants.LEFT);
	        Font font = new Font( "Verdana", Font.BOLD, 14 );
			setFont( font );
	        setOpaque( false );
	        setBorderPainted( false );
	        setContentAreaFilled( false );
	        setToolTipText( helpText );
	        addActionListener(new ActionListener(){
				@Override
				public void actionPerformed( ActionEvent e )
				{
					/* FIXME disable AppletContext for Jeff Seppi
					AppletContext appletContext = PlayerApplet.this.getAppletContext();
					if( appletContext != null )
			        {
						appletContext.showDocument(url,"_blank");
			        }
			        */

				    if (Desktop.isDesktopSupported()) {
				        try {
				          Desktop.getDesktop().browse(uri);
				        } catch (IOException exc) {
				        	Logger.println( "Exception linking to brand " + exc );
				        }
				     }
				    else
				    {
			        	Logger.println( "Could not link to brand. No Desktop." );
				    }
				}});
		}
	}

	/** Create Panel that contains logo and possible transport controls. */
	protected Panel createMainPanel()
	{
		Panel panel = new Panel();
		FlowLayout layout = new FlowLayout();
		layout.setHgap( packButtons ? 5 : 0 );
		layout.setVgap( 0 );
		panel.setLayout( layout );

		// panel.add( new Label(" JavaSonics ") );
		if( isBranded )
		{
			String domains = "http://www.javasonics.com"; // FIXME LICENSE
			panel.add( new ButtonLink("<HTML><u>" + domains + "</u></HTML>",
					"http://" + domains,
					"ListenUp licensed to " + domains ) );
		}
		else if( showLogo )
		{
			ImageLabel logo;
			try
			{
				logo = createCompanyLogo();
			} catch( IOException e )
			{
				e.printStackTrace();
				throw new RuntimeException( e.toString() );
			}
			panel.add( logo );
		}

		if( showTransport )
		{
			transportControl = getLuFactory().createTransportControl(
					getFrame(), canRecord, useTextButtons, showSpeedControl );
			transportControl.setButtonBackground( buttonBackground );
			if( !showPauseButton )
			{
				transportControl.removePauseButton();
			}

			if( packButtons )
			{
				transportControl.setLayout( new GridLayout( 1, 0 ) );
			}
			transportControl.setForceListen( forceListen );
			panel.add( transportControl );
		}

		if( showTimeText && putTimeOnTop )
		{
			panel.add( createTimeTextDisplay() );
		}

		return panel;
	}


	protected void addNorthRack( Component rack )
	{
		if( rack != null )
		{
			Panel panel = new Panel( new BorderLayout() );
			panel.add( "North", rack );
			nextPanel.add( "Center", panel );
			nextPanel = panel;
		}
	}

	protected void addSouthRack( Component rack )
	{
		if( rack != null )
		{
			nextPanel.add( "South", rack );
			// Unnest but do not go beyond outerPanel
			Panel temp = (Panel) nextPanel.getParent();
			if( temp == outerPanel )
			{
				// Insert another panel between outerPanel and nextPanel;
				Panel panel = new Panel( new BorderLayout() );
				panel.add( "Center", nextPanel );
				outerPanel.add( BorderLayout.CENTER, panel );
				nextPanel = panel;
			}
			else
			{
				nextPanel = temp;
			}
		}
	}

	protected void addCenterRack( Component rack )
	{
		if( rack != null )
		{
			nextPanel.add( "Center", rack );
		}
	}

	protected Component createWideMiddleDisplay()
	{
		if( showPositionDisplay && (showTimeText && !putTimeOnTop) )
		{
			Panel panelB = new Panel( new BorderLayout() );
			panelB.add( "Center", createPositionDisplay() );
			panelB.add( "East", createTimeTextDisplay() );
			return panelB;
		}
		else if( showPositionDisplay )
		{
			return createPositionDisplay();
		}
		else if( showTimeText && !putTimeOnTop )
		{
			return createTimeTextDisplay();
		}
		return null;
	}

	protected void addAudioGUI()
	{
		addNorthRack( createMainPanel() );
		addCenterRack( createWideMiddleDisplay() );
	}

	private Frame getFrame()
	{
		Container parent = getParent();
		while( (parent != null) && !(parent instanceof Frame) )
		{
			parent = parent.getParent();
		}
		if( parent != null )
			return (Frame) parent;
		else
			return null;
	}

	protected void setupGUI() throws IOException
	{
		setupVisualTheme();

		if( useDialog )
		{
			Frame frame = getFrame();
			if( frame == null )
			{
				throw new RuntimeException( "Could not get Applet Frame!" );
			}
			optionalDialog = new Dialog( frame, "ListenUp" )
			{
				/*
				 * Use funky old AWT 1.0.2 code because Netscape Java on Mac
				 * does not support WindowAdapter!
				 */
				@Override
				public boolean handleEvent( Event evt )
				{
					switch( evt.id )
					{
					case Event.WINDOW_DESTROY:
						optionalDialog.hide();
						return true;
					default:
						return super.handleEvent( evt );
					}
				}
			};

			showDialogButton = new Button( openDialogMessage );
			showDialogButton.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( ActionEvent e )
				{
					optionalDialog.show();
				}
			} );
			add( showDialogButton );

			guiContainer = optionalDialog;
		}
		else
		{
			guiContainer = this;
		}

		guiContainer.setLayout( new BorderLayout() );
		if( bevelSize > 0 )
		{
			outerPanel = new BevelledPanel( bevelSize );
		}
		else
		{
			outerPanel = new Panel();
		}
		guiContainer.add( "Center", outerPanel );

		outerPanel.setLayout( new BorderLayout() );
		nextPanel = outerPanel;

		try
		{
			addAudioGUI();
		} catch( SecurityException es )
		{
			throw es;
		} catch( Exception exc )
		{
			reportException(
					"Listenup Applet start probably cancelled by user.", exc );
			stop();
		}

		syncGUI();
	}

	@Override
	public void playerTimeChanged( Player player, double time )
	{
		// System.out.println("PlayerApplet.playerTimeChanged: time = " + time
		// );
	}

	@Override
	public void playerLevelChanged( Player player )
	{
	}

	private void haltGUI()
	{
		if( waitForInitLabel != null )
		{
			removeAll();
			if( optionalDialog != null )
				optionalDialog.hide();
			add( new Label( "Applet halted." ) );
			Container parent = getParent();
			if( parent != null )
			{
				parent.validate();
				Toolkit toolkit = getToolkit();
				if( toolkit != null )
					toolkit.sync();
			}
			waitForInitLabel = null;
		}
	}

	@Override
	public void reportExceptionAfterStopAudio( final Throwable exc )
	{
		// Stop recording or playback if there is an error. TODO recursion?
		stopAudio();
		reportException( exc );
	}

	public void reportException( Throwable exc )
	{
		reportException( null, exc );
	}

	public void reportException( final String message, final Throwable exc )
	{
		if( showErrorAlerts )
		{
			new Thread()
			{
				@Override
				public void run()
				{
					ErrorReporter dlg = new ErrorReporter( "JavaSonics Alert" );
					dlg.setBugReportable( PlayerApplet.this );
					if( message != null )
					{
						dlg.println( message );
					}
					dlg.showThrowable( exc );
				}
			}.start();
		}
		uploadDiagnosticStatus( "exception", true, exc.toString() );
	}

	public void reportExceptionAndHalt( Throwable exc )
	{
		haltGUI();
		reportExceptionAfterStopAudio( exc );
	}

	/**
	 * Enable or disable GUI buttons based on current mode of operation.
	 */
	@Override
	public synchronized void playerStateChanged( Player player, int state,
			Throwable thr )
	{
		Logger.println( 2, "PlayerApplet.playerStateChanged: state = " + state
				+ ", position = " + getPositionInSeconds() );

		/** Report any exceptions. */
		if( thr != null )
		{
			// TODO We used to halt here. But that could cause people to lose
			// work.
			reportExceptionAfterStopAudio( thr );
		}

		// Tell JavaScript we have changed transport state.
		// Old API
		executeJavaScriptCommand( transportListenerScript );

		reportStateChangeToDiagnosticServer( player, state, thr );
		repaintWaveDisplay();
	}

	private void reportStateChangeToDiagnosticServer( Player player, int state,
			Throwable thr )
	{
		// Report state change to diagnostic server.
		String diagMsg = null;
		if( state == Player.PLAYING )
		{
			diagMsg = "play_started";
		}
		else if( previousPlayerState == Player.PLAYING )
		{
			if( state == Player.PAUSED )
			{
				diagMsg = "play_paused";
			}
			else
			{
				diagMsg = "play_stopped";
			}

			// Enable Stop and Pause if we have heard the message once.
			if( (transportControl != null) && transportControl.getForceListen() )
			{
				// turn off forceListen
				transportControl.setForceListen( false );
			}
		}

		if( diagMsg != null )
		{
			Logger.println( 2,
					"PlayerApplet.reportStateChangeToDiagnosticServer: msg = "
							+ diagMsg );
			uploadDiagnosticStatus( diagMsg, false, null );
		}
		previousPlayerState = state;

	}

	class JavaScriptBackgroundCommand implements Runnable
	{
		private String command;

		JavaScriptBackgroundCommand(String command)
		{
			this.command = command;
		}

		// Call JavaScript in separate thread from main app.
		@Override
		public void run()
		{
			Logger.println( 1, "JavaScriptCallerThread.processCommand( "
					+ command + ")" );

			// Get the JavaScript window so we can call JavaScript from Java.
			try
			{
				setupLiveConnect();
				if( javaScriptWindow != null )
				{
					javaScriptWindow.eval( command );
				}
			} catch( Exception exc )
			{
				System.err.println( "Error executing JavaScript command: "
						+ command );
				exc.printStackTrace();
			}
		}
	}

	protected void stopExecutingJavaScript()
	{
		javaScriptExecutionAllowed = false;
		if( backgroundCommandProcessor != null )
		{
			backgroundCommandProcessor.abortCommands();
			Logger.println( 1,
					"stopExecutingJavaScript() aborted the background thread." );
			backgroundCommandProcessor.stop();
			backgroundCommandProcessor = null;
		}
	}

	public int getJavaScriptCommandQueueDepth()
	{
		if( backgroundCommandProcessor == null )
		{
			return 0;
		}
		else
		{
			return backgroundCommandProcessor.getQueueDepth();
		}
	}

	@Override
	public synchronized void executeJavaScriptCommand( String script )
	{
		if( javaScriptExecutionAllowed && (script != null)
				&& !("".equals( script )) )
		{
			// Get the JavaScript window so we can call JavaScript from Java.
			try
			{
				checkBackgroundCommandProcessor();
				Logger.println( 2, "executeJavaScriptCommand('" + script + "')" );
				JavaScriptBackgroundCommand jsCommand = new JavaScriptBackgroundCommand(
						script );
				backgroundCommandProcessor.sendCommand( jsCommand );
				Logger.println( 2, "executeJavaScriptCommand completed" );
			} catch( Exception exc )
			{
				System.err.println( "Error sending JavaScript command: "
						+ script );
				exc.printStackTrace();
			}
		}
	}

	private void checkBackgroundCommandProcessor()
	{
		if( backgroundCommandProcessor == null )
		{
			backgroundCommandProcessor = new BackgroundCommandProcessor();
			backgroundCommandProcessor.start();
		}
	}

	/**
	 * Clean up Applet. Called internally by browser. Do <b>not </b> call from
	 * JavaScript.
	 */
	@Override
	public void stop()
	{
		Logger.println( 1, "Begin PlayerApplet.stop()" );

		// Stop JavaScript from being called during shutdown and crashing
		// FireFox or Safari.
		downloadCompletionScript = null;
		downloadFailureScript = null;
		stopExecutingJavaScript();

		guaranteeStopped();

		if( downloader != null )
		{
			// abort any download in progress to fix bug [0034]
			abortDownloading();
			// stop background command processor
			downloader.stop();
		}

		if( getLuFactory() != null )
		{
			getLuFactory().stop();
		}

		if( player != null )
		{
			Logger.println( 2, "Begin player.stop()" );
			player.stop();
			Logger.println( 2, "Finished player.stop()" );
		}

		if( optionalDialog != null )
		{
			optionalDialog.hide();
		}

		teardownAudio();

		Logger.println( 1, "Finished PlayerApplet.stop()" );
	}

	/**
	 * Opposite of setupAudio()
	 */
	private void teardownAudio()
	{
		if( getRecording() != null )
		{
			// This will delete file caches.
			getRecording().erase();
		}
	}

	@Override
	public void destroy()
	{
		removeAll();
	}

	public void setTestParameter( String name, String value )
	{
		testParameters.put( name.toLowerCase(), value );
	}

	protected void setupTest()
	{
		System.out.println( "java.version = "
				+ System.getProperty( "java.version" ) );
		System.out.println( "java.vendor = "
				+ System.getProperty( "java.vendor" ) );

		// forceListen = true;
		setTestParameter( "autoPlay", "yes" );

		if( false )
		{
			setTestParameter( "transcription", "yes" );
			setTestParameter( "useSpeechMike", "no" );
			setTestParameter( "useFootPedal", "yes" );
		}
		setTestParameter( "debugLevel", "1" );
		setTestParameter( "showLogo", "true" );
		setTestParameter( "showWaveform", "true" );
		setTestParameter( "showPositionDisplay", "yes" );
		setTestParameter( "showTimeText", "yes" );
		setTestParameter( "putTimeOnTop", "true" );
		setTestParameter( "timeTextSize", "36" );
		setTestParameter( "timeTextBold", "true" );

		setTestParameter( "showErrorAlerts", "true" );
		setTestParameter( "packButtons", "false" );
		setTestParameter( "useFileCache", "false" );
		setTestParameter( "editable", "true" );
		//setTestParameter( "autoBackStep", "1.0" );
		setTestParameter( "ignoreMissingSample", "false" );
		setTestParameter( "useTextButtons", "no" );
		setTestParameter( "foreground", "405060" );
		setTestParameter( "background", "F0E0D0" );
		setTestParameter( "buttonBackground", "C0B0F0" );

		userName = "guest";
		password = "listenup";

		if( false )
		{
			hotKeyOptions.options[HotKeyOptions.RECORD_INDEX] = "r+alt+shift";
			hotKeyOptions.options[HotKeyOptions.PLAY_INDEX] = "p+alt+shift";
			hotKeyOptions.options[HotKeyOptions.STOP_INDEX] = "s+alt+shift";
		}

		// skinURLname =
		// TEST_URL + "/listenup/images/16buttonSkin.jpg";

		final String uploadsDir = TEST_URL + "/listenup/uploads";
		// String sampleName = "http://gensuitedev4.genusis.com/music.wav";
		String sampleName = uploadsDir + "/message_xyz.spx";
		// sampleName = TEST_URL + "/test/listenup/uploads/stream_whole.spx";
		// sampleName = "D:/test/listenup/uploads/stream_whole.spx";
		sampleName = uploadsDir + "/message_2minute_nick.spx";
		// sampleName = uploadsDir + "/msg1.spx";
		// sampleName =
		// "https://kenlaji.clinicyou.com/UploadSoundFiles/Generic/45/10292009101344PM45.wav";
		// sampleName = TEST_URL + "/samples/voice.nist";

		// Test downloading from a PHP script.
		// sampleName = TEST_URL + "/test/qa/handle_download_wave.php";
		// sampleName = TEST_URL + "/test/samples/K1_01_19461.spx"; // BAD

		// sampleName = TEST_URL + "/test/qa/murali_f1mod.spx";

		// sampleName = uploadsDir + "/nosuchmsg.spx";
		// sampleName = uploadsDir + "/message_hello.spx";
		// sampleName = uploadsDir + "/message_12345.wav";
		// sampleName = uploadsDir + "/welcome.wav";
		// sampleName = "http://www.javasonics.com/test/secure/welcome.wav";
		// sampleName = uploadsDir + "/message_12345.spx";
		// sampleName = uploadsDir + "/Clarinet_Bflat_062.aif";
		// sampleName = uploadsDir + "/message_abc.xyz?id=9722";
		// sampleName = uploadsDir + "/message_ulaw.wav";
		// sampleName = uploadsDir + "/file_is_missing.wav";
		// sampleName = "file:///D:/samples/Rastaman1samp2.aiff";
		// sampleName = "file:///D:/samples/test_g711.wav";
		// sampleName = uploadsDir + "/message_test.spx";
		// sampleName = uploadsDir + "/message_5min.spx";
		// sampleName = uploadsDir + "/message_2minute_nick.spx";
		// sampleName = "file:///D:/MyNewFile.wav";
		// sampleName = uploadsDir + "/message_25min.spx";
		// sampleName = uploadsDir + "/message_40min.spx";
		// sampleName = uploadsDir + "/message_12345.wav";
		// sampleName = uploadsDir + "/qa_r8000_adpcm.wav";
		// sampleName = TEST_URL
		// + "/test/clients/Full6CFD28554CBD499CB42EBEC6AB5F76E5.wav";

		// ERROR generating URLs
		// sampleName =
		// "http://www.javasonics.com/samples/file_not_found.wav";
		// sampleName = "http://www.javasonics.com/samples/garbage20.wav";
		// sampleName = uploadsDir + "/does_not_exist.spx";
		// sampleName = TEST_URL + "/test/qa/handle_download_halfwave.php";
		// sampleName = "http://www.nowhere-ncsdkhjdfkze.com/stuff.wav";
		// sampleName = TEST_URL + "/index.html";
		// Foreign sample!
		// sampleName =
		// "http://www.transjam.com/webdrum/samples/KickDrum44K.wav";

		// sine with finger snaps
		// sampleName = uploadsDir + "/msg_1228.spx";
		// sampleName = uploadsDir + "/msg_1235.spx";

		// sampleName = TEST_URL + "/listenup/examples/welcome.wav";
		setTestParameter( "sampleURL", sampleName );

		//setTestParameter( "startPosition", "37.8" );
	}

	// Just for testing.
	protected void delayedLoadRecording( int msec )
	{
		// Wait a while then load a recording using JS interface. */
		try
		{
			Thread.sleep( msec );
		} catch( InterruptedException e )
		{
		}
		System.out.println( "Try to loadRecording()" );
		String sampleName;
		// sampleName =
		// TEST_URL + "/listenup/uploads/message_test.spx" );
		// sampleName =
		// "http://www.transjam.com/webdrum/samples/KickDrum44K.wav" );
		// sampleName =
		// TEST_URL + "/listenup/uploads/bad_url.spx", true );
		// sampleName =
		// TEST_URL + "/listenup/uploads/msg_1305.spx";
		sampleName = TEST_URL + "/listenup/uploads/qa_r8000_adpcm.wav";
		sampleName = TEST_URL + "/listenup/uploads/bogus.wav";
		int result = loadRecording( sampleName );
		Logger.println( 0, "loadRecording returned " + result );
	}

	public void stopTestInFrame()
	{
		if( testFrame != null )
		{
			stop();
			destroy();
			testFrame.hide();
			testFrame = null;
		}
	}

	public void startTestInFrame()
	{
		startTestInFrame(100, 100, 900, 300);
	}

	public void startTestInFrame(int x, int y, int width, int height)
	{
		testFrame = new Frame( "JavaSonics ListenUp" )
		{
			/*
			 * Use funky old AWT 1.0.2 code because Netscape Java on Mac does
			 * not support WindowAdapter.
			 */
			@Override
			public boolean handleEvent( Event evt )
			{
				switch( evt.id )
				{
				case Event.WINDOW_DESTROY:
					stop();
					destroy();
					System.exit( 0 );
					return true;
				default:
					return super.handleEvent( evt );
				}
			}
		};

		testFrame.setBounds( x, y, width, height );
		testFrame.setLayout( new BorderLayout() );
		testFrame.add( "Center", this );
		testFrame.show();
		init();
		start();
		// frame.pack();
	}

	/* Can be run as either an application or as an applet. */
	public static void main( String args[] )
	{
		final PlayerApplet applet = new PlayerApplet();
		applet.runApplication(args);
	}

	public void parseArguments( String[] args )
	{
		Logger.println( 0, "ListenUp (C) Mobileer Inc" );
		Logger.println( 0, "ListenUp may only be used with a valid license issued by Mobileer Inc." );
		String codeBase = null;
		String propertyFileName = null;
		boolean helpPrinted = false;

		if( args.length == 0 )
		{
			printHelp();
			helpPrinted = true;
		}
		for (String arg : args)
		{
			if( arg.charAt( 0 ) == '-')
			{
				char option = arg.charAt( 1 );
				String value = arg.substring( 2 );
				switch(option)
				{
				case 'c':
					codeBase = value;
					break;
				case 'p':
					propertyFileName = value;
					break;
				case '?':
					printHelp();
					helpPrinted = true;
					break;
				}
			}
		}

		if (mEnableStandAlone)
		{
			mTestCodebase = codeBase;
			if( propertyFileName != null )
			{
				loadPropertiesFromFile(propertyFileName);
			}
		} else {
			setupTest();
		}
	}

	/**
	 * Run the Applet as an application.
	 * @param args
	 */
	protected void runApplication( String[] args )
	{
		int x = 100;
		int y = 100;
		int w = 900;
		int h = 600;
		for (String arg : args)
		{
			if( arg.charAt( 0 ) == '-')
			{
				char option = arg.charAt( 1 );
				String value = arg.substring( 2 );
				switch(option)
				{
				case 'x':
					x = Integer.parseInt( value );
					break;
				case 'y':
					y = Integer.parseInt( value );
					break;
				case 'w':
					w = Integer.parseInt( value );
					break;
				case 'h':
					h = Integer.parseInt( value );
					break;
				}
			}
		}

		parseArguments(args);

		startTestInFrame(x, y, w, h);
	}

	private void printHelp()
	{
		Logger.println( 0, "Application arguments:" );
		Logger.println( 0, "  -c{codebase}      URL of the codebase where the license file can be found" );
		Logger.println( 0, "  -p{parameterFile} local path of the name=value pair parameters" );
		Logger.println( 0, "  -x{xpos}          x location of the app frame " );
		Logger.println( 0, "  -y{ypos}          y location of the app frame " );
		Logger.println( 0, "  -w{width}         width of the app frame " );
		Logger.println( 0, "  -h{height}        height of the app frame " );
	}

	private void loadPropertiesFromFile( String propertyFileName )
	{
		InputStream input = null;

		try {
			input = new FileInputStream(propertyFileName);
			// load a properties file into a temp map
			Properties temporary = new Properties();
			temporary.load(input);
			// Convert names to lower case
			for( Object nameObject : temporary.keySet() )
			{
				String name = (String) nameObject;
				String value = temporary.getProperty( name );
				name = name.toLowerCase();
				testParameters.put( name, value );
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void testLoadAtPosition( final PlayerApplet applet,
			String url, double pos )
	{
		try
		{
			Thread.sleep(5000);
			applet.loadRecordingAtPosition( url, pos );
		} catch( InterruptedException e )
		{
			e.printStackTrace();
		}
	}

	protected static File browseForFileLoad( Frame frame, File defaultFile,
			String message )
	{
		return browseForFile( frame, defaultFile, message, FileDialog.LOAD );
	}

	protected static File browseForFile( Frame frame, File defaultFile,
			String message, int mode )
	{
		FileDialog fileDlg = new FileDialog( frame, message, mode );

		// WARNING - setFile can cause a crash in JVM 1.3 because of an AWT bug
		if( defaultFile != null )
		{
			fileDlg.setDirectory( defaultFile.getParent() );
			fileDlg.setFile( defaultFile.getName() );
		}
		fileDlg.setVisible( true );

		File file = null;
		String dirName = fileDlg.getDirectory();
		String fileName = fileDlg.getFile();
		if( fileName != null )
		{
			if( dirName != null )
				file = new File( dirName, fileName );
			else
				file = new File( fileName );
		}
		return file;
	}

	// JavaScript Interface =========================================
	/**
	 * Specify JavaScript command to be executed when player state changes, for
	 * example from STOPPED to PLAYING, RECORDING, etc.. For example:
	 *
	 * <pre>
	 * document.PlayerApplet.setRecorderStateChangedScript( &quot;updateForm();&quot; );
	 * </pre>
	 */
	public void setRecorderStateChangedScript( String scriptText )
	{
		transportListenerScript = scriptText;
	}

	/**
	 * Begin playing recorded audio. Continue until stopAudio() is called or end
	 * of recording is reached.
	 */
	public void play()
	{
		if( player != null )
		{
			player.playNormalSpeed();
		}
	}

	/**
	 * @return length of playable audio in seconds.
	 */
	public double getMaxPlayableTime()
	{
		if( player != null )
		{
			return player.getMaxPlayableTime();
		}
		else
		{
			return 0.0;
		}
	}

	/**
	 * @return beginning of selected region
	 */
	public double getStartTime()
	{
		if( player != null )
		{
			return player.getStartTime();
		}
		else
		{
			return 0.0;
		}
	}

	/**
	 * @return end of selected region
	 */
	public double getStopTime()
	{
		if( player != null )
		{
			return player.getStopTime();
		}
		else
		{
			return 0.0;
		}
	}

	/**
	 *
	 * @return current cursor position
	 */
	public double getPositionInSeconds()
	{
		if( player != null )
		{
			return player.getPositionInSeconds();
		}
		else
		{
			return 0.0;
		}
	}

	/**
	 * Set region to play or record in seconds.
	 */
	public void setAudioSelection( double startTime, double stopTime )
	{
		if( player != null )
		{
			player.setStartTime( startTime );
			player.setStopTime( stopTime );
			// This call will notify the listeners so they will redraw if
			// necessary.
			player.setPositionInSeconds( startTime );
			// repaint();
		}
	}

	/**
	 * Begin playing recorded audio. Continue until stopAudio() is called or end
	 * of section is reached.
	 */
	public void playBetween( double startTime, double stopTime )
	{
		if( player != null )
		{
			setAudioSelection( startTime, stopTime );
			player.playNormalSpeed();
		}
	}

	/**
	 * Stop playing or recording audio.
	 */
	public void stopAudio()
	{
		if( player != null )
		{
			player.stopAudio();
		}
	}

	/**
	 * Make sure that the player is stopped before continuing.
	 */
	protected void guaranteeStopped()
	{
		if( !isStopped() )
		{
			stopAudio();
			try
			{
				Logger.println( 1, "guaranteeStopped: waitUntilStopped" );
				waitUntilStopped( 4000 );
			} catch( InterruptedException e )
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param i
	 * @throws InterruptedException
	 */
	public boolean waitUntilStopped( int timeOutMSec )
			throws InterruptedException
	{
		if( player != null )
		{
			return player.waitUntilStopped( timeOutMSec );
		}
		return false;
	}

	/**
	 * @param i
	 * @throws InterruptedException
	 */
	public boolean waitUntilDownloaded( int timeOutMSec )
			throws InterruptedException
	{
		if( downloader != null )
		{
			return downloader.waitUntilComplete( timeOutMSec );
		}
		return false;
	}

	public double correlateSine( double frequency, double startTime,
			double endTime )
	{
		Recording reco = getRecording();
		double magnitude = -1.0;
		if( reco != null )
		{
			magnitude = reco.correlateSine( frequency, startTime, endTime );
		}
		return magnitude;
	}

	public void rewind()
	{
		if( (player != null) && isTranscriptionEnabled )
		{
			player.playRewind();
		}
	}

	public void fastForward()
	{
		if( (player != null) && isTranscriptionEnabled )
		{
			player.playFastForward();
		}
	}

	public void slowForward()
	{
		if( (player != null) && isTranscriptionEnabled )
		{
			player.playSlowForward();
		}
	}

	/**
	 * Pause playing or recording audio.
	 */
	public void pauseAudio()
	{
		if( player != null )
		{
			player.pauseAudio();
		}
	}

	/**
	 * Is audio player currently stopped or paused?
	 */
	public boolean isStopped()
	{

		return (player == null) ? true : player.isStopped();
	}

	/**
	 * Is audio player currently playing?
	 */
	public boolean isPlaying()
	{
		return (player == null) ? false
				: (player.getState() == Recorder.PLAYING);
	}

	/**
	 * Just to make JavaScript easier. Simply returns false because it can't
	 * record. Overridden in RecorderApplet.
	 */
	public boolean isRecording()
	{
		return false;
	}

	/** Does the current recording have any playable data? */
	public boolean isPlayable()
	{
		return (player == null) ? false : player.isPlayable();
	}

	public String getJavaVendor()
	{
		return System.getProperty( "java.vendor" );
	}

	public String getJavaVersion()
	{
		return System.getProperty( "java.version" );
	}

	/** Load a recording from a file on disk chosen by the user. */
	public void loadRecordingFromDisk()
	{
		checkBackgroundCommandProcessor();
		LoadFromDiskCommand loadCommand = new LoadFromDiskCommand();
		backgroundCommandProcessor.sendCommand( loadCommand );
	}

	/**
	 * Load a recording for playback and wait until we have something to play.
	 *
	 * @param recordingURL
	 * @return 0 for success or -1 on timeout or -2 for download error.
	 */
	public int loadRecording( String recordingURL )
	{
		return loadRecording( recordingURL, false, true );
	}

	/**
	 * Load a recording for playback. Do not wait for the recording to be
	 * loaded. Just return immediately.
	 */
	public void loadRecordingNoWait( String recordingURL )
	{
		loadRecording( recordingURL, false, false );
	}

	/**
	 * Load a recording for playback. Specify the initial starting position in seconds.
	 * Do not wait for the recording to be
	 * loaded. Just return immediately.
	 * @param recordingURL
	 * @param position Offset in seconds from beginning of recording.
	 */
	public void loadRecordingAtPosition( String recordingURL, double position )
	{
		loadRecording( recordingURL, false, false, position );
	}

	/**
	 * Load a recording for playback. Do not wait for the recording to be
	 * loaded. Just return immediately. Play recording when ready if autoStart
	 * is true.
	 */
	public void loadRecording( String recordingURL, boolean autoStart )
	{
		loadRecording( recordingURL, autoStart, false );
	}

	public int loadRecording( String recordingURL, boolean autoStart,
			boolean waitForLoad )
	{
		return loadRecording( recordingURL, autoStart,
			waitForLoad, 0.0 );
	}

	public int loadRecording( String recordingURL, boolean autoStart,
			boolean waitForLoad, double loadStartPosition )
	{
		int result = 0;
		if( initFailed )
		{
			RuntimeException e0 = new RuntimeException(
					"ListenUp Applet not yet initialized. Use readyScript parameter." );
			reportException( "Method loadRecording() called too soon.", e0 );
			throw e0;
		}

		// Abort any download currently in progress.
		// This fixes bug 0031
		abortDownloading();

		try
		{
			URL url = makeAbsoluteURL( recordingURL );
			// Prevent playing of samples from unlicensed domain.
			// getLicenseManager().checkDomain( url );

			if( player != null )
			{
				player.pauseAudio();
				player.waitUntilStopped( 4000 );
			}

			if( transportControl != null )
			{
				transportControl.setPlayEnabled( false );
			}

			DownloadAdapter waiter = new DownloadAdapter( autoStart, loadStartPosition );
			// Asynchronous request to download an audio message.
			downloader
					.requestDownload( makeAbsoluteURL( recordingURL ), waiter );

			if( waitForLoad )
			{
				// Wait a while to give sample time to download.
				result = waiter.waitForReady( 20 * 1000 );
			}
		} catch( SecurityException e1 )
		{
			SimpleDialog.alert( "Could not load recording " + recordingURL
					+ "\n" + e1.getMessage() );
		} catch( InterruptedException e2 )
		{
			SimpleDialog.alert( "Timed out waiting for audio to pause.\n" + e2.getMessage() );
		}
		return result;
	}

	/**
	 * Abort the downloading of any samples that are in progress and clear the
	 * queue.
	 */
	public void abortDownloading()
	{
		if( downloader != null )
		{
			downloader.abortCommands();
		}
	}

	/** Delete any previously recorded material. */
	public void erase()
	{
		abortDownloading();
		if( player != null )
		{
			player.erase();
		}
	}

	/** Delete selected material. */
	public void eraseSelected()
	{
		abortDownloading();
		if( player != null )
		{
			player.eraseSelected();
		}
	}

	public void loadMostRecent()
	{
		stashedRecordingManager.loadMostRecentRecording();
	}

	public boolean hasStashedRecordings()
	{
		return stashedRecordingManager.hasStashedRecordings();
	}

	public String getUserProperty( String key )
	{
		return userProperties.getProperty( key );
	}

	public void setEditable( boolean editable )
	{
		this.editable = editable;
		Recording reco = getRecording();
		if( reco != null )
		{
			reco.setEditable( editable );
		}
	}

	public boolean isEditable()
	{
		return editable;
	}

	/**
	 * @return the transportControl
	 */
	public TransportControl getTransportControl()
	{
		return transportControl;
	}

	/**
	 * @return the buttonBackground
	 */
	public Color getButtonBackground()
	{
		return buttonBackground;
	}

	/**
	 * @return the numChannels
	 */
	public int getNumChannels()
	{
		return numChannels;
	}

	@Override
	public void displayMessage( String string )
	{
		showStatus( string );
	}

	/**
	 * Specify JavaScript command to be executed when download has completed.
	 * For example:
	 *
	 * <pre>
	 * document.myApplet.setDownloadCompletionScript( &quot;downloadComplete();&quot; );
	 * </pre>
	 */
	public void setDownloadCompletionScript( String scriptText )
	{
		Logger.println( 1, "setDownloadCompletionScript('" + scriptText + "')" );
		downloadCompletionScript = scriptText;
	}

	/**
	 * Specify JavaScript command to be executed if upload fails. For example:
	 *
	 * <pre>
	 * document.myApplet.setDownloadFailureScript( &quot;downloadFailed();&quot; );
	 * </pre>
	 */
	public void setDownloadFailureScript( String scriptText )
	{
		Logger.println( 1, "setDownloadFailureScript('" + scriptText + "')" );
		downloadFailureScript = scriptText;
	}

}