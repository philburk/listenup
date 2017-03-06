package com.softsynth.javasonics.recplay;

import com.softsynth.awt.*;
import com.softsynth.javasonics.error.ErrorReporter;

import java.awt.*;
import java.io.*;

/**
 * Panel with play/stop image buttons for controlling recording. This class
 * provides a graphical front end for the non-graphical Player Class.
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */

public class VisualTheme extends Component
{
    private Image skin;
    protected ImageSlicer slicer;
    public final static int PLAY_ROW_INDEX = 0;
    public final static int STOP_ROW_INDEX = 1;
    public final static int RECORD_ROW_INDEX = 2;
    public final static int PAUSE_ROW_INDEX = 3;
	public final static int NUM_COLUMNS = 4;

    public int numRows= 4;

    public VisualTheme(Component visibleComponent, InputStream inStream)
            throws IOException
    {
		if( inStream == null )
		{
			throw new RuntimeException("Could not find images.");
		}
    	numRows = 4;
        skin = loadImage(this, inStream);
        slicer = new ImageSlicer(visibleComponent, skin);
    }

    public ImageButton createPlayButton()
    {
        return new ImageButton(slicer.createSubImageRow(PLAY_ROW_INDEX,
                numRows, NUM_COLUMNS));
    }

    public ImageButton createStopButton()
    {
        return new ImageButton(slicer.createSubImageRow(STOP_ROW_INDEX,
                numRows, NUM_COLUMNS));
    }

    public ImageButton createPauseButton()
    {
        return new ImageButton(slicer.createSubImageRow(PAUSE_ROW_INDEX,
                numRows, NUM_COLUMNS));
    }

    public ImageButton createRecordButton()
    {
        return new ImageButton(slicer.createSubImageRow(RECORD_ROW_INDEX,
                numRows, NUM_COLUMNS));
    }

    protected static byte[] loadEntireStream(InputStream inStream)
            throws IOException
    {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (true)
        {
            int numRead = inStream.read(buffer);
            if (numRead < 0)
                break; // EOF
            outStream.write(buffer, 0, numRead);
        }
        return outStream.toByteArray();
    }

    protected static Image loadImage(Component comp, InputStream inStream)
            throws IOException
    {
        byte[] bytes = loadEntireStream(inStream);
        Image img = null;
        try
        {
            MediaTracker m = new MediaTracker(comp);
            img = Toolkit.getDefaultToolkit().createImage(bytes);
            m.addImage(img, 0);
            m.waitForAll();
        } catch (Exception e)
        {
        	ErrorReporter.show("Tried to load button image.", e);
        }
        return img;
    }

}

