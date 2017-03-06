package com.softsynth.javasonics.recplay;
import java.applet.Applet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import com.softsynth.awt.ImageLabel;
import com.softsynth.javasonics.core.SonicSystem;

/**
 * Check for presence of JavaSonics plugin or JavaSound.
 * Install it if necessary.
 *
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */

public class PluginChecker
{
    private final static String DEFAULT_INSTALLER_PAGE = "http://www.javasonics.com/plugins/wizard.html";
	private URL          installerLogoURL;
    private URL          installerURL;
    private Applet       applet;
    private boolean      wasCancelled;

    public PluginChecker( Applet applet )
    {
        this.applet = applet;
    }

    void setInstallerLogo(URL insLogo) {
        installerLogoURL = insLogo;
    }
    URL getInstallerLogo() {
        return installerLogoURL;
    }

    void setInstaller( URL installer) {
        installerURL = installer;
    }
    URL getInstaller() {
        return installerURL;
    }

    /** Redirect browser to plugins page. */
    void jumpToInstall() throws MalformedURLException
    {
    	String installerPage = DEFAULT_INSTALLER_PAGE;
    	String hostName = applet.getDocumentBase().getHost();
    	if( hostName != null )
    	{
    		installerPage += "?host=" + hostName;
    	}
        setInstaller( new URL( installerPage ) );
        
        // We were using _blank but that gets hammered by popup blockers.
        applet.getAppletContext().showDocument( getInstaller(), "_self" );
    }

    /** Climb container hierarchy until we get to a Frame */
	static Frame getFrame( Component c )
	{
		do
		{
			if(c instanceof Frame) return (Frame)c;
		} while((c = c.getParent()) != null);
		return null;
	}

    private Image loadInstallerLogo() throws IOException
    {
        Image img = null;
        if (installerLogoURL != null) {
            InputStream inStream = installerLogoURL.openConnection().getInputStream();

            img = VisualTheme.loadImage(applet, inStream);
            inStream.close();
        }
        return img;
    }

/** Ask user to install the JavaSonics plugin.
 *  If they click the button then they will be sent to the JavaSonics web site.
 */
    void recommendPlugin()  throws IOException
    {
        final Label  msgLabel;
        final Button cancelButton;
        final Button installButton;
        final Panel  bottomPanel;
		final Dialog dialog = new Dialog( getFrame(applet),
			"JavaSonics Plugin Needed for Recording!", true );

        dialog.setLayout( new BorderLayout() );

        int w = 550;
        int h = 200;

        Image logo = loadInstallerLogo();
        if( logo != null )
        {
            if( logo.getWidth(applet) > w ) w = logo.getWidth(applet);
            h += logo.getHeight(applet);
            ImageLabel logoLabel = new ImageLabel( logo );
            Panel logoPanel = new Panel(); // use extra panel so logo centerred
            logoPanel.add( logoLabel );
            dialog.add( "North", logoPanel );
        }

        // center on screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - w) / 2;
        int y = (screenSize.height - h) / 2;
        dialog.setBounds( x, y, w, h );

		dialog.setBackground( Color.white );
		dialog.setForeground( Color.black );
		Panel centerPanel = new Panel();
		dialog.add( "Center", centerPanel );
		centerPanel.setLayout( new GridLayout(0, 1 ));
		

		Font bigFont = new Font("Serif", Font.BOLD, 18);
		
		Label lbl = new Label(
				"ListenUp Applet needs the free JavaSonics Audio Plugin.",
				Label.CENTER );
		lbl.setFont(bigFont);
		centerPanel.add( lbl );

		lbl = new Label(
				"The plugin adds audio recording capability to your browser.",
				Label.CENTER );
		lbl.setFont(bigFont);
		centerPanel.add( lbl );
		
		if( false )
		{
			centerPanel.add( new Label(
						"Note: Popup Blockers can prevent the installer window from opening.",
						Label.CENTER ) );
			centerPanel.add( new Label(
						"To enable popups, hold down your Control key while pressing the Install button below.",
						Label.CENTER ) );
		}
        bottomPanel = new Panel();
        dialog.add( "South", bottomPanel );

        bottomPanel.add( installButton = new Button( "Install JavaSonics Plugin" ) );
		installButton.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e)
				{
                    try
                    {
                        jumpToInstall();
                    } catch(  MalformedURLException exc ) {
                        System.err.println( exc );
                    }
					dialog.hide();
                } } );


        bottomPanel.add( cancelButton = new Button( "Cancel" ) );
		cancelButton.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e)
				{
                    wasCancelled = true;
                    dialog.hide();
                } } );

        dialog.validate();
		wasCancelled = false;
        // Wait for modal dialog to complete.
		dialog.show();

        if( wasCancelled ) throw new RuntimeException("Plugin installation cancelled!");
    }

    public boolean isPluginNeeded()
    {
    	// Check for JavaSound before we try to touch the native class.
    	// This can help avoid SonicNativeSystem FileNotFound errors in the web logs.
    	if( SonicSystem.isJavaSoundSupported() )
    	{
    		return false;
    	}
    	else
    	{
    		 return !SonicSystem.isNativeCurrent();
    	}
    }

    /**
     * Try to install JavaSonics plugin if needed.
     */
    boolean installIfNeeded() throws Exception
    {
    	boolean needIt = isPluginNeeded();
		if( needIt )
        {
            recommendPlugin();
        }
		return needIt;
    }
}
