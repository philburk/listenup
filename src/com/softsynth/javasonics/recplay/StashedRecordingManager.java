package com.softsynth.javasonics.recplay;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

import com.softsynth.javasonics.util.Logger;
import com.softsynth.storage.DynamicBuffer;
import com.softsynth.storage.StorageTools;

/**
 * Save and restore recording from a stashed area on local disk.
 *
 * @author Phil Burk (C) 2008 Mobileer Inc
 */
public class StashedRecordingManager
{
	PlayerApplet applet;
	private boolean stashedRecordingsExtant = false;
	private static int maxFiles = 40;
	private final static int MAX_DAYS = 30;

	class AudioFileFilter implements FilenameFilter
	{
		@Override
		public boolean accept( File dir, String name )
		{
			String lowerName = name.toLowerCase();
			return (lowerName.endsWith( ".wav" ) || lowerName.endsWith( ".spx" ));
		}
	}

	public StashedRecordingManager(PlayerApplet applet)
	{
		this.applet = applet;
		// This will fail if running an unsigned PlayerApplet.
		try
		{
			String stashDir = getStashDir();
			File recentFile = StorageTools.getMostRecentFile( stashDir,
					new AudioFileFilter() );
			stashedRecordingsExtant = (recentFile != null);
		} catch( Exception exc )
		{
			Logger.println( 2, "StashedRecordingManager caught "
					+ exc.getMessage() );
		}
	}

	private String getStashDir()
	{
		String licenseHash = "donotuse"; // FIXME applet.getLicenseManager().getLicenseHash();
		return "stash/" + licenseHash;
	}

	public String saveRecordedMessage( String savedFileName,
			InputStream compressedImage )
	{
		String msg = "Could not save file.";
		try
		{
			try
			{
				String stashDir = getStashDir();
				// Clean up before we make new files.
				// Don't use a filter because we want to clean all file types.
				StorageTools.cleanCompanySubDirectory( stashDir, null,
						maxFiles, MAX_DAYS );

				File saveFile = StorageTools.createSafeFile( stashDir,
						savedFileName );
				FileOutputStream outStream = new FileOutputStream( saveFile );
				DynamicBuffer.writeStreamToStream( compressedImage, outStream );
				outStream.close();
				msg = "Recorded message saved to " + saveFile;
				Logger.println( 1, msg );
				stashedRecordingsExtant = true;
			} catch( IOException e )
			{
				applet.reportExceptionAfterStopAudio( e );
			}
		} catch( SecurityException e )
		{
			System.err.println( e );
		}
		return msg;
	}

	public void loadMostRecentRecording()
	{
		try
		{
			String stashDir = getStashDir();
			File recentFile = StorageTools.getMostRecentFile( stashDir,
					new AudioFileFilter() );
			if( recentFile != null )
			{
				try
				{
					// The following only works on Java 1.2
					// URL url = recentFile.toURL();
					// The following works on Java 1.1
					URL url = new URL( "file:" + recentFile.getAbsolutePath() );
					applet.loadRecording( url.toString() );
				} catch( MalformedURLException e )
				{
					applet.reportExceptionAfterStopAudio( e );
				}
			}
		} catch( SecurityException e )
		{
			System.err.println( e );
		}
	}

	public boolean hasStashedRecordings()
	{
		return stashedRecordingsExtant;
	}

	public static int getMaxFiles()
	{
		return maxFiles;
	}

	public static void setMaxFiles( int maxFiles )
	{
		StashedRecordingManager.maxFiles = maxFiles;
	}
}
