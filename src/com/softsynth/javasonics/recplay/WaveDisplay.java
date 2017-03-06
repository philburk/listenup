package com.softsynth.javasonics.recplay;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.PopupMenu;
import java.awt.event.MouseEvent;

import com.softsynth.awt.SafeImageFactory;
import com.softsynth.javasonics.error.ErrorReporter;
import com.softsynth.upload.Query;

/**
 * Show amplitude profile of recording. Animated cursor shows position.
 * 
 * @author Nick Didkovsky, didkovn@mail.rockefeller.edu
 * 
 */
public class WaveDisplay extends PositionCanvas
{
	private int yScalar;
	private Player player;
	private short[] mins;
	private short[] maxs;
	private Image waveformImage;
	private int oldWidth = -1;
	private int oldHeight = -1;
	private int oldSamplesPlayable = -1;
	private boolean oldEnabled = false;
	private Color cursorColor = new Color( 240, 200, 100 );
	// Use alpha channel to draw transparently.
	private Color selectionColor;
	private int mouseDownX = 0;
	private int mouseDownY = 0;
	private boolean playerHasChanged = false;

	private static final boolean useRefreshLimiter = false;
	private static final int REFRESH_MSEC = 200;
	private long lastTimeRendered = System.currentTimeMillis()
			- (2 * REFRESH_MSEC);
	private WavePopupMenuFactory wavePopupMenuFactory;
	private final WaveDisplay waveDisplay = this;
	WaveWatchdog waveWatchdog = null;
	private Color backgroundColor = Color.white;
	private Color foregroundColor = Color.black;
	private boolean useXORHighlights;
	private boolean popupMenuEnabled = false;

	public WaveDisplay()
	{
		// TODO There are some serious refresh problems on Mac.
		// We may need to use some Listeners to fix this.
		// We only need this goofy delayed repaint on Apple Java.
		String vendor = System.getProperty( "java.vendor" );
		if( vendor.indexOf( "Apple" ) >= 0 )
		{
			waveWatchdog = new WaveWatchdog();
			waveWatchdog.start();
		}

		backgroundColor = getBackground();
		foregroundColor = getForeground();


		useXORHighlights = ( Query.getJavaVersion() < 1.6 );
		if( useXORHighlights )
		{
			selectionColor = new Color( 150, 200, 240 );
		}
		else
		{
			// 50% alpha
			selectionColor = new Color( 100, 150, 240, 128 );
		}
	}

	void requestDelayedRepaint()
	{
		if( waveWatchdog != null )
		{
			waveWatchdog.requestDelayedRepaint();
		}
	}

	public void setPlayer( Player player )
	{
		this.player = player;
		playerHasChanged = true;
		repaint();
	}

	/** Delayed repaint() intended to fix refresh problems on Macintosh. */
	class WaveWatchdog extends Thread
	{
		long requestedRepaintTime = 0;
		int notShowingCount = 0;
		int repaintCountDown = 0;

		public void run()
		{
			while( true )
			{
				try
				{
					long now = System.currentTimeMillis();
					if( now > requestedRepaintTime )
					{
						if( repaintCountDown > 0 )
						{
							// System.out
							// .println( "WaveWatchdog calling repaint(),
							// requestCount = "
							// + repaintCountDown );
							waveDisplay.repaint();
							repaintCountDown -= 1;
						}
						sleep( 1000 );
					}
					else
					{
						sleep( REFRESH_MSEC );
					}
				} catch( InterruptedException e )
				{
				}

				// Force thread to bail out if Applet done.
				if( !waveDisplay.isShowing() )
				{
					if( notShowingCount > 5 )
					{
						// System.out
						// .println( "WaveWatchdog exiting because not showing."
						// );
						return;
					}
					notShowingCount += 1;
				}
				else
				{
					notShowingCount = 0;
				}
			}
		}

		public void requestDelayedRepaint()
		{
			requestedRepaintTime = System.currentTimeMillis() + REFRESH_MSEC;
			repaintCountDown = 10;
			interrupt();
		}
	}

