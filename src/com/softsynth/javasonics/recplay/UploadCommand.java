package com.softsynth.javasonics.recplay;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

import com.softsynth.storage.DynamicBuffer;

/**
 * Command to upload a recording in a background thread.
 * 
 * @author Phil Burk (C) 2008 Mobileer Inc
 */
public abstract class UploadCommand
{
	private String fileName;
	private Recording recording;

	private String completionScript;
	private String failureScript;
	private Hashtable nameValuePairs;
	private Hashtable uploadHeaderPairs;
	private Vector uploadListeners = new Vector();
	private double duration;

	private InputStream compressedImageStream;

	private int chunkIndex;

	private int messageIndex;
	private String uniqueId;

	public static final String SERVER_ACTION_UPLOAD = "upload";
	public static final String SERVER_ACTION_APPEND = "append";
	public static final String SERVER_ACTION_FINISH = "finish";

	public UploadCommand(String fileName )
	{
		this.fileName = fileName;
	}


	public String getFileName()
	{
		return fileName;
	}

	/**
	 * @param fileName
	 *            the fileName to set
	 */
	public void setFileName( String fileName )
	{
		this.fileName = fileName;
	}

	public Recording getRecording()
	{
		return recording;
	}
	
	public void setRecording( Recording recording )
	{
		this.recording = recording;
	}

	public String getCompletionScript()
	{
		return completionScript;
	}

	public String getFailureScript()
	{
		return failureScript;
	}

	public void setCompletionScript( String completionScript )
	{
		this.completionScript = completionScript;
	}

	public void setFailureScript( String failureScript )
	{
		this.failureScript = failureScript;
	}

	public Hashtable getNameValuePairs()
	{
		return nameValuePairs;
	}

	/**
	 * @param nameValuePairs the nameValuePairs to set
	 */
	public void setNameValuePairs( Hashtable nameValuePairs )
	{
		this.nameValuePairs = nameValuePairs;
	}

	public Hashtable getUploadHeaderPairs()
	{
		return uploadHeaderPairs;
	}
	public void setUploadHeaderPairs( Hashtable uploadHeaderPairs )
	{
		this.uploadHeaderPairs = uploadHeaderPairs;
	}

	/**
	 * @return the uploadListeners
	 */
	public Vector getUploadListeners()
	{
		return uploadListeners;
	}

	/**
	 * @param uploadListeners the uploadListeners to set
	 */
	public void setUploadListeners( Vector uploadListeners )
	{
		this.uploadListeners = uploadListeners;
	}

	public double getDuration()
	{
		return duration;
	}

	public void setDuration( double duration )
	{
		this.duration = duration;
	}
	
	public InputStream getCompressedImageStream()
	{
		return compressedImageStream;
	}

	public void setCompressedImageStream( InputStream compressedImageStream )
	{
		this.compressedImageStream = compressedImageStream;
	}
	
	public int getChunkIndex()
	{
		return chunkIndex;
	}
	
	public void setChunkIndex( int chunkIndex )
	{
		this.chunkIndex = chunkIndex;
	}

	public abstract String getAction();

	public int getMessageIndex()
	{
		return messageIndex;
	}

	public void setMessageIndex( int messageIndex )
	{
		this.messageIndex = messageIndex;
	}

	public String getUniqueId()
	{
		return uniqueId;
	}

	public void setUniqueId( String uniqueId )
	{
		this.uniqueId = uniqueId;
	}

	public boolean isSendingRecording()
	{
		return false;
	}

	public abstract InputStream setupCompressedImageStream() throws IOException;

	public void handleUploadFinished( boolean uploaded )
	{
	}

	public boolean isRunInBackground()
	{
		return false;
	}
}
