package com.softsynth.javasonics.recplay;

import java.io.IOException;
import java.io.OutputStream;

import org.xiph.speex.OggSpeexWriter;
import org.xiph.speex.SpeexEncoder;

import com.softsynth.dsp.ShortBlockAdapter;
import com.softsynth.javasonics.util.Logger;

public class JSpeexEncoder
{
	public static final int DEFAULT_COMPLEXITY = 3;
	public static final int DEFAULT_QUALITY = 2;
    private int mode = 0;
    private int quality = DEFAULT_QUALITY;
    private int complexity = DEFAULT_COMPLEXITY;
    private int bitrate = -1;
    private int samplingRate = 8000;
    private float vbr_quality = 2.0f;
    private boolean vbr = true;
    private boolean vad = false;
    private boolean dtx = false;
    private int channels = 1;

	private byte[]         temp;
	private OutputStream   outputStream;

	private OggSpeexWriter oggWriter;
	private int            samplesPerPacket;
	private SpeexEncoder   speexEncoder;
	private ShortBlockAdapter blockAdapter;
	
    public void setQuality( int quality )
    {
        this.quality = quality;
    }
    public int getQuality()
    {
        return quality;
    }

    public void setQualityVBR( double quality )
    {
        vbr_quality = (float) quality;
    }
    public double getQualityVBR()
    {
        return vbr_quality;
    }

    /** Set true to use variable bitrate compression. */
    public void setVBR( boolean useVBR )
    {
        vbr = useVBR;
    }
    public boolean getVBR()
    {
        return vbr;
    }

    public int getSampleRate()
    {
        return samplingRate;
    }

    /** Set sample rate.
     * @param sampleRate must be 8000, 16000 or 32000
     */
    public void setSampleRate( int sampleRate )
    {
        switch( sampleRate )
        {
            case 8000:
                mode = 0;
                break;
            case 16000:
                mode = 1;
                break;
            case 32000:
                mode = 2;
                break;
            default:
                throw new IllegalArgumentException("Speex SampleRate must be 8000, 16000 or 32000, not " + sampleRate );
        }
        samplingRate = sampleRate;
    }

    public void progressReport(int samplesCompressed, int samplesTotal)
    {
    }
    
	private void setupEncoder()	throws IOException
	{
		temp = new byte[2048];
		
		// construct a new encoder
		speexEncoder = new SpeexEncoder();
		Logger.println("Speex: quality = " + quality + ", complexity = " + complexity +", vbr = " + vbr + ", vbr_quality = " + vbr_quality );
		speexEncoder.init(mode, quality, samplingRate, channels);
		if (complexity >= 0)
		{
			speexEncoder.getEncoder().setComplexity(complexity);
		}
		if (bitrate > 0) {
			speexEncoder.getEncoder().setBitRate(bitrate);
		}
		if (vbr) {
			speexEncoder.getEncoder().setVbr(vbr);
			if (vbr_quality > 0) {
				speexEncoder.getEncoder().setVbrQuality(vbr_quality);
			}
		}
		if (vad) {
			speexEncoder.getEncoder().setVad(vad);
		}
		if (dtx) {
			speexEncoder.getEncoder().setDtx(dtx);
		}
        
		oggWriter = new OggSpeexWriter(outputStream);
		oggWriter.setFormat(mode, samplingRate, channels);

		oggWriter.writeHeader();
		samplesPerPacket = channels * speexEncoder.getFrameSize();
		
		blockAdapter = new ShortBlockAdapter( samplesPerPacket )
		{
			public void processBlock( short[] data, int numSamples )
			{
				try {
					encodeExact( data, 0, numSamples );
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException( e.toString() );
				}
			}
		};
	}

    /**
     * Encodes arbitrarily sized PCM data to speex.
     */
    public void encode(short[] data, int offset, int numSamples)
        throws IOException
    {
		if( speexEncoder == null )
		{
			setupEncoder();
		}
		blockAdapter.write( data, offset, numSamples );
    }
    
	/**
	 * Encodes a PCM data to speex.
	 */
	private void encodeExact(short[] data, int offset, int numSamples)
		throws IOException
	{
        int shortsLeft = numSamples;
        // encode until we finish data
        while (shortsLeft >= samplesPerPacket)
        {
        	speexEncoder.processData(data, offset, samplesPerPacket);
            int encsize = speexEncoder.getProcessedData(temp, 0);
            if (encsize > 0)
            {
                oggWriter.writePacket(temp, 0, encsize);
            }
            offset += samplesPerPacket;
            shortsLeft -= samplesPerPacket;
            progressReport( offset, numSamples );
        }
    }
    
    public void flush()	throws IOException
    {
		if( blockAdapter != null )
		{
			blockAdapter.flush(0);
		}
    }
    
	/**
	 * @throws IOException
	 * 
	 */
	public void close() throws IOException
	{
        if( oggWriter != null )
        {
        	oggWriter.close();
        }
	}
	
	public void setComplexity( int complexity )
	{
		this.complexity = complexity;	
	}

	/**
	 * @return the outputStream containing encoded ogg/speex data.
	 */
	public OutputStream getOutputStream()
	{
		return outputStream;
	}
	/**
	 * @param outputStream The stream to write encoded ogg/speex data to.
	 */
	public void setOutputStream( OutputStream outputStream )
	{
		this.outputStream = outputStream;
	}
}