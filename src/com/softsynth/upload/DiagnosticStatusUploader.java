package com.softsynth.upload;
import java.io.*;
import java.net.*;

import com.softsynth.javasonics.util.Logger;

/**
 * Upload diagnostic files to the server.
 * @author (C) Phil Burk, http://www.softsynth.com/
*/
public class DiagnosticStatusUploader
{
	private MultipartFormDataUploader uploader;

/** Construct an uploader for sending diagnostic status to server.
 *  @param uploadURL URL for a CGI script that will accept diagnostic info.
 */
    public DiagnosticStatusUploader( URL uploadURL )
    throws IOException
    {
        URLConnection.setDefaultAllowUserInteraction( true );
		// Connect to URL and send first part of MIME data.
		uploader = MultipartFormDataUploader.createUploader( uploadURL , true);
		uploader.openSmall();
    }
	
	public void addNameValuePair( String name, String value )
	throws IOException
	{
        uploader.addFormData( name, value );
    }

	public void addFile(String name, String type,
			InputStream inStream ) throws IOException
	{
		uploader.addFile( name, "diagnostic.txt", type, inStream );
	}

	/** Add javaVendor and javaVersion info to uploaded form. */
	public void addSystemInfo()
	throws IOException
	{
		addNameValuePair( "osName", System.getProperty("os.name") );
		addNameValuePair( "osVersion", System.getProperty("os.version") );
		addNameValuePair( "javaVendor", System.getProperty("java.vendor") );
		addNameValuePair( "javaVersion", System.getProperty("java.version") );
	}
	
	/**
	 * @return HTTP response code, 200 is good, 404 means URL not found, etc.
	 * @throws IOException
	 */
	public int upload()
	throws IOException
	{
		Logger.println( 3, "Start DiagnosticStatusUploader.upload()");
		// Finish building upload image.
        uploader.close();
        // Transmit the entire formatted image to the server.
		printResponseLine("--------- begin response from server ------------");
        uploader.upload();
	
        // Print the servers response for debugging.
        BufferedReader in = uploader.getBufferedReader();
        String inputLine;
        while ((inputLine = in.readLine()) != null)
        {
            printResponseLine(inputLine);
        }
        in.close();
		Logger.println( 3, "Finish DiagnosticStatusUploader.upload()");
		int responseCode = uploader.getResponseCode();
		printResponseLine("------------------------------------------------");
		printResponseLine( "HTTP response code = " + responseCode );
		return responseCode;
    }
    
    public void printResponseLine( String line )
	{
    	System.out.println( line );
	}

	/** Upload diagnostic data in the background because the app should not have to wait. */
    public Thread dispatch()
    {
    	Thread thread = new Thread(){
    		public void run() {
    			try {
					upload();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    	};
    	thread.start();
    	return thread;
    }

}
