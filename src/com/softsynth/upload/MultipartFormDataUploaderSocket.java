package com.softsynth.upload;

import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.Hashtable;

import com.softsynth.ssl.SSLTools;

/**
 * We normally use this low level socket implementation because
 * URLConnection is broken on MRJ 2.2.2.
 * Also we cannot monitor upload progress using URLConnection because the upload
 * happens internally when the connection is closed.
 *

 * @author Phil Burk (C) 2004
 */
public class MultipartFormDataUploaderSocket extends MultipartFormDataUploader
{
	private Socket socket;

	public MultipartFormDataUploaderSocket(URL url)
	{
		super(url);
	}

	public InputStream getInputStream() throws IOException
	{
		return socket.getInputStream();
	}
	public OutputStream getOutputStream() throws IOException
	{
		return socket.getOutputStream();
	}

	private Socket createSocket() throws IOException
	{
		Socket skt;
		if (uploadURL.getProtocol().equals("https"))
		{
			skt = SSLTools.createSocket(uploadURL);
		} else
		{
			int portNum = uploadURL.getPort();
			if (portNum < 0)
				portNum = HTTP_DEFAULT_PORT;
			skt = new Socket(uploadURL.getHost(), portNum);
		}
		return skt;
	}

	/* (non-Javadoc)
	 * @see com.softsynth.upload.MultipartFormDataUploader#makeConnection()
	 */
	public void makeConnection( int contentLength ) throws IOException
	{
		socket = createSocket();
		writePostHeader(socket.getOutputStream(), uploadURL.getHost(), contentLength );
	}

	/** Write a POST request with various header fields. */
	private void writePostHeader(OutputStream out, String hostName, int contentLength)
		throws IOException
	{
		out.write(
			("POST " + uploadURL.getFile() + " HTTP/1.1\r\n").getBytes());

		if ((password != null) && (userName != null))
		{
			String userPassword = userName + ":" + password;
			// Encode the bytes of the string
			String encodedUserPassword =
				Base64Encoder.encode(userPassword.getBytes());
			out.write(
				("Authorization: Basic " + encodedUserPassword + "\r\n")
					.getBytes());
		}

		// Write custom name value pairs into the request header.
		Hashtable uploadHeaderPairs = getUploadHeaderPairs();
		if( uploadHeaderPairs != null )
		{
			Enumeration keys = uploadHeaderPairs.keys();
			while( keys.hasMoreElements() )
			{
				String name = (String) keys.nextElement();
				String value = (String) uploadHeaderPairs.get( name );
				out.write( (name + ": " + value + "\r\n").getBytes() );
			}
		}
		
		out.write(
			("Content-Type: multipart/form-data; boundary=" + CONTENT_BOUNDARY + "\r\n")
				.getBytes());
		out.write(("User-Agent: Java1.3.1\r\n").getBytes());
		out.write(("Host: " + hostName + "\r\n").getBytes());
		// required for HTTP1.1
		out.write(
			("Accept: text/html, image/gif, image/jpeg, */*\r\n").getBytes());
		out.write(("Connection: close\r\n").getBytes());
		// cuz we don't need persistent connection
		out.write(
			("Content-length: " + contentLength + "\r\n").getBytes());
		// Finish header.
		out.write(("\r\n").getBytes());
	}

}
