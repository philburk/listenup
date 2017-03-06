/*
 * Created on Dec 8, 2004 
 *
 */
package com.softsynth.javasonics.recplay;

import java.awt.Component;
import java.awt.Frame;
import java.io.IOException;
import java.net.URL;


/**
 * TODO This doesn't seem like a "factory" class. It is more like an interface to an extension.
 * @author Nick Didkovsky, didkovn@mail.rockefeller.edu
 */
public interface ListenUpFactory
{
    public void createVisualTheme( Component parent ) throws IOException;

    public TransportControl createTransportControl(Frame frame, boolean recordingEnabled, boolean useTextButtons, boolean showSpeedControl);

    public void setSkinURL( URL skinURL );

    public void setFFRewSkinURL( URL skinURL );

    public void startSpeechMike( Player player, boolean useOldServer, int speechMikePort );
    public void startHotKeys( Player player, HotKeyOptions hotKeyOptions );
	public void startFootPedal( Player player );

    /** Stop everything. */
    public void stop();

	public void setSendable( Sendable sendable );
	
	public void parseParameters( ParameterHolder parameters );


}