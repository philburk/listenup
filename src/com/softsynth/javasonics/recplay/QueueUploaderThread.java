package com.softsynth.javasonics.recplay;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import com.softsynth.javasonics.util.BackgroundCommandProcessor;
import com.softsynth.javasonics.util.Logger;
import com.softsynth.storage.DynamicBuffer;
import com.softsynth.upload.FileUploader;
import com.softsynth.upload.ProgressListener;

/**
 * Upload one recording taken from a queue.
 * 
 * @author Phil Burk (C) 2008 Mobileer Inc
 */
public class QueueUploaderThread extends BackgroundCommandProcessor
{
	private FileUploader fileUploader;
	private RecorderUploadApplet applet;
	private int uploadCount;
	private int uploadCompletionCount;
	private int uploadFailureCount;

	public QueueUploaderThread(RecorderUploadApplet recorderUploadApplet,
			FileUploader fileUploader)
	{
		this.fileUploader = fileUploader;
		applet = recorderUploadApplet;
	}

	public FileUploader getFileUploader()
	{
		return fileUploader;
	}
	
	public boolean processCommand( Object item )
	{
		// TODO We should implement subclasses of command not subclasses of the queue. Better style. Easier to add options like streaming.
		if( item instanceof UploadCommand )
		{
			UploadCommand command = (UploadCommand) item;
			return upload( command );
		}
		else
		{
			Runnable command = (Runnable) item;
			command.run();
			return false;
		}
	}

	public boolean upload( UploadCommand command )
	{
		boolean uploaded = false;
		try
		{
			fileUploader.beginTransaction( command.isRunInBackground() );
			InputStream compressedImageStream = command.setupCompressedImageStream();
			uploaded = uploadCompressedImage( command, compressedImageStream );
			
		} catch( Exception e )
		{
			// Only report the error if we are not going to handle it in
			// JavaScript.
			if( uploaded || (command.getFailureScript() == null) )
			{
				displayMessageInApplet( e.getMessage() );
				applet.reportExceptionAfterStopAudio( e );
			}
		} finally
		{
			finalizeUpload( command, uploaded );
			command.handleUploadFinished( uploaded );
			fileUploader.finishTransaction();
		}
		Logger.printMemory( 2, "finished upload" );
		return false;
	}

	protected void finalizeUpload( UploadCommand command, boolean uploaded )
	{

		if( uploaded )
		{
			uploadCompletionCount += 1;
		}
		else
		{
			uploadFailureCount += 1;
		}
		uploadCount += 1;
		
		// Do this last because it might cause the Applet to stop() and
		// go to another page.
		if( uploaded )
		{
			applet.executeJavaScriptCommand( command.getCompletionScript() );
		}
		else
		{
			applet.executeJavaScriptCommand( command.getFailureScript() );
		}
		
	}

	protected boolean uploadCompressedImage( UploadCommand command,
			InputStream compressedStream ) throws IOException
	{
		boolean uploaded = false;

		applet.uploadDiagnosticStatus( "start_upload", false, null );
		
		// Upload compressed image to server.
		// Were we canceled during the compression phase?
		if( fileUploader.isGoing() )
		{
			Hashtable nameValuePairs = command.getNameValuePairs();

			// Send message duration in seconds to server.
			nameValuePairs.put( "duration", Double.toString( command.getDuration() ) );
			nameValuePairs.put( "lup_unique_id", command.getUniqueId() );
			nameValuePairs.put( "lup_message_index", Integer.toString( command.getMessageIndex() ) );
			nameValuePairs.put( "lup_chunk_index", Integer.toString( command.getChunkIndex() ) );
			nameValuePairs.put( "lup_action", command.getAction() );

			String contentType = null;
			switch( applet.getFormat() )
			{
			case Recording.FORMAT_SPEEX:
				contentType = "audio/x-ogg";
				break;
			default:
				contentType = "audio/wav";
				break;
			}
			
			fileUploader.setNameValuePairs( nameValuePairs );
			
			fileUploader.setUploadHeaderPairs( command.getUploadHeaderPairs() );
			fileUploader.setUploadListeners( command.getUploadListeners() );
			
			try
			{
				uploaded = fileUploader.uploadFileImage( command.getFileName(),
						contentType, compressedStream );
			} finally {
				
				if( compressedStream != null )
				{
					compressedStream.close();
				}
			}
			System.out.println("Upload completeed # " + command.getChunkIndex() );
		}
		
		if( uploaded )
		{
			Recording recording = command.getRecording();
			if( (recording != null) && applet.getEraseAfterSend() )
			{
				recording.erase();
			}
			applet.uploadDiagnosticStatus( "finish_upload", false, null );
		}
		
		else if( !fileUploader.isGoing() )
		{
			displayMessageInApplet( "Upload cancelled." );
			applet.uploadDiagnosticStatus( "cancel_upload", false, null );
		}
		return uploaded;
	}


	private void displayMessageInApplet( String string )
	{
		applet.displayMessage( string );
	}

	/**
	 * @return the uploadCount
	 */
	public int getUploadCount()
	{
		return uploadCount;
	}

	public int getUploadCompletionCount()
	{
		return uploadCompletionCount;
	}
	public int getUploadFailureCount()
	{
		return uploadFailureCount;
	}

}
