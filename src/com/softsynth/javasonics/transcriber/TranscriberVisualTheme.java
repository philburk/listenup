package com.softsynth.javasonics.transcriber;

import com.softsynth.awt.*;
import com.softsynth.javasonics.recplay.VisualTheme;

import java.awt.*;
import java.io.*;

/**
 * Add Transcription buttons to theme.
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */

public class TranscriberVisualTheme extends VisualTheme
{
	public final static int FASTFORWARD_ROW_INDEX = 0;
	public final static int REWIND_ROW_INDEX = 1;
	public final static int TO_END_ROW_INDEX = 2;
	public final static int TO_BEGIN_ROW_INDEX = 3;

	public TranscriberVisualTheme(Component visibleComponent,
			InputStream inStream) throws IOException
	{
		super( visibleComponent, inStream );
		numRows = 4;
	}

	public ImageButton createFastForwardButton()
	{
		return new ImageButton( slicer.createSubImageRow(
				FASTFORWARD_ROW_INDEX, numRows, NUM_COLUMNS ) );
	}

	public ImageButton createRewindButton()
	{
		return new ImageButton( slicer.createSubImageRow( REWIND_ROW_INDEX,
				numRows, NUM_COLUMNS ) );
	}

	public ImageButton createToBeginButton()
	{
		return new ImageButton( slicer.createSubImageRow( TO_BEGIN_ROW_INDEX,
				numRows, NUM_COLUMNS ) );
	}

	public ImageButton createToEndButton()
	{
		return new ImageButton( slicer.createSubImageRow( TO_END_ROW_INDEX,
				numRows, NUM_COLUMNS ) );
	}

}

