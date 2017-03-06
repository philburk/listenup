/*
 * Created by Nick on Oct 27, 2004
 *
 */
package com.softsynth.javasonics.transcriber;

import java.awt.*;
import java.util.*;

import com.softsynth.javasonics.recplay.PositionScrollbar;

/**
 * @author Nick Didkovsky, (c) 2004 All rights reserved, Email:
 *         didkovn@mail.rockefeller.edu
 *  
 */
public class PlaybackSpeedControlPanel extends Panel
{
    Label playbackSpeedLabel;
    PositionScrollbar playbackSpeedScrollbar;
    // Speed for original playback rate is 1.0.
    static final double MAX_SPEED = 1.2;
    static final double MIN_SPEED = 0.7;

    public PlaybackSpeedControlPanel()
    {
        setLayout(new BorderLayout());
        add(BorderLayout.WEST, playbackSpeedScrollbar = new PositionScrollbar());
        playbackSpeedScrollbar.setValue(speedToValue(1.0));
        add(BorderLayout.EAST, playbackSpeedLabel = new Label("100%"));
        playbackSpeedScrollbar.addObserver(new Observer()
        {
            public void update(Observable arg0, Object arg1)
            {
				updateSpeedLabel();
               //                if (player != null)
                //                {
                //                    player.setSlowForwardSpeed(speed);
                //                } else
                //                {
                //                    System.out.println("PlaybackSpeedControlPanel player null");
                //                }
            }
        });
		updateSpeedLabel();
    }

	private double speedToValue( double speed )
	{
		double range = MAX_SPEED - MIN_SPEED;
		return ((speed - MIN_SPEED) / range);
	}
	
	private void updateSpeedLabel()
	{
		int formattedSpeed = (int) (getSpeed() * 100);
		playbackSpeedLabel.setText(formattedSpeed + "%");
	}
	
    public float getSpeed()
    {
        double range = MAX_SPEED - MIN_SPEED;
        float speed = (float) (range * playbackSpeedScrollbar.getValue() + MIN_SPEED);
        return speed;
    }

    //    /**
    //     * @return Returns the player.
    //     */
    //    public Player getPlayer()
    //    {
    //        return player;
    //    }
    //
    //    /**
    //     * @param player
    //     * The player to set.
    //     */
    //    public void setPlayer(Player player)
    //    {
    //        this.player = player;
    //    }

    /**
     * @param b
     */
    public void setBevelled(boolean b)
    {
        if (playbackSpeedScrollbar != null)
        {
            playbackSpeedScrollbar.setBevelled(b);
        }
    }

    /**
     * Add an Observer that will receive update() calls when the slider is moved
     * using the mouse.
     */
    public void addObserver(Observer o)
    {
        playbackSpeedScrollbar.addObserver(o);
    }

    public void deleteObserver(Observer o)
    {
        playbackSpeedScrollbar.deleteObserver(o);
    }

}