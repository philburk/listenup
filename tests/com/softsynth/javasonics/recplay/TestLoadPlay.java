package com.softsynth.javasonics.recplay.test;

import com.softsynth.javasonics.recplay.PlayerApplet;
import com.softsynth.javasonics.util.Logger;

/**
 * @author Phil Burk (C) 2004
 */
public class TestLoadPlay
{

	PlayerApplet applet;

	void test()
	{
		applet = new PlayerApplet();
		applet.startTestInFrame();

		final String uploadsDir = "http://www.javasonics.com/listenup/uploads";
		String sample1 = uploadsDir + "/message_2minute_nick.spx";
		String sample2 = uploadsDir + "/qa_r8000.spx";
		try
		{
			Thread.sleep( 2000 );
			
			for( int i = 0; i < 10; i++ )
			{
				int dur = i * 1000;

				Thread.sleep( dur );
				Logger.println( 1, "load and play sample1" );
				applet.loadRecording( sample1 );
				applet.play();

				Thread.sleep( dur );
				Logger.println( 1, "load and play sample2" );
				applet.loadRecording( sample2 );
				applet.play();
			}

			for( int i = 0; i < 10; i++ )
			{
				int dur = i * 1000;

				Thread.sleep( dur );
				Logger.println( 1, "load and play sample1" );
				applet.loadRecording( sample1, true );

				Thread.sleep( dur );
				Logger.println( 1, "load and play sample2" );
				applet.loadRecording( sample2, true );
			}
		} catch( InterruptedException e )
		{
		}
	}

	public static void main( String[] args )
	{
		TestLoadPlay app = new TestLoadPlay();
		app.test();

	}
}