	/**
	 * TODO This was put here because different position displays use different
	 * notions of maximum time. The scrollbar uses maxPlayerTime or
	 * maxRecordableTime depending on the situation. The WaveDisplay always uses
	 * maxPlayableTime. I recognize that this is ugly but cannot come up with a
	 * better scheme at this time.
	 * 
	 * @param player
	 * @return
	 */
	public double getMaxTime( Player player )
	{
		return player.getMaxPlayableTime();
	}

	private int getSamplesPlayable()
	{
		if( player == null )
		{
			return 0;
		}
		Recording recording = player.getRecording();
		if( recording == null )
		{
			return 0;
		}
		return recording.getMaxSamplesPlayable();
	}

	/**
	 * Measure peak values if sample is squished.
	 * 
	 * @return samples drawn
	 */
	private void measurePeaks()
	{
		mins = null;
		maxs = null;

		int width = bounds().width;
		if( width <= 0 )
		{
			return;
		}
		// Bail if more pixels than samples.
		int maxSamples = getSamplesPlayable();
		oldSamplesPlayable = maxSamples;
		if( maxSamples < width )
		{
			return;
		}

		if( player != null )
		{
			Recording recording = player.getRecording();
			if( recording != null )
			{
				PeakMeasurements measurement = recording.measurePeaks( width );
				mins = measurement.mins;
				maxs = measurement.maxs;
			}
		}
	}

	public void drawBackground( Graphics g )
	{
		oldEnabled = isEnabled();
		Color color = oldEnabled ? backgroundColor  : backgroundColor.darker() ;
		g.setColor( color );
		g.fillRect( 0, 0, bounds().width, bounds().height );
		// Draw bounding rectangle in foreground color.
		g.setColor( foregroundColor );
		g.drawRect( 0, 0, bounds().width, bounds().height );
	}

	private void redrawWaveformImage()
	{
		int width = bounds().width;
		int height = bounds().height;
		if( width < 8 )
		{
			width = 8;
		}
		if( height < 8 )
		{
			height = 8;
		}
		if( (waveformImage != null) || (width != oldWidth)
				|| (height != oldHeight) )
		{
			waveformImage = SafeImageFactory.createImage( this, width, height );
		}
		
		if( waveformImage != null )
		{
			Graphics g = waveformImage.getGraphics();
			drawBackground( g );
			g.setColor( foregroundColor );
			if( player == null )
			{
				g.drawString( "X", 10, 15 );
			}
			else if( mins == null )
			{
				drawDiagonalWaveform( g );
			}
			else
			{
				drawMinMaxWaveform( g );
			}
	
			oldWidth = width;
			oldHeight = height;
			lastTimeRendered = System.currentTimeMillis();
		}
	}

	private boolean isRecording()
	{
		if( player == null )
			return false;
		else
			return (player.getState() == Recorder.RECORDING);
	}

	private boolean decideIfRemeasureNeeded()
	{
		boolean remeasure = false;

		if( playerHasChanged || (waveformImage == null)
				|| (bounds().width != oldWidth)
				|| (getSamplesPlayable() < oldSamplesPlayable) )
		{
			remeasure = true;
			playerHasChanged = false;
		}
		else if( isRecording() || (getSamplesPlayable() > oldSamplesPlayable) )
		{
			if( useRefreshLimiter )
			{
				// Don't remeasure every time.
				long currentTime = System.currentTimeMillis();
				if( (currentTime - lastTimeRendered) >= REFRESH_MSEC )
				{
					remeasure = true;
				}
				else
				{
					// If it is not time now then repaint in a while.
					requestDelayedRepaint();
				}
			}
			else
			{
				remeasure = true;
			}
		}
		return remeasure;
	}

	private boolean decideIfRedrawNeeded()
	{
		return ((bounds().height != oldHeight) || (isEnabled() != oldEnabled));
	}

	/**
	 * Decide whether it is time to rebuild the image.
	 */
	private void rebuildImageIfNeeded()
	{
		if( decideIfRemeasureNeeded() )
		{
			measurePeaks();
			redrawWaveformImage();
		}
		else if( decideIfRedrawNeeded() )
		{
			redrawWaveformImage();
		}
	}

