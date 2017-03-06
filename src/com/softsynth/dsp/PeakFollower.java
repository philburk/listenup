package com.softsynth.dsp;

/** Detect peak amplitude of a signal.
 *
 * @author Phil Buk (C) 2009 Mobileer Inc
 */
class PeakFollower
{
    private float previousAmplitude;
    private int   holdCounter;
    private int   holdCounterReset;
    private float scaler;
    private float sampleRate;

    public PeakFollower( float sampleRate, float halflife, float holdTime )
    {
        this.sampleRate = sampleRate;
        setHalflife( halflife );
        setHoldTime( holdTime );
    }

    public void reset()
    {
        previousAmplitude = 0.0f;
        holdCounter = 0;
    }

    public void setHoldTime( float holdTime )
    {
        holdCounterReset = (int) (holdTime * sampleRate);
    }
    public void setHalflife( float halflife )
    {
        scaler = (float)Math.pow( 0.5f, 1.0f/(halflife * sampleRate));
    }

    float next( float input )
    {
        float rectifiedInput = (input > 0.0f) ? input : -input; // absolute value
        if( rectifiedInput > previousAmplitude )
        {
            holdCounter = holdCounterReset;
            previousAmplitude = rectifiedInput;
        }
        else
        {
            if( holdCounter <= 0 )
            {
                previousAmplitude *= scaler;
            }
            else
            {
                holdCounter -= 1;
            }
        }
        return previousAmplitude;
    }
}
