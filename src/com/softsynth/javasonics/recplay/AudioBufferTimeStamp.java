/*
 * Created on Oct 8, 2004 
 *
 */
package com.softsynth.javasonics.recplay;

/**
 *   
 * @author Nick Didkovsky, didkovn@mail.rockefeller.edu
 *  
 */
public class AudioBufferTimeStamp {

    private double timeStamp;
    
    private int startFrame;


    public AudioBufferTimeStamp(int startFrame, double timeStamp) {
        this.startFrame = startFrame;
        this.timeStamp=timeStamp;
    }


    /**
     * @return Returns the startFrame.
     */
    public int getStartFrame() {
        return startFrame;
    }

    public double getTimeStamp() {
        return timeStamp;
    }

}