	/**
	 * @param g
	 */
	private void drawMinMaxWaveform( Graphics g )
	{
		yScalar = bounds().height;
		for( int x = 0; x < mins.length; x++ )
		{
			int lo = mins[x];
			int hi = maxs[x];
			int y1 = sampleToY( lo );
			int y2 = sampleToY( hi );
			// show clipped regions in red
			/*
			 * if( (lo == Short.MAX_VALUE) || (hi == Short.MAX_VALUE)) {
			 * g.setColor( Color.red ); g.drawLine(x, y1, x, y2); g.setColor(
			 * getForeground() ); } else {
			 */
			g.drawLine( x, y1, x, y2 );
			// }
		}
	}

	/**
	 * @param g
	 */
	private void drawDiagonalWaveform( Graphics g )
	{
		if( player == null )
			return;

		Recording recording = player.getRecording();
		if( recording == null )
		{
			return;
		}
		int numSamples = recording.getMaxSamplesPlayable();
		if( numSamples < 4 )
		{
			return;
		}
		double xScaler = (double) bounds().width / numSamples;
		// We only do this if we don't have very many samples
		// so don't worry about memory usage.
		short[] sampleBuffer = new short[numSamples];
		recording.read( 0, sampleBuffer, 0, numSamples );
		int x1 = 0;
		int y1 = sampleToY( sampleBuffer[0] );
		for( int i = 1; i < numSamples; i++ )
		{
			int x2 = (int) (i * xScaler);
			int y2 = sampleToY( sampleBuffer[i] );
			g.drawLine( x1, y1, x2, y2 );
			x1 = x2;
			y1 = y2;
		}
	}

	/**
	 * @param s
	 * @return
	 */
	private int sampleToY( int s )
	{
		// Shift by 16 because shorts are 16 bit.
		int temp = (yScalar >> 1) - ((s * yScalar) >> 16);
		return temp;
	}

	private void drawSelection( Graphics g )
	{
		if( player != null )
		{
			int h = bounds().height;
			int x1 = valueToX( player.getStartTime() / player.getMaxTime() );
			int x2 = valueToX( player.getStopTime() / player.getMaxTime() );
			if( x2 > x1 )
			{
				// We cannot use XOR mode because of a bug in Java 1.6.0_11
				// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6635462
				if( useXORHighlights )
				{
					g.setXORMode( Color.white );
				}
				g.setColor( selectionColor );
				g.fillRect( x1, 0, x2 - x1, bounds().height );
				if( useXORHighlights )
				{
					g.setPaintMode();
				}
			}
		}
	}

	private void drawCursor( Graphics g, double scaledPosition )
	{
		int h = bounds().height;
		g.setColor( cursorColor );
		int h4 = h >> 2;
		int x = valueToX( getValue() );
		g.fillRect( x - 1, 0, 3, h4 );
		g.fillRect( x, h4, 1, h - h4 );
		g.fillRect( x - 1, h - h4, 3, h );
	}

	/*
	 * Called by AWT. Normally clears rectangle but that is unnecessary so we
	 * just paint().
	 * 
	 * @see java.awt.Component#update(java.awt.Graphics)
	 */
	public void update( Graphics g )
	{
		paint( g );
	}

	public void paint( Graphics g )
	{
		rebuildImageIfNeeded();
		if( waveformImage == null )
		{
			drawBackground( g );
		}
		else
		{
			g.drawImage( waveformImage, 0, 0, this );
		}
		drawSelection( g );
		drawCursor( g, getValue() );
	}

