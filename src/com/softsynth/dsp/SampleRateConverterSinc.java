package com.softsynth.dsp;
import java.awt.*;

/**
 * Highest quality sample rate conversion based on Hamming Windowed Sinc
 *
 * @author (C) 2002 Phil Burk, All Rights Reserved
 */

public class SampleRateConverterSinc
{
    final static int NUM_GUARD_POINTS = 1;

    final static int NUM_ZERO_CROSSINGS = 8;
    final static int NUM_POINTS = 2048;
    final static int POINTS_PER_CROSSING = NUM_POINTS / NUM_ZERO_CROSSINGS;

    static float[] sinc;
    static float[] window;
    static float[] windowedSinc;

    static
    {
        genWindowedSinc();
    }

/* Generate half of a sinc function for the lookup table. */
    static void genSinc( int numCrossings, int numValues )
    {
        sinc = new float[numValues + NUM_GUARD_POINTS]; // add extra guard points for interpolation
        sinc[0] = 1.0f;
        for( int i=1; i<sinc.length; i++ )
        {
            double phase = (i * numCrossings * Math.PI) / numValues;
            sinc[i] = (float) (Math.sin(phase) / phase);
        }
    }

/* Generate general Hamming window */
    static void genWindow( int numValues, float alpha )
    {
        window = new float[numValues + NUM_GUARD_POINTS];
        for( int i=0; i<window.length; i++ )
        {
            double phase = (i * Math.PI) / numValues;
            window[i] = (float) (alpha + ((1.0 - alpha) * Math.cos(phase)));
        }
    }

/* Multiply infinite sinc by window */
    static void genWindowedSinc()
    {
		System.out.flush();
        genSinc( NUM_ZERO_CROSSINGS, NUM_POINTS );
        genWindow( NUM_POINTS, 0.54f );
        windowedSinc = new float[NUM_POINTS + NUM_GUARD_POINTS];
        for( int i=0; i<windowedSinc.length; i++ )
        {
            windowedSinc[i] = window[i] * sinc[i];
        }
		System.out.flush();
    }

/** Calculate windowed sinc value by interpolating in lookup table. */
    static float sinc( double time )
    {
        time = Math.abs( time ); // function is symmetric about Y axis
        double findex = time * POINTS_PER_CROSSING;
        int index = (int) findex;
        double fract = findex - index; // between 0.0 and 1.0
        // get adjacent samples
        float low = windowedSinc[ index ];
        float high = windowedSinc[ index + 1 ];
        // interpolate
        return (float) (low + (fract * (high - low)));
    }

/** Create an output array by resampling the input.
 *  @ratio outputRate / inputRate
 */
    public static float[] convert( float[] input, double ratio )
    {
        // System.out.println("SampleRateConverter.convert() using Sinc");
        int numOutput = (int) (input.length * ratio);
        float[] output = new float[numOutput];
        double inputPhaseIncrement = 1.0 / ratio;
        double phase = 0.0;

        for( int i=0; i<numOutput; i++ )
        {
            int inputIndex = (int) phase;
            float sum = 0.0f;
            for( int j=(1-NUM_ZERO_CROSSINGS); j<=(NUM_ZERO_CROSSINGS-1); j++ )
            {
                int n = inputIndex + j;
                if( (n >= 0) && (n < input.length) )
                {
                    sum += input[n] * sinc( phase - n );
                }
            }

            output[i] = sum;
            phase += inputPhaseIncrement;
        }

        return output;
    }

    static void plotArray( Graphics g, int w, int h, float data[] )
    {
        int midY = h/2;
        int numPoints = data.length;
        float i2x = ((float) w) / numPoints;

        for( int i=0; i<numPoints; i++ )
        {
            int nx = (int) (i * i2x);
            int ny = midY - (int) ((data[i]*0.4) * h);
            g.drawRect( nx, ny, 2, 2 );
       }
    }

/** Rate convert a complex signal and plot result. */
/*
    static void test()
    {
        final double inputRate = 22050.0;
        final double outputRate = 96000.0;

        // generate complex input signal
        final float[] input = new float[50];
        for( int i=0; i<input.length; i++ )
        {
            input[i] = (float) ((0.5 * Math.sin(i * 0.27 )) + (0.5 * Math.sin(i * 0.71 )));
        }

        // perform sample rate conversion
        final float[] output = convert( input, outputRate / inputRate );

        // display results in a custom Canvas
        Canvas canvas = new Canvas()
        {
            public void paint( Graphics g )
            {
                int w = getWidth();
                int h = getHeight();

                g.setColor( Color.green );
                plotArray( g, w, h, input );

                g.setColor( Color.red );
                plotArray( g, w, h, output );
            }
        };

        Frame frame = new Frame("Test SRC");
        frame.setLayout( new BorderLayout() );
        frame.add( "Center", canvas );
        frame.resize( 1000, 700 );
        frame.show();
    }

    public static void main( String args[] )
    {
        SampleRateConverter.test();
    }
*/
}
