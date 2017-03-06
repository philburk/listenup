package com.softsynth.javasonics.util;

/**
 * Low level IMA Intel/DVI ADPCM decoder.
 * The IMA ADPCM algorithm is in the public domain and,
 * unlike MP3, can be used without paying any patent royalties.
 * It also requires very little CPU time.
 *
 * @author Phil Burk (C) 2001 SoftSynth.com
 * @version 0.6
 */

public class ADPCM_IMA_Decoder
{
/*  DVI ADPCM step table */
    final static int stepSizes[] = {
        7,     8,     9,    10,    11,    12,    13,    14,    16,     17,    19,
       21,    23,    25,    28,    31,    34,    37,    41,    45,     50,    55,
       60,    66,    73,    80,    88,    97,   107,   118,   130,    143,   157,
      173,   190,   209,   230,   253,   279,   307,   337,   371,    408,   449,
      494,   544,   598,   658,   724,   796,   876,   963,  1060,   1166,  1282,
     1411,  1552,  1707,  1878,  2066,  2272,  2499,  2749,  3024,   3327,  3660,
     4026,  4428,  4871,  5358,  5894,  6484,  7132,  7845,  8630,   9493, 10442,
    11487, 12635, 13899, 15289, 16818, 18500, 20350, 22385, 24623,  27086, 29794,
    32767  };

/* DVI table of stepsize index deltas */
    final static int indexDeltas[]  = { -1, -1, -1, -1, 2, 4, 6, 8 };

/* State of decoder. */
    int value = 0;
    int stepIndex = 0;

/** Set initial value. ADPCM uses deltas to generate new values from previous values.
 *  Default is zero. This method is used when decoding ADPCM from a WAV file because
 *  each block in a WAV file resets the current value and stepIndex.
 */
    public void setValue( int value )
    {
        this.value = value;
    }
    public int getValue()
    {
        return value;
    }

/** Set current stepIndex. This method is used when decoding ADPCM from a WAV file because
 *  each block in a WAV file resets the currentValue and stepIndex.
 */
    public void setStepIndex( int stepIndex )
    {
        this.stepIndex = clip( stepIndex, 0, 88 );
    }
    public int getStepIndex()
    {
        return stepIndex;
    }

/**
 * Decode an array of packed IMA Intel/DVI ADPCM data into an array of shorts.
 */
    public void decode( byte encodedArray[],
					short decodedArray[],  /* Output array must be large enough for numSamples. */
					int   numSamples     /* Two samples per byte of encodedArray. */
					)
    {
        decode( encodedArray, 0, 1, decodedArray, 0, 1, numSamples );
    }

/**
 * Decode an array of packed IMA Intel/DVI ADPCM data into an array of shorts.
 * @param encodedOffset starting byte index for encoded data
 * @param encodedStride amount to increment byte index when traversing encoded data
 * @param decodedOffset starting short index for decoded data
 * @param decodedStride amount to increment short index when traversing decoded data
 * @param numSamples there are two samples per byte of encodedArray
 */
    public void decode( byte encodedArray[], int encodedOffset, int encodedStride,
					short decodedArray[], int decodedOffset, int decodedStride,
					int   numSamples
					)
    {
        byte  encodedSample;
        int   nibbleIndex = 0;    /* which half of the byte are we using */
        int   i;
        byte  inputByte = 0;
        int  encodedIndex = encodedOffset;
        int  decodedIndex = decodedOffset;

        for( i=0; i<numSamples; i++ )
        {
            if ( nibbleIndex==0 )
            {
                inputByte = encodedArray[encodedIndex];
                encodedIndex += encodedStride;
                encodedSample = (byte) (0x000F & inputByte);   /* Use least significant byte first! */
                nibbleIndex = 1;
            }
            else
            {
                encodedSample = (byte) (0x000F & (inputByte>>4));
                nibbleIndex = 0;
            }

            value = decode( encodedSample );
            decodedArray[decodedIndex] = (short) value;
            decodedIndex += decodedStride;
        }
    }

    int clip( int n, int min, int max )
    {
        if( n < min ) n = min;
        else if( n > max ) n = max;
        return n;
    }

/**
 * Decode an IMA Intel/DVI ADPCM nibble.
 * Return decoded sample.
 */
    public int decode( byte encodedSample )
    {
        int stepSize = stepSizes[stepIndex];

    /* decode ADPCM code value to reproduce Dn and accumulates an estimated outputSample */
        int output = value + decodeDelta( stepSize, encodedSample);
        output = clip( output, -32768, 32767 );

    /* stepsize adaptation */
        stepIndex += indexDeltas[ encodedSample & 7 ];
        stepIndex = clip( stepIndex, 0, 88 );

        return output;
    }

/**
 * Calculate the delta from an ADPCM Encoded Sample
 */
    protected int decodeDelta( int stepSize, byte encodedSample )
    {
        int delta = 0;

        if( (encodedSample & 4) != 0 ) delta = stepSize;
        stepSize = stepSize >> 1;
        if( (encodedSample & 2) != 0 ) delta += stepSize;
        stepSize = stepSize >> 1;
        if( (encodedSample & 1) != 0 ) delta += stepSize;
        stepSize = stepSize >> 1;
        delta += stepSize;

        if ( (encodedSample & 8) != 0 ) delta = -delta;

        return( delta );
    }
}
