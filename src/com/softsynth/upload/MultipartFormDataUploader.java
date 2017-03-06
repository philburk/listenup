package com.softsynth.upload;

import java.io.*;
import java.net.URL;
import java.util.Hashtable;
import java.util.StringTokenizer;

import com.softsynth.javasonics.util.Logger;
import com.softsynth.ssl.SSLTools;
import com.softsynth.storage.DynamicBuffer;
import com.softsynth.storage.DynamicBufferFactory;

// import com.softsynth.upload.test.*;

/**
 * This class can be used to upload files to a web server using the HTTP POST
 * method. It mimics a browser that uploads a file using a FORM. The URL should
 * be a CGI script that can process FORM data and can accept uploaded files.
 * 
 * Here is what a properly formatted multipart/form-data might look like:
 * 
 * <pre>
 *     -----------------------------7d24227100708
 *     Content-Disposition: form-data; name=&quot;MAX_FILE_SIZE&quot;
 *    
 *     500000
 *     -----------------------------7d24227100708
 *     Content-Disposition: form-data; name=&quot;userfile&quot;; filename=&quot;E:\ping.wav&quot;
 *     Content-Type: audio/wav
 *    
 *     {binary data of WAV file}
 *     -----------------------------7d24227100708--
 * </pre>
 * 
 * @author (C) Phil Burk, http://www.softsynth.com
 */
public abstract class MultipartFormDataUploader
{
	protected String userName;
	protected String password;
	protected URL uploadURL;
	private OutputStream contentOutputStream;
	protected int responseCode;
	protected String responseMessage = null;
	private int numDataSent = 0;

	static final String DASH27 = "---------------------------";
	static final String RANDOM_BOUNDARY = "7d24227100708";
	static final String CONTENT_BOUNDARY = DASH27 + RANDOM_BOUNDARY;

	public static final int HTTP_DEFAULT_PORT = 80;
	private DynamicBuffer dynoBuffer;

	private static final boolean simulateSlowNetwork = false;

	SlowNetworkSimulator slowNetworkSimulator;
	private Hashtable uploadHeaderPairs;

	public MultipartFormDataUploader(URL cgi)
	{
		this.uploadURL = cgi;
		if( simulateSlowNetwork )
		{
			slowNetworkSimulator = new SlowNetworkSimulator();
		}
	}

	/**
	 * Open the stream that will receive the content of the post.
	 */
	public void openSmall() throws IOException
	{
		contentOutputStream = new ByteArrayOutputStream();
		numDataSent = 0;
	}

	/**
	 * Open the stream that will receive the content of the post.
	 */
	public void open() throws IOException
	{
		dynoBuffer = DynamicBufferFactory.createDynamicBuffer();
		contentOutputStream = dynoBuffer.getOutputStream();
		numDataSent = 0;
	}

	/** Send a name value pair where the value is an integer. */
	public void addFormData( String name, int value ) throws IOException
	{
		addFormDataHeader( name, null, null );
		writeInteger( value );
	}

	/** Send a name value pair similar to the way a browser uploads form data. */
	public void addFormData( String name, String value ) throws IOException
	{
		addFormDataHeader( name, null, null );
		write( value.getBytes() );
	}

	/** Add a file to the POST data. */
	public void addFile( String name, String fileName, String type,
			InputStream inStream ) throws IOException
	{
		addFormDataHeader( name, fileName, type );
		write( inStream );
	}

	/**
	 * Finish the MIME data and close the stream used to build the upload
	 * packet.
	 */
	public void close() throws IOException
	{
		contentOutputStream.write( ("\r\n--" + CONTENT_BOUNDARY + "--\r\n")
				.getBytes() );
		contentOutputStream.flush();
		contentOutputStream.close();
	}

	public abstract InputStream getInputStream() throws IOException;

	public abstract OutputStream getOutputStream() throws IOException;

	public abstract void makeConnection( int contentLength ) throws IOException;

	/** After calling close(), perform the actual post of the data. */
	public void upload() throws IOException
	{
		byte[] bar = ((ByteArrayOutputStream) contentOutputStream)
				.toByteArray();
		makeConnection( bar.length );
		BufferedOutputStream buffered = new BufferedOutputStream(getOutputStream());
		buffered.write( bar );
		buffered.flush();
	}

	/**
	 * After calling close(), perform the actual post of the data. Report to the
	 * listener as the upload progresses on each chunk.
	 */
	public void upload( int chunkSize, ProgressListener listener )
			throws IOException
	{
		InputStream inputStream = dynoBuffer.getInputStream();
		int numBytes = dynoBuffer.length();
		makeConnection( numBytes );
		upload( getOutputStream(), inputStream, numBytes, chunkSize, listener );
		inputStream.close();
	}

