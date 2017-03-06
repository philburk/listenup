package com.softsynth.javasonics.recplay;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.softsynth.javasonics.util.Logger;
import com.softsynth.storage.DynamicBuffer;

public class SaveLocalCommand implements Runnable
{
	File saveFile;
	DynamicBuffer compressedImage;
	IUserDisplay userDisplay;

	public SaveLocalCommand(IUserDisplay userDisplay, File saveFile, DynamicBuffer compressedImage)
	{
		this.saveFile = saveFile;
		this.compressedImage = compressedImage;
		this.userDisplay = userDisplay;
	}

	public DynamicBuffer getCompressedImage()
	{
		return compressedImage;
	}

	public File getSaveFile()
	{
		return saveFile;
	}

	/**
	 * Save the current recording as a WAV file. Build a default name using the
	 * date.
	 * 
	 */
	public void run()
	{
		try
		{
			DynamicBuffer compressedImage = getCompressedImage();
			File saveFile = getSaveFile();
			
			InputStream compressedStream = compressedImage.getInputStream();
			FileOutputStream outStream = new FileOutputStream( saveFile );
			DynamicBuffer.writeStreamToStream( compressedStream, outStream );
			outStream.close();
			Logger.println( 0, "Recording saved to " + saveFile );
			userDisplay.displayMessage( "File saved to " + saveFile);
		} catch( IOException e )
		{
			userDisplay.reportExceptionAfterStopAudio( e );
		}
	}
}
