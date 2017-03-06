/*
 * Created on Dec 8, 2004 
 *
 */
package com.softsynth.javasonics.recplay;

import java.awt.Component;
import java.awt.Frame;
import java.io.*;
import java.net.URL;

import com.softsynth.javasonics.util.Logger;

/**
 * @author Nick Didkovsky, didkovn@mail.rockefeller.edu
 * 
 */
public class ListenUpFactoryStandard implements ListenUpFactory
{
	URL skinURL;
	VisualTheme theme;
	//public static final String IMAGES_BASIC_SKIN = "/images/RoundedSkinImage.gif";
	public static final String IMAGES_BASIC_SKIN = "/images/DefaultBasicSkinV2.jpg";

	public void createVisualTheme( Component parent ) throws IOException
	{
		InputStream inStream;

		Logger.println( "start setupVisualTheme()" );
		if( skinURL != null )
		{
			Logger.println( "setupVisualTheme: load " + skinURL );
			inStream = skinURL.openConnection().getInputStream();
		}
		else
		{
			inStream = getClass().getResourceAsStream( IMAGES_BASIC_SKIN );
			if( inStream == null )
			{
				throw new RuntimeException("Could not find " + IMAGES_BASIC_SKIN);
			}
		}
		theme = new VisualTheme( parent, inStream );

		inStream.close();
	}

	public TransportControl createTransportControl( Frame frame,
			boolean recordingEnabled, boolean useTextButtons, boolean showSpeedControl )
	{
		TransportControl control = null;
		if( recordingEnabled )
		{
			if( useTextButtons )
			{
				control = (TransportControl) new TextRecorderControl();
			}
			else
			{
				try
				{
					control = (TransportControl) new SkinnableRecorderControl(
							theme );
				} catch( Throwable e )
				{
					System.out.println("Caught " + e );
					System.out.println("Could not create image buttons so use text buttons.");
					useTextButtons = true;
					control = (TransportControl) new TextRecorderControl();
				}
			}
		}
		else
		{
			if( useTextButtons )
			{
				control = (TransportControl) new TextPlayerControl();
			}
			else
			{
				try
				{
					control = (TransportControl) new SkinnablePlayerControl(
							theme );
				} catch( Throwable e )
				{
					System.out.println("Caught " + e );
					System.out.println("Could not create image buttons so use text buttons.");
					useTextButtons = true;
					control = (TransportControl) new TextPlayerControl();
				}
			}
		}
		return control;
	}

	public void setSkinURL( URL skinURL )
	{
		this.skinURL = skinURL;
	}

	public void setFFRewSkinURL( URL skinURL )
	{
		System.err
				.println( "ListenUpFactoryStandard.setFFRewSkinURL() unimplemented" );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.javasonics.recplay.ListenUpFactory#stop()
	 */
	public void stop()
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.javasonics.recplay.ListenUpFactory#setPlayer(com.softsynth.javasonics.recplay.Player)
	 */
	public void startSpeechMike( Player player, boolean useOldServer, int speechMikePort )
	{
		Logger.println( 0, "Speech Mike requires transcription to be enabled." );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.softsynth.javasonics.recplay.ListenUpFactory#startHotKeys(com.softsynth.javasonics.recplay.Player)
	 */
	public void startHotKeys( Player player, HotKeyOptions hotKeyOptions )
	{
		// Hot Keys require transcription to be enabled.
	}

	public void startFootPedal( Player player )
	{
		Logger.println( 0, "Foot Pedal requires transcription to be enabled." );
	}

	public void setSendable( Sendable sendable )
	{
	}

	public void parseParameters( ParameterHolder parameters )
	{		
	}

}