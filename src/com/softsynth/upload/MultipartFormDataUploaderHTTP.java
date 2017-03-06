package com.softsynth.upload;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Hashtable;

import com.softsynth.javasonics.error.WebDeveloperRuntimeException;

// import com.softsynth.upload.test.*;

/**
 * This implementation is required for SSL on Java before V1.4.
 * We would use it all the time but it buffers its post data so we can't
 * show upload progress.
 * 
 * HTTPS VERSION, ND 7/13/04
 * modified PLB 11/1/04
 * 
 * @author (C) Phil Burk, http://www.softsynth.com
*/
public class MultipartFormDataUploaderHTTP extends MultipartFormDataUploader
{
	URLConnection connection;

	public MultipartFormDataUploaderHTTP(URL url)
	{
		super(url);
	}

	/* (non-Javadoc)
	 * @see com.softsynth.upload.MultipartFormDataUploader#makeConnection(int)
	 */
	public void makeConnection(int contentLength) throws IOException
	{
		connection = uploadURL.openConnection();

		if ((password != null) && (userName != null))
		{
			// Encode the bytes of the string
			String encoding =
				Base64Encoder.encode((userName + ":" + password).getBytes());
			connection.setRequestProperty("Authorization", "Basic " + encoding);
		}

		Hashtable uploadHeaderPairs = getUploadHeaderPairs();
		if( uploadHeaderPairs != null )
		{
			Enumeration keys = uploadHeaderPairs.keys();
			while( keys.hasMoreElements() )
			{
				String name = (String) keys.nextElement();
				String value = (String) uploadHeaderPairs.get( name );
				connection.setRequestProperty( name, value );
			}
		}

		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setUseCaches(false);
		connection.setRequestProperty(
			"Content-Type",
			"multipart/form-data; boundary=" + CONTENT_BOUNDARY);
		connection.connect();
	}

	public InputStream getInputStream() throws IOException
	{
		try
		{
			return connection.getInputStream();
		} catch ( FileNotFoundException exc )
		{
			throw new WebDeveloperRuntimeException( "Error uploading file. Bad uploadURL?", exc );
		}
	}
	
	protected void parseResponseHeader(BufferedReader reader) throws IOException
	{
		// We have to parse our own status line because on Windows JVM
		// we cannot cast this to a HttpURLConnection!
		parseStatusLine( connection.getHeaderField(0) );
	}

	/* (non-Javadoc)
	 * @see com.softsynth.upload.MultipartFormDataUploader#getOutputStream()
	 */
	public OutputStream getOutputStream() throws IOException
	{
		return connection.getOutputStream();
	}

}
