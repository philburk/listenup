package com.softsynth.javasonics.recplay;

import java.awt.Color;
import java.awt.Component;
import java.awt.Panel;

/**
 * Panel with play/stop buttons for controlling recording. This class
 * provides a graphical front end for the non-graphical Player Class.
 * 
 * @author (C) 2001 Phil Burk, All Rights Reserved
 */

public class TransportControl extends Panel implements PlayerListener
{
	Player player;
	Component stopButton;
	Component pauseButton;
	Component playButton;
	Component fastForwardButton;
	// Component slowForwardButton;
	Component rewindButton;

	Component toBeginButton;
	Component toEndButton;

	boolean forceListen = false;

	public void setButtonBackground( Color color )
	{
		if( stopButton != null ) stopButton.setBackground(  color );
		if( pauseButton != null ) pauseButton.setBackground(  color );
		if( playButton != null ) playButton.setBackground(  color );
		if( fastForwardButton != null ) fastForwardButton.setBackground(  color );
		if( rewindButton != null ) rewindButton.setBackground(  color );
		if( toBeginButton != null ) toBeginButton.setBackground(  color );
		if( toEndButton != null ) toEndButton.setBackground(  color );
	}
	
	public void setPlayer( Player pPlayer )
	{
		if( this.player != null )
			player.removePlayerListener( (PlayerListener) this );
		this.player = pPlayer;
		updateButtons();
		player.addPlayerListener( (PlayerListener) this );
	}

	public void playerTimeChanged( Player player, double time )
	{
	}

	public void playerLevelChanged( Player player )
	{
	}

	public void setStopEnabled( boolean enabled )
	{
		if( stopButton != null )
			stopButton.setEnabled( enabled );
	}

	public void setPlayEnabled( boolean enabled )
	{
		if( playButton != null )
			playButton.setEnabled( enabled );
	}

	public void setPauseEnabled( boolean enabled )
	{
		if( pauseButton != null )
			pauseButton.setEnabled( enabled );
	}

	public void setFastForwardEnabled( boolean enabled )
	{
		if( fastForwardButton != null )
			fastForwardButton.setEnabled( enabled );
	}

	//    public void setSlowForwardEnabled(boolean enabled)
	//    {
	//        if (slowForwardButton != null)
	//            slowForwardButton.setEnabled(enabled);
	//    }

	public void setRewindEnabled( boolean enabled )
	{
		if( rewindButton != null )
			rewindButton.setEnabled( enabled );
	}

	public void setToBeginEnabled( boolean enabled )
	{
		if( toBeginButton != null )
			toBeginButton.setEnabled( enabled );
	}

	public void setToEndEnabled( boolean enabled )
	{
		if( toEndButton != null )
			toEndButton.setEnabled( enabled );
	}

	public void setSpeedScrollbarEnabled( boolean enabled )
	{
		// stub, only works in SkinnableTranscriberControl
	}

	public void setForceListen( boolean forced )
	{
		forceListen = forced;
	}

	public boolean getForceListen()
	{
		return forceListen;
	}

	public void updateButtons()
	{
		if( player == null )
		{
			setPlayEnabled( false );
			setStopEnabled( false );
			setPauseEnabled( false );
			setFastForwardEnabled( false );
			//            setSlowForwardEnabled(false);
			setRewindEnabled( false );
			setSpeedScrollbarEnabled( false );
			setToBeginEnabled( false );
			setToEndEnabled( false );
		}
		else
		{
			switch( player.getState() )
			{
			case Recorder.STOPPED:
				setStopEnabled( false );
				setPauseEnabled( false );
				setPlayEnabled( player.isPlayable() && isEnabled() );
				setFastForwardEnabled( player.isPlayable() && isEnabled() );
				//                setSlowForwardEnabled(player.isPlayable() && isEnabled());
				setRewindEnabled( player.isPlayable() && isEnabled() );
				setSpeedScrollbarEnabled( player.isPlayable() && isEnabled() );
				setToBeginEnabled( player.isPlayable() && isEnabled() );
				setToEndEnabled( player.isPlayable() && isEnabled() );
				break;

			case Recorder.PAUSED:
				setStopEnabled( isEnabled() );
				setPauseEnabled( false );
				setPlayEnabled( player.isPlayable() && isEnabled() );
				setFastForwardEnabled( player.isPlayable() && isEnabled() );
				//                setSlowForwardEnabled(player.isPlayable() && isEnabled());
				setRewindEnabled( player.isPlayable() && isEnabled() );
				setSpeedScrollbarEnabled( player.isPlayable() && isEnabled() );
				setToBeginEnabled( player.isPlayable() && isEnabled() );
				setToEndEnabled( player.isPlayable() && isEnabled() );
				break;

			case Recorder.PLAYING:
				setPauseEnabled( !forceListen && isEnabled() );
				setStopEnabled( !forceListen && isEnabled() );
				setPlayEnabled( player.isPlayModified() );
//				System.out.println("TransportControl.updateButtons, player.isPlayModified()=" + player.isPlayModified());
				setFastForwardEnabled( true );
				//                setSlowForwardEnabled(true);
				setSpeedScrollbarEnabled( true );
				setToBeginEnabled( true );
				setToEndEnabled( true );
				break;

			case Recorder.RECORDING:
				setPauseEnabled( !forceListen && isEnabled() );
				setStopEnabled( !forceListen && isEnabled() );
				setPlayEnabled( false );
				setFastForwardEnabled( false );
				//                setSlowForwardEnabled(false);
				setRewindEnabled( false );
				setSpeedScrollbarEnabled( false );
				setToBeginEnabled( false );
				setToEndEnabled( false );
				break;
			}
		}
	}

	/**
	 * Enable or disable buttons based on current mode of operation.
	 */
	public void playerStateChanged( Player player, int state, Throwable thr )
	{
		updateButtons();
	}

	// the following FF & Rew getters and setters are used by
	// SkinnableTranscriberControl
	// which is in another package and needs access to these field
	/**
	 * @param fastForwardButton
	 *            The fastForwardButton to set.
	 *  
	 */
	public void setFastForwardButton( Component fastForwardButton )
	{
		this.fastForwardButton = fastForwardButton;
	}

	/**
	 * @param rewindButton
	 *            The rewindButton to set.
	 *  
	 */
	public void setRewindButton( Component rewindButton )
	{
		this.rewindButton = rewindButton;
	}

	/**
	 * @return Returns the fastForwardButton.
	 */
	public Component getFastForwardButton()
	{
		return fastForwardButton;
	}

	/**
	 * @return Returns the rewindButton.
	 */
	public Component getRewindButton()
	{
		return rewindButton;
	}

	public Player getPlayer()
	{
		return player;
	}

	/**
	 * @return Returns the toBeginButton.
	 */
	public Component getToBeginButton()
	{
		return toBeginButton;
	}

	/**
	 * @return Returns the toEndButton.
	 */
	public Component getToEndButton()
	{
		return toEndButton;
	}

	/**
	 * @param toBeginButton
	 *            The toBeginButton to set.
	 */
	public void setToBeginButton( Component toBeginButton )
	{
		this.toBeginButton = toBeginButton;
	}

	/**
	 * @param toEndButton
	 *            The toEndButton to set.
	 */
	public void setToEndButton( Component toEndButton )
	{
		this.toEndButton = toEndButton;
	}

	public void removePauseButton()
	{
		remove(pauseButton);		
	}
}