	private void handleMouseSelection( int x )
	{
		int leftX;
		int rightX;
		boolean atLeftEdge = false;
		boolean atRightEdge = false;

		// Clip X to range
		if( x < 0 )
		{
			x = 0;
			atLeftEdge = true;
		}
		else if( x > bounds().width )
		{
			x = bounds().width;
			atRightEdge = true;
		}

		if( x > mouseDownX )
		{
			leftX = mouseDownX;
			rightX = x;
		}
		else
		{
			leftX = x;
			rightX = mouseDownX;
		}

		// Prevent user from selecting a very small window and getting confused.
		if( rightX <= (leftX + 2) )
			rightX = leftX;

		int oldStartIndex = player.getStartIndex();
		// Don't bother with zero crossing if at edge.
		double newStartTime = atLeftEdge ? xToTime( leftX )
				: xToZeroCrossingTime( leftX );
		player.setStartTime( newStartTime );

		int oldStopIndex = player.getStopIndex();
		double newStopTime = atRightEdge ? xToTime( rightX )
				: xToZeroCrossingTime( rightX );
		player.setStopTime( newStopTime );

		if( (oldStartIndex != player.getStartIndex())
				|| (oldStopIndex != player.getStopIndex()) )
		{
			repaint();
			requestDelayedRepaint();
		}
	}

	private double xToTime( int x )
	{
		int sampleIndex = (int) (xToValue( x ) * player.getRecording()
				.getMaxSamplesPlayable());
		return player.getRecording().sampleIndexToTime( sampleIndex );
	}

	private double xToZeroCrossingTime( int x )
	{
		int sampleIndex = (int) (xToValue( x ) * player.getRecording()
				.getMaxSamplesPlayable());
		// Logger.println(0, "sampleIndex = " + sampleIndex);
		int zeroCrossing = player.getRecording().findPreviousZeroCrossing(
				sampleIndex, 2048 );
		// Logger.println(0, "zeroCrossing = " + zeroCrossing);
		return player.getRecording().sampleIndexToTime( zeroCrossing );
	}

	void handleMouseDown( MouseEvent e, Object arg )
	{
		try
		{
			if( isEnabled() )
			{
				mouseDownX = e.getX();
				mouseDownY = e.getY();
				int mods = e.getModifiers();
				if( ((mods & MouseEvent.BUTTON1_MASK) != 0) &&
				// Add this so Mac works with one button mice.
						((mods & MouseEvent.CTRL_MASK) == 0) )
				{
					handleMouseSelection( mouseDownX );
				}
				else if( popupMenuEnabled )
				{
					showPopupWaveMenu();
				}
			}
		} catch( Exception exc )
		{
			ErrorReporter.show( exc );
		}
	}

	/**
	 * 
	 */
	private void showPopupWaveMenu()
	{
		PopupMenu menu = wavePopupMenuFactory.createMenu( player );
		getParent().add( menu );
		menu.show( this, mouseDownX, mouseDownY - 5 );
		repaint();
		requestDelayedRepaint();
	}

	void handleMouseDragged( MouseEvent e, Object arg )
	{
		if( isEnabled() ) // && (e.getButton() == 1) )
		{
			handleMouseSelection( e.getX() );
		}
	}

	void handleMouseReleased( MouseEvent e, Object arg )
	{
		if( isEnabled() && ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)
				&& player.isPlayable() )
		{
			if( e.getClickCount() == 2 )
			{
				player.setStartTime( 0.0 );
				player.setStopTime( player.getMaxTime() );
			}
			else
			{
				handleMouseSelection( e.getX() );
			}
			setValue( player.getStartTime() / player.getMaxTime() );
			observable.changed( arg );
			// Try to get display to refresh in Mac OS X in Firefox
			repaint();
			requestDelayedRepaint();
		}
	}

	public void setWavePopupMenuFactory(
			WavePopupMenuFactory wavePopupMenuFactory )
	{
		this.wavePopupMenuFactory = wavePopupMenuFactory;
	}

	public void setBackground( Color color )
	{
		super.setBackground( color );
		// save a permanent copy
		backgroundColor = color;
	}
	public void setForeground( Color color )
	{
		super.setForeground( color );
		// save a permanent copy
		foregroundColor = color;
	}

	/**
	 * @return the popupMenuEnabled
	 */
	public boolean isPopupMenuEnabled()
	{
		return popupMenuEnabled;
	}

	/**
	 * @param popupMenuEnabled the popupMenuEnabled to set
	 */
	public void setPopupMenuEnabled( boolean popupMenuEnabled )
	{
		this.popupMenuEnabled = popupMenuEnabled;
	}

}