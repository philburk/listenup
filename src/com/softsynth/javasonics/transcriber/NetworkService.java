package com.softsynth.javasonics.transcriber;

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import com.softsynth.javasonics.installer.Installer;
import com.softsynth.javasonics.installer.LibraryInstaller;
import com.softsynth.javasonics.util.Logger;
import com.softsynth.storage.StorageTools;

/**
 * Provide access to a native service through a TCP/IP connection. The native
 * code launches a server and the Applet logs in. One advantage of this is that
 * a Java Thread can easily wait for notification from the native code. This is
 * handy for native code that generates events that control an Applet.
 * 
 * @author Phil Burk (C) 2004
 */
public abstract class NetworkService implements Runnable
{
	private static final int EXECUTION_TIMEOUT_MSEC = 12 * 1000;
	private int port;
	private Thread thread;
	private boolean go;
	private Socket socket = null;
	private OutputStream outputStream;
	private BufferedReader lineReader;
	private Frame frame;

	/**
	 *  
	 */
	public NetworkService(int port, Frame frame)
	{
		this.port = port;
		this.frame = frame;
	}

	/**
	 * @return name of executable network service program
	 */
	public abstract String getApplicationName();

	/**
	 * Called when the server send a command line.
	 * @param cmd
	 */
	public abstract void handleCommandFromServer( String cmd );

	public void install() throws IOException
	{
		Installer.getInstance().installExecutableIfNeeded( frame,
				getApplicationName() );
	}

	/**
	 * Execute native code as an application stored in the Company directory.
	 * 
	 * @throws IOException
	 */
	private void execute() throws IOException
	{
		String command = getExecutableCommand();
		Logger.println( "Executable command: " + command );
		// Place in String array in case there are spaces in the exe path.
		// Spaces confuse the command parser.
		String[] args = { command, "-p" + port };
		Runtime.getRuntime().exec( args );
	}

	private String getExecutableCommand()
	{
		File binFile = LibraryInstaller
				.createCompanyFile( getApplicationName() );
		return binFile.getAbsolutePath();
	}

	/** Start thread that monitors the service through a socket. */
	public void start()
	{
		thread = new Thread( this, "NetworkService" );
		go = true;
		thread.start();
	}

	public void stop()
	{
		go = false;
		try
		{
			logoutFromServer();
		} catch( IOException e )
		{
		}
	}

	/**
	 * Force disconnection with server by closing socket.
	 * @throws IOException
	 */
	private synchronized void logoutFromServer() throws IOException
	{
		if( socket != null )
		{
			outputStream = null;
			// This will trigger an IOException in the waiting thread
			// so it can exit its loop.
			socket.close();
			socket = null;
		}
	}

	/**
	 * Connect to a service running on the same machine as localhost. Notify any
	 * threads waiting for login.
	 * 
	 * @param portnum
	 *            TCP/IP port to connect to.
	 * @throws IOException
	 */
	synchronized void loginToServer( int portnum ) throws IOException
	{
		lineReader = null;
		Logger.println( 1, "NetworkService.loginServer() for "
				+ getApplicationName() );
		InetAddress dst = InetAddress.getLocalHost();

		socket = new Socket( dst, portnum );
		if( socket == null )
		{
			throw new IOException( "Client: could not make socket on port "
					+ portnum );
		}
		InputStreamReader reader = new InputStreamReader( socket
				.getInputStream() );
		lineReader = new BufferedReader( reader );
		outputStream = socket.getOutputStream();
		//setSocket(socket, timeoutMSec); // for areYouAlive testing
		notifyAll();
	}

	/**
	 * Send text message to server.
	 * 
	 * @param message
	 * @throws IOException
	 */
	protected void sendMessageToServer( String message ) throws IOException
	{
		if( outputStream != null )
		{
			outputStream.write( message.getBytes() );
			outputStream.write( "\n".getBytes() );
			outputStream.flush();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		runLogin();
		loadOptions();
		try
		{
			runCommands();
		} catch( Exception e )
		{
			if( go ) // Maybe we are just shutting down.
			{
				Logger.println( 0,
				"Error communicating with " + getApplicationName() );
				Logger.println( 0, "Caught " + e );
				e.printStackTrace();
			}
			go = false;
		}
		finally
		{
			runLogout();
		}
	}

	/** This is called after the NetworkService is up and running. */
	public void loadOptions()
	{
	}

	private void runLogin()
	{
		try
		{
			// Try to login to network service using TCP/IP.
			loginToServer( port );
		} catch( IOException e1 )
		{
			// If that fails then check for installation, then run service and wait.
			Logger.println( 1, "NetworkService.runLogin: caught " + e1 );
			try
			{
				Logger.println( 0, "Launching network service "
						+ getApplicationName() );
				install(); // checks first to see if already installed
				
				Logger.println( 0, "NetworkService " + getApplicationName() + " installed, try to execute." );
				execute(); // will take moment to launch asynchronously
				
				// Try connecting for a few seconds.
				long stopAt = System.currentTimeMillis() + EXECUTION_TIMEOUT_MSEC;
				boolean loggedIn = false;
				while( !loggedIn && ((stopAt - System.currentTimeMillis()) > 0) )
				{
					Thread.sleep( 100 );
					try
					{
						// This will fail if the native app is not yet ready.
						Logger.println( 0, "NetworkService try to connect to " + getApplicationName() );
						loginToServer( port );
						loggedIn = true;
						Logger.println( 0, "NetworkService SUCCESS connecting to " + getApplicationName() );
					} catch( IOException eLogin )
					{
						// Sort of expected this. Just try again until we timeout.
					}
				}
				if( !loggedIn )
				{
					throw new RuntimeException( "Timed out after " + EXECUTION_TIMEOUT_MSEC + " msec waiting for "
							+ getApplicationName() );
				}
			} catch( Exception e3 )
			{
				Logger.println( 0,
						"Could not install or run " + getApplicationName() );
				Logger.println( 0,
						"Command line: " + getExecutableCommand() );
				Logger.println( 0, "Caught " + e3 );
				go = false;
				return;
			}
		}
		Logger.println( 1, "NetworkService.runLogin() connected to " + getApplicationName() );
	}

	private void runCommands() throws IOException
	{
		if( lineReader != null )
		{
			while( go )
			{
				try
				{
					String cmd = lineReader.readLine();
					if( (cmd == null) )
					{
						if( go )
						{
							Logger.println( 0, "Unexpected closing of " + getApplicationName());
							go = false;
						}
					}
					else
					{
						handleCommandFromServer( cmd );
					}
				} catch( SocketException e )
				{
					if( go )
					{
						Logger.println( 0, "Lost connection with " + getApplicationName());
						go = false;
					}
				}
			}
		}
	}

	private void runLogout()
	{
		try
		{
			logoutFromServer();
		} catch( IOException e )
		{
		}
	}

	/**
	 * @return Returns the frame.
	 */
	public Frame getFrame()
	{
		return frame;
	}
	

}