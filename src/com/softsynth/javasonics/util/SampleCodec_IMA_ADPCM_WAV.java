package com.softsynth.javasonics.util;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Read or Write IMA ADPCM to a WAV formatted stream.
 *
 * @author (C) 2001 Phil Burk, SoftSynth.com, All Rights Reserved
 */

class SampleCodec_IMA_ADPCM_WAV extends SampleDecoderBasic implements SampleEncoder
{
    private byte                block[];
    private short               samples[]; // sample block buffer
    private int                 sampleCursor = 0;  // how many samples are in the sample block buffer
    private ADPCM_IMA_Encoder   encoders[];
    private ADPCM_IMA_Decoder   decoders[];
    private int                 bytesPerBlock = 0;
    private int                 samplesPerFrame;
    private int                 samplesPerBlock;
    private static final int    BYTES_PER_CHUNK = 4;
    private static final int    SAMPLES_PER_CHUNK = BYTES_PER_CHUNK * 2;

    SampleCodec_IMA_ADPCM_WAV( int bytesPerBlock, int samplesPerFrame )
    {
        this.bytesPerBlock = bytesPerBlock;
        this.samplesPerFrame = samplesPerFrame;
        samplesPerBlock = calculateFramesPerBlock() * samplesPerFrame;
        block = new byte[bytesPerBlock];
        samples = new short[samplesPerBlock];
        // TODO review
        // Indicate that there are no samples available in the decoded sample buffer.
        sampleCursor = samplesPerBlock;
    }

/** Calculate size in bytes required to store numSamples assuming this is the entire data chunk. */
    public int calculateSize( int numSamples )
    {
        int numBlocks = (numSamples + samplesPerBlock - 1) / samplesPerBlock;
        return numBlocks * bytesPerBlock;
    }

    public int calculateFramesPerBlock()
    {
        int bytesPerChannel = bytesPerBlock/samplesPerFrame;
        int bytesInRowsPerChannel = bytesPerChannel - 4;
        int samplesInRowsPerChannel = 2 * bytesInRowsPerChannel;
        return samplesInRowsPerChannel + 1;
    }

    private void encodeNextBlock( OutputStream stream ) throws IOException
    {
        if( encoders == null )
        {
            encoders = new ADPCM_IMA_Encoder[samplesPerFrame];
            for( int i=0; i<samplesPerFrame; i++ )
            {
                encoders[i] = new ADPCM_IMA_Encoder();
            }
        }

    /* Write block header.
     * For each channel there are two values:
     *     short initialValue
     *     short initialStepIndex (ranges between 0 and 88)
     */
        int byteIndex = 0;
        int sampleIndex = 0;
        for( int i=0; i<samplesPerFrame; i++ )
        {
            ADPCM_IMA_Encoder encoder = encoders[i];

            encoder.encode( samples[sampleIndex++] );

            int initialValue = encoder.getValue();
            block[byteIndex++] = (byte) initialValue; // low byte
            block[byteIndex++] = (byte) (initialValue >> 8); // high byte

            block[byteIndex++] = (byte) encoder.getStepIndex();
            block[byteIndex++] = 0; // reserved byte
        }

    /* Encode rest of data in 4 byte (8 sample) chunks per channel. */
        int numRows = (bytesPerBlock - byteIndex) / (BYTES_PER_CHUNK * samplesPerFrame);
        for( int ir=0; ir<numRows; ir++ )
        {
            for( int i=0; i<samplesPerFrame; i++ )
            {
                ADPCM_IMA_Encoder encoder = encoders[i];

                encoder.encode( samples, sampleIndex + i, samplesPerFrame,
                    block, byteIndex, 1,
                    SAMPLES_PER_CHUNK
                         );
                byteIndex += BYTES_PER_CHUNK;
            }
            sampleIndex += (SAMPLES_PER_CHUNK * samplesPerFrame);
        }
        sampleCursor = 0;

    /* Write next encoded block to stream. */
        stream.write( block );
    }

/** Assume stream is positioned within the sample data area.
 */
	public void write( OutputStream stream, short[] shorts, int offset, int numShorts ) throws IOException
    {
        int numLeft = numShorts;
        int numCopied = 0;

        while( numLeft > 0 )
        {
            int roomInBuffer = samples.length - sampleCursor;
            if( roomInBuffer > 0 )
            {
                int numToCopy = (roomInBuffer < numLeft) ? roomInBuffer : numLeft;
                System.arraycopy(shorts, offset,
                                 samples, sampleCursor,
                                 numToCopy );
                offset += numToCopy;
                numLeft -= numToCopy;
                numCopied += numToCopy;
                sampleCursor += numToCopy;
            }
            else
            {
                encodeNextBlock( stream );
            }
        }
	}

/** If any samples are stuck in a buffer, pad the buffer with zeroes, and encode last buffer.
 *  Only call this when you are done encoding samples.
 */
	public void finish( OutputStream stream ) throws IOException
    {
        if( sampleCursor > 0 )
        {
            for( ; sampleCursor < samples.length; sampleCursor++ )
            {
                samples[sampleCursor] = 0;
            }
            encodeNextBlock( stream );
        }
	}

