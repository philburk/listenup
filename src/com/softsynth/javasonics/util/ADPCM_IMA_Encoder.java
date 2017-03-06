package com.softsynth.javasonics.util;
/**
 * Low level IMA Intel/DVI ADPCM encoder.
 * The IMA ADPCM algorithm is in the public domain and,
 * unlike MP3, can be used without paying any patent royalties.
 * It also requires very little CPU time.
 *
 * @author Phil Burk (C) 2001 SoftSynth.com
 * @version 0.6
 */

public class ADPCM_IMA_Encoder extends ADPCM_IMA_Decoder
{

    /**
     * Encode an array of packed IMA Intel/DVI ADPCM data from an array of shorts.
     */
    public void encode(
                    short decodedArray[], /* Input array of uncompressed samples. */
                    int   numSamples,
                    byte  encodedArray[]  /* Two samples per byte of encodedArray. */
                    )
    {
        encode( decodedArray, 0, 1, encodedArray, 0, 1, numSamples );
    }

/**
 * Encode an array of shorts to an array of packed IMA Intel/DVI ADPCM data.
 * @param decodedOffset starting short index for decoded data
 * @param decodedStride amount to increment short index when traversing decoded data
 * @param encodedOffset starting byte index for encoded data
 * @param encodedStride amount to increment byte index when traversing encoded data
 * @param numSamples there are two samples per byte of encodedArray
 */
    public void encode(
					short decodedArray[], int decodedOffset, int decodedStride,
                    byte  encodedArray[], int encodedOffset, int encodedStride,
					int   numSamples
					)
    {
        short inputSample;
        byte  encodedNibble;
        int   nibbleIndex = 0;    /* which half of the byte are we using */
        int   i;
        byte  outputByte = 0;
        int   encodedIndex = encodedOffset;
        int   decodedIndex = decodedOffset;

        for( i=0; i<numSamples; i++ )
        {
            inputSample = decodedArray[decodedIndex];
            decodedIndex += decodedStride;

            encodedNibble = encode( inputSample );

            if ( nibbleIndex==0 )
            {
                outputByte = (byte) (0x000F & encodedNibble);   /* Write least significant nibble first! */
                nibbleIndex = 1;
            }
            else
            {
                outputByte |= (byte) (((0x000F & encodedNibble) << 4));
                nibbleIndex = 0;
                encodedArray[encodedIndex] = outputByte;
                encodedIndex += encodedStride;
            }
        }
    }

/**
 * Encode a short as an IMA Intel/DVI ADPCM nibble.
 * Return encoded value.
 */
    public byte encode( int inputSample )
    {
        int  stepSize;
        int  delta;
        byte encodedSample;

        stepSize = stepSizes[ stepIndex ];

    /* calculate delta */
        delta = inputSample - value;
        delta = clip( delta, -32768, 32767 );

    /* encode delta relative to the current stepsize */
        encodedSample = encodeDelta( stepSize, delta );

    /* decode ADPCM code value to reproduce delta and generate an estimated InputSample */
        value += decodeDelta( stepSize, encodedSample);
        value = clip( value, -32768, 32767 );

    /* adapt stepsize */
        stepIndex += indexDeltas[ encodedSample & 7 ];
        stepIndex = clip( stepIndex, 0, 88 );

        return encodedSample;
    }

/**
 * Encode the differential value and output an ADPCM Encoded Sample
 */
    byte encodeDelta( int stepSize, int delta )
    {
        byte encodedSample = 0;

        if ( delta < 0 )
        {
            encodedSample = 8;
            delta = -delta;
        }
        if ( delta >= stepSize )
        {
            encodedSample |= 4;
            delta -= stepSize;
        }
        stepSize = stepSize >> 1;
        if ( delta >= stepSize )
        {
            encodedSample |= 2;
            delta -= stepSize;
        }
        stepSize = stepSize >> 1;
        if ( delta >= stepSize ) encodedSample |= 1;

        return encodedSample;
    }
}
