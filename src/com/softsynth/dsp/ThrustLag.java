package com.softsynth.dsp;
import java.awt.*;
/**
 * Lag based on limited acceleration model.
 * Imagine a rocket that can move in one dimension.
 * It can fire from 0 to max in either direction.
 *
 * @author (C) 2002 Phil Burk, All Rights Reserved
 */

public class ThrustLag
{
    private float value;
	private float velocity; // first derivative
	private float maxUpAcceleration; // second derivative
	private float maxDownAcceleration; // second derivative

    public void reset()
    {
        value = (float) 0.0;
        velocity = 0;
    }

    public void setAcceleration( float distance, int numIterations  )
    {
        setUpAcceleration( distance, numIterations );
        setDownAcceleration( distance, numIterations );
    }

    private float timeToAcceleration( float distance, int numIterations )
    {
        return (2.0f * distance / (numIterations * numIterations));
    }
/** Set maximum acceleration sufficient to move the given distance
 *  from a standing start and end up stopped at the new location,
 *  by calling next() a given number of iterations.
 */
    public void setUpAcceleration( float distance, int numIterations )
    {
        maxUpAcceleration = timeToAcceleration( distance, numIterations );
    }
    public void setDownAcceleration( float distance, int numIterations )
    {
        maxDownAcceleration = timeToAcceleration( distance, numIterations );
    }

    public float getMaxAcceleration( float delta )
    {
        if( delta >= 0 ) return maxUpAcceleration; // signal above lag
        else return maxDownAcceleration; // signal below lag
    }

   float nextAcceleration( float input )
    {
        float idealVelocity;
        float delta = input - value;
        float maxAcceleration = getMaxAcceleration( delta );
        if( delta < 0.0 )
        {
            idealVelocity = (float)-Math.sqrt( -delta * maxAcceleration );
        }
        else
        {
            idealVelocity = (float)Math.sqrt( delta * maxAcceleration );
        }
        // acceleration is the difference in velocities
        float acceleration = idealVelocity - velocity;

        // Clip acceleration to max.
        if( acceleration > maxAcceleration ) acceleration = maxAcceleration;
        else if( acceleration < -maxAcceleration ) acceleration = -maxAcceleration;

        return acceleration;
    }

/** Try to force output to match input without exceeding maximum acceleration. */
    float next( float input )
    {
        float acceleration = nextAcceleration( input );
        velocity += acceleration;
        value += velocity;
        // System.out.println("acceleration = " + acceleration + ", velocity = " + velocity );
        return value;
    }

    public static void main( String args[] )
    {
        Frame frame = new Frame("Test Thrust Lag");
        frame.setLayout( new BorderLayout() );
        Canvas canvas = new Canvas()
        {
            public void paint( Graphics g )
            {
                int w = bounds().width;
                int h = bounds().height;
                int x = 0;
                int y = h;
                ThrustLag lag = new ThrustLag();
                lag.setUpAcceleration( 40.0f, 20 );
                lag.setDownAcceleration( 100.0f, 20 );
                float target = 1234.567f;
                int numVals = w;
                for( int i=0; i<numVals; i++ )
                {
                    //System.out.println("=========" );
                    if( i == 150 ) target = 345.6f;
                    else if( i == 300 ) target = 1679.2f;
                    else if( i == 600 ) target = 234.5f;
                    else if( i == 640 ) target = 1234.5f;
                    float val = lag.next( target );

                    int nx = i;
                    int ty = h - (int) ((target * h) / 2000.0f );
                    g.setColor( Color.green );
                    g.drawLine( x, ty, nx, ty );
                    g.setColor( Color.red );
                    int ny = h - (int) ((val * h) / 2000.0f );
                    g.drawLine( x, y, nx, ny );
                    x = nx;
                    y = ny;
                    //System.out.println("[" + i + "] target = " + target + ", value = " + val );
                }
                //System.out.println("paint() finished.");
            }
        };
        frame.add( "Center", canvas );
        frame.resize( 800, 500 );
        frame.show();
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
                /**
                 * Invoked when a window is in the process of being closed.
                 * The close operation can be overridden at this point.
                 */
                public void windowClosing(java.awt.event.WindowEvent e) {
                    System.exit(0);
                }
                });
    }

}