	private void upload( OutputStream httpOutStream, InputStream inputStream,
			int numBytes, int chunkSize, ProgressListener listener )
			throws IOException
	{
		Logger.printMemory( 2, "MultipartFormDataUploader: got MIME data" );

		// Write to the server in chunks so we can show progress.
		int byteCursor = 0;
		int numLeft = numBytes;
		boolean go = true;
		if( simulateSlowNetwork )
		{
			slowNetworkSimulator.start();
		}
		byte[] buffer = new byte[chunkSize];
		while( go )
		{
			int numRead = inputStream.read( buffer );
			if( numRead < 0 )
			{
				break; // EOF
			}
			httpOutStream.write( buffer, 0, numRead );
			byteCursor += numRead;
			go = listener.progressMade( byteCursor, numBytes );
			if( simulateSlowNetwork )
			{
				slowNetworkSimulator.simulateNetworkDelay( numRead );
			}
		}
		Logger.println( 2, "MultipartFormDataUploader: POST size = "
				+ byteCursor );

		httpOutStream.flush();
		if( simulateSlowNetwork )
		{
			slowNetworkSimulator.stop();
		}
	}

	public int getResponseCode()
	{
		return responseCode;
	}

	public String getResponseMessage()
	{
		return responseMessage;
	}

	/** Get reader for content of response from server. */
	public BufferedReader getBufferedReader() throws IOException
	{
		InputStream inStream = getInputStream();
		return setupReaderAndParseHeader( inStream );
	}

	BufferedReader setupReaderAndParseHeader( InputStream inStream )
			throws IOException
	{
		BufferedReader reader = new BufferedReader( new InputStreamReader(
				inStream ) );
		parseResponseHeader( reader );
		return reader;
	}

	/** ********************************************************************************* */

	/**
	 * Send the header for the new form-data. Should be followed by the actual
	 * data. Then either send another data item or call close() to terminate the
	 * multipart/form-data.
	 */
	private void addFormDataHeader( String name, String fileName, String type )
			throws IOException
	{
		if( numDataSent > 0 )
		{
			contentOutputStream.write( ("\r\n").getBytes() );
		}
		contentOutputStream.write( ("--" + CONTENT_BOUNDARY + "\r\n")
				.getBytes() );

		contentOutputStream.write( ("Content-Disposition: form-data; name=\""
				+ name + "\"").getBytes() );

		if( fileName == null )
		{
			contentOutputStream.write( ("\r\n").getBytes() );
		}
		else
		{
			contentOutputStream.write( ("; filename=\"" + fileName + "\"\r\n")
					.getBytes() );
		}

		if( type != null )
		{
			contentOutputStream.write( ("Content-Type: " + type + "\r\n")
					.getBytes() );
		}
		contentOutputStream.write( ("\r\n").getBytes() );
		numDataSent += 1;
	}

	/**
	 * Write to the HTTP OutputStream. This is used to write the binary file
	 * data.
	 */
	private void write( byte[] bar ) throws IOException
	{
		contentOutputStream.write( bar );
	}

	/**
	 * Write data from InputStream to the HTTP OutputStream. This is used to
	 * write the binary file data.
	 */
	private void write( InputStream inStream ) throws IOException
	{
		DynamicBuffer.writeStreamToStream( inStream, contentOutputStream );
	}

	/** Write an integer as string. */
	private void writeInteger( int num ) throws IOException
	{
		contentOutputStream.write( Integer.toString( num ).getBytes() );
	}

	/**
	 * Get the status response from the server. Hopefully "HTTP/1.1 200 OK" Sets
	 * responseMessage and responseCode.
	 */
	protected int parseStatusLine( String line )
	{
		responseCode = 0;
		if( line == null )
		{
			Logger.println( "parseStatusLine: null" );
			responseMessage = "null response";
		}
		else
		{
			Logger.println( "parseStatusLine: " + line + ", len "
					+ line.length() );

			StringTokenizer st = new StringTokenizer( line );
			String methodToken = st.nextToken();
			String codeToken = st.nextToken();
			int indexCodeToken = line.indexOf( codeToken );
			responseMessage = line.substring( indexCodeToken
					+ codeToken.length() + 1 );
			try
			{
				responseCode = Integer.parseInt( codeToken );
			} catch( NumberFormatException e )
			{
				System.err.println( e.toString() );
			}
		}
		return responseCode;
	}

