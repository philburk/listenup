package com.softsynth.storage;

/**
 * @author Phil Burk (C) 2004
 */
public class DynamicBufferFactory
{
	private static boolean useFiles = false;
	
	public static DynamicBuffer createDynamicBuffer()
	{
		if( useFiles )
		{
			return new DynamicFileBuffer();
		}
		else
		{
			return new DynamicMemoryBuffer();
		}
	}
	
	/**
	 * @return should temporary buffers be stored in files?
	 */
	public static boolean isUseFiles()
	{
		return useFiles;
	}
	
	/**
	 * @param useFiles true if temporary buffers should be stored in files
	 */
	public static void setUseFiles( boolean useFiles )
	{
		DynamicBufferFactory.useFiles = useFiles;
	}
}
