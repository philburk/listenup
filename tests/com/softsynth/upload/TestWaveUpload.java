package com.softsynth.upload.test;
import java.applet.Applet;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.softsynth.javasonics.util.WAVWriter;
import com.softsynth.storage.DynamicBuffer;
import com.softsynth.storage.DynamicBufferFactory;
import com.softsynth.upload.FileUploader;

/**
 * Generate a synthetic WAV file and upload it to the server.
 * @author (C) Phil Burk, http://www.softsynth.com
*/
public class TestWaveUpload extends Applet
{
    boolean      isApplet = true;
//    String       CGI_CODEBASE = "http://www.javasonics.com/testup/";
//    String       CGI_GETID_URL = "get_unique_id.php";
//    String       CGI_UPLOAD_URL = "handle_upload_sql.php";
    String       CGI_GETID_URL = "http://www.javasonics.com/testup/get_fake_id.php";
    String       CGI_UPLOAD_URL = "http://www.javasonics.com/cgi-bin/echoinput.pl";
    URL          getIdURL;
    Button       uploadButton;
    int          FRAME_RATE = 22050;
    int          NUM_SAMPLES = 50;
    TextField    freqText;
    FileUploader fileUploader;

    public void start()
    {
        setupUploader();
        setupGUI();
    }

    void setupGUI()
    {
        setLayout( new GridLayout(0,1) );

        Panel topPanel = new Panel();
        add( topPanel );

        topPanel.add( new Label("Frequency") );
        topPanel.add( freqText = new TextField("449") );

        boolean showSendButton = true;
        String pSendText = "Send";
        Color buttonBackground = new Color( 200, 230, 210 );
        Panel uploadPanel = fileUploader.setupGUI(showSendButton,  pSendText,  buttonBackground);
        uploadPanel.setBackground( new Color( 240, 230, 210 ) );
        add( uploadPanel );


/* Synchronize Java display. */
		getParent().validate();
		getToolkit().sync();
    }

    void setupUploader()
    {
        try
        {
        // Use CodeBase for URL if running as an Applet.
            getIdURL = new URL( CGI_GETID_URL);
            URL uploadURL = new URL( CGI_UPLOAD_URL);

            fileUploader = new FileUploader( uploadURL )
			{
				public void sendButtonPressed()
				{
					testUpload();
				}
			};

		} catch( IOException e ) {
			System.err.println(e);
		} catch( SecurityException e ) {
			System.err.println(e);
		}
    }

    public void stop()
    {
        removeAll();
    }

/** Synthesize and upload a WAVE file to the server with a unique ID. */
    void testUpload()
    {
        String fileName = null;
        try
        {
            try
            {
                fileName = fileUploader.getFileNameFromServer( getIdURL );
            } catch( IOException e ) {
                fileUploader.displayMessage("Could not connect to ID generator.");
                System.err.println(e);
                return;
            }

            try
            {
                if( fileName != null )
                {
                    short[] samples = genSound( NUM_SAMPLES, getFrequency() );
                    InputStream waveStream = convertSampleToWaveFormat( samples, 1, FRAME_RATE );
                    fileUploader.uploadFileImage( fileName, "audio/wav", waveStream );
                }
                else
                {
                    fileUploader.displayMessage("Server did not provide unique name for file.");
                }
		    } catch( IOException e ) {
                fileUploader.displayMessage("Could not connect to upload script.");
            }

		} catch( SecurityException e ) {
			System.err.println(e);
		}
    }

/** Get frequency from user for synthesized waveform so we can tell one file from another. */
    int getFrequency()
    {
        int freq = 440;
        String text = freqText.getText();
        try
        {
            freq = Integer.parseInt( text );
        } catch( NumberFormatException e ) {
            freqText.setText( "440" );
        }
        return freq;
    }

/** Generate a sawtooth audio sample that we can upload to the server. */
    short[] genSound( int numSamples, int frequency )
    {
        System.out.println("Frequency = " + frequency);
        short[] samples = new short[numSamples];
        short phase = 0;
        for( int i=0; i<numSamples; i++ )
        {
            samples[i] = phase;
            phase += (((1<<16) * frequency) / FRAME_RATE);
        }
        return samples;
    }

/** Convert the sample data to Wave format for storage on the server. */
    InputStream convertSampleToWaveFormat( short samples[], int samplesPerFrame, int frameRate ) throws IOException
    {
        DynamicBuffer dynoBuffer = DynamicBufferFactory.createDynamicBuffer();
        WAVWriter writer = new WAVWriter( dynoBuffer.getOutputStream(), WAVWriter.FORMAT_S16 );
        //WAVWriter writer = new WAVWriter( stream, WAVWriter.FORMAT_IMA_ADPCM );
        writer.write(samples, samplesPerFrame, frameRate);
        return dynoBuffer.getInputStream();
    }

	static public void main( String[] argv )
	{
		System.out.println( "Test Wave File Upload" );
		final TestWaveUpload applet = new TestWaveUpload();
        applet.isApplet = false;

		Frame frame = new Frame("Test JavaSonics Controls")
		{
		/* Use funky old AWT 1.0.2 code because Netscape Java on Mac does not support WindowAdapter! */
			public boolean handleEvent( Event evt )
			{
				switch (evt.id)
				{
					case Event.WINDOW_DESTROY:
						applet.stop();
						applet.destroy();
						System.exit(0);
						return true;
					default:
						return super.handleEvent(evt);
				}
			}
		};

		frame.setSize( 400, 200 );
		frame.setLayout( new BorderLayout() );
		frame.add( "Center", applet );
		frame.show();
		applet.init();
		applet.start();
	}
}
