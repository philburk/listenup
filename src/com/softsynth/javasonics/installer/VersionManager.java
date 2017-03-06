package com.softsynth.javasonics.installer;

import java.io.*;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import com.softsynth.javasonics.util.Logger;

/**
 * Manages installed versions of libraries and native code.
 * 
 * @author Phil Burk (C) 2004
 */
class VersionManager
{
	private Properties localProperties;
	private Properties remoteProperties;
	private String libDirUrlName;
	private String PROP_FILE_NAME = "installer.properties";

	public VersionManager(String pCompanyName, String pLibDirUrlName)
			throws IOException
	{
		libDirUrlName = pLibDirUrlName;
		try
		{
			loadLocalProperties();
		} catch( IOException e )
		{
			// That's OK. It may not have been created yet.
			Logger
					.println( 0,
							"VersionManager. Local ListenUp properties could not be loaded." );
			Logger.println( 1, "VersionManager. caught " + e );
		}
	}

	private String createVersionKey( String appName )
	{
		return appName.toLowerCase() + ".version";
	}

	public void updateVersionInfo( String appName ) throws IOException
	{
		if( localProperties == null )
		{
			localProperties = new Properties();
		}
		loadRemoteProperties();
		if( remoteProperties != null )
		{
			double remoteVersion = getVersion( remoteProperties, appName );
			localProperties.setProperty( createVersionKey( appName ), Double
					.toString( remoteVersion ) );
			saveLocalProperties();
		}
	}

	/**
	 * @throws IOException
	 * 
	 */
	private void saveLocalProperties() throws IOException
	{
		File localFile = LibraryInstaller
				.createCompanyFile( "installer.properties" );
		FileOutputStream outStream = new FileOutputStream( localFile );
		localProperties.save( outStream,
				"Version info for installed native code." );
		outStream.close();
	}

	public boolean isCurrent( String appName ) throws IOException
	{
		loadRemoteProperties();
		if( remoteProperties == null )
		{
			Logger.println( 0, "VersionManager has no remote version info." );
			return true;
		}

		if( localProperties == null )
			return false;

		double localVersion = getVersion( localProperties, appName );
		double remoteVersion = getVersion( remoteProperties, appName );
		Logger.println( 1, "VersionManager: local = " + localVersion
				+ ", remote = " + remoteVersion );

		return localVersion >= remoteVersion;
	}

	/**
	 * @param props
	 *            to get version from
	 * @param appName
	 * @return
	 */
	private double getVersion( Properties props, String appName )
	{
		double version = 0.0;
		String key = createVersionKey( appName );
		String versionText = props.getProperty( key );
		if( versionText == null )
		{
			Logger.println( 0, "VersionManager: getVersion cannot find " + key );
		}
		else
		{
			version = Double.parseDouble( versionText );
		}
		return version;
	}

	/**
	 * Load Installer information from local file. We use it to keep track of
	 * installed version number and maybe other info.
	 */
	private void loadLocalProperties() throws IOException
	{
		File localFile = LibraryInstaller.createCompanyFile( PROP_FILE_NAME );
		FileInputStream inStream = new FileInputStream( localFile );
		Properties temp = new Properties();
		temp.load( inStream );
		inStream.close();
		localProperties = temp;
	}

	/**
	 * Load current version information from remote server. The server may not
	 * be available if we are on a LAN with no Internet access.
	 */
	private void loadRemoteProperties() throws IOException
	{
		if( remoteProperties == null )
		{
			// Add time to defeat caches.
			URL url = new URL( libDirUrlName + "/" + PROP_FILE_NAME + "?time="
					+ System.currentTimeMillis() );
			Logger.println( 0, "loadRemoteProperties: from " + url );
			try
			{
				URLConnection conn = url.openConnection();
				InputStream inStream = conn.getInputStream();
				Properties temp = new Properties();
				temp.load( inStream );
				inStream.close();
				remoteProperties = temp;
			} catch( Exception exc )
			{
				Logger.println( 0, "Could not connect to " + libDirUrlName
						+ " for latest version of native code. " + exc );
			}
		}
	}

	/**
	 * @return the libDirUrlName
	 */
	public String getLibDirUrlName()
	{
		return libDirUrlName;
	}

	/**
	 * @param libDirUrlName
	 *            the libDirUrlName to set
	 */
	public void setLibDirUrlName( String libDirUrlName )
	{
		this.libDirUrlName = libDirUrlName;
	}

}