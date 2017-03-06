package com.softsynth.javasonics.recplay;
import java.awt.*;
import java.awt.event.*;

import com.softsynth.awt.ImageLabel;
import com.softsynth.javasonics.error.ErrorReporter;

/**
 * Alert user to fact that recording is now being made.
 * Pop up dialog with VU meter level.
 *
 * @author (C) 2001 Phil Burk, SoftSynth.com, All Rights Reserved
 */

public class RecordingAlert extends Frame
{
	static RecordingAlert singleton;
	int level = 3;
	int MAX_LEVEL = 6;
	RecordingImageLabel imageLabel;

	public RecordingAlert()
	{
		//		super(new Frame(), "Recording", false);
		super("Recording");

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				hide();
			}
		});

		Image img = null;
		try
		{
			MediaTracker m = new MediaTracker(this);
			img = Toolkit.getDefaultToolkit().createImage(HeadMicImage.data);
			m.addImage(img, 0);
			m.waitForAll();
			imageLabel = new RecordingImageLabel(img);
			add(imageLabel);
			pack();

		} catch (Exception e)
		{
			ErrorReporter.show("Trying to display recording alert.", e);
		}
	}

	/** Place randomly near left edge of screen */
	private void randomizeLocation()
	{
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int x = 20 + (int) (Math.random() * 200 );
		int y = 20 + (int) (Math.random() * (screenSize.height - 150));
		setLocation(x, y);
	}
	
	/** ImageLabel with arcs showing recording animation. */
	class RecordingImageLabel extends ImageLabel
	{

		public void paint(Graphics g)
		{
			super.paint(g);
			g.setColor(Color.white);
			int x = 10;
			int xInc = 6;
			int y = 15;
			int yInc = -5;
			int w = 40;
			int wInc = 8;
			int h = 40;
			int hInc = 8;
			int startAngle = -20;
			int angleInc = 5;
			int angleExtent = 60;
			// Draw series of arcs.
			for (int i = 0; i < MAX_LEVEL; i++)
			{

				if (i < level)
					g.setColor(Color.yellow);
				else
					g.setColor(Color.gray);
				g.drawArc(x, y, w, h, startAngle, angleExtent);
				x += xInc;
				y += yInc;
				startAngle -= angleInc;
				angleExtent += 2 * angleInc;
				w += wInc;
				h += hInc;
			}
		}
		/** Cycle through changing arc color. */
		public void animate()
		{
			level += 1;
			if (level > MAX_LEVEL)
				level = 0;
			repaint();
		}

		public RecordingImageLabel(Image img)
		{
			super(img);
		}
	}

	static synchronized void showAlert()
	{
		if (singleton == null)
		{
			singleton = new RecordingAlert();
		}
		singleton.alertUser();
	}
	
	static synchronized void hideAlert()
	{
		if (singleton != null)
		{
			singleton.hide();
		}
	}

	private void alertUser()
	{
		randomizeLocation();
		
		show();

		// Launch thread that will run animation and
		// bring dialog to front in case it gets pushed back.
		new Thread()
		{
			public void run()
			{
				int i = 2;
				try
				{
					while (isShowing())
					{
						if (i-- > 0)
							toFront();
						sleep(400);
						imageLabel.animate();
					}
				} catch (InterruptedException exc)
				{
				}
			}
		}
		.start();
	}

}