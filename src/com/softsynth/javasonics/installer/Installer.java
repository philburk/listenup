package com.softsynth.javasonics.installer;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Observable;
import java.util.Observer;

import com.softsynth.javasonics.error.ErrorReporter;
import com.softsynth.javasonics.util.Logger;

/**
 * Various tools to support the installation of Native Libraries and
 * Executables.
 * 
 * @author Phil Burk (C) 2002 Phil Burk
 */

public class Installer
{
	private static Installer instance = null;
	private VersionManager versionManager;

	public static final String WEBSITE_URL_NAME = "http://www.javasonics.com/";
	private String libFolder = "libs";
	public static final String COMPANY_NAME = "javasonics";

	private Installer() throws IOException
	{
		versionManager = new VersionManager( COMPANY_NAME, getLibFolderUrl() );
	}

	private String getLibFolderUrl()
	{
		return WEBSITE_URL_NAME + libFolder;
	}

	public static Installer getInstance() throws IOException
	{
		if( instance == null )
		{
			instance = new Installer();
		}
		return instance;
	}

	public void loadOrInstallLibrary( Frame frame, String libName )
	{
		try
		{
			getInstance().installNativeLibraryIfNeeded( frame, libName );
			try
			{
				System.loadLibrary( libName );
			} catch( UnsatisfiedLinkError e2 )
			{
				ErrorReporter.show( "Could not load native library!", e2 );
			}
		} catch( IOException e3 )
		{
			ErrorReporter.show( "Tried to install native library!", e3 );
		}
	}

	/**
	 * @param frame
	 *            for modal permission dialog
	 * @param libName
	 *            to be loaded. Suffix will be added if needed.
	 */
	public void installNativeLibraryIfNeeded( Frame frame, String libName )
			throws IOException
	{
		File binFile = LibraryInstaller.createNativeFile( libName );
		installBinaryIfNeeded( frame, binFile );
	}

	/**
	 * @param frame
	 *            for modal permission dialog
	 * @param libName
	 *            to be loaded. Suffix will be added if needed.
	 */
	public void installCompanyLibraryIfNeeded( Frame frame, String libName )
			throws IOException
	{
		// Add .exe for Windows executables.
		int hostType = LibraryInstaller.determineHost();
		if( hostType == LibraryInstaller.HOST_WINDOWS )
		{
			libName += ".dll";
		}
		File binFile = LibraryInstaller.createCompanyFile( libName );
		installBinaryIfNeeded( frame, binFile );
	}

	/**
	 * @param frame
	 *            for modal permission dialog
	 * @param appName
	 *            to be loaded. Suffix will be added if needed.
	 */
	public void installExecutableIfNeeded( Frame frame, String appName )
			throws IOException
	{
		Logger.println( 1, "installExecutableIfNeeded( " + frame + ", "
				+ appName + " )" );
		// Add .exe for Windows executables.
		int hostType = LibraryInstaller.determineHost();
		if( hostType == LibraryInstaller.HOST_WINDOWS )
		{
			appName += ".exe";
		}
		Logger.println( 1, "installExecutableIfNeeded: hostType = " + hostType
				+ ", " + appName );
		File binFile = LibraryInstaller.createCompanyFile( appName );
		installBinaryIfNeeded( frame, binFile );
	}

	private void installBinaryIfNeeded( Frame frame, File binFile )
			throws IOException
	{
		String binName = getLibFolderUrl() + "/" + binFile.getName() + "?time="
				+ System.currentTimeMillis();
		Logger.println( 1, "installExecutableIfNeeded: bin url = \"" + binName
				+ '"' );
		URL url = new URL( binName );
		if( !binFile.exists() )
		{
			Logger.println( 0,
					"installBinaryIfNeeded: file does not exist, install it?" );
			askToInstallCode( frame, url, binFile );
		}
		else if( !versionManager.isCurrent( binFile.getName() ) )
		{
			Logger
					.println( 0,
							"installBinaryIfNeeded: version not current, reinstall it?" );
			String msg = "May I please install a more recent version in \""
					+ binFile.getParent() + "\"?";
			askToInstallCode( frame, url, binFile, msg );
		}
		else
		{
			Logger.println( 1, "installBinaryIfNeeded: already installed." );
		}
	}

	/**
	 * @param url
	 *            remote file to be downloaded
	 * @param destFile
	 *            local file to be created
	 */
	private void askToInstallCode( Frame frame, URL url, File destFile )
			throws IOException
	{
		String msg = "May I please install it in \"" + destFile.getParent()
				+ "\"?";
		askToInstallCode( frame, url, destFile, msg );
	}

	/**
	 * @param url
	 *            remote file to be downloaded
	 * @param destFile
	 *            local file to be created
	 */
	private void askToInstallCode( Frame frame, URL url, File destFile,
			String message ) throws IOException
	{
		// Verify that we can access the binary before asking user to install.
		URLConnection connection = url.openConnection();
		int binSize = connection.getContentLength();
		if( binSize < 0 )
		{
			throw new IOException( "Could not read size of " + url );
		}
		Logger.println( 0, "askToInstallCode: url = " + url );
		Logger.println( 0, "askToInstallCode: binSize = " + binSize );
		AskToInstallDialog dialog = new AskToInstallDialog( frame, destFile
				.getName(), destFile.getParent(), message );
		if( dialog.ask() )
		{
			Logger.println( 0, "askToInstallCode: user requested download." );
			final InstallProgressDialog progressDialog = new InstallProgressDialog(
					frame );

			final LibraryInstaller installTools = new LibraryInstaller();
			Observer observer = new Observer()
			{
				public void update( Observable o, Object arg )
				{
					progressDialog
							.showProgress( installTools.getBytesDownloaded(),
									installTools.getTotalBytes() );
				}
			};
			installTools.addObserver( observer );

			final URL urlFinal = url;
			final File destFileFinal = destFile;
			Thread thread = new Thread()
			{
				public void run()
				{
					try
					{
						Logger.println( 0,
								"askToInstallCode: copy thread started." );
						installTools.copyURLToFile( urlFinal, destFileFinal );
						versionManager.updateVersionInfo( destFileFinal
								.getName() );
						Logger.println( 0,
								"askToInstallCode: copy thread finished." );
					} catch( Exception e )
					{
						Logger.println( 0, "askToInstallCode: copy failed." );
						ErrorReporter
								.show(
										"Tried to install native library!\n"
												+ "You may need administrative privilege to install DLLs.",
										e );
					}
				};
			};
			thread.start();
			if( !progressDialog.ask() )
			{
				Logger
						.println( 0,
								"askToInstallCode: user cancelled download." );
				installTools.cancel();
			}
		}
		else
		{
			Logger.println( 0, "askToInstallCode: user refused download." );
		}
	}

	/**
	 * @return the libFolder
	 */
	public String getLibFolder()
	{
		return libFolder;
	}

	/**
	 * @param libFolder
	 *            the libFolder to set
	 */
	public void setLibFolder( String libFolder )
	{
		this.libFolder = libFolder;
		versionManager.setLibDirUrlName( getLibFolderUrl() );
	}
}