    private int decodeNextBlock( InputStream stream ) throws IOException
    {
        if( decoders == null )
        {
            decoders = new ADPCM_IMA_Decoder[samplesPerFrame];
            for( int i=0; i<samplesPerFrame; i++ )
            {
                decoders[i] = new ADPCM_IMA_Decoder();
            }
        }

    // Read next encoded block from stream.
        int numRead = stream.read( block );
        if( numRead == bytesPerBlock )
        {
        // Parse block header.
        // For each channel there are two values:
        //     short initialValue
        //     short initialStepIndex (ranges between 0 and 88)
            int byteIndex = 0;
            int sampleIndex = 0;
            for( int i=0; i<samplesPerFrame; i++ )
            {
                ADPCM_IMA_Decoder decoder = decoders[i];

                int initialValue = (block[byteIndex++] & 0xFF) | (block[byteIndex++] << 8);
                decoder.setValue( initialValue );
                //System.out.println("initialValue = " + initialValue );

                int initialStepIndex = block[byteIndex++] & 0xFF;
                //System.out.println("initialStepIndex = " + initialStepIndex );
                byteIndex++; // skip reserved byte
                decoder.setStepIndex( initialStepIndex );

                samples[sampleIndex++] = (short) initialValue;
            }

        // Decode rest of data in 4 byte (8 sample) chunks per channel.
            int numRows = (bytesPerBlock - byteIndex) / (BYTES_PER_CHUNK * samplesPerFrame);
            //System.out.println("numRows = " + numRows );
            
            for( int ir=0; ir<numRows; ir++ )
            {
                for( int i=0; i<samplesPerFrame; i++ )
                {
                    ADPCM_IMA_Decoder decoder = decoders[i];

                    decoder.decode( block, byteIndex, 1,
                            samples, sampleIndex + i, samplesPerFrame, SAMPLES_PER_CHUNK );
                    byteIndex += BYTES_PER_CHUNK;
                }
                sampleIndex += (SAMPLES_PER_CHUNK * samplesPerFrame);
            }
            sampleCursor = 0;
            return 0;
        }
        else return -1;
    }

/** Assume stream is positioned within the sample data area.
 *  You can use:
 *  <br>
 *  stream.skip( sample.getDataPosition() );
 *  <br>
 *  on a freshly opened stream to seek to the beginning of data.
 */
	public int read( InputStream stream, short[] shorts, int offset, int numShorts ) throws IOException
    {
        int numLeft = numShorts;
        int numCopied = 0;

        while( numLeft > 0 )
        {
            int samplesReady = samples.length - sampleCursor;
            if( samplesReady > 0 )
            {
                int numToCopy = (samplesReady < numLeft) ? samplesReady : numLeft;
                System.arraycopy(samples, sampleCursor,
                                 shorts, offset,
                                 numToCopy );
                offset += numToCopy;
                numLeft -= numToCopy;
                numCopied += numToCopy;
                sampleCursor += numToCopy;
            }
            else
            {
                int result = decodeNextBlock( stream );
                if( result < 0 ) return (numCopied == 0) ? -1 : numCopied;
            }
        }

        return numCopied;
	}
}