	protected void parseResponseHeader( BufferedReader reader )
			throws IOException
	{
		boolean isChunked = false;
		String line = reader.readLine();
		parseStatusLine( line );

		line = reader.readLine();
		while( (line != null) && (line.length() > 0) )
		{
			int firstChar = line.charAt( 0 );
			boolean continuation = ((firstChar == ' ') || (firstChar == '\t'));
			if( !continuation )
			{
				Logger.println( 1, "header: " + line );
				// TODO remove ND
				int colonIndex = line.indexOf( ':' );
				if( colonIndex != -1 )
				{ // MOD ND
					String headerName = line.substring( 0, colonIndex );
					String value = line.substring( colonIndex + 1, line
							.length() );
					value = value.trim();
					if( headerName.equalsIgnoreCase( "Transfer-Encoding" ) )
					{
						if( value.equalsIgnoreCase( "chunked" ) )
						{
							isChunked = true;
						}
					}
				} // MOD ND
			}
			line = reader.readLine();
		}
		if( isChunked )
		{
			line = reader.readLine();
		}
	}

	static public void main( String[] argv )
	{
		// String cgiName =
		// "https://hatten-server.rockefeller.edu/imageserver/handle_image_upload.jsp";
		String cgiName = "https://www.softsynth.com/listenup/php_ssl/handle_upload_ssl.php";

		try
		{
			SSLTools.disableCertificateValidation();

			MultipartFormDataUploader uploader;
			if( false )
			{
				boolean forceUseURLConnection = false;
				uploader = MultipartFormDataUploader.createUploader( new URL(
						cgiName ), forceUseURLConnection );
			}
			else if( false )
			{
				uploader = new MultipartFormDataUploaderHTTP( new URL( cgiName ) );
			}
			else
			{
				uploader = new MultipartFormDataUploaderSocket( new URL(
						cgiName ) );
			}

			uploader.userName = "guest";
			uploader.password = "listenup";

			uploader.open();

			uploader.addFormData( "comment", "blah blah https" );
			uploader.addFormData( "username", "dudehttps" );

			byte fakeWAV[] = new byte[256];
			for( int i = 0; i < fakeWAV.length; i++ )
			{
				fakeWAV[i] = (byte) i;
			}
			uploader.addFile( "userfile", "fake.wav", "audio/wav",
					new ByteArrayInputStream( fakeWAV ) );

			uploader.close();
			uploader.upload( 20, new ProgressListener()
			{
				public boolean progressMade( int numSoFar, int numTotal )
				{
					System.out.println( numSoFar + "/" + numTotal );
					return true;
				}
			} );

			System.out.println( "Finished writing." );
			System.out.println();

			System.out.println( "CGI script" );
			System.out.println( "=========== begin ===========" );
			BufferedReader in = uploader.getBufferedReader();
			String inputLine;
			while( (inputLine = in.readLine()) != null )
			{
				System.out.println( inputLine );
			}
			in.close();
			System.out.println( "=========== end ===========" );

		} catch( IOException e )
		{
			System.err.println( e );
		} catch( SecurityException e )
		{
			System.err.println( e );
		}
	}

	/**
	 * @return
	 */
	public String getPassword()
	{
		return password;
	}

	/**
	 * @return
	 */
	public String getUserName()
	{
		return userName;
	}

	/**
	 * @param string
	 */
	public void setPassword( String string )
	{
		password = string;
	}

	/**
	 * @param string
	 */
	public void setUserName( String string )
	{
		userName = string;
	}

	/**
	 * if useURLConnection == true, will use MultipartFormDataUploaderHTTP if
	 * useURLConnection == false, might still use MultipartFormDataUploaderHTTP
	 * if Java version less than 1.4 and using https
	 * 
	 * @param uploadURL
	 * @return
	 */
	public static MultipartFormDataUploader createUploader( URL url,
			boolean useURLConnection )
	{
		if( url.getProtocol().equals( "file" ) )
		{
			throw new RuntimeException( "Cannot POST to " + url + "\n"
					+ "Can only upload to an actual web server." );
		}
		else if( ((Query.getJavaVersion() < 1.4) && url.getProtocol().equals(
				"https" ))
				|| useURLConnection )
		{
			// System.out.println( "Using MultipartFormDataUploaderHTTP" );
			return new MultipartFormDataUploaderHTTP( url );
		}
		else
		{
			// System.out.println( "Using MultipartFormDataUploaderSocket" );
			return new MultipartFormDataUploaderSocket( url );
		}
	}

	public void setUploadHeaderPairs( Hashtable uploadHeaderPairs )
	{
		this.uploadHeaderPairs = uploadHeaderPairs;
	}

	/**
	 * @return the uploadHeaderPairs
	 */
	public Hashtable getUploadHeaderPairs()
	{
		return uploadHeaderPairs;
	}

}