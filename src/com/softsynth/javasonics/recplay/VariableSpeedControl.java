/*
 * Created on Oct 13, 2004 
 *
 */
package com.softsynth.javasonics.recplay;

/**
 * Manage cycling through variable speeds
 * 
 * @author Nick Didkovsky, didkovn@mail.rockefeller.edu
 *
 */
public class VariableSpeedControl
{
    
    private static final float[] SLOW_FWD_SPEEDS = { 0.8f, 0.65f, 0.5f };
    private static final float[] FAST_FWD_SPEEDS = { 1.5f, 2f, 3f, 1f };
    // 1.0f is the signal to play normal speed with skipping forwards
    private float[] REWIND_SPEEDS = { 1.1f, 2f, 3f, 1f };
    // 1.0f is the signal to play normal speed with skipping backwards
    private int slowForwardSpeedIndex = 0;
    private int fastForwardSpeedIndex = 0;
    private int rewindSpeedIndex = 0;
    
    boolean advanceSFSpeed = false;
    boolean advanceFFSpeed = false;
    boolean advanceRewSpeed = false;
    
    private Player player;
    
    
    public VariableSpeedControl(Player player) {
        setPlayer(player);
    }
    
    /**
     * Increment slowForwardSpeed index, unless recovering from PAUSE
     * 
     * @return current slowForwardSpeed found at index
     */
    public float nextSlowForwardSpeed()
    {
        if (!advanceSFSpeed || player.getState() == Recorder.STOPPED
                || player.getState() == Recorder.PAUSED)
        {
            System.out.println("SF did not advance speed index");
        } else
        {
            slowForwardSpeedIndex = (slowForwardSpeedIndex + 1)
                    % SLOW_FWD_SPEEDS.length;
            System.out.println("SF INCREMENTED to " + slowForwardSpeedIndex);
        }
        advanceSFSpeed = true;
        advanceFFSpeed = false;
        advanceRewSpeed = false;
        player.setSkipping(false);
        float currentSpeed = SLOW_FWD_SPEEDS[slowForwardSpeedIndex];
        return currentSpeed;
    }

    /**
     * Increment fastForwardSpeed index, unless recovering from PAUSE
     * 
     * @return current fastForwardSpeed found at index
     */
    public float nextFastForwardSpeed()
    {
        if (!advanceFFSpeed || player.getState() == Recorder.STOPPED
                || player.getState() == Recorder.PAUSED)
        {
            //System.out.println("FF did not advance speed index");
        	fastForwardSpeedIndex = 0;
        } else
        {
            fastForwardSpeedIndex = (fastForwardSpeedIndex + 1)
                    % FAST_FWD_SPEEDS.length;
            //System.out.println("FF INCREMENTED to " + fastForwardSpeedIndex);
        }
        advanceSFSpeed = false;
        advanceFFSpeed = true;
        advanceRewSpeed = false;
        float currentSpeed = FAST_FWD_SPEEDS[fastForwardSpeedIndex];
        player.setSkipping(currentSpeed == 1f);
        return currentSpeed;
    }


    /**
     * Increment rewindSpeed index, unless recovering from PAUSE
     * 
     * @return current rewindSpeed found at index
     */
    public float nextRewindSpeed()
    {
        if (!advanceRewSpeed || player.getState() == Recorder.STOPPED
                || player.getState() == Recorder.PAUSED)
        {
            //System.out.println("Rewind did not advance speed index");
        	rewindSpeedIndex = 0;
        } else
        {
            rewindSpeedIndex = (rewindSpeedIndex + 1) % REWIND_SPEEDS.length;
            //System.out.println("Rewind INCREMENTED to " + rewindSpeedIndex);
        }
        advanceSFSpeed = false;
        advanceFFSpeed = false;
        advanceRewSpeed = true;
        float currentSpeed = REWIND_SPEEDS[rewindSpeedIndex];
        player.setSkipping(currentSpeed == 1f);
        return currentSpeed;
    }


    /** player.setPlaybackSpeed(1f) */
    private void resetPlaybackSpeed()
    {
        player.setPlaybackSpeed(1f);
    }


    /** block speed advance for all varispeed buttons */
    private void resetSpeedAdvanceFlags()
    {
        advanceSFSpeed = false;
        advanceFFSpeed = false;
        advanceRewSpeed = false;
    }


    private void resetDirection()
    {
        player.setReverse(false);
    }


    public void reset()
    {
        resetPlaybackSpeed();
        resetSpeedAdvanceFlags();
        resetDirection();
        player.setSkipping(false);
    }
    /**
     * @param player The player to set.
     */
    public void setPlayer(Player player)
    {
        this.player = player;
    }
}
