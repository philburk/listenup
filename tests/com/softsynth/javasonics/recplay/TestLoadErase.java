package com.softsynth.javasonics.recplay.test;

import com.softsynth.javasonics.recplay.RecorderUploadApplet;
import com.softsynth.javasonics.util.Logger;

/**
 * Test for collisions between loading samples and displaying waveforms.
 * @author Phil Burk (C) 2004
 */
public class TestLoadErase
{

	RecorderUploadApplet applet;

	void test()
	{
		applet = new RecorderUploadApplet();
		applet.setTestParameter("uploadURL", "http://www.javasonics.com/listenup/examples/stub_success.txt");
		applet.startTestInFrame();

		final String uploadsDir = "http://www.javasonics.com/listenup/uploads";
		String sample1 = uploadsDir + "/message_2minute_nick.spx";
		String sample2 = uploadsDir + "/qa_r8000.spx";
		try
		{
			Thread.sleep( 2000 );
			
			for( int i = 0; i < 5; i++ )
			{
				int dur = i * 1000;

				Logger.println( 1, "load and erase sample1" );
				applet.loadRecording( sample1 );
				Thread.sleep( dur );
				applet.erase();

				Logger.println( 1, "load and erase sample2" );
				applet.loadRecording( sample2 );
				Thread.sleep( dur );
				applet.erase();
			}

			for( int i = 0; i < 5; i++ )
			{
				int dur = i * 1000;

				Logger.println( 1, "load, play, erase sample1" );
				applet.loadRecording( sample1 );
				applet.play();
				Thread.sleep( dur );
				applet.erase();

				Logger.println( 1, "load, play, erase sample2" );
				applet.loadRecording( sample2 );
				applet.play();
				Thread.sleep( dur );
				applet.erase();
			}
		} catch( InterruptedException e )
		{
		}
	}

	public static void main( String[] args )
	{
		TestLoadErase app = new TestLoadErase();
		app.test();

	}
}