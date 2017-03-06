package com.softsynth.javasonics.installer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Vector;

import com.softsynth.storage.StorageTools;

/**
 * Various tools to support the installation of Native Libraries.
 * 
 * @author Phil Burk (C) 2002 Phil Burk
 */

public class LibraryInstaller extends Observable
{
	public final static int HOST_UNKNOWN = 0;
	public final static int HOST_WINDOWS = 1;
	public final static int HOST_MACINTOSH = 2;
	public final static int HOST_UNIX = 3;

	private int totalBytes;
	private int bytesDownloaded;
	private boolean go;

	/** Return index identifying host OS. */
	public static int determineHost()
	{
		int host = HOST_UNKNOWN;
		String osName = System.getProperty( "os.name" );
		if( osName != null )
		{
			if( osName.startsWith( "Windows" ) )
				host = HOST_WINDOWS;
			else if( osName.startsWith( "Unix" ) )
				host = HOST_UNIX;
			else if( osName.startsWith( "Mac" ) )
				host = HOST_MACINTOSH;
			else
				host = HOST_UNKNOWN;
		}
		return host;
	}

	public static boolean isHostWindows()
	{
		return( determineHost() == HOST_WINDOWS );
	}
	
	public static boolean isHostMacintosh()
	{
		return( determineHost() == HOST_MACINTOSH );
	}
	
	private void copyURLToStream( URL url, OutputStream outStream )
			throws IOException
	{
		// Open URL
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		totalBytes = conn.getContentLength();
		int responseCode = conn.getResponseCode();
		if( responseCode != 200 )
		{
			throw new IOException( " Code " + responseCode + ", " + conn.getResponseMessage() + ", " + url);
		}
		InputStream inStream = conn.getInputStream();

		byte[] data = new byte[2048];
		int count = 0;
		while( go )
		{
			// May not get full array when reading from URL!
			int numRead = inStream.read( data );
			if( numRead < 0 )
				break;
			count += numRead;
			outStream.write( data, 0, numRead );
			setBytesDownloaded( count );
		}

		inStream.close();
	}

	protected void copyURLToFile( URL url, File destFile ) throws IOException
	{
		go = true;
		boolean exists = destFile.exists();
		FileOutputStream outStream = null;
		try
		{
			// Open File to Write
			outStream = new FileOutputStream( destFile );
			copyURLToStream( url, outStream );
		} catch( IOException exc )
		{
			if( exists )
			{
				System.out
						.println( "Existing native library was probably in use." );
				System.out
						.println( "Close all browser windows, restart browser and reinstall before using plugin." );
			}
			throw exc;
		} finally
		{
			if( outStream != null )
				outStream.close();
		}
		if( !go )
		{
			// download was cancelled so delete file.
			destFile.delete();
		}
	}

	public void cancel()
	{
		go = false;
	}

	public static File createNativeFile( String libraryName )
	{
		// Convert to system specific name for a JNI library.
		String libraryFileName = System.mapLibraryName( libraryName );

		File libDir = getNativeLibraryDirectory();
		StorageTools.createDirectoryIfNeeded( libDir );
		File binFile = new File( libDir, libraryFileName );
		return binFile;
	}

	public static File createCompanyFile( String fileName )
	{
		File libDir = StorageTools.createCompanyDirectory( Installer.COMPANY_NAME );
		File binFile = new File( libDir, fileName );
		return binFile;
	}

	public void installNativeCode( String libraryName, String urlDir )
			throws IOException
	{
		File binFile = createNativeFile( libraryName );
		URL url = new URL( urlDir + "/" + binFile.getName() );
		copyURLToFile( url, binFile );
	}

	/**
	 * Break a string apart into separate strings based on the separator
	 * character. This can be used to parse a pathname into separate filenames.
	 */
	private static Vector splitString( String text, char separator )
	{
		Vector strings = new Vector();
		int index;
		int last = 0;
		while( true )
		{
			index = text.indexOf( separator, last );
			if( index < 0 )
				break;
			String temp = text.substring( last, index );
			strings.addElement( temp );
			last = index + 1;
		}
		return strings;
	}

	private static String getBestWindowsDir()
	{
		String name = null;
		String path = System.getProperty( "java.library.path" );
		if( path != null )
		{
			Vector strings = splitString( path, File.pathSeparatorChar );
			// First look for directory in Windows
			Enumeration stringers = strings.elements();
			while( stringers.hasMoreElements() )
			{
				String fileName = (String) stringers.nextElement();
				if( fileName.indexOf( "C:\\WIN" ) == 0 )
				{
					return fileName;
				}
			}
			// Just use first one.
			name = (String) strings.elementAt( 0 );
		}
		return name;
	}

	/**
	 * @return
	 */
	private static File getNativeLibraryDirectory()
	{
		File dir = null;
		int osHost = determineHost();
		switch( osHost )
		{
		case HOST_WINDOWS:
			dir = new File( getBestWindowsDir() );
			break;
		}
		return dir;
	}

	/**
	 * Load native code using privileged access. This allows unsigned Applets
	 * to use installed plugins.
	 */
	/*
	 * private static boolean loadLibraryPriveledgedZZZ( String libName ) {
	 * final String libNameFinal = libName; Boolean result = (Boolean)
	 * AccessController .doPrivileged( new PrivilegedAction() { public Object
	 * run() { System.loadLibrary( libNameFinal ); return new Boolean( true ); } } );
	 * return (result != null); }
	 */
	/**
	 * @return Returns the totalBytes.
	 */
	public int getTotalBytes()
	{
		return totalBytes;
	}

	/**
	 * @return Returns the bytesDownloaded.
	 */
	public int getBytesDownloaded()
	{
		return bytesDownloaded;
	}

	/**
	 * @param bytesDownloaded
	 *            The bytesDownloaded to set.
	 */
	private void setBytesDownloaded( int bytesDownloaded )
	{
		this.bytesDownloaded = bytesDownloaded;
		setChanged();
		notifyObservers();
	}
}