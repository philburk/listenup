/*
 * Created on Dec 8, 2004 
 *
 */
package com.softsynth.javasonics.transcriber;

import java.awt.Component;
import java.awt.Frame;
import java.io.*;
import java.net.URL;

import com.softsynth.javasonics.recplay.*;
import com.softsynth.javasonics.util.Logger;

/**
 * @author Nick Didkovsky, didkovn@mail.rockefeller.edu
 * 
 */
public class ListenUpFactoryTranscriber implements ListenUpFactory
{
	//public static final String IMAGES_FFREW_SKIN = "/images/FFRewBeginEndSkin.gif";
	public static final String IMAGES_FFREW_SKIN = "/images/DefaultFFRewSkinV2.jpg";
	
	private URL skinURL;
	private URL skinFFRewURL;
	private Frame frame;
	private Player player;
	private VisualTheme theme;
	private TranscriberVisualTheme transcriberTheme;
	private PlayerControlHandler controller;
	private FootPedalInterface footPedal;
	private HotKeyController hotKeyController;
	private SpeechMikeController speechMikeController;

	public void createVisualTheme( Component parent ) throws IOException
	{
		InputStream inStream1;
		InputStream inStream2;

		Logger.println( "ListenUpFactoryTranscriber start setupVisualTheme()" );
		if( skinURL != null )
		{
			Logger.println( 0,
					"ListenUpFactoryTranscriber setupVisualTheme: load "
							+ skinURL + ", and " + skinFFRewURL );
			inStream1 = skinURL.openConnection().getInputStream();
		}
		else
		{
			//inStream1 = new ByteArrayInputStream( RoundedSkinImage.data );
			inStream1 = getClass().getResourceAsStream( ListenUpFactoryStandard.IMAGES_BASIC_SKIN );
			if( inStream1 == null )
			{
				throw new IOException("Could not load images from: " + ListenUpFactoryStandard.IMAGES_BASIC_SKIN );
			}
		}
		theme = new VisualTheme( parent, inStream1 );
		inStream1.close();

		if( skinFFRewURL != null )
		{
			inStream2 = skinFFRewURL.openConnection().getInputStream();
		}
		else
		{
			//inStream2 = new ByteArrayInputStream( FFRewBeginEndSkin.data );
			inStream2 = getClass().getResourceAsStream( IMAGES_FFREW_SKIN );
			if( inStream2 == null )
			{
				throw new IOException("Could not load images from: " + IMAGES_FFREW_SKIN );
			}
		}
		transcriberTheme = new TranscriberVisualTheme( parent, inStream2 );
		inStream2.close();
	}

	public TransportControl createTransportControl( Frame frame,
			boolean recordingEnabled,
			boolean useTextButtons,
			boolean showSpeedControl )
	{
		this.frame = frame;

		if( recordingEnabled )
		{
			controller = new SkinnableRecorderTranscriberControl( theme,
					transcriberTheme, showSpeedControl );
		}
		else
		{
			controller = new SkinnableTranscriberControl( theme,
					transcriberTheme );
		}
		return controller;
	}

	public void setSkinURL( URL url )
	{
		this.skinURL = url;
	}

	public void setFFRewSkinURL( URL url )
	{
		this.skinFFRewURL = url;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.javasonics.recplay.ListenUpFactory#stop()
	 */
	public void stop()
	{
		if( footPedal != null )
		{
			footPedal.stop();
		}
		if( speechMikeController != null )
		{
			speechMikeController.stop();
		}
		if( hotKeyController != null )
		{
			hotKeyController.stop();
		}
	}

	public void startFootPedal( Player pPlayer )
	{
		player = pPlayer;
		if( FootPedalInterface.isSupported() )
		{
			footPedal = new FootPedalInterface( frame, controller );
			footPedal.start();
		}
		else
		{
			System.err
					.println( "Foot pedal not supported on this platform." );
		}
	}
	
	public void startSpeechMike( Player pPlayer, boolean useOldServer, int speechMikePort )
	{
		player = pPlayer;

		if( SpeechMikeController.isSupported() )
		{
			if( speechMikeController != null )
			{
				speechMikeController.setPlayer( pPlayer );
			}
			else
			{
				if( useOldServer )
				{
					speechMikeController = new SpeechMikeController( player, frame, speechMikePort );
				}
				else
				{
					speechMikeController = new AdvancedSpeechMikeController( player, frame, speechMikePort );
				}
				speechMikeController.start();
			}
		}
		else
		{
			System.err.println( "Speech Mike not supported on this platform." );
		}
	}

	public void startHotKeys( Player pPlayer, HotKeyOptions hotKeyOptions )
	{
		player = pPlayer;
		if( hotKeyOptions.useHotKeys() )
		{
			if( HotKeyController.isSupported() )
			{
				hotKeyController = new HotKeyController( player, frame, hotKeyOptions );
				hotKeyController.start();
			}
		}
	}

	public void setSendable( Sendable sendable )
	{
		if( speechMikeController != null )
		{
			speechMikeController.setSendable( sendable );
		}
	}

	public void parseParameters( ParameterHolder parameters )
	{
		if( speechMikeController != null )
		{
			speechMikeController.parseParameters( parameters );
		}
	}
}