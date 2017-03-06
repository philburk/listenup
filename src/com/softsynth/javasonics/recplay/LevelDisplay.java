package com.softsynth.javasonics.recplay;
import java.awt.*;
import com.softsynth.awt.*;

/**
 * Display audio volume levels.
 *
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */

class VUMeter extends BarGraphDisplay
{
    Color safeColor = new Color( 100, 200, 100 );
    Color unsafeColor = new Color( 255, 220, 80 );

	public VUMeter( int orientation )
    {
        super( orientation, 0.0, 0.0, 1.0 );
    }

    public void paint( Graphics g )
	{
        double level = get();
        if( (level > 0.2) && (level < 0.8) ) g.setColor( safeColor );
        else g.setColor( unsafeColor );
        super.paint( g );
    }
}

public class LevelDisplay extends Panel implements PlayerListener
{
    Player player;
    BarGraphDisplay  leftMeter;
    BarGraphDisplay  rightMeter;

    /** Construct a GUI for recording and playing back audio with standard tape transport controls.
     */
    public LevelDisplay()
    {
        setLayout( new GridLayout( 0, 1 ) );
        leftMeter = addMeter();
    }

	public void setPlayer(Player pPlayer)
	{
		if (this.player != null)
			player.removePlayerListener((PlayerListener) this);
		player = pPlayer;
		player.addPlayerListener((PlayerListener) this);

		// TODO stereo - if( player.getSamplesPerFrame() > 1 )
		//{
		//	rightMeter = addMeter();
		//}
	}

    private BarGraphDisplay createMeter() {
        if (false) {
            return (BarGraphDisplay) new VUMeter(VUMeter.HORIZONTAL);
        }
        else {
            return (BarGraphDisplay) new LEDBarDisplay(VUMeter.HORIZONTAL, 0.0, 0.0, 1.0);
        }
    }

    private BarGraphDisplay addMeter() {
        BarGraphDisplay meter;
        Panel panel = new InsetPanel( 8 );
        panel.setLayout( new BorderLayout() );
        panel.add( "Center", meter = createMeter() );
        add( panel );
        return meter;
    }

    public void addLabels( String lowLabelText, String midLabelText, String highLabelText )
    {
        Panel panel = new Panel();
        panel.setLayout( new BorderLayout() );
        if( lowLabelText != null ) panel.add( "West", new Label( lowLabelText, Label.CENTER ) );
        if( midLabelText != null ) panel.add( "Center", new Label( midLabelText, Label.CENTER ) );
        if( highLabelText != null ) panel.add( "East", new Label( highLabelText, Label.CENTER ) );
        add( panel );
    }

   	public void playerTimeChanged( Player player, double time ) {}

/** Called when volume level changes.
 */
   	public void playerLevelChanged( Player player )
       {
           leftMeter.set(player.getLeftLevel());
           if (rightMeter != null)
               rightMeter.set(player.getRightLevel());
       }

   	public void playerStateChanged( Player player, int state, Throwable thr )
   	{
        switch ( state )
        {
            case Player.STOPPED:
			case Player.PAUSED:
                leftMeter.set( 0.0 );
                if( rightMeter != null ) rightMeter.set( 0.0 );
                break;
        }
    }
}