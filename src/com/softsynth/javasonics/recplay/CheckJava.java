package com.softsynth.javasonics.recplay;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Label;

/**
 * Very simple app that can be used to see if Java is working.
 * 
 * @author Phil Burk (C) 2012 Phil Burk
 */

public class CheckJava extends Applet
{
	public static void main( String args[] )
	{
		CheckJava applet = new CheckJava();
		final Frame f = new Frame( "Report Java Information" );
		f.add( "Center", applet );
		f.setSize( 30, 40 );
		applet.init();
		f.show();
		applet.start();
	}

	public void start()
	{
		setLayout( new BorderLayout() );
		setBackground( new Color(128, 255, 128 ) );
		add( "Center", new Label("Java OK") );
		validate();
	}

	public void stop()
	{
		removeAll();
	}

	/** These can be called by JavaScript in the browser. */
	String getJavaVersion()
	{
		return System.getProperty( "java.version" );
	}
	
	String getJavaVendor()
	{
		return System.getProperty( "java.vendor" );
	}

}