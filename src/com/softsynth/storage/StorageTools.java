package com.softsynth.storage;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import com.softsynth.javasonics.error.ErrorReporter;
import com.softsynth.javasonics.util.Logger;

/**
 * Tools for accessing files on local disk.
 * 
 * @author Phil Burk (C) 2004
 */
public class StorageTools
{
	private static int index = 0;
	private final static String COMPANY = "softsynth";

	/***************************************************************************
	 * Create directory with the given name if it doesn't exist.
	 */
	public static void createDirectoryIfNeeded( File libDir )
	{
		if( !libDir.isDirectory() )
		{
			if( !libDir.mkdirs() )
			{
				Logger.println( "Could not make output directory " + libDir );
			}
		}
	}

	/**
	 * @return directory for company specific data
	 */
	public static File getCompanyDirectory( String companyName )
	{
		String userHome = System.getProperty( "user.home" );
		companyName = companyName.toLowerCase();
		// prepend '.' to make like other folders
		File homeDir = new File( userHome );
		return new File( homeDir, "." + companyName );
	}

	/**
	 * @return directory for company specific data
	 */
	public static File createCompanyDirectory( String companyName )
	{
		File companyDir = getCompanyDirectory( companyName );
		createDirectoryIfNeeded( companyDir );
		return companyDir;
	}

	public static File getCompanySubDirectory( String subDirName )
	{
		File companyDir = StorageTools.getCompanyDirectory( COMPANY );
		return new File( companyDir, subDirName );
	}

	public static File createCompanySubDirectory( String subDirName )
	{
		File file = null;
		try
		{
			File temp = getCompanySubDirectory( subDirName );
			StorageTools.createDirectoryIfNeeded( temp );
			file = temp;
		} catch( Exception exc )
		{
			ErrorReporter
					.show(
							"Could not create temp file. May need to use SUN Java or Signed Applet.",
							exc );
			throw new RuntimeException( exc.getMessage() );
		}
		return file;
	}

	private static void deleteFile( File file )
	{
		File companion = getCompanionFile( file );
		if( companion.exists() )
			companion.delete();
		Logger.println( 0, "Deleting old saved file: " + file + ", "
				+ new Date( file.lastModified() ) );
		file.delete();
	}

	private static File getCompanionFile( File file )
	{
		// getParentFile() is only in Java 1.2
		return new File( new File( file.getParent() ), file.getName() + ".txt" );
	}

	/**
	 * Make a Vector with most oldest files first.
	 * 
	 * @param subDirName
	 * @param filter
	 * @return
	 */
	static Vector getFilesByDate( String subDirName, FilenameFilter filter )
	{
		Vector filesByDate = new Vector();
		File companySubDir = StorageTools.getCompanySubDirectory( subDirName );
		if( companySubDir.exists() )
		{
			String fileNameList[] = companySubDir.list( filter );
			// Make a list by date.
			for( int i = 0; i < fileNameList.length; i++ )
			{
				File nextFile = new File( companySubDir, fileNameList[i] );
				// Insertion sort by increasing date.
				int vectorSize = filesByDate.size();
				int location = vectorSize;
				for( int j = 0; j < vectorSize; j++ )
				{
					File candidate = (File) filesByDate.elementAt( j );
					if( nextFile.lastModified() <= candidate.lastModified() )
					{
						location = j;
						break;
					}
				}
				filesByDate.insertElementAt( nextFile, location );
			}
		}
		return filesByDate;
	}

	/**
	 * Make list of all files in directory and then delete any older than
	 * maxDays or more than maxFiles.
	 */
	public static int cleanCompanySubDirectory( String subDirName,
			FilenameFilter filter, int maxFiles, int maxDays )
	{
		int numDeleted = 0;
		Vector filesByDate = getFilesByDate( subDirName, filter );
		// Get rid of files over the limit.
		while( filesByDate.size() > maxFiles )
		{
			File nextFile = (File) filesByDate.firstElement();
			deleteFile( nextFile );
			filesByDate.removeElementAt( 0 );
		}
		// Get rid of too old files.
		Enumeration enumer = filesByDate.elements();
		long cutoffDate = System.currentTimeMillis()
				- (maxDays * 1000L * 60L * 60L * 24L);
		while( enumer.hasMoreElements() )
		{
			File nextFile = (File) enumer.nextElement();
			if( nextFile.lastModified() < cutoffDate )
			{
				deleteFile( nextFile );
			}
		}
		return numDeleted;
	}

	public static File createTempFile( String subDirName, String suffix )
	{
		File file = null;
		try
		{
			File companySubDir = StorageTools
					.createCompanySubDirectory( subDirName );
			String tempName = StorageTools.createTempName( suffix );
			file = new File( companySubDir, tempName );
		} catch( Exception exc )
		{
			ErrorReporter
					.show(
							"Could not create temp file. May need to use SUN Java or Signed Applet.",
							exc );
			throw new RuntimeException( exc.getMessage() );
		}
		return file;
	}

	public static File createSafeFile( String subDirName, String fileName )
	{
		File file = null;
		try
		{
			File companyDir = StorageTools.createCompanyDirectory( COMPANY );
			File subDir = new File( companyDir, subDirName );
			StorageTools.createDirectoryIfNeeded( subDir );
			// Strip path from name in case somebody is trying to hack us.
			File candidateFile = new File( fileName );
			String cleanName = candidateFile.getName();
			file = new File( subDir, cleanName );
		} catch( Exception exc )
		{
			ErrorReporter
					.show(
							"Could not create file. May need to use SUN Java or Signed Applet.",
							exc );
			throw new RuntimeException( exc.getMessage() );
		}
		return file;
	}

	/**
	 * @return
	 */
	public static synchronized String createTempName( String suffix )
	{
		// Bump index just in case we make two files at the same time.
		return "temp_" + index++ + "_" + System.currentTimeMillis() + suffix;
	}

	public static File getMostRecentFile( String stashDir, FilenameFilter filter )
	{
		File mostRecentFile = null;
		File companySubDir = StorageTools.getCompanySubDirectory( stashDir );
		if( companySubDir.exists() )
		{
			String fileNameList[] = companySubDir.list( filter );
			// Look for most recent
			for( int i = 0; i < fileNameList.length; i++ )
			{
				File nextFile = new File( companySubDir, fileNameList[i] );
				if( (mostRecentFile == null) )
				{
					mostRecentFile = nextFile;
				}
				else if( nextFile.lastModified() > mostRecentFile
						.lastModified() )
				{
					mostRecentFile = nextFile;
				}
			}
		}
		return mostRecentFile;
	}
}
