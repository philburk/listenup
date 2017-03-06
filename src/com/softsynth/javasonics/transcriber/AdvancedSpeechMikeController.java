package com.softsynth.javasonics.transcriber;

import java.awt.Frame;
import java.io.IOException;

import com.softsynth.javasonics.installer.Installer;
import com.softsynth.javasonics.recplay.Player;
import com.softsynth.javasonics.recplay.PlayerApplet;
import com.softsynth.javasonics.util.Logger;
import com.softsynth.javasonics.util.MD5;

public class AdvancedSpeechMikeController extends SpeechMikeController
{
	private static final String CMD_CHALLENGE = "challenge";
	private static final String CMD_RESPONSE = "response";
	
	private static final String PHILIPS_DLL_NAME = "PIA.SpMikeCtrl";
	
	public AdvancedSpeechMikeController(Player player, Frame frame, int speechMikePort)
	{
		super( player, frame, speechMikePort );
	}

	public void install() throws IOException
	{
		Installer.getInstance().installCompanyLibraryIfNeeded( getFrame(), PHILIPS_DLL_NAME );
		super.install();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.javasonics.recplay.NetworkService#getApplicationName()
	 */
	public String getApplicationName()
	{
		return  "AdvancedSpeechMikeServer";
	}


	/** Overrides default from NetworkServices. */
	public void loadOptions()
	{
		sendMessageToServerSafely( "clvers " + PlayerApplet.VERSION_NUMBER );
	}

	/**
	 * @param cmd
	 */
	public void handleCommandFromServer( String cmd )
	{
		Logger.println( 1, "SpeechMike: command = " + cmd );
		if( cmd.startsWith(  CMD_CHALLENGE ))
		{
			String[] words = cmd.split("\\s");
			String code = words[1];
            // Note that the funny pass code must match the code in SpeechMikeServer.
			String response = MD5.toHash( code + "Xk88FGbAh56WtKQncUuz84F");
			sendMessageToServerSafely( CMD_RESPONSE + " " + response );
		}
		else
		{
			super.handleCommandFromServer( cmd );
		}
	}
	
}
